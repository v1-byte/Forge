# Forge Workbench — Native C++/Qt 6 Migration

Port native ini menggantikan runtime Node.js/React dengan aplikasi Qt 6:

- **UI:** QML + Qt Quick Controls 2
- **Logika dan backend:** C++17 (`src/ForgeBackend.*`)
- **Build:** CMake + `qt_add_qml_module`
- **Target:** Linux, Windows, macOS, dan Android

## Build desktop

Pastikan Qt 6.4+ dan CMake tersedia:

```bash
cmake -S . -B build -DCMAKE_PREFIX_PATH=/path/to/Qt/6.8/gcc_64
cmake --build build --parallel
./build/ForgeWorkbench
```

## Build Android

Gunakan Qt Creator dengan kit Android Qt 6, atau CMake dengan toolchain Android Qt. Pilih ABI perangkat (arm64-v8a direkomendasikan), lalu deploy ke perangkat/emulator.

## Arsitektur

`ForgeBackend` berjalan in-process sehingga desktop dan Android memakai kode C++ yang sama. Workspace berada di Documents/ForgeWorkspace secara default dan dapat diganti melalui `FORGE_WORKSPACE_ROOT` pada desktop. Fitur utama yang sudah dipindahkan ke backend C++ meliputi:

- daftar workspace dan klasifikasi file;
- baca/tulis file teks dengan path confinement;
- membuat file/folder dan menghapus path;
- pencarian isi workspace;
- status hardware dan status engine;
- tokenizer baseline Unicode;
- penyimpanan job lokal dan pembatalan job;
- toggle safe mode/native execution.

UI QML mempertahankan struktur Forge lama: top bar, activity rail, Explorer, editor tab, status bar, AI Engine, Copilot, Search, Git, Run & Debug, bottom terminal/output/problems/debug console, theme picker, command palette, dan Archive. `qml/Main.qml` harus diperlakukan sebagai port visual dari `../frontend/index.html`; tombol yang belum memiliki adapter native tetap ditampilkan dan memberi status yang jelas, bukan dihapus.

## Status APK

APK yang dibangun dari branch Android Compose/Kotlin lama bukan release dari port ini. Release Qt 6/C++ hanya boleh dibuat setelah dibangun menggunakan Qt for Android, Android SDK/NDK, dan CMake terhadap direktori `native-qt6/`. Jangan mengganti APK Qt dengan artifact Kotlin lama karena tampilan dan runtime-nya berbeda.

## Catatan port lanjutan

Adapter inference/compiler/trainer yang benar-benar native tetap harus disambungkan ke binary engine C++ yang tersedia pada deployment. Saat ini job tidak mengklaim inference/training berhasil bila engine belum dikonfigurasi. Dukungan ZIP/GGUF/Git lanjutan dapat ditambahkan sebagai service C++ berikutnya tanpa mengubah UI QML.
