# Forge Android (native)

Aplikasi ini adalah port Android native berbasis Kotlin dan Jetpack Compose untuk UI Forge Workbench yang disertakan pengguna. UI web bukan runtime aplikasi. Palet, struktur workbench, activity bar, editor, panel bawah, dan AI panel dipakai sebagai acuan visual.

Spesifikasi tampilan, breakpoint, fungsi tiap komponen, token warna, dan batas fitur saat ini dijelaskan di [UI_SPEC.md](UI_SPEC.md).

## Komponen native

- `app/`: UI Jetpack Compose, pemilihan file dengan Storage Access Framework, notifikasi Firebase, dan Google Play In-App Updates.
- `:llamaRuntime`: langsung menunjuk ke `forge-engine/forge-inference/examples/llama.android/lib`, yaitu wrapper JNI llama.cpp yang telah berada di repo Forge.
- `minSdk 33`, `compileSdk 36`; ABI aplikasi `arm64-v8a`.
- Package ID sementara: `com.v1byte.forge`. Ganti hanya sebelum membuat app listing Firebase/Play permanen; package ID harus sama pada keduanya.

## Build

Perlu Android Studio atau Android SDK dengan platform Android 36, NDK `29.0.13113456`, CMake `3.31.6`, JDK 17, serta koneksi untuk dependency Gradle.

```bash
cd apps/forge-android
./gradlew :app:assembleDebug
```

Build membutuhkan checkout repo Forge utuh karena modul runtime llama.cpp dirujuk dari `forge-engine/forge-inference/examples/llama.android/lib` dan CMake-nya membangun source `forge-engine/forge-inference`.

## Google Play in-app update

Kode memeriksa pembaruan melalui Play Core flexible update. Google Play menampilkan alur persetujuan dan unduhan kepada pengguna; aplikasi tidak memasang update secara diam-diam. Untuk menguji, aplikasi harus memakai application ID/signing yang sama dan dipasang dari Play testing track. Belum ada app listing atau signing key yang disediakan.

## Firebase push notification

FCM disiapkan sebagai integrasi opsional. Buat/register project Firebase memakai package ID final, unduh `google-services.json`, lalu letakkan di `app/google-services.json`. File itu diabaikan Git. Tanpa konfigurasi tersebut, build tetap dapat berjalan, tetapi push belum dapat didaftarkan/diterima. Pengiriman ke banyak pengguna membutuhkan Firebase Console atau server aman dengan Firebase Admin SDK; kredensial server tidak boleh disimpan di APK.

## Batas migrasi fungsi

Ini adalah aplikasi native, bukan WebView. Runtime inference lokal diarahkan ke llama.cpp Android yang sudah ada di repo. Training Unsloth/LitGPT dan sebagian pipeline compiler masih membutuhkan toolchain/backend yang belum tersedia sebagai runtime Android; UI tidak boleh menampilkan preview job sebagai pekerjaan yang benar-benar selesai. Listing atau upload ke Google Play tidak termasuk perubahan kode ini.
