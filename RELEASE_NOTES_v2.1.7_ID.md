# Forge Workbench Android v2.1.7 — perbaikan metadata versi APK

## Perubahan

- Memasukkan `versionName` dan `versionCode` dari konfigurasi CMake Qt ke AndroidManifest hasil build.
- Mempertahankan package ID `com.v1byte.forgeworkbench` dan kunci signing rilis agar instalasi v2.1.5 dapat diperbarui sebagai aplikasi yang sama.
- Workflow memeriksa versi APK, package ID, dan signature sebelum menerbitkan APK arm64-v8a serta checksum SHA-256.

## Catatan kompatibilitas

Versi ini memperbaiki metadata APK yang membuat rilis v2.1.6 gagal pada pemeriksaan versionName. Tag v2.1.6 tetap dipertahankan; rilis patch ini memakai tag baru v2.1.7. Instalasi yang memakai package ID dan sertifikat signing resmi v2.1.5 dapat diperbarui ke versi ini. Jangan menghapus aplikasi atau data sebelum memastikan backup tersedia.

## Batasan yang diketahui

Rilis ini adalah aplikasi Qt/QML arm64-v8a dari branch `main`. Beberapa menu masih berupa UI atau placeholder; adapter inference/compiler/training, Git/debug, serta tool lanjutan model/arsip belum tersedia sebagai fungsi native siap produksi. SHA-256 memeriksa integritas transfer, sementara Android memverifikasi signature APK. Checksum dirilis bersama APK dan bukan tanda tangan independen.

## Instalasi

Unduh APK hanya dari halaman GitHub Release resmi. Verifikasi checksum yang disertakan sebelum memasang. Android akan meminta konfirmasi pemasangan; tidak ada instalasi diam-diam.
