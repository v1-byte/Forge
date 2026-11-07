# Pembaruan Forge Workbench Android

## Cara kerja updater

Aplikasi memeriksa release publik `vX.Y.Z` di GitHub setelah startup dan kemudian setiap 6 jam. Tombol **Check update** menjalankan pemeriksaan manual. Updater hanya menerima release stabil (bukan draft/prerelease), tag semver, dan pasangan asset yang tepat: `ForgeWorkbench-X.Y.Z.apk` serta `ForgeWorkbench-X.Y.Z.apk.sha256`.

Jika versi lebih baru ditemukan, pengguna melihat versi terpasang dan versi target, kemudian memilih **Unduh & verifikasi**. Aplikasi mengunduh sidecar SHA-256, memvalidasi format digest 64 digit heksadesimal, lalu mengunduh APK melalui HTTPS ke file sementara. Hash dihitung selama transfer. Installer hanya dibuka jika respons HTTP berhasil, APK tidak kosong, file tersimpan utuh, dan digest cocok. Kegagalan checksum, jaringan, atau penyimpanan membatalkan update. Ukuran APK dibatasi 1 GiB dan transfer memiliki timeout.

Setelah lolos verifikasi, Android membuka installer sistem dan tetap meminta persetujuan pengguna. Pada Android 8+, jika pemasangan dari Forge belum diizinkan, aplikasi membuka pengaturan izin khusus; pengguna kembali ke Forge lalu menekan update lagi. Android mensyaratkan APK versi baru ditandatangani dengan sertifikat yang sama untuk memperbarui instalasi lama.

## Batas keamanan

SHA-256 yang diunduh dari GitHub Release yang sama mendeteksi kerusakan atau ketidakcocokan file, tetapi **bukan tanda tangan independen** dari pemilik release. Keamanan update bergantung pada keamanan akun/repo GitHub dan validasi sertifikat oleh Android. Jangan mengganti asset setelah dipublikasikan. Untuk verifikasi independen lebih kuat, tambahkan tanda tangan metadata dengan kunci terpisah dan implementasikan verifikasinya di aplikasi.

Tidak ada instalasi diam-diam. Jangan meminta pengguna mengirim token GitHub/password/keystore melalui chat atau menyimpan kunci di repo.

## Siapkan kunci rilis sebelum menjalankan workflow

Workflow menolak menerbitkan APK tanpa signing key tetap. Buat satu keystore rilis dan simpan cadangannya di tempat aman. Tambahkan secrets melalui **GitHub repository → Settings → Secrets and variables → Actions**:

- `ANDROID_KEYSTORE_BASE64`: file keystore yang telah di-Base64, satu baris.
- `ANDROID_KEYSTORE_PASSWORD`: password keystore.
- `ANDROID_KEY_ALIAS`: alias key.
- `ANDROID_KEY_PASSWORD`: password key.

Jangan commit keystore atau password. Kehilangan kunci/sertifikat berbeda akan memutus update in-place. APK v2.1.2 yang telah dipublikasikan teridentifikasi memakai package `org.qtproject.example.ForgeWorkbench` dan sertifikat `Android Debug` (SHA-256 `D8:BE:39:59:05:80:E9:1F:6F:50:2C:AA:8C:2A:8E:9C:F2:57:9F:5E:5E:EA:CE:56:F4:C8:FA:B7:96:51:16:63`). Rilis berikutnya menetapkan package baru `com.v1byte.forgeworkbench` dan kunci rilis permanen. Jadi v2.1.2 **tidak dapat diperbarui in-place**: rilis baru akan menjadi aplikasi terpisah. Sebelum migrasi, uji backup/ekspor workspace dan model, siapkan instruksi backup lalu uninstall/install, dan jelaskan data yang tidak dapat dipindahkan. Jangan mengklaim rilis baru sebagai update langsung.

## Prosedur rilis

1. Naikkan `project(... VERSION ...)` di `CMakeLists.txt` dan samakan dengan `package.json`. Versi runtime aplikasi diambil dari definisi compile-time CMake.
2. Pertahankan `android:versionCode="-- %%INSERT_VERSION_CODE --"` dan `android:versionName="-- %%INSERT_VERSION_NAME --"` pada `android/AndroidManifest.xml`; Qt mengisi kedua nilai dari properti CMake `QT_ANDROID_VERSION_CODE` dan `QT_ANDROID_VERSION_NAME`.
3. Siapkan catatan rilis yang hanya menjanjikan fitur yang sudah diuji dan memuat known issues.
4. Buat/push tag (misalnya `v2.1.7`) yang cocok persis dengan versi CMake. Workflow memeriksa tag, membangun arm64-v8a, menandatangani APK, memverifikasi signature dan package/version APK, lalu menerbitkan APK dan checksum.
5. Periksa log workflow, fingerprint sertifikat, checksum, dan install/update di perangkat uji sebelum mengumumkan rilis. Status workflow berhasil bukan bukti bahwa semua fitur lolos uji perangkat.

Workflow `workflow_dispatch` pada branch sengaja ditolak; jalankan pada tag. Perlakukan tag dan release yang sudah diumumkan sebagai immutable.

## Workbench browser

Pemeriksaan versi Workbench browser hanya memberi tautan ke release; gateway tidak mengganti instalasi server otomatis. Administrator perlu memvalidasi paket, memperbarui source, lalu me-restart service secara terencana.
