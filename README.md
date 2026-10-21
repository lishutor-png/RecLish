# RecLish - Browser Audio Recorder

**RecLish** adalah aplikasi Android canggih untuk merekam suara dari browser (seperti **Google Chrome**, Firefox, Brave) dengan tombol kontrol mengambang (*floating overlay*), perekaman di latar belakang, penyimpanan langsung ke memori HP dalam format **MP3**, dan fitur manajemen berkas sederhana.

![RecLish Logo](app/src/main/res/drawable/ic_reclish_logo.jpg)

---

## 🚀 Fitur Utama

1. **Merekam Suara dari Google Chrome & Browser Lain**
   - **Internal Audio Capture (Android 10+ / API 29+)**: Menangkap audio digital internal langsung dari tab browser Chrome tanpa menangkap suara bising sekitar.
   - **Mikrofon & Komentar**: Pilihan alternatif untuk merekam suara speaker dan narasi pengguna.
   - **Tab Studio Web Terintegrasi**: Browser bawaan dalam aplikasi dengan pintasan audio populer (SoundHelix, SoundCloud, Radio Garden, dll.).

2. **Berjalan di Atas Aplikasi Lain (*Floating Overlay Mode*)**
   - Bubble kontrol mengambang minimalis dengan desain tombol rekam flat.
   - Dapat digeser (*draggable*) ke mana saja di layar saat membuka Google Chrome.
   - Mulai, Jeda, Lanjut, dan Simpan rekaman tanpa perlu keluar dari tab Chrome.

3. **Bekerja di Latar Belakang (*Background Foreground Service*)**
   - Perekaman tidak terhenti saat layar berpindah atau saat browsing di aplikasi lain.
   - Notifikasi latar belakang yang hening dan tidak mengganggu.

4. **Penyimpanan Langsung ke Memori HP & Format MP3**
   - File tersimpan di `Music/BrowserRecordings` pada penyimpanan internal perangkat.
   - Terdaftar otomatis di `MediaStore` Android, langsung muncul di Google Files, Samsung My Files, dan aplikasi pemutar musik.
   - Mendukung format **MP3**, **WAV**, dan **M4A**.

5. **Manajemen File Sederhana**
   - Daftar rekaman dengan pencarian, filter kategori, dan pengurutan (Terbaru, Nama, Durasi, Ukuran).
   - Pemutar audio terintegrasi dengan *waveform scrubber* dan kontrol kecepatan (0.75x–2.0x).
   - Fitur Ganti Nama, Hapus, Bagikan (*Share* ke WhatsApp/Drive), dan Ekspor ke folder Download.

---

## 🛠️ Build di GitHub (GitHub Actions CI/CD)

Proyek ini telah dikonfigurasi untuk langsung dapat di-build di GitHub melalui **GitHub Actions**.

### Cara Build & Download APK dari GitHub:
1. Push repositori ini ke GitHub Anda (`git push origin main`).
2. Masuk ke tab **Actions** di repositori GitHub Anda.
3. Alur kerja **"Build RecLish Android APK"** (`.github/workflows/android.yml`) akan otomatis berjalan.
4. Setelah selesai, download file APK siap pakai dari bagian **Artifacts** (`RecLish-debug-apk`).

### Build Lokal:
```bash
# Pastikan Java 17 atau lebih baru terpasang
./gradlew assembleDebug

# Hasil APK berada di:
# app/build/outputs/apk/debug/app-debug.apk
```
