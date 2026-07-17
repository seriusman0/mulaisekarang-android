PRD: Menyelesaikan mulaisekarang-android Secara Menyeluruh
Status: Draft
Fokus Area: Ringkasan Eksekutif, Definisi Selesai, Keputusan, Peta Fase
Dokumen Terkait: 01-roadmap.md (rincian eksekusi), 02-gap-analysis.md (tabel referensi), best_practice.md (arsitektur data layer, slice lama)

1. Ringkasan Eksekutif

`mulaisekarang-android` adalah aplikasi Jetpack Compose untuk pelajar (learner-only), belum pernah dirilis (versionCode=1, versionName="1.0.0"), klien untuk backend Laravel LMS/mentoring `mulaisekarang` (repo sibling, live di v2.mulaisekarang.com). PRD ini disusun untuk menjawab permintaan "menyelesaikan seutuhnya" proyek, diselaraskan dengan backend.

Angka kunci hasil audit: 19 layar, 0 unit/instrumented test, 0 CI, 0 crash reporting/analytics, 0 push notification. Backend punya model data jauh lebih luas dari yang dikonsumsi mobile (lihat 02-gap-analysis.md). Persiapan rilis Play Store sebenarnya sudah cukup jauh (keystore + SHA-1 OAuth sudah terdaftar, store-assets sebagian besar sudah ada) — yang tertinggal justru penyelesaian kode & kesenjangan fitur, bukan admin console.

Riset sesi ini mencakup: seluruh file viewmodel/repository/screen/local-db Android, seluruh `routes/web.php` + `routes/api.php` + `database/migrations/` (55 file) + `config/services.php` backend, seluruh 22 dokumen FDD di `fdd/`, dan verifikasi langsung untuk klaim-klaim berisiko tinggi (bug silent-failure di `ChatConversationViewModel.kt:80-84`, `CourseDetailScreen.kt` benar-benar nol render review, pola `EnrollmentController.php`, isi `release-checklist.md`). Fetch situs live (v2.mulaisekarang.com) gagal (HTTP 403, diblokir WAF) — pemahaman realita backend bersandar sepenuhnya pada repo Laravel lokal, yang menurut README-nya sendiri persis yang dipakai build `release`.

2. Definisi "Selesai"

"Completely finished" untuk rilis pertama ini berarti: **P0 + P1 + P2 + P4 selesai dan terverifikasi**. P3 (nice-to-have) didokumentasikan sebagai backlog eksplisit di 01-roadmap.md, bukan syarat kelulusan rilis pertama.

3. Keputusan Terkonfirmasi (disetujui user pada sesi ini)

- **D1 — Monetisasi chat mobile: disamakan dengan web.** Web menggerbangi (gate) 100% chat mentor lewat `chat_subscriptions` (Rp30-50k, tanpa pengecualian enrollment) — mobile saat ini bebas gerbang sepenuhnya (keputusan lama, sebelum detail ini terpetakan penuh). Keputusan baru: mobile mengikuti gating identik web. Ini menjadi item full-stack terbesar di roadmap (lihat 01-roadmap.md P2.4).
- **D2 — Definisi selesai: P0+P1+P2+P4.** P3 = backlog, tidak wajib untuk rilis pertama.

4. Asumsi Kerja Default (didokumentasikan, boleh dikoreksi saat eksekusi — bukan diam-diam mengubah scope)

- **A1 — Ikon adaptif:** direstorasi (vector adaptive-icon + Android 13+ themed icon), bukan menerima flat-PNG yang sedang berjalan di working tree saat ini.
- **A2 — Sistem desain Glass:** direstorasi konsisten di `GlassBottomNavBar` (saat ini `GlassCard`/`GlassBackground` sudah nol call-site di working tree).
- **A3 — Vendor crash reporting:** Firebase Crashlytics (selaras proyek Google Cloud yang sudah dipakai OAuth); perlu revisi `data-safety.md` sebelum submit Play Console berikutnya.
- **A4 — Aturan bisnis review:** harus enrolled di course, satu review per user per course (tabel `reviews` belum ada unique constraint — perlu ditambah), dapat diedit dalam jangka waktu wajar.
- **A5 — Event notifikasi inbox:** pesan baru, tugas dinilai, pengumuman, status pembayaran.
- **A6 — Hapus akun:** tetap proses manual/eksternal yang sudah didokumentasikan di `data-safety.md` untuk saat ini; self-service in-app masuk backlog P3.

5. Peta Indeks Fase

| Fase | Tujuan | Ketergantungan Backend |
|---|---|---|
| P0 — Stabilisasi | Perbaiki bug diam, regresi, dokumentasi basi | Android-only |
| P1 — Fondasi Produksi | Test, CI, crash reporting, email nyata, privacy-policy live | Mixed (mayoritas Android, 2 item backend-only) |
| P2 — Kesenjangan Inti Full-Stack | Ganti sandi, riwayat transaksi, reviews, paritas monetisasi chat, lampiran, notifikasi | Mixed — bagian terbesar, mayoritas full-stack |
| P3 — Backlog (tidak wajib) | Settings, Q&A, offline download, biometric, push, dll | Mixed |
| P4 — Kesiapan Peluncuran Store | Listing copy, screenshot tablet, ToS, konfirmasi privacy-policy live, walk release-checklist | Backend-only (2 item) + admin console |

6. Risiko

- `MAIL_MAILER=log` membuat alur lupa-password yang sudah lengkap di UI Android diam-diam tidak berfungsi di lingkungan nyata manapun hari ini — bug tersembunyi paling serius di seluruh audit, bukan sekadar "infra default belum diisi."
- Belum ada basis-instal produksi (belum pernah rilis) — perubahan API sekarang berisiko rendah terhadap pengguna nyata, tapi disiplin versioning (`versionCode` naik tiap upload) mulai penting setelah rilis pertama.
- Menambah Crashlytics = data flow pihak ketiga baru → perlu tinjau ulang `data-safety.md` sebelum submit Play Console berikutnya.
- Live-site fetch diblokir 403 pada sesi ini — pemahaman backend bersandar penuh pada repo lokal, bukan observasi langsung produksi.

7. Lampiran

- `prd/best_practice.md` — PRD arsitektur data-layer (slice lama, tetap berlaku).
- `store-assets/release-checklist.md` — checklist Play Console, dirujuk dari P4, tidak diduplikasi.
- `../mulaisekarang/fdd/` — 22 dokumen spesifikasi fitur backend (indeks di 02-gap-analysis.md).
