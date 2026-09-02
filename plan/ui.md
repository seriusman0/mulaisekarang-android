# Role
Bertindaklah sebagai Senior Android Developer & UI/UX Designer yang spesialis dalam optimasi performa untuk perangkat spesifikasi rendah (low-end devices).

# Context Proyek
- **Aplikasi:** Mulai Sekarang.
- **Target User:** Mayoritas menggunakan HP Android spesifikasi rendah (RAM kecil, CPU lambat).
- **Gaya Desain Target:** Minimalis, Cepat, dengan sentuhan "Light Glassmorphism" (efek kaca tanpa blur berat yang membebani GPU).

# Tugas 1: Optimasi & Update UI (Global)
Lakukan refactoring UI pada halaman utama dan daftar course dengan prinsip berikut:
1. **Ringan:** Ganti semua aset gambar statis ke format WebP. Pastikan tidak ada nested scrolling yang berlebihan.
2. **Glassmorphism Efisien:** Terapkan efek kaca menggunakan transparansi warna (alpha channel) dan border tipis, HINDARI real-time blur effect yang berat.
3. **Responsivitas:** Pastikan animasi transisi sederhana (fade/slide) dan tidak menyebabkan jank saat scrolling.

# Tugas 2: Implementasi Fitur Voucher Checkout
Tambahkan fitur input voucher pada halaman Checkout/Pembayaran sebelum user melakukan payment.
Pastikan kamu pelajari dulu di backend, di server atau vps untuk hal ini. kamu bisa akses melelui ssh ykde@100.101.150.38 dengan password root 123456 dan projectnya ada di ~/mulaisekarang

## Spesifikasi UI Voucher:
- Buat komponen input field yang terintegrasi dengan desain Glassmorphism di atas.
- Tambahkan tombol "Terapkan" di sebelah input field.
- Tampilkan feedback visual yang jelas:
  - **Sukses:** Tampilkan harga baru yang dicoret (harga lama) dan harga akhir (harga diskon) dengan warna hijau/tegas.
  - **Gagal:** Tampilkan pesan error singkat jika kode tidak valid atau expired.

## Spesifikasi Logika Backend/API:
- Buat fungsi `applyVoucher(code: String, courseId: String)` yang memanggil API.
- Handle state loading (tampilkan spinner/loading ringan saat menunggu respon API).
- Validasi sisi klien: Pastikan input tidak kosong sebelum mengirim request.

# Output yang Diharapkan
1. **Kode UI:** Snippet kode untuk komponen Card Course yang sudah di-optimize dan komponen Input Voucher dengan gaya Glassmorphism ringan.
2. **Logika State:** Contoh implementasi ViewModel/State Management untuk menangani proses apply voucher (Loading, Success, Error).
3. **Tips Performa:** 3 poin spesifik apa yang harus dihindari di kode ini agar tetap lancar di HP low-end.

# Catatan Penting
Fokus utama adalah **KECEPATAN LOAD** dan **KEHALUSAN SCROLLING**. Jangan mengorbankan performa demi estetika yang berlebihan.