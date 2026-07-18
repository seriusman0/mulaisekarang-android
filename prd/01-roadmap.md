PRD: Menyelesaikan mulaisekarang-android Secara Menyeluruh — Roadmap
Status: Draft
Fokus Area: Rincian Eksekusi per Fase (P0–P4) + Backlog P3
Dokumen Terkait: 00-overview.md (ringkasan & keputusan), 02-gap-analysis.md (tabel referensi)

Tag per item: `[Android]` hanya sisi Android, `[Backend-only]` hanya sisi Laravel, `[Full-stack]` keduanya.

## P0 — Stabilisasi

Semua Android-only, tanpa ketergantungan backend. Dikerjakan lebih dulu karena murah dan menurunkan risiko semua fase berikutnya.

1. ✅ **Selesaikan diff belum-commit yang sedang berjalan** — icon rebrand (`drawable-nodpi/`, `mipmap-nodpi/`), `ProfileScreen.kt`, `GlassBottomNavBar.kt`, `LoginScreen.kt`/`SplashScreen.kt`. Dikonsolidasikan bareng item 4 & 5 di commit `P0.1/P0.4/P0.5`.
2. ✅ **Perbaiki 5 silent-failure bug** (pola `.onSuccess{}` tanpa `.onFailure{}` sama sekali) — commit `P0.2`:
   - `ChatConversationViewModel.sendMessage` — sekarang emit `sendError` SharedFlow -> snackbar di `ChatConversationScreen`.
   - `LessonPlayerViewModel.markComplete` — `LessonPlayerEvent.Error` baru -> snackbar di `LessonPlayerScreen`.
   - `CourseDetailViewModel.checkPendingPayment` — pakai ulang `CheckoutEvent.Error` yang sudah ada -> snackbar.
   - `CourseDetailViewModel.startConversationWithMentor` — pakai ulang `CheckoutEvent.Error`, `onResult(null)` tetap dipanggil.
   - `MyCoursesViewModel` (recommended-courses fetch) — no-op eksplisit + komentar (non-kritis, tidak boleh menimpa error utama).
3. ✅ **Tambahkan `GoogleAuthClient.signOut()`**/pembersihan credential-state, panggil dari `AuthRepository.logout()` — commit `P0.3`.
4. ✅ **Perbaiki regresi ikon adaptif** (A1) — `mipmap-anydpi-v26/ic_launcher.xml` + vector background + foreground (inset raster logo baru) + `monochrome` Android 13+, dikembalikan di commit `P0.1/P0.4/P0.5`.
5. ✅ **Selesaikan status sistem Glass** (A2) — `GlassBottomNavBar.kt` kembali pakai `GlassBackground`, dikembalikan di commit `P0.1/P0.4/P0.5`.
6. ✅ **Tulis ulang bagian Arsitektur di `README.md`** — commit `P0.6`, klaim `AppContainer` basi diganti deskripsi Hilt yang benar + catatan cakupan Room.

**P0 selesai seluruhnya (2026-07-17).**

## P1 — Fondasi Produksi

1. ✅ (sebagian) `[Android]` Unit test ViewModel — commit `P1.1`, 5 test meng-cover tepat jalur repository-failure → error-state yang diperbaiki P0.2 (`ChatConversationViewModel`, `LessonPlayerViewModel`, `MyCoursesViewModel`). Test DAO Room & Compose test **ditunda** — keduanya butuh `androidTest` di device/emulator fisik, tidak tersedia di lingkungan eksekusi sesi ini (`adb devices` kosong). Jalankan `./gradlew :app:testDebugUnitTest`.
2. ✅ `[Android]` CI baru — commit `P1.2`, `.github/workflows/android-ci.yml`: `lintDebug` + `testDebugUnitTest` + `assembleDebug` di setiap PR/push ke `main`. Menjalankan `lintDebug` lokal untuk validasi menemukan 1 error nyata pra-eksisting (`VideoPlayer.kt` pakai API `UnstableApi` Media3 tanpa opt-in) — sudah diperbaiki di commit yang sama.
3. 🟡 (scaffolding selesai, blocked on Firebase Console) `[Android]` Crash reporting minimum viable — Firebase Crashlytics (A3), commit `P1.3`. Plugin + dependency terpasang, aktif hanya jika `app/google-services.json` ada (pola sama seperti `keystore.properties`) — file itu sendiri perlu dibuat manual di Firebase Console (project GCP yang sama dengan OAuth Google Sign-In), baru lanjut wiring `Application.onCreate()` + uncaught-exception handler.
4. `[Backend-only]` Ganti `MAIL_MAILER=log` ke pengiriman email nyata (SMTP/Postmark/SES) — **prioritas tertinggi** di seluruh audit: alur lupa-password/reset-password sudah lengkap di UI Android (`ForgotPasswordScreen`/`ResetPasswordScreen`) tapi diam-diam tidak berfungsi di lingkungan nyata manapun hari ini.
5. `[Backend-only]` Pastikan/deploy route privacy-policy live (`release-checklist.md` sudah menandai ini belum dikonfirmasi) — `resources/views/public/privacy-policy.blade.php` + route baru di `routes/web.php`.

## P2 — Kesenjangan Inti Full-Stack

Bagian terbesar. Disusun 2a (risiko desain rendah, mulai duluan/paralel) lalu 2b (butuh keputusan atau memang UX baru).

### 2a

1. ✅ `[Full-stack]` **Ganti kata sandi** — commit `P2a.1`. Backend: `PasswordController@update` baru (`PATCH /api/v1/profile/password`, gaya thin-controller `EnrollmentController.php`), validasi diport dari `PasswordSettings.php` (`current_password` + `Hash::check`, `new_password` min:8|confirmed). Android: `ApiService.changePassword` + `AuthRepository.changePassword` + `AuthViewModel.changePassword`/`ChangePasswordEvent` + form baru di `EditProfileScreen.kt`.
2. `[Full-stack]` **Riwayat transaksi** — port query `OrderHistory.php` (`transactions()->with('orderItems.orderable')`) ke `Api/V1/TransactionController@index` baru + Resource, paginasi persis seperti `EnrollmentController.php` (bentuk `meta`: current_page/last_page/per_page/total). Android: layar baru dari `ProfileScreen.kt`.
3. ✅ `[Android]` **Reviews-read** — commit `P2a.3`. Bagian "Ulasan" (rata-rata + jumlah + `ReviewCard` per ulasan) ditambahkan setelah daftar Materi di `CourseDetailScreen.kt`.

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
