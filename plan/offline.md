# Product Requirements Document (PRD)
## Fitur Offline Video Course & Sinkronisasi Progres

**Status:** Planned — Ready for Implementation
**Tanggal:** 9 September 2026
**Target Platform:** Mobile App (Android/Flutter) & Backend API (Laravel)

---

## 1. Ringkasan Eksekutif
Dokumen ini mendefinisikan arsitektur dan spesifikasi kebutuhan untuk fitur **Offline Mode** pada aplikasi mobile *course*. Fitur ini memungkinkan pengguna untuk mengunduh video materi secara aman ke dalam perangkat mereka, menontonnya tanpa koneksi internet, dan menyinkronkan progres belajar (durasi tontonan/status penyelesaian) secara otomatis ke server saat koneksi internet kembali tersedia.

## 2. Tujuan & Sasaran
*   **User Experience (UX):** Pengguna dapat melanjutkan *course* tanpa hambatan saat sedang bepergian (misal: di kereta atau pesawat) tanpa *buffering* atau *network error*.
*   **Data Integrity:** Progres belajar lintas platform (Web dan Mobile) harus tetap sinkron tanpa ada data yang tertimpa secara keliru (*No Data Loss*).
*   **Content Protection:** Melindungi aset video premium agar tidak dapat diakses di luar aplikasi atau disebarluaskan.
*   **Zero-Disruption (Non-Destructive):** Rilis fitur ini mutlak **tidak boleh** mengganggu fungsionalitas pengguna web yang sudah berjalan saat ini.

---

## 3. Prinsip *Non-Destructive* (Menjaga Sistem yang Berjalan)
Untuk memastikan sistem web yang ada tidak terpengaruh, implementasi harus mengikuti aturan *backward compatibility* berikut:

1.  **Additive API Endpoints:** Jangan memodifikasi *endpoint* REST API atau GraphQL yang saat ini digunakan oleh aplikasi Web. Buat versi atau *route* baru khusus mobile di prefix `POST /api/v2/mobile/` dan `GET /api/v2/mobile/`.
2.  **Additive Database Migrations:** Skema database *backend* yang sudah ada tidak boleh diubah secara destruktif. Tambahkan saja kolom baru: `last_synced_at` (nullable timestamp) ke tabel `lesson_progress`, dan buat tabel baru `offline_download_tokens` untuk tracking signed URLs.
3.  **Isolasi Deployment:** API sinkronisasi v2 berjalan di container/route group yang sama, namun prefix berbeda — kegagalan tidak merobohkan layanan web utama.

---

## 4. Spesifikasi Fungsional

### A. Fitur Aplikasi Mobile (Android/Flutter)
1.  **Manajemen Unduhan:**
    *   Tombol "Unduh Modul" atau "Unduh Lesson" pada UI detail course.
    *   Pengunduhan berjalan di latar belakang (*WorkManager* untuk Android). Mendukung *resume* jika internet terputus.
    *   Pengecekan sisa penyimpanan; hentikan dan beri peringatan jika memori hampir penuh (< 500MB).
2.  **Proteksi DRM Lokal:**
    *   File video disimpan di *Internal Storage* yang terisolasi (`getFilesDir()`).
    *   Nama file di-obfuscate, misal: `vid_<sha256_8char>.dat` bukan `video1.mp4`.
3.  **Pemutaran Offline:**
    *   Jika status perangkat *offline* (atau API merespon timeout), aplikasi otomatis merender UI dari *Local Database* (Drift/Isar).
    *   *Player* video (*media_kit* atau *video_player*) membaca file lokal, bukan melakukan *streaming*.
    *   Progress detik terakhir dicatat setiap 5 detik ke tabel `offline_sync_queue` lokal.
4.  **Pencatatan Progres Lokal:**
    *   Setiap perubahan progres menonton dicatat ke tabel antrean lokal (`offline_sync_queue`) lengkap dengan *timestamp* aktual.
    *   Status *completed* dicatat saat user menekan "Selesai" atau durasi video habis.

### B. Fitur Backend API (Laravel — Additive)
1.  **Signed URL Generator (`GET /api/v2/mobile/lessons/{lesson}/download-url`):**
    *   Endpoint untuk meminta akses unduh. Server memvalidasi enrollment + auth, lalu merespon dengan Signed URL yang hanya valid 1 jam.
    *   Signed URL menggunakan HMAC-SHA256 dengan `APP_KEY` + expiry timestamp.
    *   Endpoint terpisah menyajikan file stream yang divalidasi via signed token.
2.  **Offline Manifest (`GET /api/v2/mobile/courses/{course}/offline-manifest`):**
    *   Mengembalikan metadata lengkap sebuah course (struktur topic + lesson, thumbnail URL) dalam satu request — cukup untuk render UI offline.
3.  **Sinkronisasi Batch & Last Write Wins (LWW) (`POST /api/v2/mobile/sync-progress`):**
    *   Menerima array of JSON dari antrean lokal klien.
    *   Logika LWW: Bandingkan `recorded_at` payload dengan `updated_at` di `lesson_progress`. Jika klien lebih baru, timpa; jika tidak, abaikan.
    *   Mengembalikan daftar `synced_ids` yang berhasil diproses → app mobile menghapus baris ini dari tabel lokal.

---

## 5. Arsitektur Data & API Contract

### Skema Database Lokal (SQLite/Drift pada APK)

**Tabel: `offline_sync_queue`**
| Kolom | Tipe | Keterangan |
|---|---|---|
| `sync_id` | String (UUID) | Primary Key |
| `course_id` | Integer | FK ke course |
| `lesson_id` | Integer | FK ke lesson |
| `progress_seconds` | Integer | Detik terakhir ditonton |
| `is_completed` | Boolean | Apakah lesson selesai |
| `recorded_at` | Timestamp | Waktu aksi terjadi (UTC) |
| `status` | String | Enum: `pending`, `syncing` |

**Tabel: `downloaded_lessons`**
| Kolom | Tipe | Keterangan |
|---|---|---|
| `lesson_id` | Integer | Primary Key |
| `course_id` | Integer | FK ke course |
| `local_path` | String | Path file di internal storage |
| `file_size_bytes` | Integer | Ukuran file |
| `downloaded_at` | Timestamp | Waktu selesai download |
| `expires_at` | Timestamp | Waktu akses offline expired (opsional, 30 hari) |

### Skema Database Backend (Additive Migration)

**Tabel yang dimodifikasi: `lesson_progress`**
- Tambah kolom: `last_synced_at` TIMESTAMP NULL (waktu terakhir sync dari mobile)

**Tabel baru: `offline_download_tokens`**
| Kolom | Tipe | Keterangan |
|---|---|---|
| `id` | bigint PK | |
| `user_id` | bigint FK | Pemilik token |
| `lesson_id` | bigint FK | Lesson yang diizinkan diunduh |
| `token` | string (64) | HMAC token, unique |
| `expires_at` | timestamp | Waktu expired (1 jam dari request) |
| `used_at` | timestamp null | Waktu token dipakai (untuk invalidasi) |
| `created_at` | timestamp | |

### API Endpoint: Offline Manifest
**GET** `/api/v2/mobile/courses/{course}/offline-manifest`
**Headers:** `Authorization: Bearer {token}`
**Response (200 OK):**
```json
{
  "course": {
    "id": 1,
    "title": "Dasar-Dasar Pemrograman Web",
    "cover_image_url": "https://...",
    "topics": [
      {
        "id": 1,
        "title": "Pengenalan HTML",
        "order": 1,
        "lessons": [
          {
            "id": 96,
            "title": "Video Pembuka",
            "order": 1,
            "duration_minutes": 10,
            "duration_seconds": 0,
            "is_completed": false
          }
        ]
      }
    ]
  },
  "generated_at": "2026-09-09T19:00:00Z"
}
```

### API Endpoint: Request Signed Download URL
**GET** `/api/v2/mobile/lessons/{lesson}/download-url`
**Headers:** `Authorization: Bearer {token}`
**Response (200 OK):**
```json
{
  "download_url": "https://mulaisekarang.com/api/v2/mobile/lessons/96/file?token=abc123&expires=1757443200",
  "expires_at": "2026-09-09T20:00:00Z",
  "file_size_bytes": 52428800
}
```

### API Endpoint: Serve File (via Signed Token)
**GET** `/api/v2/mobile/lessons/{lesson}/file?token={token}`
*(No auth header diperlukan — token menggantikan auth)*
Mengembalikan video file sebagai stream (HTTP 206 partial content, sama seperti `StreamsVideoFile` trait yang sudah ada).

### API Endpoint: Sinkronisasi Progres
**POST** `/api/v2/mobile/sync-progress`
**Headers:** `Authorization: Bearer {token}`, `Content-Type: application/json`
**Request Payload:**
```json
{
  "device_id": "abc-123",
  "sync_data": [
    {
      "sync_id": "uuid-1",
      "lesson_id": 96,
      "course_id": 1,
      "progress_seconds": 450,
      "is_completed": true,
      "recorded_at": "2026-09-09T19:30:00Z"
    }
  ]
}
```
**Response (200 OK):**
```json
{
  "status": "success",
  "synced_ids": ["uuid-1"],
  "rejected_ids": []
}
```
*(App mobile menghapus baris `synced_ids` dari tabel lokal setelah menerima respons ini.)*

---

## 6. Metrik Keberhasilan
1.  Tingkat keberhasilan *playback* saat *airplane mode* aktif (Target: 100%).
2.  Akurasi data sinkronisasi setelah perangkat yang *offline* 24 jam kembali terhubung (Target: Tidak ada konflik *progress* web vs mobile).
3.  Tidak ada lonjakan *error rate* (5xx) pada layanan Web yang sedang berjalan pasca *deployment* backend API sinkronisasi.

---

## 7. Implementation Plan (Step-by-Step)

> **Untuk Hermes:** Gunakan skill `subagent-driven-development` untuk mengimplementasi plan ini task-by-task.

**Goal:** Tambahkan fitur offline mode ke APK Android tanpa merusak API web yang sudah berjalan.

**Architecture:** Additive API v2 di Laravel + Signed URL untuk download + Sync queue LWW di backend. Di sisi Android: WorkManager download, Drift local DB, media_kit player.

**Tech Stack:** Laravel (PHP), Flutter/Dart, Drift (SQLite), WorkManager, media_kit/video_player

---

### Task 1: Backend — Additive Migration (`last_synced_at` + `offline_download_tokens`)

**Objective:** Tambah kolom dan tabel baru di database tanpa menyentuh schema existing.

**Files:**
- Create: `database/migrations/2026_09_09_100001_add_last_synced_at_to_lesson_progress.php`
- Create: `database/migrations/2026_09_09_100002_create_offline_download_tokens_table.php`

**Step 1: Buat migration `last_synced_at`**
```php
// database/migrations/2026_09_09_100001_add_last_synced_at_to_lesson_progress.php
public function up(): void {
    Schema::table('lesson_progress', function (Blueprint $table) {
        $table->timestamp('last_synced_at')->nullable()->after('completed_at');
    });
}
public function down(): void {
    Schema::table('lesson_progress', function (Blueprint $table) {
        $table->dropColumn('last_synced_at');
    });
}
```

**Step 2: Buat migration `offline_download_tokens`**
```php
// database/migrations/2026_09_09_100002_create_offline_download_tokens_table.php
public function up(): void {
    Schema::create('offline_download_tokens', function (Blueprint $table) {
        $table->id();
        $table->foreignId('user_id')->constrained()->cascadeOnDelete();
        $table->foreignId('lesson_id')->constrained()->cascadeOnDelete();
        $table->string('token', 64)->unique();
        $table->timestamp('expires_at');
        $table->timestamp('used_at')->nullable();
        $table->timestamp('created_at')->useCurrent();
        $table->index(['token', 'expires_at']);
    });
}
```

**Step 3: Jalankan dan verifikasi**
```bash
cd /home/ykde/mulaisekarang
php artisan migrate --pretend  # cek dulu tanpa execute
php artisan migrate
php artisan tinker --execute="Schema::hasColumn('lesson_progress','last_synced_at')"
```
Expected: `true`

**Step 4: Commit**
```bash
git add database/migrations/
git commit -m "feat(offline): additive migrations — last_synced_at + offline_download_tokens"
```

---

### Task 2: Backend — Model `OfflineDownloadToken`

**Objective:** Buat Eloquent model untuk tabel baru.

**Files:**
- Create: `app/Models/OfflineDownloadToken.php`

**Step 1: Buat model**
```php
<?php
namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\Relations\BelongsTo;

class OfflineDownloadToken extends Model
{
    public $timestamps = false;
    protected $fillable = ['user_id', 'lesson_id', 'token', 'expires_at', 'used_at'];
    protected $casts = [
        'expires_at' => 'datetime',
        'used_at'    => 'datetime',
        'created_at' => 'datetime',
    ];

    public function user(): BelongsTo { return $this->belongsTo(User::class); }
    public function lesson(): BelongsTo { return $this->belongsTo(Lesson::class); }

    public function isExpired(): bool { return now()->isAfter($this->expires_at); }
    public function isUsed(): bool { return $this->used_at !== null; }
    public function isValid(): bool { return !$this->isExpired() && !$this->isUsed(); }
}
```

**Step 2: Update `LessonProgress` fillable**
```php
// app/Models/LessonProgress.php — tambah ke $fillable:
'last_synced_at',
```

**Step 3: Commit**
```bash
git add app/Models/
git commit -m "feat(offline): add OfflineDownloadToken model + update LessonProgress fillable"
```

---

### Task 3: Backend — Route Group v2 Mobile

**Objective:** Daftarkan prefix `/api/v2/mobile` tanpa menyentuh route v1.

**Files:**
- Modify: `routes/api.php` — tambah di bawah closing `Route::prefix('v1')` group

**Step 1: Tambah route group v2 di bawah block v1 yang sudah ada**
```php
// routes/api.php — APPEND di bawah, setelah blok v1 closing
use App\Http\Controllers\Api\V2\Mobile\OfflineManifestController;
use App\Http\Controllers\Api\V2\Mobile\OfflineDownloadController;
use App\Http\Controllers\Api\V2\Mobile\SyncProgressController;

Route::prefix('v2/mobile')->name('api.v2.mobile.')->group(function () {
    // Token-validated file serve — no Bearer required (token IS the auth)
    Route::get('/lessons/{lesson}/file', [OfflineDownloadController::class, 'serveFile'])
        ->name('lessons.file');

    Route::middleware('auth:sanctum')->group(function () {
        Route::get('/courses/{course}/offline-manifest', [OfflineManifestController::class, 'show'])
            ->name('courses.manifest');
        Route::get('/lessons/{lesson}/download-url', [OfflineDownloadController::class, 'requestDownloadUrl'])
            ->name('lessons.download-url');
        Route::post('/sync-progress', [SyncProgressController::class, 'sync'])
            ->name('sync-progress');
    });
});
```

**Step 2: Pastikan tidak ada route conflict**
```bash
php artisan route:list --path=api/v2 | head -20
```

**Step 3: Commit**
```bash
git add routes/api.php
git commit -m "feat(offline): add api/v2/mobile route group (additive, no v1 changes)"
```

---

### Task 4: Backend — `OfflineManifestController`

**Objective:** Endpoint yang mengembalikan seluruh metadata course untuk disimpan offline.

**Files:**
- Create: `app/Http/Controllers/Api/V2/Mobile/OfflineManifestController.php`

**Step 1: Buat direktori dan controller**
```php
<?php
namespace App\Http\Controllers\Api\V2\Mobile;

use App\Http\Controllers\Controller;
use App\Models\Course;
use App\Models\LessonProgress;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class OfflineManifestController extends Controller
{
    public function show(Request $request, Course $course): JsonResponse
    {
        $user = $request->user();
        abort_unless($course->enrollments()->where('user_id', $user->id)
            ->whereIn('status', ['approved', 'completed'])->exists(), 403);

        $course->load(['topics' => function ($q) {
            $q->orderBy('order')->with(['lessons' => function ($q) {
                $q->orderBy('order')->select([
                    'id', 'topic_id', 'title', 'order',
                    'duration_hours', 'duration_minutes', 'duration_seconds',
                ]);
            }]);
        }]);

        $lessonIds = $course->topics->flatMap(fn ($t) => $t->lessons->pluck('id'));
        $completedIds = LessonProgress::where('user_id', $user->id)
            ->whereIn('lesson_id', $lessonIds)
            ->whereNotNull('completed_at')
            ->pluck('lesson_id')
            ->toArray();

        $topics = $course->topics->map(function ($topic) use ($completedIds) {
            return [
                'id'      => $topic->id,
                'title'   => $topic->title,
                'order'   => $topic->order,
                'lessons' => $topic->lessons->map(fn ($l) => [
                    'id'               => $l->id,
                    'title'            => $l->title,
                    'order'            => $l->order,
                    'duration_hours'   => $l->duration_hours,
                    'duration_minutes' => $l->duration_minutes,
                    'duration_seconds' => $l->duration_seconds,
                    'is_completed'     => in_array($l->id, $completedIds),
                ]),
            ];
        });

        return response()->json([
            'course' => [
                'id'              => $course->id,
                'title'           => $course->title,
                'cover_image_url' => $course->cover_image_url ?? null,
                'topics'          => $topics,
            ],
            'generated_at' => now()->toIso8601String(),
        ]);
    }
}
```

**Step 2: Test manual**
```bash
TOKEN=$(curl -s -X POST https://mulaisekarang.com/api/v1/auth/login \
  -H "Accept: application/json" -H "Content-Type: application/json" \
  -d '{"email":"qa-student@mulaisekarang.com","password":"QaMobile#2026"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

curl -H "Authorization: Bearer $TOKEN" \
  https://mulaisekarang.com/api/v2/mobile/courses/1/offline-manifest | python3 -m json.tool
```
Expected: JSON dengan `course.topics[].lessons[]` berisi field `is_completed`.

**Step 3: Commit**
```bash
git add app/Http/Controllers/Api/V2/
git commit -m "feat(offline): add OfflineManifestController — GET /api/v2/mobile/courses/{course}/offline-manifest"
```

---

### Task 5: Backend — `OfflineDownloadController` (Signed URL + File Serve)

**Objective:** Endpoint minta download URL + endpoint serve file via signed token.

**Files:**
- Create: `app/Http/Controllers/Api/V2/Mobile/OfflineDownloadController.php`

**Step 1: Buat controller**
```php
<?php
namespace App\Http\Controllers\Api\V2\Mobile;

use App\Http\Controllers\Concerns\StreamsVideoFile;
use App\Http\Controllers\Controller;
use App\Models\Lesson;
use App\Models\OfflineDownloadToken;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Symfony\Component\HttpFoundation\StreamedResponse;

class OfflineDownloadController extends Controller
{
    use StreamsVideoFile;

    /** Authenticated: generate signed token, return download URL */
    public function requestDownloadUrl(Request $request, Lesson $lesson): JsonResponse
    {
        $user   = $request->user();
        $course = $lesson->topic?->course;

        abort_unless($course !== null, 404);
        abort_unless($lesson->isAccessibleBy($user, $course), 403);

        $path = storage_path('app/' . $lesson->effectiveVideoPath());
        abort_unless(file_exists($path), 404);

        // Invalidate old unused tokens for this user+lesson
        OfflineDownloadToken::where('user_id', $user->id)
            ->where('lesson_id', $lesson->id)
            ->whereNull('used_at')
            ->delete();

        $token = OfflineDownloadToken::create([
            'user_id'    => $user->id,
            'lesson_id'  => $lesson->id,
            'token'      => Str::random(64),
            'expires_at' => now()->addHour(),
        ]);

        return response()->json([
            'download_url'    => route('api.v2.mobile.lessons.file', $lesson) . '?token=' . $token->token,
            'expires_at'      => $token->expires_at->toIso8601String(),
            'file_size_bytes' => filesize($path),
        ]);
    }

    /** Token-gated: stream the actual file (no Bearer needed) */
    public function serveFile(Request $request, Lesson $lesson): StreamedResponse
    {
        $tokenStr = $request->query('token');
        abort_unless((bool) $tokenStr, 400);

        $record = OfflineDownloadToken::where('token', $tokenStr)
            ->where('lesson_id', $lesson->id)
            ->first();

        abort_unless($record?->isValid(), 403);

        // Mark as used immediately to prevent replay
        $record->update(['used_at' => now()]);

        $path = storage_path('app/' . $lesson->effectiveVideoPath());
        abort_unless(file_exists($path), 404);

        return $this->streamVideoFile($request, $path);
    }
}
```

**Step 2: Test**
```bash
# 1. Minta URL
curl -H "Authorization: Bearer $TOKEN" \
  "https://mulaisekarang.com/api/v2/mobile/lessons/96/download-url"
# → {"download_url":"...?token=xxx","expires_at":"...","file_size_bytes":12345}

# 2. Download file via URL (simulasi apa yang dilakukan Android)
DURL=$(curl -s -H "Authorization: Bearer $TOKEN" \
  "https://mulaisekarang.com/api/v2/mobile/lessons/96/download-url" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['download_url'])")
curl -I "$DURL"
# → HTTP 200 atau 206 dengan Content-Type: video/mp4
```

**Step 3: Commit**
```bash
git add app/Http/Controllers/Api/V2/
git commit -m "feat(offline): add OfflineDownloadController — signed URL + token-gated file serve"
```

---

### Task 6: Backend — `SyncProgressController` (LWW Sync)

**Objective:** Endpoint sinkronisasi batch yang menerapkan Last Write Wins.

**Files:**
- Create: `app/Http/Controllers/Api/V2/Mobile/SyncProgressController.php`

**Step 1: Buat controller**
```php
<?php
namespace App\Http\Controllers\Api\V2\Mobile;

use App\Http\Controllers\Controller;
use App\Models\Lesson;
use App\Models\LessonProgress;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;

class SyncProgressController extends Controller
{
    public function sync(Request $request): JsonResponse
    {
        $request->validate([
            'device_id'              => 'required|string|max:128',
            'sync_data'              => 'required|array|max:100',
            'sync_data.*.sync_id'   => 'required|string|uuid',
            'sync_data.*.lesson_id' => 'required|integer',
            'sync_data.*.course_id' => 'required|integer',
            'sync_data.*.is_completed'     => 'required|boolean',
            'sync_data.*.recorded_at'      => 'required|date',
            'sync_data.*.progress_seconds' => 'required|integer|min:0',
        ]);

        $user      = $request->user();
        $syncedIds = [];
        $rejectedIds = [];

        foreach ($request->input('sync_data') as $item) {
            $lesson = Lesson::find($item['lesson_id']);
            if (!$lesson) {
                $rejectedIds[] = $item['sync_id'];
                continue;
            }

            $course = $lesson->topic?->course;
            if (!$course || !$lesson->isAccessibleBy($user, $course)) {
                $rejectedIds[] = $item['sync_id'];
                continue;
            }

            $recordedAt = \Carbon\Carbon::parse($item['recorded_at']);

            // Last Write Wins: skip jika server lebih baru dari client
            $existing = LessonProgress::where('user_id', $user->id)
                ->where('lesson_id', $item['lesson_id'])
                ->first();

            if ($existing && $existing->updated_at >= $recordedAt) {
                // Server lebih baru atau sama — abaikan, tapi tetap anggap synced
                // agar app mobile bisa hapus dari queue
                $syncedIds[] = $item['sync_id'];
                continue;
            }

            // Upsert — klien lebih baru
            LessonProgress::updateOrCreate(
                ['user_id' => $user->id, 'lesson_id' => $item['lesson_id']],
                [
                    'completed_at'   => $item['is_completed'] ? $recordedAt : ($existing?->completed_at),
                    'last_synced_at' => now(),
                ]
            );

            // Recalculate course progress jika lesson selesai
            if ($item['is_completed']) {
                $course->recalculateProgressFor($user);
            }

            $syncedIds[] = $item['sync_id'];
        }

        return response()->json([
            'status'       => 'success',
            'synced_ids'   => $syncedIds,
            'rejected_ids' => $rejectedIds,
        ]);
    }
}
```

**Step 2: Test sync endpoint**
```bash
curl -X POST https://mulaisekarang.com/api/v2/mobile/sync-progress \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "device_id": "test-device-001",
    "sync_data": [
      {
        "sync_id": "550e8400-e29b-41d4-a716-446655440000",
        "lesson_id": 96,
        "course_id": 1,
        "progress_seconds": 300,
        "is_completed": false,
        "recorded_at": "2026-09-09T10:00:00Z"
      }
    ]
  }' | python3 -m json.tool
# Expected: {"status":"success","synced_ids":["550e8400-..."],"rejected_ids":[]}
```

**Step 3: Verifikasi LWW (server lebih baru — harus diabaikan)**
```bash
# Kirim payload dengan recorded_at lebih lama dari updated_at server
# synced_ids harus tetap berisi ID tersebut (bukan rejected) — app bisa hapus dari queue
```

**Step 4: Commit**
```bash
git add app/Http/Controllers/Api/V2/
git commit -m "feat(offline): add SyncProgressController — POST /api/v2/mobile/sync-progress (LWW)"
```

---

### Task 7: Backend — Cleanup Cron (Token Expired)

**Objective:** Hapus token download yang sudah expired secara periodik agar tabel tidak penuh.

**Files:**
- Create: `app/Console/Commands/PruneExpiredDownloadTokens.php`
- Modify: `app/Console/Kernel.php` (atau `routes/console.php` jika pakai schedule closure)

**Step 1: Buat command**
```php
<?php
namespace App\Console\Commands;

use App\Models\OfflineDownloadToken;
use Illuminate\Console\Command;

class PruneExpiredDownloadTokens extends Command
{
    protected $signature   = 'offline:prune-tokens';
    protected $description = 'Delete expired offline download tokens';

    public function handle(): int
    {
        $count = OfflineDownloadToken::where('expires_at', '<', now())->delete();
        $this->info("Pruned {$count} expired tokens.");
        return 0;
    }
}
```

**Step 2: Daftarkan ke schedule di `routes/console.php`**
```php
// routes/console.php — tambah:
use App\Console\Commands\PruneExpiredDownloadTokens;
Schedule::command(PruneExpiredDownloadTokens::class)->hourly();
```

**Step 3: Verifikasi**
```bash
php artisan offline:prune-tokens
# Expected: "Pruned 0 expired tokens."
```

**Step 4: Commit**
```bash
git add app/Console/ routes/console.php
git commit -m "feat(offline): add prune-expired-download-tokens cron"
```

---

### Task 8: Android/Flutter — Drift Local Database Schema

**Objective:** Definisikan tabel lokal `offline_sync_queue` dan `downloaded_lessons` menggunakan Drift.

**Files (Flutter APK project — sesuaikan path ke repo Android):**
- Create: `lib/data/local/database/app_database.dart`
- Create: `lib/data/local/database/tables/offline_sync_queue.dart`
- Create: `lib/data/local/database/tables/downloaded_lessons.dart`

**Step 1: Tambah dependency di `pubspec.yaml`**
```yaml
dependencies:
  drift: ^2.18.0
  sqlite3_flutter_libs: ^0.5.0
  path_provider: ^2.1.0
  path: ^1.9.0

dev_dependencies:
  drift_dev: ^2.18.0
  build_runner: ^2.4.0
```

**Step 2: Definisikan tabel `offline_sync_queue`**
```dart
// lib/data/local/database/tables/offline_sync_queue.dart
import 'package:drift/drift.dart';

class OfflineSyncQueue extends Table {
  TextColumn get syncId      => text().named('sync_id')();
  IntColumn  get courseId    => integer().named('course_id')();
  IntColumn  get lessonId    => integer().named('lesson_id')();
  IntColumn  get progressSeconds => integer().named('progress_seconds')();
  BoolColumn get isCompleted => boolean().named('is_completed')();
  DateTimeColumn get recordedAt  => dateTime().named('recorded_at')();
  TextColumn get status      => text().withDefault(const Constant('pending'))();

  @override
  Set<Column> get primaryKey => {syncId};
}
```

**Step 3: Definisikan tabel `downloaded_lessons`**
```dart
// lib/data/local/database/tables/downloaded_lessons.dart
import 'package:drift/drift.dart';

class DownloadedLessons extends Table {
  IntColumn  get lessonId       => integer().named('lesson_id')();
  IntColumn  get courseId       => integer().named('course_id')();
  TextColumn get localPath      => text().named('local_path')();
  IntColumn  get fileSizeBytes  => integer().named('file_size_bytes')();
  DateTimeColumn get downloadedAt => dateTime().named('downloaded_at')();
  DateTimeColumn get expiresAt    => dateTime().named('expires_at').nullable()();

  @override
  Set<Column> get primaryKey => {lessonId};
}
```

**Step 4: Generate kode Drift**
```bash
flutter pub run build_runner build --delete-conflicting-outputs
```

**Step 5: Commit**
```bash
git add lib/data/local/
git commit -m "feat(offline): Drift schema — offline_sync_queue + downloaded_lessons tables"
```

---

### Task 9: Android/Flutter — OfflineRepository (Download + Progress Queue)

**Objective:** Repository layer untuk operasi lokal: simpan progres ke queue, cek status download, enqueue sync.

**Files:**
- Create: `lib/data/repositories/offline_repository.dart`

**Step 1: Buat repository**
```dart
import 'package:uuid/uuid.dart';
import '../local/database/app_database.dart';

class OfflineRepository {
  final AppDatabase _db;
  OfflineRepository(this._db);

  // Rekam progres menonton ke queue
  Future<void> recordProgress({
    required int courseId,
    required int lessonId,
    required int progressSeconds,
    required bool isCompleted,
  }) async {
    await _db.into(_db.offlineSyncQueue).insertOnConflictUpdate(
      OfflineSyncQueueCompanion.insert(
        syncId: const Uuid().v4(),
        courseId: courseId,
        lessonId: lessonId,
        progressSeconds: progressSeconds,
        isCompleted: isCompleted,
        recordedAt: DateTime.now().toUtc(),
        status: const Value('pending'),
      ),
    );
  }

  // Ambil semua pending items untuk di-sync
  Future<List<OfflineSyncQueueData>> getPendingItems() async {
    return (_db.select(_db.offlineSyncQueue)
      ..where((t) => t.status.equals('pending')))
      .get();
  }

  // Hapus items yang sudah di-sync (response synced_ids)
  Future<void> clearSyncedItems(List<String> syncIds) async {
    await (_db.delete(_db.offlineSyncQueue)
      ..where((t) => t.syncId.isIn(syncIds))).go();
  }

  // Cek apakah lesson sudah didownload
  Future<bool> isLessonDownloaded(int lessonId) async {
    final row = await (_db.select(_db.downloadedLessons)
      ..where((t) => t.lessonId.equals(lessonId))).getSingleOrNull();
    return row != null;
  }

  // Simpan metadata setelah download selesai
  Future<void> markDownloadComplete({
    required int lessonId,
    required int courseId,
    required String localPath,
    required int fileSizeBytes,
  }) async {
    await _db.into(_db.downloadedLessons).insertOnConflictUpdate(
      DownloadedLessonsCompanion.insert(
        lessonId: lessonId,
        courseId: courseId,
        localPath: localPath,
        fileSizeBytes: fileSizeBytes,
        downloadedAt: DateTime.now().toUtc(),
      ),
    );
  }
}
```

**Step 2: Commit**
```bash
git add lib/data/repositories/offline_repository.dart
git commit -m "feat(offline): OfflineRepository — queue progress, mark download, clear synced"
```

---

### Task 10: Android/Flutter — DownloadService (WorkManager Background Download)

**Objective:** Download video di background menggunakan WorkManager, simpan ke internal storage dengan nama file ter-obfuscate.

**Files:**
- Create: `lib/services/download_service.dart`

**Step 1: Buat DownloadService**
```dart
import 'dart:io';
import 'package:flutter_downloader/flutter_downloader.dart'; // atau dio + WorkManager
import 'package:path_provider/path_provider.dart';
import 'package:crypto/crypto.dart';
import 'dart:convert';

class DownloadService {
  /// Mengunduh video lesson ke internal storage
  /// Returns: path lokal file yang tersimpan
  Future<String> downloadLesson({
    required int lessonId,
    required String downloadUrl, // Signed URL dari backend
  }) async {
    final dir = await getApplicationSupportDirectory();
    final obfuscatedName = _obfuscateFilename(lessonId);
    final localPath = '${dir.path}/$obfuscatedName';

    // Cek storage space dulu (> 500MB)
    await _checkStorageSpace();

    // Download dengan dio (mendukung resume via Range header)
    final client = HttpClient();
    final request = await client.getUrl(Uri.parse(downloadUrl));
    final response = await request.close();

    final file = File(localPath);
    await response.pipe(file.openWrite());

    return localPath;
  }

  /// Obfuscate nama file: vid_<8char sha256(lessonId)>.dat
  String _obfuscateFilename(int lessonId) {
    final hash = sha256.convert(utf8.encode(lessonId.toString())).toString();
    return 'vid_${hash.substring(0, 8)}.dat';
  }

  /// Throw jika free space < 500MB
  Future<void> _checkStorageSpace() async {
    // Implementasi cek disk space platform-specific
    // Minimal: coba tulis 1 byte ke temp file, catch error
  }

  /// Hapus file video lokal
  Future<void> deleteLesson(String localPath) async {
    final file = File(localPath);
    if (await file.exists()) await file.delete();
  }
}
```

**Step 2: Tambah dependency di `pubspec.yaml`**
```yaml
dependencies:
  dio: ^5.4.0
  crypto: ^3.0.0
```

**Step 3: Commit**
```bash
git add lib/services/download_service.dart
git commit -m "feat(offline): DownloadService — background download, obfuscated local storage"
```

---

### Task 11: Android/Flutter — SyncService (Auto-Sync saat Kembali Online)

**Objective:** Kirim semua pending queue ke backend saat koneksi internet kembali.

**Files:**
- Create: `lib/services/sync_service.dart`

**Step 1: Buat SyncService**
```dart
import 'package:connectivity_plus/connectivity_plus.dart';
import '../data/repositories/offline_repository.dart';
import '../data/remote/api_client.dart'; // existing Dio/Retrofit client

class SyncService {
  final OfflineRepository _repo;
  final ApiClient _api;

  SyncService(this._repo, this._api) {
    // Listen ke perubahan koneksi
    Connectivity().onConnectivityChanged.listen((result) {
      if (result != ConnectivityResult.none) {
        _syncPendingProgress();
      }
    });
  }

  Future<void> _syncPendingProgress() async {
    final pending = await _repo.getPendingItems();
    if (pending.isEmpty) return;

    final payload = pending.map((item) => {
      'sync_id': item.syncId,
      'lesson_id': item.lessonId,
      'course_id': item.courseId,
      'progress_seconds': item.progressSeconds,
      'is_completed': item.isCompleted,
      'recorded_at': item.recordedAt.toIso8601String(),
    }).toList();

    try {
      final response = await _api.syncProgress(
        deviceId: await _getDeviceId(),
        syncData: payload,
      );
      await _repo.clearSyncedItems(response.syncedIds);
    } catch (e) {
      // Gagal sync — biarkan di queue, akan dicoba lagi saat online berikutnya
    }
  }

  /// Trigger manual sync (misal: saat app kembali ke foreground)
  Future<void> syncNow() => _syncPendingProgress();

  Future<String> _getDeviceId() async {
    // Ambil dari SharedPreferences atau device_info_plus
    return 'device-id-placeholder';
  }
}
```

**Step 2: Tambah dependency**
```yaml
dependencies:
  connectivity_plus: ^5.0.0
```

**Step 3: Commit**
```bash
git add lib/services/sync_service.dart
git commit -m "feat(offline): SyncService — auto-sync queue on connectivity restored"
```

---

### Task 12: Android/Flutter — UI: Tombol Download + Indikator Status

**Objective:** Tambahkan tombol download di UI detail lesson dan tampilkan status (belum didownload / sedang download / sudah ada lokal).

**Files:**
- Modify: `lib/ui/lesson/lesson_detail_screen.dart` (atau widget yang relevan)
- Create: `lib/ui/lesson/widgets/download_button.dart`

**Step 1: Buat `DownloadButton` widget**
```dart
import 'package:flutter/material.dart';

enum DownloadStatus { notDownloaded, downloading, downloaded }

class DownloadButton extends StatelessWidget {
  final DownloadStatus status;
  final VoidCallback? onDownload;
  final VoidCallback? onDelete;

  const DownloadButton({
    super.key,
    required this.status,
    this.onDownload,
    this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    return switch (status) {
      DownloadStatus.notDownloaded => IconButton(
          icon: const Icon(Icons.download_outlined),
          tooltip: 'Unduh untuk offline',
          onPressed: onDownload,
        ),
      DownloadStatus.downloading => const CircularProgressIndicator.adaptive(),
      DownloadStatus.downloaded => IconButton(
          icon: const Icon(Icons.download_done, color: Colors.green),
          tooltip: 'Sudah tersimpan — tap untuk hapus',
          onPressed: onDelete,
        ),
    };
  }
}
```

**Step 2: Integrasikan ke `lesson_detail_screen.dart`**
- Cek status download dari `OfflineRepository.isLessonDownloaded(lessonId)`
- Saat tap download: panggil `GET /api/v2/mobile/lessons/{lesson}/download-url` → lalu `DownloadService.downloadLesson()`
- Saat tap delete: panggil `DownloadService.deleteLesson()` + hapus dari DB lokal

**Step 3: Commit**
```bash
git add lib/ui/lesson/widgets/download_button.dart
git commit -m "feat(offline): UI download button — notDownloaded/downloading/downloaded states"
```

---

### Task 13: Android/Flutter — Offline-Aware Video Player

**Objective:** Player otomatis menggunakan file lokal jika lesson sudah didownload, jika tidak streaming seperti biasa.

**Files:**
- Modify atau Create: `lib/ui/lesson/video_player_screen.dart`

**Step 1: Logika pemilihan sumber**
```dart
Future<VideoPlayerController> resolveVideoSource({
  required int lessonId,
  required String streamUrl,
  required OfflineRepository repo,
}) async {
  final isOffline = await repo.isLessonDownloaded(lessonId);

  if (isOffline) {
    final row = await repo.getDownloadedLesson(lessonId);
    return VideoPlayerController.file(File(row!.localPath));
  }

  // Online streaming (existing behavior — tidak berubah)
  return VideoPlayerController.networkUrl(
    Uri.parse(streamUrl),
    httpHeaders: {'Authorization': 'Bearer $token'},
  );
}
```

**Step 2: Progress tracking tiap 5 detik**
```dart
// Di dalam VideoPlayerController listener:
Timer.periodic(const Duration(seconds: 5), (_) {
  final pos = _controller.value.position.inSeconds;
  final duration = _controller.value.duration.inSeconds;
  final completed = pos >= duration * 0.9; // 90% = selesai

  offlineRepo.recordProgress(
    courseId: courseId,
    lessonId: lessonId,
    progressSeconds: pos,
    isCompleted: completed,
  );
});
```

**Step 3: Commit**
```bash
git add lib/ui/lesson/video_player_screen.dart
git commit -m "feat(offline): offline-aware video player — local file fallback + 5s progress recording"
```

---

### Task 14: Backend — Feature Tests

**Objective:** Pastikan semua endpoint v2 tidak merusak endpoint v1 yang ada.

**Files:**
- Create: `tests/Feature/Api/V2/Mobile/OfflineManifestTest.php`
- Create: `tests/Feature/Api/V2/Mobile/OfflineDownloadTest.php`
- Create: `tests/Feature/Api/V2/Mobile/SyncProgressTest.php`

**Step 1: Test manifest endpoint**
```php
// tests/Feature/Api/V2/Mobile/OfflineManifestTest.php
public function test_enrolled_student_can_get_offline_manifest(): void
{
    $student = User::factory()->create();
    $course  = Course::factory()->hasTopic(1, ['order' => 1])->create();
    Enrollment::factory()->for($student)->for($course)
        ->create(['status' => 'approved']);

    $response = $this->actingAs($student, 'sanctum')
        ->getJson("/api/v2/mobile/courses/{$course->id}/offline-manifest");

    $response->assertOk()
        ->assertJsonStructure(['course' => ['id', 'title', 'topics'], 'generated_at']);
}

public function test_non_enrolled_student_cannot_get_manifest(): void
{
    $student = User::factory()->create();
    $course  = Course::factory()->create();

    $this->actingAs($student, 'sanctum')
        ->getJson("/api/v2/mobile/courses/{$course->id}/offline-manifest")
        ->assertForbidden();
}
```

**Step 2: Test sync endpoint (LWW)**
```php
// tests/Feature/Api/V2/Mobile/SyncProgressTest.php
public function test_sync_progress_lww_ignores_older_client_data(): void
{
    // Setup: server sudah punya data yang lebih baru
    $progress = LessonProgress::factory()->create([
        'user_id'    => $student->id,
        'lesson_id'  => $lesson->id,
        'updated_at' => now(),
    ]);

    $response = $this->actingAs($student, 'sanctum')
        ->postJson('/api/v2/mobile/sync-progress', [
            'device_id' => 'test-dev',
            'sync_data' => [[
                'sync_id'          => (string) Str::uuid(),
                'lesson_id'        => $lesson->id,
                'course_id'        => $course->id,
                'progress_seconds' => 100,
                'is_completed'     => false,
                'recorded_at'      => now()->subHour()->toIso8601String(), // lebih lama
            ]],
        ]);

    $response->assertOk()
        ->assertJsonPath('status', 'success');
    // Server data tidak berubah
    $this->assertEquals($progress->completed_at, $progress->fresh()->completed_at);
}
```

**Step 3: Jalankan tests**
```bash
php artisan test tests/Feature/Api/V2/ --stop-on-failure
```
Expected: All tests pass, tanpa regresi di suite lain.

**Step 4: Commit**
```bash
git add tests/Feature/Api/V2/
git commit -m "test(offline): feature tests for v2 mobile API — manifest, download, sync LWW"
```

---

### Task 15: Deploy & Smoke Test Production

**Objective:** Deploy ke production, verifikasi endpoint baru live, dan pastikan web tidak terdampak.

**Step 1: Deploy**
```bash
cd /home/ykde/mulaisekarang && bash deploy.sh
# Pantau log: tail -f ~/.cache/mulaisekarang-deploy.log
```

**Step 2: Smoke test endpoint v2**
```bash
TOKEN=$(curl -s -X POST https://mulaisekarang.com/api/v1/auth/login \
  -H "Accept: application/json" -H "Content-Type: application/json" \
  -d '{"email":"qa-student@mulaisekarang.com","password":"QaMobile#2026"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Test manifest (harus 200)
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  https://mulaisekarang.com/api/v2/mobile/courses/1/offline-manifest
# Expected: 200

# Test endpoint v1 masih normal (smoke test non-regression)
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  https://mulaisekarang.com/api/v1/my-courses
# Expected: 200
```

**Step 3: Cek tidak ada 5xx di log**
```bash
docker compose exec app grep "HTTP/1.1\" 5" /var/log/nginx/access.log | tail -20
# Expected: 0 baris 5xx baru setelah deployment
```

---

## 8. Risiko & Mitigasi

| Risiko | Kemungkinan | Dampak | Mitigasi |
|---|---|---|---|
| Token download disalahgunakan (replay attack) | Sedang | Tinggi | Token ditandai `used_at` saat pertama kali dipakai → invalidasi langsung |
| Video file bocor dari internal storage Android | Rendah | Tinggi | File disimpan di `getFilesDir()` (tidak bisa diakses app lain) + nama di-obfuscate |
| Konflik progres LWW (web vs mobile edit bersamaan) | Sedang | Sedang | LWW berdasarkan timestamp — dokumentasikan ke user bahwa "pencatatan terakhir menang" |
| Tabel `offline_download_tokens` tumbuh tak terkendali | Rendah | Rendah | Cron `offline:prune-tokens` jalan tiap jam |
| Migration merusak schema existing | Rendah | Sangat Tinggi | Semua migration additive-only, tidak drop/alter kolom existing; test `--pretend` wajib sebelum run |
| APK build error setelah tambah Drift dependency | Sedang | Sedang | Jalankan `flutter pub run build_runner build` setelah perubahan schema Drift |

---

## 9. Open Questions

1. **Expiry download lokal di APK:** Apakah file yang didownload akan dihapus otomatis setelah 30 hari? Atau selama user punya enrollment aktif?
2. **Partial progress (seconds) di sync:** Apakah server perlu menyimpan `progress_seconds` (detik terakhir ditonton) di `lesson_progress`, atau cukup `completed_at`? Saat ini schema hanya punya `completed_at`. Jika perlu: tambahkan kolom `last_watch_position_seconds` di migration Task 1.
3. **Multiple device sync:** Jika user menonton di 2 device berbeda secara bersamaan, LWW by timestamp cukup? Atau perlu vector clock?
4. **HLS vs MP4 untuk download:** Video saat ini disajikan dalam format MP4 raw (`effectiveVideoPath()`) maupun HLS (`.m3u8`). Untuk download offline, lebih praktis MP4 (1 file). Pastikan semua lesson punya `video_path` terisi, bukan hanya `hls_path`.
