# Panduan Kontribusi Forge

## Baca sebelum mulai

**Dokumen acuan utama proyek:** [`docs/CATATAN-SUPER-LENGKAP-FINAL.md`](docs/CATATAN-SUPER-LENGKAP-FINAL.md)

Baca dokumen tersebut sebelum membuka issue, mengubah kode, atau membuat pull request. Dokumen itu memuat tujuan produk, arsitektur, keputusan implementasi, spesifikasi UI dan engine, persyaratan Android, keamanan, QA, serta checklist rilis.

## Aturan kontribusi inti

- Jangan menghapus atau mengganti source engine asli tanpa persetujuan eksplisit.
- Jangan menampilkan status READY, progres, hasil, atau output contoh seolah-olah nyata. Status dan progres harus berasal dari pemeriksaan atau pekerjaan engine yang benar-benar berjalan.
- Setiap aksi UI harus menjalankan fungsi nyata atau menjelaskan dengan terang mengapa aksi tidak dapat dijalankan.
- Sertakan pengujian yang relevan dan catat batasan perangkat/backend yang belum didukung.
- Pertahankan lisensi dan notice pihak ketiga di `LICENSES/`.
- Jangan pernah commit token, keystore, kredensial, atau data pribadi.
- Jika perubahan menyimpang dari keputusan di dokumen acuan, jelaskan alasannya dalam issue/pull request dan minta persetujuan sebelum implementasi.

## Pull request

Jelaskan tujuan dan cakupan perubahan, komponen yang terdampak, cara menguji, hasil pengujian, batasan yang diketahui, serta dampak pada dokumentasi/lisensi. Jangan menyatakan build atau test lulus jika belum dijalankan.
