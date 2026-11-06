# Update otomatis Forge Workbench

Aplikasi Qt native memeriksa release terbaru dari:

`https://api.github.com/repos/v1-byte/Forge/releases/latest`

Saat release mempunyai asset `.apk` dan `.sha256` dengan versi lebih tinggi, tombol **Check update** berubah menjadi tombol versi update. Aplikasi kemudian:

1. Mengunduh APK ke cache aplikasi Android.
2. Menghitung SHA-256 APK.
3. Membandingkan checksum dengan asset `.sha256` release.
4. Membuka installer Android melalui `FileProvider` dan URI `content://`.

Android tetap menampilkan konfirmasi instalasi. Instalasi diam-diam tidak dilakukan karena tidak diperbolehkan pada perangkat Android konsumen biasa; silent install hanya tersedia pada perangkat yang dikelola enterprise/Device Owner atau konfigurasi khusus.

Release APK dibuat oleh `.github/workflows/android-release.yml` saat tag `vX.Y.Z` dipush. Workflow melampirkan APK dan checksum ke GitHub Release. Build pertama harus dilakukan oleh GitHub Actions karena mesin pengembangan lokal belum tentu memiliki Qt for Android, Android SDK, dan NDK.

Untuk keamanan, updater hanya memakai HTTPS dan tidak menerima URL update dari input pengguna. Jangan mengganti release asset setelah publish tanpa mengganti checksum dan membuat release baru.
