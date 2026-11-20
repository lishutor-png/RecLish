# RecLish - Browser Audio Recorder

**RecLish** adalah aplikasi Android canggih untuk merekam suara dari browser (seperti **Google Chrome**, Firefox, Brave) dengan tombol kontrol mengambang (*floating overlay*), perekaman di latar belakang, penyimpanan langsung ke memori HP dalam format **MP3**, dan fitur manajemen berkas sederhana.

![RecLish Logo](app/src/main/res/drawable/ic_reclish_logo.jpg)

---

## 🚀 Fitur Utama

1. **Merekam Suara dari Google Chrome & Browser Lain**
   - **Internal Audio Capture (Android 10+ / API 29+)**: Menangkap audio digital internal langsung dari tab browser Chrome tanpa menangkap suara bising sekitar.
   - **Mengikuti Volume Media HP**: Tingkat kenyaringan rekaman otomatis menyesuaikan volume media perangkat dengan penguatan (*boost*) dan *limiter* anti-pecah.
   - **Tab Studio Web Terintegrasi**: Halaman awal bersih tanpa file audio bawaan, dengan pintasan 1-tap ke **Google Chrome (Bubble Siaga)** dan kolom URL manual.

2. **Berjalan di Atas Aplikasi Lain (*Floating Overlay Mode*)**
   - Bubble kontrol mengambang minimalis dengan desain tombol rekam flat dan mode **Siaga** (tidak langsung merekam saat bubble terbuka).
   - Dapat digeser (*draggable*) ke mana saja di layar saat membuka Google Chrome.
   - Mulai, Jeda, Lanjut, **Batal (`✕ Batal`)**, dan Simpan rekaman tanpa perlu keluar dari tab Chrome.

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
2. Masuk ke tab **Actions** dan tunggu workflow **"Build RecLish Android APK"** selesai (centang hijau).
3. **Cara Termudah (Unduh Langsung `.apk`)**: Buka halaman **Releases** di sisi kanan repositori GitHub Anda (tag `latest`), lalu klik **`RecLish-release.apk`** atau **`RecLish-debug.apk`** di bagian **Assets**.
4. **Alternatif via Actions**: Masuk ke workflow run yang selesai lalu unduh dari bagian **Artifacts** (`RecLish-release-apk` / `RecLish-debug-apk`, berbentuk `.zip` yang perlu diekstrak).

### Build Lokal:
```bash
# Pastikan JDK 21 (atau minimal JDK 17) terpasang
./gradlew testDebugUnitTest --no-configuration-cache
./gradlew assembleDebug --no-configuration-cache

# Hasil APK berada di:
# app/build/outputs/apk/debug/app-debug.apk
```
