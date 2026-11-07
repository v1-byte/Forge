# Forge Workbench Android v2.1.6 — migrasi package aplikasi

## Penting untuk pengguna v2.1.2

Rilis ini memakai package aplikasi baru `com.v1byte.forgeworkbench` dan sertifikat signing rilis permanen. APK v2.1.2 memakai package `org.qtproject.example.ForgeWorkbench` dan sertifikat Android Debug. Karena package dan signer berbeda, Android tidak bisa memperbarui v2.1.2 secara langsung; instalasi v2.1.6 akan muncul sebagai aplikasi terpisah.

Sebelum menghapus aplikasi lama, ekspor atau salin workspace dan model yang ingin dipertahankan. Uji bahwa data dapat dipulihkan. Setelah memastikan backup, pengguna dapat menghapus versi lama secara manual bila tidak lagi diperlukan. Jangan menghapus aplikasi lama sebelum memastikan file penting tersimpan.

## Perubahan

- Auto-update hanya menerima release stabil dengan versi dan nama APK/checksum yang cocok.
- APK diunduh setelah persetujuan pengguna; checksum SHA-256 dari GitHub Release harus cocok sebelum installer Android dibuka.
- UI menampilkan versi, status verifikasi, progres download, dan konfirmasi installer Android.
- Jika Android belum mengizinkan pemasangan dari Forge, aplikasi membuka pengaturan izin yang relevan.
- Workflow memeriksa kecocokan tag/versi, signature release, package ID, dan versionName sebelum menerbitkan APK dan checksum.
- Nama aplikasi dan launcher icon resmi ditambahkan.

## Batasan yang diketahui

Rilis ini arm64-v8a. SHA-256 melindungi integritas transfer; checksum diterbitkan bersama APK pada GitHub Release yang sama, bukan tanda tangan independen. Android tetap memverifikasi signature paket. Beberapa fitur yang terlihat di UI belum tersedia sebagai fungsi native—termasuk adapter inference/compiler/training, Git/debug, serta tool lanjutan model/arsip. Jangan menganggap semua menu adalah fungsi siap produksi.

## Instalasi

Unduh APK hanya dari halaman GitHub Release resmi. Verifikasi checksum yang disertakan sebelum memasang. Android akan meminta konfirmasi pemasangan; tidak ada instalasi diam-diam.
