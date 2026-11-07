# Forge Android Native — Kotlin + llama.cpp JNI

Modul Android native pertama Forge. UI ditulis dengan Kotlin `Activity`/Views (tanpa HTML/WebView). Inference memanggil C API dari source `forge-engine/forge-inference` yang sama melalui wrapper JNI C++; build menargetkan `arm64-v8a`, GGML CPU, dan model GGUF yang dipilih pengguna.

## Isi vertical slice

- Editor teks lokal sederhana untuk `workspace/main.kt`, disimpan ke penyimpanan privat aplikasi.
- Pemilih model Android (Storage Access Framework); file `.gguf` disalin ke area privat aplikasi.
- Muat/lepas model melalui `llama_model_load_from_file` / `llama_model_free` dan API context resmi.
- Completion teks satu prompt melalui tokenisasi, `llama_decode`, sampler suhu/distribusi, dan konversi token ke teks pada `forge-inference/`.
- Pemanggilan engine berjalan pada worker thread, bukan main/UI thread. Model dapat dilepas untuk membebaskan memori.
- CMake membangun library llama dan bridge JNI; server, tests, examples, dan tools yang tidak diperlukan tidak dimasukkan ke APK.

## Build

Prasyarat: JDK 17+, Android SDK Platform 35, Android Build Tools 35.0.0, NDK `27.2.12479018`, CMake `3.22.1`, dan Gradle Wrapper.

```bash
cd forge-android
./gradlew :app:assembleDebug
```

APK debug jika build berhasil:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Buka folder `forge-android/` di Android Studio bila ingin memilih SDK/NDK lokal. Build pertama mengompilasi llama.cpp dan ggml dari source; waktunya dan konsumsi disk/RAM jauh lebih besar dibanding build UI biasa.

## Uji pada perangkat

1. Instal debug APK pada Android arm64 (`arm64-v8a`).
2. Pilih satu model `.gguf` yang kompatibel dan cukup kecil untuk RAM perangkat; file disalin ke penyimpanan privat aplikasi.
3. Tekan **Muat model ke llama.cpp**, lalu tunggu status **Model siap**. Jika gagal, pesan status menampilkan error native.
4. Isi prompt dan jalankan completion; hasil harus berasal dari model yang benar-benar dimuat. Tekan **Lepaskan model** setelah selesai.
5. Uji penyimpanan `main.kt` secara terpisah. Editor file tahap ini tidak menjalankan atau mengompilasi file Kotlin tersebut.

Model GGUF tidak disertakan dalam repo/APK. Pemilihan, lisensi, ukuran, arsitektur, context training, kuantisasi, RAM, thermal, dan performa model tetap tanggung jawab deployment/pengguna. Model split belum didukung. Prompt dikirim langsung (belum memakai chat template khusus model).

## Batas tahap pertama

Ini **bukan** konversi penuh Workbench HTML dan **belum** paritas VS Code. Repo web lama tetap ada; modul ini adalah aplikasi native baru agar migrasi dapat diuji tanpa menghapus jalur yang sudah bekerja. Editor saat ini adalah input teks native tanpa syntax highlighting engine, IntelliSense/LSP, tree explorer, multi-cursor, formatter, build/run/debug Kotlin, Git UI, terminal, streaming token, pembatalan generation, GPU/NPU, atau history chat. Engine trainer/litgpt (Python) serta compiler/editor (Python/TypeScript/Node services) belum ditanam atau dijembatani oleh modul ini.

Cakupan NDK yang dibuat eksplisit hanya `forge-inference` CPU arm64. Dukungan API sumber untuk Android bukan bukti berhasil pada setiap perangkat; build dan uji perangkat nyata tetap wajib sebelum menyatakan release siap.
