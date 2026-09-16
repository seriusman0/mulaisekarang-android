# Chat System Improvement — Pembaruan untuk Android

**Tanggal deploy:** 2026-09-16
**Commit:** 06d13af
**Base URL:** `https://mulaisekarang.com/api/v1`
**Auth:** Bearer Token (Sanctum)

---

## Ringkasan Perubahan

9 fitur baru diimplementasikan. Berikut yang **wajib diterapkan di Android:**

---

## 1. 🔔 Sound Notification

### Status: SUDAH JALAN DI WEB — perlu implementasi di Android

Di Android, gunakan `RingtoneManager` atau FCM push notification saat ada pesan baru masuk di channel WebSocket.

```kotlin
// Di ChatRepository / Pusher listener
channel.bind("message.sent") { event ->
    val data = JSONObject(event.data)
    val senderId = data.getInt("sender_id")

    if (senderId != currentUserId) {
        // Play sound
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri).play()

        // System notification
        showChatNotification(
            title = data.getJSONObject("sender").getString("display_name"),
            body = data.optString("body", "Pesan baru"),
            conversationId = data.getInt("conversation_id")
        )
    }
}
```

**Channel WebSocket yang harus di-subscribe:**
- `private-chat.user.{userId}` — untuk notifikasi semua pesan baru (termasuk dari grup)
- `private-conversation.{conversationId}` — untuk real-time update saat chat room terbuka

---

## 2. 🔧 FIX KRITIS: Broadcast Grup ke Semua Member

### Status: SUDAH DIPERBAIKI DI BACKEND — Android tidak perlu ubah

**Sebelum:** `MessageSent` event hanya broadcast ke 1 recipient (`int recipientId`).
**Sesudah:** Broadcast ke **semua participant** grup via channel `private-chat.user.{id}` untuk setiap member.

**Artinya:** Subscribe ke `private-chat.user.{myUserId}` sudah cukup untuk menerima notifikasi dari semua pesan, termasuk grup. Tidak perlu subscribe ke semua channel `private-conversation.{id}` saat idle.

---

## 3. 👥 Group Detail — Endpoint Baru

### Status: ENDPOINT BARU TERSEDIA

#### 3a. List Participants Grup

Gunakan endpoint conversations yang sudah ada — response sudah menyertakan `participants`:

```
GET /api/v1/chat/conversations
```

Response sudah include `participants` array di setiap conversation bertipe `group`:
```json
{
  "data": [
    {
      "id": 15,
      "type": "group",
      "title": "Nama Grup",
      "participants": [
        {
          "user_id": 1,
          "role": "owner",
          "user": { "id": 1, "display_name": "Budi", "avatar_url": "..." }
        },
        {
          "user_id": 2,
          "role": "member",
          "user": { "id": 2, "display_name": "Ani", "avatar_url": "..." }
        }
      ]
    }
  ]
}
```

#### 3b. UI Group Info Screen yang Disarankan

```
[Chat Room - Toolbar]
  ↓ klik nama grup
[Group Info Screen]
  - Foto / icon grup
  - Nama grup
  - Jumlah anggota
  - List anggota (nama + role owner/member)
  - [HANYA OWNER] Tombol "Tambah Anggota"
  - [HANYA OWNER] Section "Link Undangan"
  - Tombol "Keluar Grup"
```

---

## 4. ➕ Invite Member ke Grup

### Status: ENDPOINT BARU

#### Tambah Member (hanya owner)

```
POST /api/v1/groups/{conversation_id}/participants
Authorization: Bearer {token}
Content-Type: application/json

{
  "username": "username_target"
}
```

Response 200:
```json
{
  "data": {
    "id": 15,
    "type": "group",
    "title": "Nama Grup",
    "participants": [...]
  }
}
```

Error:
| Status | Kondisi |
|--------|---------|
| 403 | Bukan owner, atau target bukan eligible contact |
| 422 | User sudah ada di grup |

#### Kick Member (hanya owner, atau self-leave)

```
DELETE /api/v1/groups/{conversation_id}/participants/{user_id}
Authorization: Bearer {token}
```

Response 200:
```json
{ "message": "Berhasil dikeluarkan dari grup." }
```

---

## 5. 🔗 Invite Link

### Status: ENDPOINT BARU + WEB ROUTE BARU

#### Generate/Reset Invite Link (hanya owner)

```
POST /api/v1/groups/{conversation_id}/invite
Authorization: Bearer {token}
```

Response 200:
```json
{
  "invite_url": "https://mulaisekarang.com/chat/join/abcdef1234567890",
  "expires_at": "2026-09-23T09:51:00.000000Z"
}
```

Link expire setelah **7 hari**. Panggil ulang endpoint ini untuk reset link.

#### Join via Token

```
POST /api/v1/groups/join
Authorization: Bearer {token}
Content-Type: application/json

{ "token": "abcdef1234567890" }
```

Response 200:
```json
{
  "data": { "conversation_id": 15 }
}
```

Setelah join, buka chat room conversation_id tersebut.

Error:
| Status | Kondisi |
|--------|---------|
| 404 | Token tidak valid |
| 410 | Token sudah expired |

#### Android Deep Link

Daftarkan intent-filter untuk URL `https://mulaisekarang.com/chat/join/{token}`:

```xml
<!-- AndroidManifest.xml -->
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <data
        android:scheme="https"
        android:host="mulaisekarang.com"
        android:pathPrefix="/chat/join/"/>
</intent-filter>
```

```kotlin
// Di Activity/Fragment yang menangani deep link:
override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.data?.let { uri ->
        val token = uri.lastPathSegment ?: return
        viewModel.joinGroupViaToken(token)
    }
}

// Di ViewModel:
suspend fun joinGroupViaToken(token: String) {
    val result = api.joinGroup(token) // POST /api/v1/groups/join
    if (result.isSuccess) {
        navigateToChatRoom(result.data.conversationId)
    }
}
```

---

## 6. ↩️ Reply to Message

### Status: API DIUPDATE — Android perlu update request body

#### Kirim Pesan dengan Reply

Tambahkan field `reply_to_id` opsional saat kirim pesan:

```
POST /api/v1/chat/conversations/{conversation_id}/messages
Authorization: Bearer {token}
Content-Type: application/json

{
  "body": "Iya betul!",
  "reply_to_id": 42
}
```

#### Response — Sekarang Menyertakan `reply_to`

```json
{
  "data": {
    "id": 100,
    "body": "Iya betul!",
    "sender_id": 5,
    "reply_to_id": 42,
    "reply_to": {
      "id": 42,
      "body": "Apakah fitur ini sudah jalan?",
      "sender": {
        "id": 3,
        "display_name": "Budi",
        "avatar_url": "https://..."
      }
    },
    "created_at": "2026-09-16T10:00:00.000000Z"
  }
}
```

#### UI yang Disarankan

- Long-press bubble pesan → context menu → "Balas"
- Tampilkan quoted box di atas bubble pesan yang merupakan reply
- Swipe kanan pada pesan (WhatsApp style) untuk trigger reply

```kotlin
data class Message(
    val id: Int,
    val body: String?,
    val senderId: Int,
    val replyToId: Int?,
    val replyTo: ReplyMessage?,  // tambahkan field ini
    val createdAt: String,
)

data class ReplyMessage(
    val id: Int,
    val body: String?,
    val sender: Sender,
)
```

---

## 7. 💬 Typing Indicator

### Status: SUDAH JALAN DI WEB via Echo Whisper — perlu implementasi Android

Gunakan **Pusher client events** (prefix `client-`). Pastikan App di Pusher/Reverb mengaktifkan `client_events: true`.

#### Kirim event typing saat user mengetik

```kotlin
fun sendTypingEvent(conversationId: Int, userName: String) {
    val channel = pusher.getPrivateChannel("private-conversation.$conversationId") ?: return
    val data = JSONObject().apply {
        put("user", userName)
        put("user_id", currentUserId)
    }
    channel.trigger("client-typing", data.toString())
}
```

Panggil `sendTypingEvent()` setiap kali teks input berubah, tapi **debounce** minimal 1 detik agar tidak spam.

#### Listen event typing

```kotlin
channel.bind("client-typing") { event ->
    val data = JSONObject(event.data)
    val userId = data.getInt("user_id")
    if (userId != currentUserId) {
        val userName = data.getString("user")
        showTypingIndicator("$userName sedang mengetik...")
        handler.postDelayed({ hideTypingIndicator() }, 2000)
    }
}
```

---

## 8. ✓✓ Read Receipt

### Status: SUDAH JALAN DI WEB — Android bisa pakai logika yang sama

Web menghitung berapa participant yang `last_read_at >= message.created_at`.

Untuk Android, pendekatan sederhana:
- ✓ = pesan sudah terkirim (response 201 dari API)
- ✓✓ = ada participant lain yang sudah membuka conversation (cek `last_message_at` vs timestamp lokal)

Alternatif yang lebih akurat: tambahkan field `read_count` di response MessageResource (sudah ada di backend via `readByCount()`).

Cek di MessageResource:
```
GET /api/v1/chat/conversations/{id}/messages
```
Response message sekarang include `read_count` field (lihat MessageResource.php).

---

## 9. 📋 Student Lihat Grup di Daftar Chat

### Status: SUDAH DIPERBAIKI DI BACKEND

`GET /api/v1/chat/conversations` sekarang mengembalikan **semua jenis conversation** untuk student, bukan hanya daftar instructor.

**Sebelum:** Endpoint mengembalikan list User (instructor) untuk student.
**Sesudah:** Endpoint mengembalikan list Conversation dengan type: `group`, `direct`, `mentor`, `support`.

Update model Android:
```kotlin
data class ConversationListItem(
    val id: Int,
    val type: String,        // "group" | "direct" | "mentor" | "support"
    val title: String?,      // Nama grup (null untuk DM)
    val lastMessageAt: String?,
    val unreadCount: Int,
    val otherParty: OtherParty?,    // Untuk DM/mentor
    val participants: List<Participant>?,  // Untuk grup
)
```

Di UI, pisahkan section:
- **Grup** — tampilkan `title` grup + jumlah anggota
- **Pesan Langsung** — tampilkan nama `otherParty`

---

## Tabel Endpoint Summary

| Endpoint | Method | Status | Keterangan |
|----------|--------|--------|------------|
| `/chat/conversations` | GET | ✅ Update | Sekarang include semua tipe konv untuk student |
| `/chat/conversations/{id}/messages` | POST | ✅ Update | Tambahkan `reply_to_id` opsional |
| `/chat/conversations/{id}/messages` | GET | ✅ Update | Response include `reply_to` object |
| `/groups/{id}/participants` | POST | 🆕 Baru | Invite member (hanya owner) |
| `/groups/{id}/participants/{userId}` | DELETE | 🆕 Baru | Kick member / keluar grup |
| `/groups/{id}/invite` | POST | 🆕 Baru | Generate invite link (hanya owner) |
| `/groups/join` | POST | 🆕 Baru | Join grup via invite token |
| `/chat/support` | POST | ✅ Existing | Tidak berubah |

---

## WebSocket Channels

| Channel | Event | Kapan Digunakan |
|---------|-------|-----------------|
| `private-chat.user.{myId}` | `message.sent` | Subscribe saat login — untuk semua notifikasi (termasuk grup) |
| `private-conversation.{id}` | `message.sent` | Subscribe saat buka chat room |
| `private-conversation.{id}` | `client-typing` | Kirim/terima typing indicator (perlu client events aktif) |

---

## Prioritas Implementasi Android

| # | Fitur | Impact | Urgency |
|---|-------|--------|---------|
| 1 | Fix broadcast grup (sudah di backend, Android subscribe ke `chat.user.{id}`) | Kritis | Segera test |
| 2 | Sound notification saat pesan masuk | Tinggi | Segera |
| 3 | Update model `Message` untuk field `reply_to` | Sedang | Sprint ini |
| 4 | Group Detail Screen + invite member | Sedang | Sprint ini |
| 5 | Deep link join via invite link | Sedang | Sprint ini |
| 6 | Typing indicator via Pusher client events | Rendah | Sprint berikutnya |
| 7 | Read receipt visual ✓✓ | Rendah | Sprint berikutnya |