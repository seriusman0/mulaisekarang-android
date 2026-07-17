PRD: Menyelesaikan mulaisekarang-android Secara Menyeluruh — Analisis Kesenjangan
Status: Draft
Fokus Area: Tabel Referensi (model data, FDD, API surface, inventaris layar, gap)
Dokumen Terkait: 00-overview.md (ringkasan), 01-roadmap.md (eksekusi)

## 1. Model Data Backend Lengkap

- **Identitas:** `users` (role administrator/tutor_instructor/subscriber; is_verified_instructor; google_id), `user_meta`, `personal_access_tokens` (Sanctum).
- **Katalog:** `courses` (draft/published; course/bundle), `topics`, `lessons`, `categories` (self-referencing), `tags`+`course_tag`, `bundle_items`.
- **Aktivitas belajar:** `enrollments` (approved/cancelled/pending + progress_percent), `lesson_progress`, `quizzes`+`quiz_questions`+`quiz_attempts`, `assignments`+`assignment_submissions`, `questions` (forum Q&A lesson).
- **Komersial:** `carts`/`cart_items`, `transactions` (polymorphic `payable`, Xendit invoice/webhook fields), `order_items`, `order_histories`, `payment_logs`, `earnings`, `withdrawals`.
- **Chat:** `conversations` (mentor/ai/group), `conversation_participants`, `messages`, `chat_subscriptions` (trial/purchase, pending_payment→active→expired/cancelled).
- **Konten/lainnya:** `reviews`, `faqs`, `testimonials`, `announcements`, `notifications` (polymorphic Laravel standar), `settings`.
- **Konfirmasi tidak ada di manapun** (tidak ada tabel/model/route/FDD selain satu label UI nyasar): coupons/kupon, wishlist, affiliate/referral, certificates/sertifikat.

## 2. Indeks Dokumen FDD (22 dokumen, `../mulaisekarang/fdd/`)

| Dokumen | Ringkasan |
|---|---|
| init.md | Blueprint MVP: Laravel 11 bare-metal, tanpa link video eksternal (streaming lokal saja). |
| chat.md | Chat Add-on Premium: trial 14 hari → Rp30k/7hr promo → Rp50k standar, Gate-based paywall. |
| order.md | Manajemen order/transaksi admin (paritas WooCommerce), status change memicu auto-enroll/revoke. |
| payment.md | Integrasi Xendit: webhook idempoten + signature check + row-locking. |
| payment-verify.md | Finalisasi webhook: aturan signature/idempotency, auto-enrollment transaksional. |
| spec-public-front.md | Storefront publik: header/cart/footer, homepage, filter marketplace live, Google SSO. |
| spec-user-dashboard.md | Dashboard akun: sidebar Reviews/Wishlist/Order History/Q&A/Calendar, tab settings. |
| admin/spec-admin-category.md | CRUD kategori hierarkis. |
| admin/spec-admin-course-builder.md | Course builder admin/instruktur: upload video lokal wajib. |
| admin/spec-admin-enrollment.md | Datatable enrollment, "Reset Progress", wizard manual + bulk CSV. |
| admin/spec-admin-instructor.md | Roster instruktur, commission-rate/earnings summary. |
| admin/spec-admin-reports.md | Analytics: 8 KPI card, chart earnings, drill-down per course/siswa. |
| admin/spec-users-admin-tutor-clone.md | Admin user ala WP/Tutor-clone. |
| admin/verified-instructor.md | Kolom `verified_at` + toggle Gate-protected + badge centang biru. |
| admin+instructor/coursebuilder.md | Pemindahan routing course-builder ke bawah `/dashboard`, role-gated. |
| fix/contrast.md | Kosong. |
| fix/home-fixing.md | `scopeProductionReady()` sembunyikan course draft/tanpa thumbnail dari homepage. |
| frontend/spec-public-front.md | (Nama menyesatkan) — sebenarnya spec Design System global/mobile-responsiveness. |
| instructor/spec-instructor-dashboard.md | Portal instruktur: isolasi data per instructor_id, onboarding profil. |
| migration/migration-master.md | Log migrasi WP→Laravel: 14 course/~214 user/83-dari-84 video video migrasi. |
| public/course-navigation.md | Halaman detail course: card stretched-link, tab Alpine, sidebar sticky "Certificate status". |
| special-feature/ | Direktori kosong. |

## 3. Permukaan API `/api/v1` Saat Ini

| Endpoint | Method | Auth |
|---|---|---|
| auth/register, auth/login, auth/google | POST | Publik |
| auth/forgot-password, auth/reset-password | POST | Publik |
| courses, courses/{id}, categories, instructors/{username} | GET | Publik |
| auth/me, auth/logout | GET/POST | Sanctum |
| profile | POST (multipart) | Sanctum |
| my-courses, dashboard/summary | GET | Sanctum |
| courses/{id}/checkout | POST | Sanctum |
| payment/status/{referenceId} | GET | Sanctum |
| courses/{id}/lessons/{id}, .../complete | GET/POST | Sanctum |
| lessons/{id}/stream | GET | Sanctum |
| quizzes/{id}, quizzes/{id}/attempts | GET/POST | Sanctum |
| assignments/{id}, assignments/{id}/submissions | GET/POST (multipart) | Sanctum |
| chat/conversations, chat/ai/start, chat/groups, chat/eligible-contacts | GET/POST | Sanctum (tanpa gate chat_subscriptions — lihat 01-roadmap.md P2.4) |
| chat/conversations/{id}/messages | GET/POST | Sanctum |

## 4. Inventaris 19 Layar

| Layar | API Pendukung | Status Offline |
|---|---|---|
| Splash, Login, Register, Forgot/Reset Password | auth/* | Network-only |
| Home | courses, categories | Cache (Room, feed default) |
| Marketplace | courses (search/filter/sort) | Network-only (hasil filter tidak di-cache) |
| MyCourses | my-courses | Network-only |
| CourseDetail | courses/{id} | Cache (Room, satu-satunya layar offline-first penuh) |
| CheckoutSummary, PaymentSuccess | checkout, payment/status | Network-only |
| Quiz, Assignment | quizzes/*, assignments/* | Network-only |
| InstructorProfile | instructors/{username} | Network-only |
| LessonPlayer | lessons/{id}, lessons/{id}/stream | Network-only |
| ChatList, NewGroup, ChatConversation | chat/* | Network-only (polling 4 detik) |
| Profile, EditProfile | auth/me, profile | Network-only |

Catatan: hanya feed katalog course default (`CourseEntity`/`CourseDetailEntity`/`RemoteKeyEntity` via Room) yang benar-benar di-cache; semua layar lain adalah pass-through tipis ke Retrofit tanpa fallback offline.

## 5. Tabel Gap: CORE / NICE-TO-HAVE / OUT-OF-SCOPE

| Fitur | Tag | Kategori |
|---|---|---|
| Ganti kata sandi | `[Full-stack]` | CORE — lihat 01-roadmap.md P2a.1 |
| Riwayat transaksi | `[Full-stack]` | CORE — P2a.2 |
| Reviews (baca) | `[Android]` | CORE — P2a.3, plumbing sudah ada, tinggal render |
| Reviews (tulis) | `[Full-stack]` | CORE — P2b.5 |
| Paritas monetisasi chat | `[Full-stack]` | CORE, item terbesar — P2b.4, keputusan D1 |
| Unduh lampiran chat | `[Full-stack]` | CORE-adjacent — P2b.6 |
| Notifications inbox | `[Full-stack]` | CORE — P2b.7 |
| Cart/checkout multi-item | `[Full-stack]` | NICE-TO-HAVE — backlog P3 |
| Social profile links/cover photo | `[Full-stack]` | NICE-TO-HAVE — backlog P3 |
| Q&A forum pelajaran | `[Full-stack]` | NICE-TO-HAVE — backlog P3 |
| Wishlist | `[Full-stack, belum dibangun sama sekali]` | OUT-OF-SCOPE |
| Sertifikat | `[belum dibangun sama sekali]` | OUT-OF-SCOPE |
| Kupon/diskon | `[belum dibangun sama sekali]` | OUT-OF-SCOPE |
| Program afiliasi | `[belum dibangun sama sekali]` | OUT-OF-SCOPE |
| Tools instruktur/admin (course builder, enrollment CSV, withdrawal, analytics, user admin) | Web-only by design | OUT-OF-SCOPE — role-gated Livewire, di luar cakupan aplikasi learner-only ini |
