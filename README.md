# Xichi — Android Companion App for ESP32

[![GitHub release](https://img.shields.io/github/v/release/Franklnir/Chronchi?include_prereleases)](https://github.com/Franklnir/Chronchi/releases/latest)
![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)

**Xichi** adalah aplikasi pendamping Android yang menghubungkan ponsel Anda dengan perangkat ESP32 secara mulus. Aplikasi ini menangani penerusan notifikasi pilihan, instruksi navigasi Google Maps, status GPS, cuaca, serta status ponsel/jaringan melalui protokol Bluetooth LE yang aman dan efisien.

## 📱 Download Aplikasi Android (Xichi Companion App)

Scan QR Code Barcode di bawah ini menggunakan kamera ponsel atau aplikasi pemindai QR untuk langsung mengunduh file APK versi terbaru (`Xichi-v1.4.0-debug.apk`):

<p align="center">
  <img src="https://api.qrserver.com/v1/create-qr-code/?size=300x300&margin=10&ecc=M&data=https%3A%2F%2Fgithub.com%2FFranklnir%2FChronchi%2Freleases%2Fdownload%2Fv1.4.0%2FXichi-v1.4.0-debug.apk" alt="QR Code Barcode Download Xichi APK" width="260" height="260" />
</p>

### 🔗 Link Unduhan APK:
- 🚀 **[Klik di Sini untuk Mengunduh Langsung APK v1.4.0](https://github.com/Franklnir/Chronchi/releases/download/v1.4.0/Xichi-v1.4.0-debug.apk)** *(Ukuran file: ~44.3 MB)*
- 🌐 **[Mirror Unduh Versi Rilis Terbaru (GitHub Latest)](https://github.com/Franklnir/Chronchi/releases/latest/download/Xichi-v1.4.0-debug.apk)**
- 📦 **[Daftar Semua Rilis & Catatan Pembaruan](https://github.com/Franklnir/Chronchi/releases)**

## ✨ Fitur Utama

- **Desain Illustrative Sketch**: Antarmuka modern dengan gaya "sketch" yang bersih, konsisten, dan memiliki identitas visual yang kuat.
- **Offline First**: Mampu meneruskan notifikasi melalui Bluetooth LE bahkan tanpa koneksi internet.
- **Dynamic Theming**: Pilihan tema antara "Sketch Illustrative" dan "Comic Retro".
- **Real-time Hub**: Monitor data yang dikirim ke hardware secara langsung melalui aplikasi.
- **Security Key Management**: Pengelolaan ID perangkat dan kunci rahasia yang aman menggunakan Android Keystore.

## 🚀 Persiapan Hardware

Aplikasi ini mengharapkan ESP32 Anda mengiklankan layanan Bluetooth berikut:

- **Service UUID**: `7c9e0001-6f2f-4d4d-9f25-0d7fd4f0a001`
- **Phone → ESP32 Write**: `7c9e0002-6f2f-4d4d-9f25-0d7fd4f0a001`
- **ESP32 → Phone Notify**: `7c9e0003-6f2f-4d4d-9f25-0d7fd4f0a001`

Lihat [BLE_PROTOCOL.md](docs/BLE_PROTOCOL.md) untuk detail paket data.

## 🛠️ Pengembangan (Android Studio)

1. Buka folder **Chronchi** sebagai proyek Android Studio.
2. Tunggu sinkronisasi Gradle selesai (membutuhkan koneksi internet).
3. Gunakan perangkat fisik Android dengan API 26+ (Bluetooth LE sangat disarankan menggunakan perangkat fisik, bukan emulator).
4. Jika tidak menggunakan Firebase, aplikasi akan masuk ke **Preview Mode** secara otomatis. Untuk konfigurasi Firebase penuh, lihat [FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md).

## 🌳 Struktur Folder

```text
Chronchi/
├── collector/       Notification, navigation, location, phone state
├── core/            Models, permissions, Keystore security
├── data/            Auth, Firebase, DataStore, weather
├── service/         Background connection services
├── state/           PhoneStateHub memory model
├── transport/       BLE + cloud routing
└── ui/              Modern Theme, components and screens
```

## 📄 Lisensi
Proyek ini dikembangkan oleh **IRSYAD LABS**. Gunakan untuk kebutuhan edukasi dan implementasi hardware ESP32.
