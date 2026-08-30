# Cost-saving workflow: delegate heavy work to agy

Goal: hemat per-request (Claude turns) dan hemat per-token (context/output Claude).

- Task besar/berat (implementasi fitur besar, refactor lintas banyak file, analisis/riset panjang) → JANGAN dikerjakan langsung oleh Claude. Delegasikan ke local CLI `agy` (Google Antigravity, terinstall di `C:\Users\Krisman\AppData\Local\agy\bin\agy`).
- Panggil via Bash tool: `agy --dangerously-skip-permissions "<instruksi lengkap self-contained>"`.
- Instruksi ke agy harus lengkap (file path, konteks, tujuan) — agy tidak lihat percakapan ini.
- Claude tetap pegang: planning singkat, orkestrasi, review hasil akhir (baca diff/output agy, verifikasi benar), dan komunikasi ke user.
- Task kecil/quick fix (1-2 file, perubahan sempit, typo, config kecil) tetap dikerjakan Claude langsung — jangan delegasi hal remeh, overhead invoke agy lebih mahal dari kerjanya.
- Jangan panggil agy berkali-kali untuk sub-langkah kecil dari satu task besar — gabung jadi satu instruksi agy per task (hemat per-request juga berlaku ke agy).
