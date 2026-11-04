# Release Notes — Mulai Sekarang v1.0.12 (versionCode 100)

**Tanggal build:** 2026-11-03
**Versi sebelumnya:** 1.0.10 (versionCode 17)
**Package:** `com.mulaisekarang.app`
**Artifact:** `app-release.aab` (Play Console) + `app-release.apk` (uji manual / sideload)

Catatan: Play Console menolak versionCode 18 dan 19 (sudah terpakai), jadi rilis ini memakai
**versionCode 100** secara sengaja agar tidak bentrok lagi.

Ringkasan: rilis ini mengaktifkan kembali **R8 (minify + shrink resources)** sehingga: (1) ukuran
bundle turun ~50%, dan (2) **deobfuscation file (`mapping.txt`) tersedia** untuk dianalisis
Play Console/Crashlytics.

---

## 1. Teks "What's new" untuk Play Console (Bahasa Indonesia)

```
Pembaruan 1.0.12

• Ukuran aplikasi lebih kecil (optimasi R8).
• Peningkatan stabilitas dan perbaikan bug.
• Peningkatan performa aplikasi.
```

**English (optional):**

```
Update 1.0.12

• Smaller app size (R8 optimization).
• Stability improvements and bug fixes.
• Performance improvements.
```

---

## 2. Detail versi

| Field | Sebelum | Sesudah |
|---|---|---|
| `versionCode` | 17 | **100** |
| `versionName` | 1.0.10 | **1.0.12** |
| `minSdk` | 26 | 26 |
| `targetSdk` | 36 | 36 |
| `compileSdk` | 36 | 36 |
| `isMinifyEnabled` | false | **true** |
| `isShrinkResources` | false | **true** |

### Artifact hasil build (terverifikasi)

| Artifact | Path | Ukuran | Status |
|---|---|---|---|
| Bundle (Play Console) | `app/build/outputs/bundle/release/app-release.aab` | 8.93 MB (turun dari 18.22 MB) | Signed |
| Deobfuscation file | `app/build/outputs/mapping/release/mapping.txt` | 67 MB | Untuk diunggah ke Play Console |
| APK (uji manual) | `app/build/outputs/apk/release/app-release.apk` | ±4.28 MB | Signed |

Verifikasi manifest: `versionCode="100"`, `versionName="1.0.12"`.
Build: `BUILD SUCCESSFUL` — `minifyReleaseWithR8`, `shrinkBundleReleaseResources`, `bundleRelease`
dan `uploadCrashlyticsMappingFileRelease` semua lulus.

---

## 3. Menangani 2 warning Play Console

### Warning "no deobfuscation file" — SELESAI
Penyebab: sebelumnya R8 dimatikan (`isMinifyEnabled = false`) untuk mengakali error build, sehingga
`mapping.txt` tidak pernah dibuat dan tidak bisa diunggah.
Perbaikan: R8 diaktifkan kembali (akar masalah build lama: `local.properties` salah path — sudah
diperbaiki; heap Gradle juga dinaikkan ke 4 GB agar daemon tidak OOM saat R8).
`mapping.txt` kini tersedia.

### Warning "native code tanpa debug symbols" — TIDAK ADA YANG BISA DIUNGGAH
Bundle berisi 2 library native prebuilt dari Google (`libandroidx.graphics.path.so`,
`libdatastore_shared_counter.so`). File aslinya sudah **di-strip di upstream** (ukuran hanya 4–10 KB),
jadi tidak ada symbol yang bisa diekstrak. Proyek ini tidak punya kode native buatan sendiri maupun
build NDK. `debugSymbolLevel = "FULL"` dan `ndkVersion = "28.2.13676358"` sudah diset.
Warning ini bersifat saran dan aman untuk diabaikan.

---

## 4. Catatan rilis / operasional

1. Upload `app-release.aab` ke track **Internal/Closed testing** dulu, lalu promosikan ke Production
   (`versionCode` 100 > 17).
2. Upload `mapping.txt` ke Play Console (App bundle explorer → Deobfuscation file) agar ANR/crash
   terdeobfuscate. Crashlytics juga sudah mengunggahnya otomatis saat build.
3. Simpan keystore & password di luar mesin ini.
4. `ndkVersion` dan `org.gradle.jvmargs` (4 GB) sudah diubah — jangan turunkan heap atau R8 akan OOM lagi.

---

## 5. Checklist uji manual (smoke test)

- [ ] Install APK release (R8-obfuscated) → aplikasi terbuka tanpa crash.
- [ ] Login Google → berhasil.
- [ ] Buka katalog/kursus → data termuat (serialization tidak dirusak R8).
- [ ] Buka pelajaran → video diputar.
- [ ] Buka chat → kirim/terima pesan.
- [ ] Tidak ada `ClassNotFoundException`/`NoSuchMethodError` di logcat saat navigasi antar layar.