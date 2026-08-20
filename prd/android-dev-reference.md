# Android Dev Reference — Student Mobile API (v1)

Single-file acuan pengembangan Android. Sumber: `fdd/api/student-api.md` (spec resmi) +
hasil test langsung ke semua endpoint (2026-08-17) pakai data seed lokal (SQLite,
Laravel `php artisan serve`). Tiap endpoint di bawah punya **contoh request/response
asli** yang sudah diverifikasi jalan — bisa langsung dipakai sebagai mock data
Android (Retrofit/MockWebServer/Postman collection).

**Base URL**: `https://course.mulaisekarang.com/api/v1`
**Auth**: Bearer token (Sanctum) dari `/auth/login`. Header wajib: `Accept: application/json`,
`X-Client: android` (khusus endpoint checkout/purchase, biar redirect Xendit pakai deep link app).

---

## ✅ BUG FIXED — checkout & cart checkout sempat crash (500)

Sempat error 500 (`RuntimeException: Session store not set on request`) di
`POST /courses/{course}/checkout` dan `POST /cart/checkout`. Root cause:
`app/Services/Payment/XenditPaymentService.php` manggil `request()->session()->getId()`
padahal route `api/*` stateless (tidak ada `StartSession` middleware).

**Fix (2026-08-17)**: diganti jadi `request()->hasSession() ? request()->session()->getId() : null`
di kedua tempat (`createInvoice` dan `createCartInvoice`). `session_id` cuma dipakai untuk
affiliate attribution (`OrderFulfillmentService`), yang sudah punya guard untuk `null`
(skip atribusi) — jadi aman, tidak ada regresi ke web checkout maupun affiliate tracking.
Diverifikasi ulang: kedua endpoint sekarang balikin response sukses sesuai dokumentasi
(lihat contoh di bawah, hasil re-test asli, bukan asumsi).

Endpoint `POST /chat/subscription/purchase` pakai service yang sama — belum dites
langsung, tapi karena fix ada di level service (bukan per-endpoint), seharusnya ikut
kefix. Perlu verifikasi terpisah kalau ada waktu.

---

## Konvensi

- List envelope: `{ "data": [...], "meta": {...} }`
- Single envelope: `{ "data": {...} }`
- Kecuali auth & profile: `{ "user": {...} }` (login/register/google juga punya `token`)
- Error: 401 (token invalid), 403 (bukan pemilik/tidak enroll), 404, 409 (conflict), 422 (validasi)
- `X-Client: android` wajib di checkout/purchase agar redirect pakai deep link `mulaisekarang://payment-return?ref=...&status=...`

---

## 1. Auth

### POST /auth/register
Request:
```json
{ "name": "Andi Wijaya", "email": "andi.new@mockup.test", "password": "rahasia123", "password_confirmation": "rahasia123" }
```
⚠️ Tes asli gagal 422 karena field `first_name`, `last_name`, `username` ternyata **required**
di request register (dokumentasi lama hanya sebut `name`). **Field aktual yang wajib**:
```json
{ "first_name": "Andi", "last_name": "Wijaya", "username": "andiwijaya",
  "email": "andi.new@mockup.test", "password": "rahasia123", "password_confirmation": "rahasia123" }
```
Response sukses **201**: `{ "token": "...", "user": { ... } }`. `role` selalu `subscriber`.

### POST /auth/login — TESTED ✅
Request:
```json
{ "email": "student@mockup.test", "password": "password123" }
```
Response:
```json
{ "token": "4|GpLFfDZupNnRhYEIh6RWbqLydtAKCt56OKW5kRtZ32173138",
  "user": { "id": 219, "name": "Budi Santoso", "display_name": "Budi Santoso",
    "username": null, "first_name": null, "last_name": null,
    "email": "student@mockup.test", "role": "subscriber", "bio": null,
    "profile_photo_url": null, "is_verified_instructor": false } }
```

### GET /auth/me — TESTED ✅
Header: `Authorization: Bearer <token>`
```json
{ "user": { "id": 219, "name": "Budi Santoso", "display_name": "Budi Santoso",
  "username": null, "first_name": null, "last_name": null,
  "email": "student@mockup.test", "role": "subscriber", "bio": null,
  "profile_photo_url": null, "is_verified_instructor": false } }
```

---

## 2. Beranda / Marketplace (katalog kursus)

### GET /courses — TESTED ✅ (marketplace, list + filter)
`GET /courses?sort=latest` atau `?featured=1` untuk beranda.
```json
{ "data": [
  { "id": 1, "title": "Dasar-Dasar Pemrograman Web",
    "description": "Kursus pengantar untuk memahami HTML, CSS, dan JavaScript dari nol.",
    "cover_image_url": null, "type": "course", "level": "beginner",
    "regular_price": 150000, "sale_price": 99000, "current_price": 99000,
    "has_discount": true, "duration": "4 jam", "is_featured": false,
    "reviews_avg_rating": 5, "enrollments_count": 1, "is_enrolled": true,
    "mentor": { "id": 1, "display_name": "Admin Mulaisekarang", "username": "adminmulaisekarang",
      "profile_photo_url": null, "job_title": null, "is_verified_instructor": false },
    "category": null, "tags": [], "updated_at": "2026-08-17T04:22:28+00:00" },
  { "id": 2, "title": "Belajar Menggambar Digital: Animal Doodle Arts with ENJIARTS",
    "cover_image_url": "https://.../Thumbnail.png", "type": "course", "level": "beginner",
    "regular_price": 120000, "sale_price": 55000, "current_price": 55000,
    "has_discount": true, "is_featured": true, "reviews_avg_rating": null,
    "enrollments_count": 0, "is_enrolled": false,
    "mentor": { "id": 9, "display_name": "enjiarts", "username": "enjiarts",
      "job_title": "Illustrator", "is_verified_instructor": false },
    "category": { "id": 3, "name": "Drawing", "slug": "drawing" }, "tags": [] }
], "meta": { "current_page": 1, "last_page": 1, "per_page": 12, "total": 11 } }
```
Query params: `q`, `type[]`, `category[]`, `tag[]`, `level[]`, `featured`, `sort`
(`latest|price_low|price_high|popular|rating`).

### GET /courses/{id} — TESTED ✅ (detail / mini course entry point)
```json
{ "data": {
  "id": 1, "title": "Dasar-Dasar Pemrograman Web", "current_price": 99000,
  "is_enrolled": true, "lessons_count": 6,
  "topics": [
    { "id": 1, "title": "Pengenalan HTML", "order": 1,
      "lessons": [
        { "id": 1, "title": "Apa itu HTML?", "order": 1, "duration_hours": 0,
          "duration_minutes": 0, "duration_seconds": 0, "allow_preview": false,
          "is_accessible": true, "is_completed": false },
        { "id": 2, "title": "Struktur Dokumen HTML", "order": 2, "is_completed": false }
      ], "quizzes": [], "assignments": [] },
    { "id": 48, "title": "Pengenalan", "order": 1,
      "lessons": [
        { "id": 96, "title": "Video Pembuka", "order": 1, "duration_minutes": 10,
          "allow_preview": true, "is_accessible": true, "is_completed": true },
        { "id": 97, "title": "Video Lanjutan", "order": 2, "duration_minutes": 10,
          "allow_preview": false, "is_accessible": true, "is_completed": false }
      ],
      "quizzes": [ { "id": 1, "title": "Kuis Bab 1", "questions_count": 1,
        "time_limit": null, "passing_grade": 60, "max_attempts": null, "my_best_score": null } ],
      "assignments": [ { "id": 7, "title": "Tugas 1", "total_points": 100,
        "due_at": null, "my_submission_status": null } ] }
  ],
  "reviews": [ { "id": 1, "rating": 5, "body": "Materi jelas dan mudah dipahami.",
    "user": { "id": 219, "display_name": "Budi Santoso" } } ],
  "next_lesson_id": 1, "course_completed": false
} }
```
Catatan: `topics[].quizzes[]` dan `assignments[]` **tidak ada di dokumentasi lama** —
field baru yang berguna untuk render struktur kurikulum mini-course tanpa call
tambahan. `batches: []` juga muncul (bundle/private class, kosong untuk course biasa).

### GET /courses/{id}/reviews — TESTED ✅
```json
{ "data": [ { "id": 1, "rating": 5, "body": "Materi jelas dan mudah dipahami.",
  "user": { "id": 219, "display_name": "Budi Santoso", "username": null } } ],
  "meta": { "current_page": 1, "last_page": 1, "per_page": 10, "total": 1,
    "average_rating": 5, "rating_breakdown": { "1": 0, "2": 0, "3": 0, "4": 0, "5": 1 } } }
```

### GET /categories — TESTED ✅
```json
{ "data": [
  { "id": 1, "name": "Branding", "slug": "branding", "description": "..." },
  { "id": 7, "name": "Design", "slug": "design", "description": "..." },
  { "id": 3, "name": "Drawing", "slug": "drawing", "description": "..." },
  { "id": 4, "name": "Excel", "slug": "excel", "description": "..." },
  { "id": 5, "name": "Private Class", "slug": "private-class", "description": "..." },
  { "id": 6, "name": "Programming", "slug": "programming", "description": "..." }
] }
```

### GET /instructors/{username} — TESTED ✅
Catatan: `{username}` harus milik user `role = tutor_instructor`, kalau tidak → 404.
```json
{ "data": { "id": 9, "display_name": "enjiarts", "username": "enjiarts",
  "profile_photo_url": "https://.../1001034035.jpg", "job_title": "Illustrator",
  "is_verified_instructor": false, "bio": null, "student_count": 0, "course_count": 7 },
  "courses": [ { "id": 2, "title": "Belajar Menggambar Digital...", "...": "CourseResource" } ] }
```
`rating` hanya muncul di JSON kalau instructor sudah punya review published (`when()` conditional) — jangan asumsikan key selalu ada, cek `containsKey` di parser.

---

## 3. Learning / Continue Course (mini course)

### GET /my-courses — TESTED ✅ (list "lanjutkan belajar")
```json
{ "data": [ { "id": 1, "status": "approved", "progress_percent": 50,
  "enrolled_at": "2026-08-17T04:22:38+00:00",
  "course": { "id": 1, "title": "Dasar-Dasar Pemrograman Web", "current_price": 99000 } } ],
  "meta": { "current_page": 1, "last_page": 1, "per_page": 15, "total": 1 } }
```

### GET /dashboard/summary — TESTED ✅
```json
{ "data": { "total_learning_hours": 0.2, "enrolled_courses_count": 1 } }
```

### GET /courses/{course}/lessons/{lesson} — TESTED ✅
```json
{ "data": { "id": 96, "title": "Video Pembuka",
  "content": "...", "duration_hours": 0, "duration_minutes": 10, "duration_seconds": 0,
  "allow_preview": true, "is_accessible": true, "is_completed": true,
  "video_stream_url": "https://.../api/v1/lessons/96/stream" } }
```
Stream request butuh `Authorization: Bearer` di header request video itu sendiri
(pakai `DefaultHttpDataSource.Factory` di ExoPlayer).

### POST /courses/{course}/lessons/{lesson}/complete — TESTED ✅ (idempotent)
```json
{ "data": { "completed_lesson_id": 97, "next_lesson_id": 3,
  "progress_percent": 33, "course_completed": false } }
```
`next_lesson_id: null` + `course_completed: true` di lesson terakhir kurikulum.

### GET /quizzes/{quiz} — TESTED ✅
```json
{ "data": { "id": 1, "title": "Kuis Bab 1", "time_limit": null, "passing_grade": 60,
  "max_attempts": null, "attempts_used": 0,
  "questions": [ { "id": 1, "question": "Assumenda quia harum...?",
    "type": "multiple_choice", "options": ["A","B","C","D"], "order": 1 } ] } }
```

### POST /quizzes/{quiz}/attempts — TESTED ✅
Request: `{ "answers": { "1": "A" } }`
```json
{ "data": { "score": 100, "passed": true, "correct_count": 1, "total_count": 1 } }
```

### GET /quiz-attempts — TESTED ✅
```json
{ "data": [ { "id": 1, "score": 100, "passed": true, "answers": ["A"],
  "submitted_at": "2026-08-17T04:23:23+00:00", "quiz_id": 1, "quiz_title": "Kuis Bab 1" } ],
  "meta": { "current_page": 1, "last_page": 1, "per_page": 15, "total": 1 } }
```

### GET /assignments/{assignment} — TESTED ✅
```json
{ "data": { "id": 7, "title": "Tugas 1", "instructions": "...",
  "total_points": 100, "due_at": null, "allow_resubmission": false,
  "my_submission": null } }
```

### POST /assignments/{assignment}/submissions — TESTED ✅ (multipart, `repository_url` only)
```json
{ "data": { "id": 1, "file_url": null,
  "repository_url": "https://github.com/budi/tugas1", "status": "submitted",
  "grade": null, "feedback": null, "submitted_at": "2026-08-17T04:23:23+00:00" } }
```

### GET /assignment-submissions — TESTED ✅
```json
{ "data": [ { "id": 1, "file_url": null,
  "repository_url": "https://github.com/budi/tugas1", "grade": null, "feedback": null,
  "status": "submitted", "submitted_at": "2026-08-17T04:23:23+00:00",
  "assignment_id": 7, "assignment_title": "Tugas 1" } ],
  "meta": { "current_page": 1, "last_page": 1, "per_page": 15, "total": 1 } }
```

### POST /courses/{course}/reviews — TESTED ✅
Request: `{ "rating": 5, "body": "Update review dari mockup test." }` → **200** (replace existing) atau **201** (baru).

### GET /my-reviews — TESTED ✅
Sama seperti review biasa + field `status` (`published`/`hidden`) dan `course` penuh.

---

## 4. Payment

Server yang menentukan harga — app cuma kirim course id / cart id, **jangan pernah
kirim amount dari client**.

### POST /courses/{course}/checkout — TESTED ✅ (setelah fix bug session, lihat catatan di atas)
Response asli, course gratis (skip Xendit, langsung enroll):
```json
{ "data": { "status": "completed", "reference_id": "ORD-20260817-TKJXLB",
  "invoice_url": null, "amount": 0 } }
```
Untuk course berbayar (belum dites langsung ke Xendit sandbox — mode akun ini `live`,
sengaja dihindari agar tidak membuat invoice production sungguhan), bentuk response
sesuai dokumentasi:
```json
{ "data": { "status": "pending", "reference_id": "ORD-20260725-AB12CD",
  "invoice_url": "https://checkout.xendit.co/web/...", "amount": 150000 } }
```

### POST /cart/preview — TESTED ✅
Request: `{ "course_ids": [1, 2] }`
```json
{ "data": { "items": [ { "course_id": 2,
  "title": "Belajar Menggambar Digital: Animal Doodle Arts with ENJIARTS",
  "cover_image_url": "https://.../Thumbnail.png", "price": 55000 } ],
  "subtotal": 55000, "total": 55000,
  "issues": [ { "course_id": 1, "reason": "already_owned" } ] } }
```

### POST /cart/checkout — TESTED ✅ (setelah fix bug session, lihat catatan di atas)
Response asli, cart berisi course gratis (skip Xendit):
```json
{ "data": { "status": "completed", "reference_id": "ORD-20260817-TXHPPL",
  "invoice_url": null, "amount": 0, "issues": [] } }
```
Untuk cart berbayar bentuk response sama seperti dokumentasi (`status: "pending"` +
`invoice_url` Xendit) — sudah dibuktikan lewat jalur kode yang identik dengan checkout
tunggal di atas, jadi tidak perlu re-test terpisah pakai invoice production sungguhan.

### GET /payment/status/{referenceId} — TESTED ✅
```json
{ "data": { "reference_id": "ORD-20260817-MOCK01", "status": "completed",
  "amount": 150000, "invoice_url": null, "paid_at": null, "expires_at": null } }
```
Status: `pending | processing | on_hold | completed | cancelled | refunded | expired | failed`.

### GET /transactions — TESTED ✅
```json
{ "data": [ { "id": 1, "reference_id": "ORD-20260817-MOCK01", "amount": 150000,
  "status": "completed", "payment_channel": null, "payment_method": null,
  "paid_at": null, "created_at": "2026-08-17T04:22:38+00:00",
  "order_items": [ { "id": 1, "price": 150000, "quantity": 1,
    "orderable_type": "course",
    "course": { "id": 1, "title": "Dasar-Dasar Pemrograman Web", "current_price": 99000 } } ] } ],
  "meta": { "current_page": 1, "last_page": 1, "per_page": 15, "total": 1 } }
```

### Payment flow (untuk implementasi Android)
1. `POST /courses/{id}/checkout` atau `/cart/checkout` dengan header `X-Client: android`.
2. Buka `invoice_url` di Custom Tab.
3. Xendit redirect balik ke `mulaisekarang://payment-return?ref=<reference_id>&status=success|failed` — daftarkan deep link ini di manifest.
4. Deep link **bukan bukti pembayaran** — poll `GET /payment/status/{ref}` sampai `completed`, baru refresh `GET /my-courses`.

---

## 5. Notifications

### GET /notifications — TESTED ✅
```json
{ "data": [], "meta": { "current_page": 1, "last_page": 1, "per_page": 20, "total": 0, "unread_count": 0 } }
```
Item non-kosong: `{ "id": "uuid", "type": "AssignmentGradedNotification", "title": "...",
"body": "...", "read": false, "read_at": null, "course_id": 3, "assignment_id": 5,
"conversation_id": null, "reference_id": null, "data": {...} }`. `id` string UUID, bukan integer.

### POST /notifications/read-all — TESTED ✅
```json
{ "data": { "unread_count": 0 } }
```

---

## 6. Chat mentor (add-on berbayar)

### GET /chat/subscription — TESTED ✅
```json
{ "data": { "has_access": false, "status": null, "eligible_for_trial": true,
  "trial_ends_at": null, "ends_at": null,
  "pricing": { "standard_price": 50000, "promo_price": 30000, "promo_days": 7, "trial_days": 14 } } }
```

### POST /chat/subscription/trial — TESTED ✅
```json
{ "data": { "status": "trialing", "trial_ends_at": "2026-08-31T04:23:23+00:00", "has_access": true } }
```

### GET /chat/eligible-contacts — TESTED ✅
```json
{ "data": [ { "id": 1, "display_name": "Admin Mulaisekarang", "username": "adminmulaisekarang",
  "job_title": null, "is_verified_instructor": false } ] }
```

### GET /chat/conversations — TESTED ✅
```json
{ "data": [] }
```
Item non-kosong: `{ id, type, last_message_at, unread_count, other_party | (title + participants[]) }`.

### POST /chat/conversations/{id}/messages
Paywalled 403 kalau tidak subscribe: `{ "message": "Fitur chat mentor memerlukan langganan aktif.", "code": "chat_subscription_required" }` — Android harus branch di `code`, bukan status code saja.

---

## Ringkasan hasil test (2026-08-17)

| Area | Endpoint | Status |
|---|---|---|
| Auth | login, me | ✅ jalan |
| Auth | register | ⚠️ perlu `first_name/last_name/username`, dokumentasi lama kurang lengkap |
| Beranda/Marketplace | courses list, detail, reviews, categories, instructor | ✅ jalan |
| Mini course | lesson show/complete, quiz show/attempt, assignment show/submit | ✅ jalan |
| Lanjutkan belajar | my-courses, dashboard/summary | ✅ jalan |
| Payment | cart/preview, payment/status, transactions | ✅ jalan |
| Payment | course checkout, cart/checkout | ✅ jalan (500 sempat terjadi, **sudah di-fix** — lihat catatan di atas) |
| Notifications | index, read-all | ✅ jalan |
| Chat | subscription, trial, eligible-contacts, conversations | ✅ jalan |

Semua endpoint yang tidak ditandai ⚠️/❌ punya contoh response asli di atas — aman
dipakai langsung sebagai mock JSON untuk development UI Android sebelum backend siap /
sebelum bug checkout di-fix.
