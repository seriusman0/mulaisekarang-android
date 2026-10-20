# Release Notes — Mulai Sekarang v1.0.9 (versionCode 16)

**Tanggal build:** 2026-10-18
**Versi sebelumnya:** 1.0.8 (versionCode 15)
**Package:** `com.mulaisekarang.app`
**Artifact:** `app-release.aab` (Play Console) + `app-release.apk` (uji manual / sideload)

Ringkasan: rilis ini berfokus pada **pengalaman menonton video yang lebih baik** (mini player +
cache otomatis), **mode offline-first** untuk katalog, dan **pemenuhan kebijakan Google Play**.

---

## 1. Teks "What's new" untuk Play Console (Bahasa Indonesia)

Siap ditempel ke Play Console → Releases → What's new (batas 500 karakter):

```
Pembaruan 1.0.9

• Mini player video: lanjut menonton sambil menjelajah aplikasi.
• Video tetap diputar saat pindah layar, tidak terputus lagi.
• Katalog kini tersedia offline — data dimuat dari cache lebih cepat.
• Unduhan video berjalan otomatis di latar belakang.
• Chat diperbaiki: pesan terbaru selalu tampil di bawah.
• Info grup lebih stabil pada anggota dengan data tidak lengkap.
• Peningkatan stabilitas dan perbaikan bug.
```

**English (optional, untuk internal/listing `en-US`):**

```
Update 1.0.9

• Video mini player: keep watching while browsing the app.
• Video keeps playing across screens instead of restarting.
• Offline-first catalog — cached data loads faster.
• Lesson videos are cached automatically in the background.
• Chat fix: the newest message always appears at the bottom.
• More robust group info for members with incomplete data.
• Stability improvements and bug fixes.
```

---

## 2. Daftar perubahan (internal / changelog teknis)

### Fitur baru

| # | Perubahan | File utama |
|---|---|---|
| 1 | **Floating mini player** — saat pengguna keluar dari layar pelajaran sementara video masih diputar, player menyusut menjadi mini player melayang di kanan-bawah dengan tombol tutup, dan dapat dikembalikan ke layar penuh. | `ui/components/FloatingMiniPlayer.kt` (baru), `util/GlobalVideoManager.kt` (baru), `MainActivity.kt` |
| 2 | **Sesi ExoPlayer bersama (global)** — satu instance ExoPlayer dipakai lintas layar sehingga video **tidak release/restart** saat navigasi. Player lama ditutup otomatis hanya bila URL stream berbeda. | `ui/components/VideoPlayer.kt`, `util/GlobalVideoManager.kt` |
| 3 | **Offline-first catalog** — OkHttp cache disk 50 MB, respons `GET` diberi `Cache-Control: max-age=7 hari`; bila respons dilayani dari cache, aplikasi melakukan refresh jaringan di latar belakang (stale-while-revalidate). | `di/NetworkModule.kt` |
| 4 | **Cache video otomatis** — tombol "Unduh" manual dihapus; caching video kini ditangani otomatis di latar belakang. | `ui/screens/LessonPlayerScreen.kt`, `ui/components/VideoPlayer.kt` |

### Perbaikan bug

| # | Perbaikan | File utama |
|---|---|---|
| 5 | **Chat** — `LazyColumn` memakai `reverseLayout = true` dan daftar terbalik, sehingga pesan terbaru konsisten muncul di dasar layar (sebelumnya auto-scroll ke item terakhir gagal pada pesan pertama/halaman baru). | `ui/screens/ChatConversationScreen.kt` |
| 6 | **Progress upload** — penggunaan `!!` diganti safe-call (`uploadProgress ?: 0`); koneksi menggantung tidak lagi memicu crash. | `ui/screens/ChatConversationScreen.kt` |
| 7 | **Info grup** — `Participant.userId` dan `Participant.role` dibuat nullable + default; anggota dengan payload parsial tidak lagi gagal deserialisasi. Fallback "Anggota" dan nama kosong. | `data/model/Models.kt`, `ui/screens/GroupInfoScreen.kt` |
| 8 | **Bottom navigation** — item nav memakai `Box` + `clip` + `clickable` (ripple terbatas pada bentuk pill), menggantikan `Surface` yang memaksa warna permukaan. | `ui/components/GlassBottomNavBar.kt` |

### Kepatuhan & keamanan (Google Play)

| # | Perubahan | Detail |
|---|---|---|
| 9 | **Hapus fitur sideload/self-update APK** | `AppUpdateManager.downloadAndInstall()` dihapus total — melanggar kebijakan *Device and Network Abuse*. Dialog update kini **hanya** mengarahkan ke listing Play Store (`openPlayStoreListing()`). Tanpa progress bar, tanpa izin instalasi. |
| 10 | **Deteksi installer non-deprecated** | `PackageManager.getInstallerPackageName()` (deprecated API 30) diganti `getInstallSourceInfo().installingPackageName` dengan fallback untuk Android < 11 dan `try/catch` agar `SecurityException` tidak mematikan app. |
| 11 | **Aturan backup eksplisit** | `android:fullBackupContent="@xml/backup_rules"` (≤ Android 11) dan `android:dataExtractionRules="@xml/data_extraction_rules"` (Android 12+) — mengecualikan token auth (DataStore) dan file Room DB agar sesi tidak dipulihkan di perangkat lain. |
| 11a | **Perbaikan aturan backup (build blocker)** | Kedua file di atas memakai `domain="cache"`, yang bukan domain sah menurut skema Android (`file`, `database`, `sharedpref`, `external`, `root`, `device_*`) sehingga `lintVitalRelease` gagal dan build release tidak pernah selesai. Entri tidak valid dihapus — cache OkHttp memang tidak pernah ikut di-backup/ditransfer. |
| 12 | **Font/atribut ProGuard diperluas** | Keep rules untuk Hilt/Dagger (`@HiltViewModel`, `@AndroidEntryPoint`, `@Inject`), Pusher Java Client (reflection), OkHttp/Okio, serta `SourceFile`/`LineNumberTable` agar stack trace Crashlytics terbaca. |
| 13 | **Dialog update dilokalisasi** | "Update Available" → "Update Tersedia", "Later" → "Nanti", "Update" → "Perbarui", dengan copy yang menyebut Google Play Store. |

---

## 3. Detail versi

| Field | Sebelum | Sesudah |
|---|---|---|
| `versionCode` | 15 | **16** |
| `versionName` | 1.0.8 | **1.0.9** |
| `minSdk` | 26 | 26 |
| `targetSdk` | 36 | 36 |
| `compileSdk` | 36 | 36 |

Perubahan hanya pada `defaultConfig` di `app/build.gradle.kts`; konfigurasi SDK tidak berubah.

### Artifact hasil build (terverifikasi)

| Artifact | Path | Ukuran | Status |
|---|---|---|---|
| Bundle (Play Console) | `app/build/outputs/bundle/release/app-release.aab` | 8.59 MB | Signed `CN=Mulai Sekarang`, SHA-1 `B7:C3:73:0B:...:4C:23` |
| APK (uji manual) | `app/build/outputs/apk/release/app-release.apk` | 4.11 MB | Signed, `apksigner verify` lulus |

Verifikasi `aapt2 dump badging` pada APK:
`package name='com.mulaisekarang.app' versionCode='16' versionName='1.0.9'`, `minSdkVersion:'26'`, `targetSdkVersion:'36'`.
Manifest AAB juga terbaca `com.mulaisekarang.app` / `1.0.9`.
Build: `BUILD SUCCESSFUL` — `lintVitalRelease`, `bundleRelease`, dan `assembleRelease` semuanya lulus.

---

## 4. Catatan rilis / operasional

1. **Version endpoint backend** — dialog update dalam aplikasi membaca
   `release_notes`, `version_name`, dan `version_code` dari endpoint versi backend.
   Agar pengguna versi lama melihat catatan rilis di atas saat diminta update,
   perbarui sesuai nilai barunya (`version_code: 16`, `version_name: "1.0.9"`).
2. **Upload** — unggah `app-release.aab` ke track **Internal/Closed testing** lebih dulu,
   verifikasi, lalu promosikan ke Production. `versionCode` 16 > 15 sehingga Play Console menerima unggahan.
3. **Signing** — APK/AAB ditandatangani dengan upload key dari `keystore.properties`
   (`mulaisekarang-release.jks`). Simpan keystore & password di luar mesin ini.
4. **Mapping file** — `app/build/outputs/mapping/release/mapping.txt` harus diunggah
   ke Play Console (atau konsisten dengan Crashlytics) untuk setiap rilis agar crash deobfuscate.

---

## 5. Checklist uji manual (smoke test)

- [ ] Buka pelajaran → video diputar → tekan Back → mini player muncul dan tetap berjalan.
- [ ] Ketuk mini player / tombol tutup berfungsi; buka pelajaran lain → player lama tertutup, video baru diputar.
- [ ] Putar video ke tengah → buka aplikasi lain → kembali → posisi lanjut (bukan dari 0).
- [ ] Matikan data → buka katalog → daftar kursus tetap tampil dari cache.
- [ ] Buka chat grup → kirim pesan → pesan baru muncul di bawah dan terlihat.
- [ ] Buka Info grup dengan anggota tanpa nama → tampil "User" / "Anggota", tidak crash.
- [ ] Hapus data aplikasi → login Google → berhasil (SHA-1 release terdaftar).
- [ ] Install dari Play Store → tidak ada dialog update in-app (Play yang menangani).
