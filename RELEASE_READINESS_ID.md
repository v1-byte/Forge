# Forge Workbench — daftar kesiapan rilis profesional

Status audit repository pada 7 November 2026. Ini adalah penilaian atas source yang diperiksa, bukan sertifikasi keamanan atau bukti uji perangkat.

## Ringkasan keputusan

- **GitHub APK preview:** sudah pernah berhasil dibangun dan dipublikasikan sebagai v2.1.2.
- **Rilis Android stabil:** **belum siap**. Updater dan CI membutuhkan kunci signing rilis permanen, APK v2.1.2 yang sudah beredar belum diketahui fingerprint sertifikatnya, dan pengujian instalasi/update di perangkat nyata belum diserahkan.
- **Google Play:** belum dinyatakan siap. Perlu verifikasi Play Console terkini, listing/data safety, kebijakan privasi, asset listing, target distribusi yang sesuai, dan jalur signing/distribusi Play.
- **Kesiapan fitur:** bukan semua menu/engine siap. Panel UI masih mencakup fitur yang belum tersedia; beberapa deskripsi telah diperjelas, tetapi native inference, training, Git/debug dan tool lanjutan tetap bukan fitur produksi yang terbukti.

Pemeriksaan APK v2.1.2 yang dipublikasikan menemukan **package ID lama `org.qtproject.example.ForgeWorkbench`** dan sertifikat **Android Debug** (SHA-256 fingerprint `D8:BE:39:59:05:80:E9:1F:6F:50:2C:AA:8C:2A:8E:9C:F2:57:9F:5E:5E:EA:CE:56:F4:C8:FA:B7:96:51:16:63`). Perubahan ini sengaja menetapkan package ID baru bermerek `com.v1byte.forgeworkbench` serta signing key rilis tetap. Akibatnya, rilis baru **tidak dapat memperbarui APK lama in-place**; ini perlu diperlakukan sebagai migrasi aplikasi dengan instruksi backup, uninstall versi lama, lalu install versi baru. Jangan mengumumkan update sampai alur migrasi dan nasib data lokal diuji.

## Perubahan yang sudah dikerjakan di workspace ini

- Updater menerima hanya release stabil `vX.Y.Z` dengan asset APK dan sidecar checksum yang namanya cocok persis dengan tag.
- Nilai SHA-256 kini diambil dari asset `.sha256` terlebih dahulu; formatnya diwajibkan tepat 64 digit hex. APK diunduh ke file sementara, hash dihitung selama transfer, transfer/ukuran dibatasi, dan file hanya dipublikasikan ke cache bila HTTP dan digest valid.
- Kegagalan metadata, transfer, checksum, atau penyimpanan membatalkan pemasangan. Pemeriksaan update ganda dicegah dan URL lama dibersihkan saat mulai memeriksa.
- Dialog update memperlihatkan versi sekarang/target, memberi penjelasan pemeriksaan SHA-256 dan konfirmasi Android, serta memperlihatkan progres sebelum pengguna menyetujui download.
- Installer Android memberi panduan jika izin pemasangan aplikasi dari sumber ini belum aktif dan menampilkan error, bukan gagal tanpa pesan.
- Versi runtime diambil dari versi project CMake; package ID ditetapkan supaya identitas instalasi stabil.
- Workflow release memeriksa kecocokan tag dengan versi CMake, menolak rilis branch, mewajibkan keystore rilis dari GitHub Actions Secrets, memverifikasi signature, dan baru menghitung checksum sesudah APK final ditandatangani.
- Klaim beberapa fitur AI Engine di UI dan dokumentasi updater diperjelas.

## Blocker yang masih memerlukan tindakan pemilik repo

### P0 — sebelum rilis stabil berikutnya

1. **Siapkan secrets signing GitHub Actions:** `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, dan `ANDROID_KEY_PASSWORD`. Jangan unggah atau kirim keystore/password lewat chat. Workflow baru sengaja gagal aman jika secret tersebut tidak ada.
2. **Rencanakan migrasi v2.1.2.** APK v2.1.2 yang saat ini diunduh dari GitHub sudah diverifikasi bertanda `Android Debug` dengan fingerprint yang tercantum di atas. Rilis baru memakai package ID baru dan key release tetap, jadi Android tidak akan memperbaruinya in-place. Uji prosedur ekspor/backup workspace/model terlebih dahulu, lalu berikan instruksi uninstall/install ulang dan jelaskan apakah data lokal hilang. Jangan menyebut rilis baru sebagai update langsung.
3. **Bangun dan uji perangkat.** Belum dilakukan dari workspace ini karena tidak tersedia bukti build Qt Android/perangkat. Uji fresh install, update di atas versi sebelumnya dengan signer yang sama, versi Android yang didukung, permission “install unknown apps”, batal/lanjut installer, ruang disk rendah, offline, sidecar hilang/rusak, APK korup, dan update yang lebih baru.
4. **Uji identitas package aktual hasil build.** Pastikan package ID `com.v1byte.forgeworkbench` sudah sama dengan distribusi lama yang ingin diperbarui. Jika tidak sama, perubahan package membuat Android menganggapnya aplikasi baru.
5. **Jangan klaim aman hanya karena checksum cocok.** SHA-256 yang disajikan dari repo/release yang sama memberi integritas terhadap kerusakan, bukan tanda tangan independen. Lindungi akun GitHub dengan MFA, batasi hak workflow, dan tetapkan proses persetujuan release. Pertimbangkan signed release manifest dengan kunci terpisah.
6. **Privasi dan dukungan:** publikasikan kebijakan privasi yang mencerminkan perilaku aplikasi dan updater, alamat dukungan, panduan instalasi/update, known issues, serta catatan rilis. Jangan menyatakan aplikasi tidak mengirim data sampai perilaku jaringan seluruh aplikasi ditinjau.

### P1 — sebelum memasarkan sebagai produk profesional

- Tulis test plan dan hasil uji per perangkat/versi Android; tambahkan uji emulator smoke test dan pemeriksaan artifact ke CI.
- Buat matriks “tersedia / preview / belum tersedia” per fitur dan pastikan tombol/teks UI konsisten. Native inference/model, training, tokenizer model, adapter compiler, Git push/debug, dan tool ZIP/GGUF lanjutan memerlukan implementasi serta uji tersendiri.
- Periksa akses file Android, kegagalan permission, lifecycle ketika app kembali dari Settings/installer, konsumsi RAM/storage, stabilitas file workspace, dan pesan error.
- Tetapkan proses issue triage, changelog, rollback/stop rollout, dukungan versi lama, dan kebijakan kompatibilitas workspace/model.
- Untuk Google Play, tinjau persyaratan distribusi/target SDK dan policy terkini di Play Console sebelum mengunggah. Build saat ini menghasilkan APK arm64-v8a melalui GitHub Release; belum ada alur AAB/Play yang disiapkan atau diverifikasi.

## Tes yang perlu dijalankan setelah pemilik memasang secret

1. Jalankan workflow pada tag baru yang versi CMake-nya sama.
2. Pastikan `apksigner verify --print-certs` berhasil dan fingerprint sesuai kunci yang disimpan.
3. Cocokkan file `.sha256` dengan APK final yang sudah ditandatangani.
4. Pasang APK di perangkat uji; catat package ID, version name/code, signer fingerprint, ABI, versi OS, dan hasil smoke test.
5. Pasang update bertanda tangan sama di atas versi sebelumnya dan buktikan data pengguna tetap ada.
6. Uji pembatalan pemasangan dan kasus gagal; pastikan aplikasi tetap aman dan statusnya menjelaskan tindakan berikutnya.
7. Baru setelah seluruh uji lolos, publikasikan sebagai stable. Jika tidak, gunakan label beta/preview dan tulis batasannya dengan jelas.
