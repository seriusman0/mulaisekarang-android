PRD: Implementasi Data Layer Android Native - Mulai Sekarang LMS
Status: Draft
Fokus Area: Client-Side Data Management & Offline-First Architecture

1. Tujuan (Objective)
Membangun lapisan data aplikasi Android yang tangguh dan responsif, memastikan pengguna dapat mengakses konten secara instan tanpa terhambat (blocking) oleh proses pemuatan jaringan. Aplikasi harus tetap fungsional dan mulus meskipun koneksi internet lambat atau data sedang disinkronisasi dari backend.

2. Arsitektur & Teknologi Utama
Sistem akan dibangun menggunakan standar arsitektur modern Android (MVVM) dengan prinsip Single Source of Truth (SSOT).

Pola Arsitektur: Model-View-ViewModel (MVVM) dengan Repository Pattern.

Komunikasi Jaringan: Retrofit (dengan OkHttp interceptor).

Penyimpanan Lokal (SSOT): Room Database (SQLite).

Pemrosesan Asinkron: Kotlin Coroutines & Flow.

Dependency Injection: Dagger Hilt (untuk efisiensi inisialisasi).

Image Caching: Coil atau Glide.

Pagination: Paging 3 Library.

3. Kebutuhan Fungsional (Functional Requirements)
3.1. Alur Sinkronisasi Data (Pola SSOT)
Semua modul yang menampilkan daftar data dinamis (seperti Daftar Kursus, Riwayat Chat, atau Profil) harus mematuhi alur berikut:

Observasi UI: Layar mengobservasi state dari ViewModel melalui StateFlow.

Pengambilan Instan: Saat layar dimuat, Repository langsung memancarkan (emit) data yang ada di Room Database ke UI. Layar terisi tanpa jeda waktu jaringan.

Sinkronisasi Latar Belakang: Secara paralel, Repository memicu pemanggilan API Retrofit di Dispatchers.IO untuk mengambil data terbaru dari backend.

Pembaruan Lokal: Respons dari API tidak boleh langsung dikirim ke UI. Respons tersebut wajib di-insert/update ke dalam Room Database.

Reaksi Otomatis: Room Database (yang dikembalikan sebagai Flow) akan otomatis memancarkan data terbaru ke UI saat ada perubahan di tingkat tabel.

3.2. Penanganan State UI & Asynchronous Programming
Non-Blocking UI: Tidak boleh ada operasi jaringan atau database yang berjalan di Main Thread.

Indikator Pemuatan: Selama pemanggilan API berlangsung di latar belakang, UI harus menampilkan Skeleton Loading atau indikator non-intrusif pada elemen tertentu, tanpa mengunci scroll atau interaksi pengguna.

3.3. Manajemen Sumber Daya & Stabilitas (Resource Management)
Lifecycle Awareness: Semua Coroutines harus diluncurkan menggunakan viewModelScope (di level ViewModel) atau lifecycleScope (di level UI) agar operasi jaringan dibatalkan otomatis jika pengguna menutup layar/aplikasi.

Efisiensi Memori (DI): Komponen berat seperti instance Room Database dan Retrofit klien harus dideklarasikan sebagai @Singleton menggunakan Dagger Hilt.

Manajemen Aset Gambar: Semua gambar yang diunduh dari server harus diproses oleh library image loader (seperti Coil) yang secara otomatis menangani caching di disk dan RAM lokal. Gambar yang sama dilarang diunduh dua kali.

Pemuatan Bertahap (Pagination): Untuk modul dengan data masif, implementasikan Paging 3. Data harus dimuat per 10-20 item secara asinkron saat pengguna melakukan scroll mendekati batas bawah layar.

4. Kebutuhan Integrasi Backend
Mengingat backend Anda di-deploy langsung pada lingkungan lokal non-Docker, endpoint API yang melayani aplikasi Android ini perlu mendukung beberapa mekanisme berikut agar sinkronisasi SSOT berjalan optimal:

Versioning/Timestamp: Setiap entitas data (misalnya objek "Kursus") idealnya memiliki field updated_at. Ini membantu klien Android memutuskan apakah data di Room perlu ditimpa atau tidak.

Pagination Parameters: API harus mendukung parameter page dan limit (atau penanda kursor) untuk mendukung fungsionalitas Paging 3 di aplikasi mobile.
