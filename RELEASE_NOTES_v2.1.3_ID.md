# Forge Workbench Android v2.1.3 — migrasi package aplikasi

## Penting untuk pengguna v2.1.2

Rilis ini menggunakan package aplikasi baru `com.v1byte.forgeworkbench` dan sertifikat signing rilis permanen. APK v2.1.2 sebelumnya menggunakan package `org.qtproject.example.ForgeWorkbench` dan sertifikat Android Debug. Karena package dan signer berbeda, Android tidak bisa memperbarui v2.1.2 secara langsung. Instalasi v2.1.3 akan muncul sebagai aplikasi terpisah.

Sebelum menghapus aplikasi lama, ekspor atau salin workspace dan model yang ingin dipertahankan. Uji bahwa data bisa dipulihkan di perangkat baru. Setelah memastikan backup, pengguna dapat menghapus versi lama secara manual bila tidak lagi diperlukan. Jangan menghapus aplikasi lama sebelum memastikan file penting tersimpan.

## Perubahan

- Alur auto-update menerima hanya release stabil dengan versi dan nama APK/checksum yang cocok.
- APK diunduh hanya setelah pengguna menyetujui; checksum SHA-256 diambil dari release dan wajib cocok sebelum installer Android dibuka.
- UI menunjukkan versi sumber/target, status verifikasi, progres download, dan kebutuhan konfirmasi installer Android.
- Jika Android belum mengizinkan pemasangan dari Forge, aplikasi membuka pengaturan izin yang relevan.
- Workflow rilis memeriksa versi/tag, menandatangani dengan release keystore tetap yang disimpan sebagai GitHub Actions Secrets, dan memverifikasi signature.
- Label aplikasi dan launcher icon resmi ditambahkan.

## Batasan yang diketahui

Rilis ini arm64-v8a. Auto-update melindungi integritas transfer dengan SHA-256; checksum berasal dari GitHub Release yang sama, bukan tanda tangan independen. Android tetap memverifikasi signature paket. Beberapa fitur yang tampak di UI masih belum tersedia sebagai fungsi native, termasuk adapter inference/compiler/training, Git/debug, dan tool lanjutan model/arsip. Baca catatan fitur native dan jangan anggap semua menu adalah fungsi siap produksi.

## Instalasi

Unduh APK hanya dari halaman GitHub Release resmi. Verifikasi checksum yang disertakan sebelum memasang. Android akan meminta konfirmasi pemasangan. Tidak ada instalasi diam-diam.
