PRD: Menyelesaikan mulaisekarang-android Secara Menyeluruh — Roadmap
Status: Draft
Fokus Area: Rincian Eksekusi per Fase (P0–P4) + Backlog P3
Dokumen Terkait: 00-overview.md (ringkasan & keputusan), 02-gap-analysis.md (tabel referensi)

Tag per item: `[Android]` hanya sisi Android, `[Backend-only]` hanya sisi Laravel, `[Full-stack]` keduanya.

## P0 — Stabilisasi

Semua Android-only, tanpa ketergantungan backend. Dikerjakan lebih dulu karena murah dan menurunkan risiko semua fase berikutnya.

1. **Selesaikan diff belum-commit yang sedang berjalan** — icon rebrand (`drawable-nodpi/`, `mipmap-nodpi/`), `ProfileScreen.kt`, `GlassBottomNavBar.kt`, `LoginScreen.kt`/`SplashScreen.kt`. Putuskan & commit sebelum menyentuh file-file ini lagi untuk item 4 & 5 di bawah — dua di antaranya justru target P0 ini sendiri.
2. **Perbaiki 5 silent-failure bug** (pola `.onSuccess{}` tanpa `.onFailure{}` sama sekali):
   - `ChatConversationViewModel.sendMessage` (baris 80-84) — pesan gagal terkirim hilang tanpa jejak. Referensi pola yang benar ada di file yang sama: `refresh()` (baris 67-75).
   - `LessonPlayerViewModel.markComplete` — kegagalan tandai-selesai tidak muncul ke user.
   - `CourseDetailViewModel.checkPendingPayment` — kegagalan polling status pembayaran Xendit ditelan.
   - `CourseDetailViewModel.startConversationWithMentor` — `.getOrNull()` menelan error, caller cuma dapat `null`.
   - `MyCoursesViewModel` (recommended-courses fetch) — severity rendah, tetap perbaiki untuk konsistensi.
3. **Tambahkan `GoogleAuthClient.signOut()`**/pembersihan credential-state, panggil dari `AuthRepository.logout()` — saat ini logout tidak membersihkan sesi Google, account picker bisa auto-pilih akun lama.
4. **Perbaiki regresi ikon adaptif** (A1) — kembalikan `mipmap-anydpi-v26/ic_launcher.xml` + vector background/foreground, agar Android 13+ themed/monochrome icon tidak hilang.
5. **Selesaikan status sistem Glass** (A2) — restorasi frosted-glass di `GlassBottomNavBar.kt` agar konsisten dengan `Glass.kt`/`GlassCard` yang masih dipakai di layar lain.
6. **Tulis ulang bagian Arsitektur di `README.md`** — Hilt itu nyata dan dipakai (`di/NetworkModule.kt`, `di/DatabaseModule.kt`, `@HiltViewModel` di semua viewmodel); tidak ada `AppContainer.kt` di tree saat ini, klaim README sudah basi total.

## P1 — Fondasi Produksi

1. `[Android]` Unit test ViewModel (repository-failure → error-state, langsung meng-cover regresi dari perbaikan P0.2), test DAO Room, beberapa Compose test untuk alur login/checkout.
2. `[Android]` CI baru — `.github/workflows/`: lint + unit test + `assembleDebug` di setiap PR. (Backend sudah punya budaya test nyata — `tests/Feature`, `tests/Unit`, `tests/e2e`, `phpunit.xml`, `playwright.config.ts` — ini menyamakan sisi Android, bukan memperkenalkan praktik baru ke proyek.)
3. `[Android]` Crash reporting minimum viable — Firebase Crashlytics (A3).
4. `[Backend-only]` Ganti `MAIL_MAILER=log` ke pengiriman email nyata (SMTP/Postmark/SES) — **prioritas tertinggi** di seluruh audit: alur lupa-password/reset-password sudah lengkap di UI Android (`ForgotPasswordScreen`/`ResetPasswordScreen`) tapi diam-diam tidak berfungsi di lingkungan nyata manapun hari ini.
5. `[Backend-only]` Pastikan/deploy route privacy-policy live (`release-checklist.md` sudah menandai ini belum dikonfirmasi) — `resources/views/public/privacy-policy.blade.php` + route baru di `routes/web.php`.

## P2 — Kesenjangan Inti Full-Stack

Bagian terbesar. Disusun 2a (risiko desain rendah, mulai duluan/paralel) lalu 2b (butuh keputusan atau memang UX baru).

### 2a

1. `[Full-stack]` **Ganti kata sandi** — port aturan validasi dari `app/Livewire/Dashboard/PasswordSettings.php` ke endpoint baru `Api/V1` (mis. `PATCH /api/v1/profile/password`), ikuti pola thin-controller+Resource `EnrollmentController.php`. Android: perluas `EditProfileScreen.kt` + `AuthRepository` + `ApiService`.
2. `[Full-stack]` **Riwayat transaksi** — port query `OrderHistory.php` (`transactions()->with('orderItems.orderable')`) ke `Api/V1/TransactionController@index` baru + Resource, paginasi persis seperti `EnrollmentController.php` (bentuk `meta`: current_page/last_page/per_page/total). Android: layar baru dari `ProfileScreen.kt`.
3. `[Android]` **Reviews-read** — render `course.reviews`/`reviewsAvgRating` (sudah di-deserialize di `Models.kt:99-133`, sudah di-cache via `CourseDetailEntity`, sudah dikembalikan backend via `CourseController@show`'s `->load(['reviews.user'])->loadAvg(...)`) di `CourseDetailScreen.kt` — dikonfirmasi langsung: layar saat ini lompat dari deskripsi ke jumlah pelajaran lalu langsung ke daftar Materi, nol bagian review. Tanpa dependency backend — kerjakan lebih dulu di P2.

### 2b

4. `[Full-stack, item terbesar]` **Paritas monetisasi chat (D1)** — mobile harus menyamai gating web: seluruh chat mentor (bukan cuma cold-outreach — dikonfirmasi `ChatConversationController::startWith` di web memanggil `abort_unless($user->hasActiveChatAccess(), 403)` tanpa pengecualian enrollment) mensyaratkan `chat_subscriptions` aktif.
   - Backend: tambahkan gate `hasActiveChatAccess()` yang sama ke `Api/V1/ConversationController@startWithMentor` dan `MessageController@store` (saat ini sengaja tidak dijaga). Tambah endpoint baru untuk trial-start/purchase (mencerminkan `/chat/trial`, `/chat/purchase` di web, kemungkinan pakai ulang alur checkout Xendit dari `CheckoutController`).
   - Android: layar paywall/upsell saat menerima 403, UI hitung-mundur trial, alur pembelian (kemungkinan pakai ulang pola `CheckoutSummaryScreen`/`PaymentSuccessScreen`), status `chat_subscriptions` ditampilkan (mis. di Profile atau daftar Chat).
   - Perlakukan sebagai mini-proyek tersendiri di dalam P2 — item roadmap tunggal terbesar.
5. `[Full-stack]` **Reviews-write** — `Api/V1/ReviewController@store` baru (`POST /courses/{course}/reviews`) dengan aturan bisnis default (A4): harus enrolled, satu review per user per course (tambahkan unique constraint, belum ada hari ini), dapat diedit dalam jangka waktu wajar. Android: UI submit rating/komentar di `CourseDetailScreen.kt`, kemungkinan digate sama seperti tombol chat-dengan-mentor (butuh `isEnrolled`).
6. `[Full-stack]` **Unduh lampiran chat** — port route download lampiran chat web ke `Api/V1` (cek dulu dukungan attachment saat ini di `MessageController`/`ChatRepository` sebelum menentukan scope pastinya).
7. `[Full-stack]` **Notifications inbox** — `Api/V1/NotificationController` baru (list/mark-read) mencakup event set default (A5): pesan baru, tugas dinilai, pengumuman, status pembayaran (tidak ada referensi UI web, jadi ini benar-benar UX baru). Android: layar baru + entry point dari bottom-nav atau Profile.

## P4 — Kesiapan Peluncuran Store

1. Isi placeholder email kontak di `listing-copy.md`.
2. Tambahkan screenshot tablet (saat ini hanya 5 screenshot ponsel).
3. Tulis Terms of Service (konten + route hosting — belum ada sama sekali; privacy policy sudah punya halaman/route, ToS belum punya keduanya).
4. Pastikan privacy-policy sudah live di production (kalau belum ditutup lewat P1.5).
5. Ikuti `store-assets/release-checklist.md` apa adanya untuk langkah Play Console (create app, kuesioner data-safety, content rating, Play App Signing, upload ke Closed/Internal testing dulu sebelum Production) — jangan duplikasi isinya di sini, cukup rujuk + catat gap di atas.

## Backlog / Masa Depan (P3 — tidak wajib untuk "selesai", per keputusan D2)

- Layar Settings: dark-mode toggle (sekaligus memperbaiki hardcoded hex color di `ProfileScreen.kt` dari P0), ganti bahasa, preferensi notifikasi, hapus akun self-service (A6 — untuk saat ini tetap proses manual).
- Forum Q&A pelajaran (model `Question` sudah ada di backend, controller belum).
- Link profil sosial/cover photo di edit profile (port `SocialSettings.php`).
- Unduhan pelajaran offline — perlu investigasi dulu (apakah `VideoStreamController` bisa aman mendukung fetch offline pre-authorized untuk konten berbayar) sebelum layak di-scope.
- Biometric app-lock.
- Push notification (FCM) — urutkan setelah notifications-inbox (P2.7) rilis, butuh model event yang sama; investasi infra terpisah (endpoint registrasi device-token + client FCM Android).
- Cart/checkout multi-item (mobile tetap buy-now-only untuk sekarang).
- **Dikecualikan bahkan dari backlog**: wishlist, sertifikat, kupon, afiliasi — nol fondasi backend (dikonfirmasi: tidak ada tabel/model/route/FDD selain satu label UI nyasar). Membangun salah satu dari ini adalah pekerjaan produk full-stack baru, bukan menyelesaikan aplikasi yang sudah ada, dan berada di luar horizon PRD ini sepenuhnya.
