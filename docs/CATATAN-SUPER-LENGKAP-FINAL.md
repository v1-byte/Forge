FORGE REAL APK PRODUCTION — CATATAN SUPER LENGKAP FINAL
Repo: https://github.com/v1-byte/Forge
Branch acuan: main
Commit acuan yang terbaca dari halaman root repo: 5cb71acc
Target akhir: APK Android publik production, semua system dan semua engine asli terbawa di dalam HP, semua tampilan memanggil engine asli, semua alur jalan lancar di HP.
Prinsip: ukuran tidak menjadi batas. Kelancaran, kejujuran status, dan bukti nyata menjadi prioritas.
Dokumen ini adalah acuan utama. Catatan lama diarsipkan di docs dan archive dan tidak dipakai sebagai acuan utama lagi.

DAFTAR ISI
Ringkasan eksekutif
Hasil analisa repo lengkap
Inventaris total file dan folder
Definisi terbawa versi production
Arsitektur lengkap
Keputusan final lengkap
Spesifikasi system forge-editor lengkap
Spesifikasi forge-compiler lengkap
Spesifikasi forge-inference lengkap
Spesifikasi forge-trainer lengkap
Spesifikasi forge-litgpt lengkap
Spesifikasi UI lengkap per layar
Spesifikasi navigasi, state, error, loading, empty
Spesifikasi data, storage, file, model, checkpoint, log, config, backup
Spesifikasi keamanan, privasi, permission, jaringan
Spesifikasi performa, baterai, RAM, storage, threading, lifecycle
Spesifikasi Android lengkap: Manifest, Gradle, CMake, JNI, Python, Service, Broadcast, Notification
Spesifikasi build lengkap: debug, release, signing, versioning, artifact
Spesifikasi CI dan CD lengkap
Spesifikasi testing lengkap
Spesifikasi QA lengkap
Spesifikasi rilis GitHub lengkap
Spesifikasi rilis Google Play lengkap
Spesifikasi dokumentasi, README, changelog, privacy, licenses
Spesifikasi operasional pasca rilis, maintenance, update, hotfix
Daftar kerja total Fase 0 sampai Fase 10
Daftar file wajib lengkap
Checklist akhir sebelum rilis
Lampiran: endpoint, model, setting, error code, log format, nama file
1. RINGKASAN EKSEKUTIF
Forge adalah kumpulan runtime AI dan tooling mesin. Forge terdiri dari system dan lima engine. System adalah forge-editor. Lima engine adalah forge-compiler, forge-inference, forge-trainer, forge-litgpt, ditambah forge-editor sebagai system yang juga menjalankan server lokal. Semua komponen tersebut harus terbawa di dalam satu APK Android. APK tersebut harus dapat jalan offline setelah model terdownload. APK tersebut harus menampilkan status jujur untuk setiap engine. APK tersebut harus menolak model yang melebihi RAM dengan pesan jelas. APK tersebut harus menyimpan checkpoint training dan dapat resume. APK tersebut harus menampilkan log, diagnostics, licenses, dan privacy. APK tersebut harus dirilis sebagai GitHub Release publik production, lalu Google Play setelah stabil. Dokumen ini mengatur semua hal tersebut sampai detail file, fungsi, endpoint, setting, dan checklist.

2. HASIL ANALISA REPO LENGKAP
2.1 Data repo yang terbaca dari halaman root
Repo bersifat publik.
Jumlah commit yang tercatat di halaman root: 26.
Jumlah rilis yang tercatat: 3.
Jumlah tag yang tercatat: 8.
Tanggal pembuatan repo yang tercatat: 2 November 2026.
Folder di root: .github dan workflows, LICENSES, forge-engine, forge-workbench.
File di root: .gitignore, ENGINE_DESCRIPTIONS.txt, FORGE_WORKBENCH_UI_NOTES_ID.md, README.md, RELEASE_NOTES_v2.1.6_ID.md, RELEASE_NOTES_v2.1.7_ID.md, RELEASE_READINESS_ID.md.
2.2 Isi README yang terbaca
Forge menyatukan runtime AI dan tooling mesin.
Payload engine upstream tidak menyertakan UI browser.
Aplikasi Workbench tersedia terpisah di forge-workbench, tanpa mengganti backend engine.
Deskripsi teknis lengkap tersedia di ENGINE_DESCRIPTIONS.txt.
Workbench dijalankan dengan Node.js 18 atau lebih baru.
Perintah jalan: cd forge-workbench, lalu npm start.
Alamat jalan: http://localhost:8787.
Detail penggunaan ada di forge-workbench dan README.md.
Ringkasan perubahan UI ada di FORGE_WORKBENCH_UI_NOTES_ID.md.
Versi tersebut menghapus tombol quick-action di area layar kosong.
Versi tersebut menambahkan tema Dark, Light, System.
Versi tersebut menambahkan syntax highlighting lokal dengan deteksi bahasa.
Versi tersebut menambahkan pengelola pack bahasa.
Plugin DLL dan native tidak dieksekusi oleh browser.
Lisensi dan notice pihak ketiga disimpan terpusat di LICENSES.
Tidak ada UI, tutorial, README upstream, contoh aplikasi, atau test upstream di dalam payload engine.
Port native Qt 6 dan C++ untuk desktop dan Android tersedia di forge-workbench dan native-qt6.
UI web lama dan gateway tetap dipertahankan.
2.3 Daftar engine yang tercantum di README
forge-compiler: compiler dan runtime deployment model untuk target CPU, GPU, dan backend tertentu.
forge-editor: server dan common backend, HTTP, WebSocket, proxy, health, dan konektivitas editor tanpa browser UI.
forge-inference: runtime C dan C++ untuk loading model, GGUF, tokenizer, sampling, KV cache, backend CPU dan GPU, dan serving.
forge-trainer: engine training dan fine-tuning Unsloth dengan kernel optimized, quantization, precision, dan distributed training.
forge-litgpt: engine Python untuk pretraining, full fine-tuning, LoRA dan adapter, generation, evaluasi, conversion, dan serving.
2.4 Kesimpulan analisa
Repo saat ini berisi source engine dan UI web.
Repo saat ini belum berbentuk aplikasi Android production.
APK lama berisikan UI saja karena engine C, C++, dan Python tidak ikut ter-compile dan tidak ikut ter-embed.
UI browser tidak dapat mengeksekusi DLL dan native.
Pekerjaan yang harus dilakukan adalah membangun lapisan Android di atas source asli, tanpa menghapus source asli.
Semua engine harus terbawa di dalam APK.
Semua tampilan harus memanggil engine asli.
Tidak ada tampilan pajangan.
Tidak ada status palsu.
Tidak ada output contoh yang ditampilkan sebagai hasil nyata.
3. INVENTARIS TOTAL FILE DAN FOLDER
3.1 Root repo setelah lengkap
.github dan workflows: workflow debug dan workflow release.
LICENSES: lisensi dan notice pihak ketiga.
forge-engine: source asli, tidak diubah isinya.
forge-engine dan forge-compiler: source compiler asli.
forge-engine dan forge-editor: source editor asli.
forge-engine dan forge-inference: source inference asli.
forge-engine dan forge-trainer: source trainer asli.
forge-engine dan forge-litgpt: source litgpt asli.
forge-workbench: UI web untuk PC, tetap dipertahankan.
forge-workbench dan native-qt6: port Qt 6 dan C++ desktop, tetap dipertahankan sebagai varian desktop.
forge-android: aplikasi Android baru, UI Kotlin, JNI, Python, Service.
docs: catatan lengkap, keputusan, QA, rilis, privacy, libraries, model, arsitektur.
docs dan archive: catatan lama yang diarsipkan.
scripts: checker, verifier, rapikan catatan, generate SHA256, bongkar APK.
README.md: pintu masuk repo.
CATATAN-LENGKAP-FINAL.md dan CATATAN-SUPER-LENGKAP-FINAL.md: acuan utama.
ENGINE_DESCRIPTIONS.txt: deskripsi teknis engine.
RELEASE_NOTES_v3.0.0_ID.md: catatan rilis production.
.gitignore: aturan ignore, termasuk keystore, local.properties, build, .idea, dan file sensitif.
3.2 forge-android lengkap
settings.gradle: nama proyek dan include app.
build.gradle root: classpath gradle.
gradlew dan gradlew.bat: wrapper gradle.
gradle dan wrapper: versi gradle dan distribusi.
app dan build.gradle: plugin, android, signing, dependencies, python, cmake.
app dan src dan main dan AndroidManifest.xml: permission, activity, service, receiver.
app dan src dan main dan java dan com dan forge dan app: semua file Kotlin.
app dan src dan main dan cpp: CMakeLists, JNI inference, JNI compiler, header bridge.
app dan src dan main dan python: trainer mobile, litgpt mobile, requirements mobile, model manager python bila perlu.
app dan src dan main dan res dan layout: layout tiap layar.
app dan src dan main dan res dan values: strings, colors, themes, dimens.
app dan src dan main dan res dan drawable: ikon, background, vector.
app dan src dan main dan res dan mipmap: launcher icon tiap densitas.
app dan src dan main dan assets: licenses, privacy, help, contoh config, prompt contoh.
3.3 File Kotlin lengkap
MainActivity.kt: activity utama, navigasi, bottom nav, toolbar.
ForgeApp.kt: application class, init Python, init Service, init FileManager, init Logger.
ForgeEditorService.kt: foreground service editor.
ForgeInference.kt: bridge Kotlin ke JNI inference.
ForgeCompiler.kt: bridge Kotlin ke JNI compiler.
PythonBridge.kt: bridge Kotlin ke Chaquopy Python.
EngineStatusManager.kt: satu sumber kebenaran status.
ForgeFileManager.kt: folder, file, model, checkpoint, output, log, config.
ModelManager.kt: download, import, verifikasi SHA256, hapus, detail.
Logger.kt: tulis log, rotasi log, export log, sensor prompt.
DiagnosticsManager.kt: kumpulkan RAM, storage, ABI, versi, log.
SettingsManager.kt: simpan dan baca setting, DataStore.
PermissionManager.kt: minta permission, cek status, arahkan ke setting.
NotificationHelper.kt: notifikasi service, training, download, compile.
DownloadManager.kt: download model dengan progress, pause, resume, verifikasi.
CheckpointManager.kt: simpan, list, resume, hapus checkpoint.
ErrorHandler.kt: ubah exception menjadi pesan UI dan log.
Screens: DashboardScreen.kt, EditorScreen.kt, InferenceScreen.kt, CompilerScreen.kt, TrainerScreen.kt, LitGPTScreen.kt, ModelManagerScreen.kt, DiagnosticsScreen.kt, SettingsScreen.kt, LicensesScreen.kt, AboutScreen.kt, OnboardingScreen.kt.
3.4 File native lengkap
CMakeLists.txt: target inference, target compiler, standar C++17, link log dan android.
forge_inference_jni.cpp: JNI inference.
forge_compiler_jni.cpp: JNI compiler.
forge_inference_bridge.h: deklarasi fungsi asli inference.
forge_compiler_bridge.h: deklarasi fungsi asli compiler.
native_logger.cpp: log native ke file dan logcat.
native_utils.cpp: util path, RAM, thread, error.
3.5 File Python lengkap
forge_trainer_mobile.py: status, listTasks, trainLoRA, pause, resume, getProgress, getLastError.
forge_litgpt_mobile.py: status, generate, evaluate, convert, serve, trainLoRA, getLastError.
forge_model_utils.py: validasi GGUF, hitung SHA256, baca header model.
requirements-mobile.txt: numpy, sentencepiece, safetensors, onnxruntime-mobile.
requirements-server.txt: torch, transformers, accelerate, peft, trl, bitsandbytes, datasets, deepspeed.
4. DEFINISI TERBAWA VERSI PRODUCTION
Sebuah komponen dinyatakan terbawa apabila semua kondisi berikut terpenuhi.

Kodenya berada di dalam APK.
Kode tersebut dapat di-load dan di-start dari dalam APK tanpa internet, setelah model terdownload.
Terdapat layar di UI yang memanggilnya dan menampilkan hasil nyata.
Terdapat log yang membuktikan komponen tersebut jalan, lengkap dengan versi, path, dan waktu.
Terdapat test otomatis yang gagal apabila komponen tersebut hilang.
Terdapat fallback dan pesan error yang jujur apabila perangkat tidak mendukung.
Aturan anti palsu yang berlaku untuk seluruh tim dan seluruh layar.
Status READY hanya boleh tampil setelah loadLibrary, import, dan health check benar-benar sukses.
Output contoh dilarang ditampilkan sebagai hasil inference atau training.
Error dilarang disembunyikan dengan toast kosong.
Semua error masuk ke log dan tampil di layar Diagnostics.
Setiap layar engine memiliki tiga state: LOADING, READY dengan bukti, dan FAILED dengan alasan dan cara perbaiki.
Setiap tombol yang ditekan harus menghasilkan aksi nyata atau pesan jelas.
Setiap progress harus berasal dari data nyata, bukan animasi palsu.
Setiap file output harus benar-benar dibuat dan dapat dibuka.
5. ARSITEKTUR LENGKAP
5.1 Lapisan
Lapisan UI: MainActivity Kotlin dan tujuh layar utama ditambah layar pendukung.
Lapisan System: ForgeEditorService sebagai Foreground Service.
Lapisan Bridge: JNI untuk native, Chaquopy untuk Python, HTTP lokal untuk editor.
Lapisan Engine Asli: forge-compiler, forge-inference, forge-trainer, forge-litgpt.
Lapisan Data: folder Forge di storage.
5.2 Alur lengkap
Buka aplikasi.
Application init: buat folder, init logger, init setting, init Python, start Service.
Onboarding apabila pertama buka: penjelasan, permission, buat folder, cek storage.
Service start, health check jalan.
UI menampilkan status tiap engine dengan bukti.
Inference: pilih model GGUF, load melalui JNI, tokenizer, generate token streaming, tampil, simpan log.
Compiler: pilih model dan target CPU atau GPU, compile melalui JNI, simpan hasil, tampil ukuran, waktu, dan log.
Trainer: pilih config LoRA, validasi RAM dan storage, jalan mode mobile, checkpoint per N step, bisa pause dan resume, tampil loss, step, dan log.
LitGPT: pilih tugas generate, eval, convert, serve, atau LoRA, jalan melalui Python, tampil hasil dan file output.
Editor: buka file di folder Forge, edit, simpan, lewat ForgeEditorService, health tetap hijau.
Model Manager: download, import, verifikasi, hapus, detail.
Diagnostics: status, log, test, export.
Settings: port, folder, RAM, thread, tema, bahasa, cache, reset.
Offline: semua alur di atas jalan tanpa internet, kecuali download model pertama dan download update.
5.3 Model
Model GGUF tidak dimasukkan ke dalam APK.
Alasan: model dapat berganti lisensi, APK tidak perlu rilis ulang hanya untuk ganti model, dan update model menjadi mandiri.
Pengganti: Model Manager di dalam aplikasi.
Model Manager melakukan download GGUF dari URL resmi, import dari storage, verifikasi SHA256 dan ukuran, lalu simpan ke folder models.
Yang masuk APK adalah engine-nya.
Model diunduh terpisah.
5.4 Diagram data
Input user masuk ke UI.
UI memanggil EngineStatusManager untuk cek status.
UI memanggil Service atau Bridge sesuai engine.
Bridge memanggil native atau Python.
Native dan Python membaca model dari folder models dan menulis output ke folder outputs dan checkpoints.
Semua aksi menulis log ke folder logs.
Diagnostics membaca log, status, RAM, storage, dan versi.
6. KEPUTUSAN FINAL LENGKAP
6.1 Build system
Build system: CMake 3.22.1.
NDK: 25.2.9519653.
Standar C++: C++17.
ABI utama: arm64-v8a.
ABI emulator: x86_64 hanya untuk test UI.
Optimasi release: O2.
Optimasi debug: O0 dengan log penuh.
Thread inference: maksimal 4 thread di HP RAM 4GB, maksimal 6 sampai 8 thread di HP RAM 8GB ke atas.
Semua core dilarang dipakai penuh agar HP tidak panas dan tidak throttle.
6.2 Library eksternal
Library yang dipakai: libc, libm, liblog, libandroid, pthread, C++ STL bawaan NDK.
Loader GGUF, tokenizer, sampler, dan KV cache memakai kode asli dari forge-engine dan forge-inference.
Library yang tidak dipakai di rilis pertama: CUDA, cuDNN, OpenCL full, Vulkan full, Boost full, OpenCV, FFmpeg.
Backend GPU penuh ditambahkan di versi berikutnya setelah versi CPU stabil dan fallback CPU sudah terbukti.
Setiap library dicatat di docs libraries dengan nama, versi, lisensi, dan alasan pakai.
Library yang membuat build gagal dua kali dibuang dan diganti dengan kode sendiri atau fallback CPU.
6.3 forge-editor
Bahasa: Kotlin.
Framework: Ktor Server ringan.
Alternatif super minimal apabila Ktor membuat APK terlalu besar: NanoHTTPD.
Bentuk: Foreground Service bernama ForgeEditorService.
Port: 127.0.0.1:8787.
Port tersebut sama dengan Workbench PC agar dokumentasi, UI web lama, dan UI Android konsisten.
Alamat 0.0.0.0 dilarang dipakai agar HP tidak menjadi server terbuka.
Notifikasi service: Forge Engine Running dengan tombol Stop.
Service tetap jalan saat user pindah aplikasi agar inference dan training tidak putus.
Service berhenti bersih saat user menekan Stop atau saat HP low memory, dengan log penutup.
Health check berkala tiap 10 detik.
Apabila service mati, UI menampilkan FAILED dengan tombol Restart Service.
6.4 Endpoint
Semua response berbentuk JSON dengan field ok, data, dan error.
Daftar endpoint lengkap ada di Bagian 7.
6.5 Requirements Python
Lapis A di dalam APK: numpy 1.26 ke atas, sentencepiece 0.2.0 ke atas, safetensors 0.4.0 ke atas, onnxruntime-mobile 1.17 ke atas, kode asli trainer dan litgpt mode mobile.
Lapis B di server dan PC saja: torch, transformers, accelerate, peft, trl, bitsandbytes, datasets, deepspeed.
Python di-start satu kali saat aplikasi dibuka.
Python dilarang di-start tiap panggil fungsi.
Semua panggilan Python jalan di background thread.
Main thread dilarang dipakai untuk Python.
Exception Python ditangkap dan ditampilkan di UI dan log.
Checkpoint disimpan tiap N step dan dapat di-resume.
Checkpoint dilarang hanya disimpan di RAM.
6.6 UI utama
UI utama: Kotlin Android Native.
Qt6 tetap berada di repo di forge-workbench dan native-qt6 sebagai varian desktop.
Qt6 tidak dijadikan UI utama APK.
UI web Workbench tetap berada di repo untuk PC.
UI web tidak dijadikan UI utama APK.
Tema UI: Dark, Light, System.
Bahasa UI utama: Indonesia, dengan pilihan English di Settings.
Jumlah layar utama: tujuh layar, ditambah layar pendukung.
6.7 Model GGUF pertama
Nama: Qwen2.5-0.5B-Instruct-GGUF.
File: qwen2.5-0.5b-instruct-q4_k_m.gguf.
Ukuran: sekitar 397 MB, varian Q4_K_M.
URL resmi: https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF
File langsung: https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf
Lisensi: Apache 2.0, tetap ditampilkan di layar Licenses.
SHA256 dicek langsung di halaman Hugging Face saat download.
Prompt test: Halo, jelaskan dalam 2 kalimat apa itu Forge.
Setting test: max tokens 128, temperature 0.7, threads 4 di HP RAM 4GB, threads 6 di HP RAM 8GB ke atas.
Target hasil: keluar teks Indonesia yang nyambung dalam waktu kurang dari 30 detik di HP RAM 4GB.
Batasan RAM: HP RAM 4GB maksimal 0.5B sampai 1B Q4, HP RAM 6GB maksimal 1.5B sampai 3B Q4, HP RAM 8GB ke atas maksimal 3B sampai 7B Q4, 7B hanya apabila HP adem dan storage lega.
UI menolak model yang melebihi RAM dengan pesan jelas.
UI dilarang force close saat model ditolak.
Model berikutnya: Level 2 Qwen2.5-1.5B Q4_K_M sekitar 986 MB, Level 3 Qwen2.5-3B Q4_K_M sekitar 1.9 GB, Level 4 Qwen2.5-7B Q4_K_M sekitar 4.7 GB, HP RAM 8GB ke atas saja. Loncat langsung ke Level 4 dilarang.
6.8 Keystore
Status: dianggap belum ada karena tidak ada bukti keystore lama di repo.
Tindakan: buat baru satu kali.
Perintah buat keystore dijalankan di laptop, bukan di chat.
Perintah: keytool -genkeypair -alias forge -keyalg RSA -keysize 4096 -validity 10000 -keystore forge-release.keystore
Penyimpanan file keystore: backup di dua tempat aman, di luar repo.
Penyimpanan password dan keystore di GitHub: memakai GitHub Secrets Actions.
Nama secrets: ANDROID_KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
Nilai KEY_ALIAS: forge.
Keystore dan password dilarang di-commit ke repo.
Keystore dan password dilarang dikirim ke chat, email, atau screenshot.
Keystore dilarang diganti setelah rilis pertama.
Penggantian keystore setelah rilis pertama membuat APK lama tidak dapat update dan harus install ulang.
6.9 Target rilis
Tahap 1: GitHub Releases, versi v3.0.0-production.
Isi Tahap 1: APK signed, SHA256SUMS.txt, Release Notes, link Privacy Policy, screenshot dan video bukti engine jalan.
Tahap 2: Google Play, versi v3.0.1 atau v3.1, setelah satu sampai dua minggu stabil.
Bentuk ke Play: Android App Bundle, bukan APK.
Syarat ke Play: crash nol di GitHub selama satu sampai dua minggu, Privacy Policy URL aktif, ikon dan screenshot dan listing lengkap, targetSdk 34, tidak ada permission berlebihan.
Jadwal: Hari 1 sampai 2 isi JNI dan Python, build debug, test HP. Hari 3 QA 12 test, perbaiki crash. Hari 4 rilis GitHub v3.0.0. Minggu 2 pantau Issues, patch v3.0.1 apabila perlu. Minggu 3 siapkan Play, rilis v3.1.
6.10 Versioning
versionName: 3.0.0-production.
versionCode: 1 untuk rilis pertama production.
Setiap patch menambah versionCode 1.
Setiap minor menambah versionName minor dan versionCode.
Setiap major menambah versionName major dan versionCode.
Changelog wajib untuk setiap versionCode.
APK debug memakai applicationIdSuffix .debug agar tidak bentrok dengan APK release di HP yang sama.
7. SPESIFIKASI SYSTEM forge-editor LENGKAP
7.1 Peran
forge-editor adalah system yang menjalankan server lokal.
forge-editor mengatur HTTP, WebSocket, proxy, health, dan konektivitas editor.
forge-editor menjadi jembatan antara UI dan engine.
forge-editor tidak menggantikan engine, forge-editor mengatur jalannya engine.
7.2 Bentuk Android
ForgeEditorService sebagai Foreground Service.
Service di-start dari ForgeApp dan dari MainActivity apabila belum jalan.
Service menampilkan notifikasi Forge Engine Running.
Notifikasi memiliki tombol Stop dan tombol Buka Aplikasi.
Service memiliki health check internal tiap 10 detik.
Service menulis log start, stop, error, dan restart.
7.3 Endpoint lengkap
GET health: status editor, inference, versi, uptime.
GET models: daftar model.
GET status: status semua engine.
POST inference dan generate: model, prompt, max tokens, temperature, streaming.
POST compiler dan compile: input, output, target.
POST trainer dan start: config, job id.
GET trainer dan status: job id, progress, loss, step, checkpoint.
POST trainer dan pause: job id.
POST trainer dan resume: job id.
POST trainer dan stop: job id.
POST litgpt dan run: tugas, params.
GET editor dan files: daftar file.
GET editor dan file: isi file.
POST editor dan file: simpan file.
DELETE editor dan file: hapus file.
GET logs: 200 baris log terakhir.
GET diagnostics: RAM, storage, ABI, versi so, versi Python.
POST settings dan update: simpan setting.
GET settings: baca setting.
7.4 Format response
Sukses: ok true, data berisi hasil, error null.
Gagal: ok false, data null, error berisi kode, pesan, dan cara perbaiki.
Semua timestamp memakai format ISO 8601.
Semua ukuran memakai byte dan MB.
Semua error memakai error code yang tercantum di Lampiran.
7.5 WebSocket
WebSocket dipakai untuk streaming token inference.
WebSocket dipakai untuk streaming log training.
WebSocket dipakai untuk streaming log compile.
WebSocket memakai path ws dan log dan ws dan inference.
Apabila WebSocket gagal, fallback ke polling HTTP tiap 1 detik.
Fallback tersebut wajib ada agar HP lama tetap jalan.
7.6 Proxy
Proxy dipakai apabila UI web lama memanggil engine melalui editor.
Proxy meneruskan request ke engine yang sesuai.
Proxy mencatat waktu, status, dan error.
Proxy tidak menyimpan prompt ke file kecuali toggle log prompt aktif.
7.7 Lifecycle Service
Start: buat folder, init logger, start server, health check, notifikasi.
Running: layani request, health check berkala, tulis log.
Low memory: simpan checkpoint training apabila ada, tulis log, kurangi thread.
Stop: hentikan training dengan checkpoint, hentikan server, hapus notifikasi, tulis log penutup.
Restart: apabila service mati tidak sengaja, UI menampilkan tombol Restart Service, dan Service di-start ulang dengan checkpoint terakhir.
7.8 File yang terlibat
ForgeEditorService.kt.
ForgeApp.kt.
NotificationHelper.kt.
Logger.kt.
EngineStatusManager.kt.
Endpoint didefinisikan di ForgeEditorService.kt dan diuji di test Service.
8. SPESIFIKASI forge-compiler LENGKAP
8.1 Peran
forge-compiler adalah compiler dan runtime deployment model.
forge-compiler mengubah model menjadi bentuk siap inference untuk target CPU, GPU, dan backend tertentu.
forge-compiler tidak menghapus model asli, forge-compiler membuat file hasil baru.
8.2 Input dan output
Input: path model GGUF atau safetensors, target CPU atau GPU, opsi optimasi, path output.
Output: file model ter-compile, ukuran output, waktu compile, log compile, dan status.
Output disimpan di folder outputs dan compile.
Output dapat dipakai langsung di layar Inference melalui tombol Pakai Hasil untuk Inference.
8.3 Target
Target utama: CPU arm64-v8a.
Target kedua: CPU x86_64 emulator.
Target GPU: hanya apabila source mendukung dan HP mendukung, apabila tidak, fallback CPU.
Target yang tidak didukung ditampilkan dengan pesan jelas dan tidak dapat dipilih.
8.4 JNI
File: forge_compiler_jni.cpp, forge_compiler_bridge.h, ForgeCompiler.kt.
Fungsi JNI: compileModel, getSupportedTargets, getLastError, getVersion.
Fungsi compileModel menerima inputPath, outputPath, target, options, dan callback progress.
Fungsi compileModel mengembalikan kode sukses atau gagal dan menulis log.
Fungsi getSupportedTargets mengembalikan daftar target yang didukung perangkat ini.
Fungsi getLastError mengembalikan error terakhir dengan kode dan pesan.
8.5 UI Compiler
Pilih model dari daftar Model Manager.
Pilih target dari daftar target yang didukung.
Pilih opsi optimasi: O2 untuk HP, O0 untuk debug.
Tombol Compile.
Progress bar dengan persen nyata dari callback JNI.
Log compile streaming.
Hasil: path, ukuran, waktu, tombol Buka File, tombol Pakai Hasil untuk Inference, tombol Hapus Hasil.
Apabila compile gagal: tampil alasan, tampil log, tombol Coba Lagi, tombol Kembali.
8.6 Error compiler
Model tidak ditemukan: tampil path yang dicari dan tombol Pilih Model Lagi.
Storage penuh: tampil sisa storage dan tombol Bersihkan Cache.
Target tidak didukung: tampil daftar target yang didukung.
RAM tidak cukup: tampil RAM tersedia dan RAM dibutuhkan.
File output tidak dapat ditulis: tampil path dan permission.
Semua error masuk log dan Diagnostics.
8.7 Test compiler
Compile model kecil Qwen2.5-0.5B, output ada, ukuran lebih dari nol, waktu tercatat.
Compile model yang sama dua kali, output konsisten.
Compile dengan target tidak didukung, ditolak dengan pesan.
Compile saat storage hampir penuh, ditolak dengan pesan.
Compile dibatalkan di tengah jalan, file parsial dihapus atau ditandai tidak lengkap.
9. SPESIFIKASI forge-inference LENGKAP
9.1 Peran
forge-inference adalah runtime C dan C++ untuk loading model, GGUF, tokenizer, sampling, KV cache, backend CPU dan GPU, dan serving.
forge-inference adalah engine paling kritis. Apabila inference tidak jalan, APK dinyatakan tidak real.
9.2 Input dan output
Input: path model GGUF, prompt, max tokens, temperature, top-p, top-k, threads, dan opsi KV cache.
Output: token streaming, teks lengkap, jumlah token, waktu, backend yang dipakai, dan log.
Output disimpan di folder outputs dan inference dengan timestamp dan nama model.
9.3 Komponen inference
Loader GGUF: baca header, validasi magic, versi, dan arsitektur.
Tokenizer: encode prompt, decode token, handle special token.
Sampler: temperature, top-p, top-k, repeat penalty, seed.
KV cache: simpan key dan value, hitung ukuran, bersihkan saat unload.
Backend CPU: jalan di semua HP.
Backend GPU: jalan apabila source mendukung dan HP mendukung, apabila tidak, fallback CPU.
Serving: layani request inference melalui Service dan endpoint.
9.4 JNI
File: forge_inference_jni.cpp, forge_inference_bridge.h, ForgeInference.kt.
Fungsi JNI: loadModel, tokenize, generate dengan callback streaming, unloadModel, getInfo, getLastError, getVersion.
Fungsi loadModel menerima path, threads, dan opsi, mengembalikan sukses atau gagal.
Fungsi generate menerima prompt, max tokens, temperature, dan callback, mengembalikan streaming token.
Fungsi unloadModel membebaskan RAM dan KV cache dan menulis log.
Fungsi getInfo mengembalikan versi, backend, KV cache, RAM, dan model yang sedang di-load.
Fungsi getLastError mengembalikan error terakhir.
9.5 UI Inference
Pilih model dari Model Manager.
Tulis prompt di text field multi baris.
Setting: max tokens, temperature, top-p, top-k, threads.
Tombol Generate.
Area output streaming dengan teks yang muncul token demi token.
Tombol Stop untuk membatalkan generate.
Tombol Salin, tombol Simpan Output, tombol Hapus Output.
Tampil info: model, backend, token, waktu, KV cache.
Apabila generate gagal: tampil alasan, log, tombol Coba Lagi.
Apabila model belum dipilih: tampil tombol Pilih Model dan pesan jelas.
9.6 Keamanan RAM
Cek RAM sebelum load model.
Hitung RAM dibutuhkan dari ukuran model ditambah KV cache ditambah overhead.
Apabila RAM tidak cukup, tolak dengan pesan yang berisi RAM tersedia dan RAM dibutuhkan dan tombol Pilih Model Lebih Kecil.
Dilarang force close.
Dilarang load model diam-diam di background tanpa izin.
9.7 Threading
Inference dilarang di main thread.
Inference memakai Coroutine dan Worker.
UI tetap responsif saat inference jalan.
Tombol Cancel harus selalu dapat ditekan.
Apabila aplikasi pindah background, inference tetap jalan melalui Service dan notifikasi menampilkan progress.
9.8 Error inference
Model tidak ditemukan, file rusak, SHA256 tidak cocok, tokenizer gagal, KV cache gagal, OOM, timeout, Service mati.
Semua error memakai error code, pesan, dan cara perbaiki.
Semua error masuk log dan Diagnostics.
9.9 Test inference
Load model Qwen2.5-0.5B, generate 20 token, output tidak kosong, streaming jalan.
Generate 128 token dengan prompt test, output nyambung, waktu kurang dari 30 detik di HP RAM 4GB.
Generate dibatalkan di tengah jalan, RAM dibebaskan.
Load model besar melebihi RAM, ditolak dengan pesan, tidak crash.
Unload model, RAM kembali normal.
Inference offline penuh tanpa internet.
10. SPESIFIKASI forge-trainer LENGKAP
10.1 Peran
forge-trainer adalah engine training dan fine-tuning Unsloth dengan kernel optimized, quantization, precision, dan distributed training.
Di HP, forge-trainer jalan mode mobile.
Di server dan PC, forge-trainer jalan mode server penuh.
Kode trainer tetap dibundel di APK, eksekusi dibedakan agar HP tetap lancar.
10.2 Mode mobile
LoRA kecil, batch kecil, quantization, precision rendah yang aman, checkpoint tiap N step, pause dan resume.
Batch size default: 1 di HP RAM 4GB, 2 di HP RAM 8GB ke atas.
Learning rate default: sesuai config, tidak diubah diam-diam.
Max steps default: 50 untuk test, dapat diubah di UI.
Checkpoint tiap 10 step default, dapat diubah.
10.3 Mode server
Full fine-tuning, distributed training, batch besar, precision penuh.
Di HP, mode server ditampilkan dengan badge SERVER dan tombol Export Config ke Server.
Export Config menghasilkan file JSON yang dapat dipakai di PC dan server.
File config tersebut berisi model, dataset, LoRA, quantization, precision, steps, dan checkpoint.
10.4 Input dan output
Input: model, dataset, config LoRA, quantization, precision, steps, learning rate, checkpoint awal.
Output: checkpoint tiap N step, log loss dan step, file config hasil, dan status job.
Output disimpan di folder checkpoints dan outputs dan trainer.
10.5 Python
File: forge_trainer_mobile.py, PythonBridge.kt.
Fungsi: status, listTasks, trainLoRA, pause, resume, getProgress, getLastError.
Fungsi trainLoRA menerima config dict dan mengembalikan job id.
Fungsi getProgress mengembalikan step, loss, waktu, dan checkpoint terakhir.
Semua fungsi memanggil kode asli forge-trainer, bukan contoh.
10.6 UI Trainer
Pilih model, pilih dataset, pilih config LoRA.
Tampil badge MOBILE atau SERVER.
Tampil RAM dan storage yang dibutuhkan.
Tampil perkiraan waktu apabila dapat dihitung.
Tombol Start, Pause, Resume, Stop.
Progress bar loss dan step dengan data nyata.
Log training streaming.
Daftar checkpoint dengan waktu, step, loss, tombol Lanjut dari Checkpoint, tombol Hapus Checkpoint.
Tombol Export Config ke Server.
Apabila training gagal: tampil alasan, log, tombol Coba Lagi.
10.7 Checkpoint
Checkpoint disimpan tiap N step.
Checkpoint berisi bobot LoRA, step, loss, config, dan waktu.
Checkpoint dapat di-resume setelah aplikasi ditutup dan HP restart.
Checkpoint dilarang hanya disimpan di RAM.
Checkpoint lama dapat dihapus dari UI.
Checkpoint rusak ditolak dengan pesan dan tidak dipakai.
10.8 Error trainer
Dataset tidak ditemukan, config tidak valid, RAM tidak cukup, storage penuh, checkpoint rusak, Python gagal start, Service mati.
Semua error memakai error code, pesan, dan cara perbaiki.
Semua error masuk log dan Diagnostics.
10.9 Test trainer
Train LoRA kecil 5 step, checkpoint tersimpan, resume jalan.
Train dibatalkan di tengah jalan, checkpoint terakhir dapat di-resume.
Train dengan config tidak valid, ditolak dengan pesan.
Train dengan storage hampir penuh, ditolak dengan pesan.
Train offline penuh.
11. SPESIFIKASI forge-litgpt LENGKAP
11.1 Peran
forge-litgpt adalah engine Python untuk pretraining, full fine-tuning, LoRA dan adapter, generation, evaluasi, conversion, dan serving.
Di HP, litgpt jalan mode mobile untuk tugas ringan dan menengah.
Di server dan PC, litgpt jalan mode server penuh untuk pretraining full.
11.2 Tugas litgpt
Generate: hasilkan teks dari prompt dan model.
Eval: evaluasi model dengan dataset eval, hasilkan skor dan laporan.
Convert: ubah format model, misalnya safetensors ke GGUF atau sebaliknya, sesuai kemampuan source asli.
Serve: jalankan server litgpt lokal untuk inference.
LoRA: training LoRA dan adapter.
Pretrain: hanya mode server, di HP ditampilkan badge SERVER dan tombol Export Config.
11.3 Input dan output
Input: tugas, model, dataset, params, output path.
Output: teks hasil, skor eval, file model hasil convert, log, dan status.
Output disimpan di folder outputs dan litgpt.
11.4 Python
File: forge_litgpt_mobile.py, PythonBridge.kt.
Fungsi: status, generate, evaluate, convert, serve, trainLoRA, getLastError.
Semua fungsi memanggil kode asli forge-litgpt.
11.5 UI LitGPT
Pilih tugas dari daftar Generate, Eval, Convert, Serve, LoRA, Pretrain.
Form berubah sesuai tugas yang dipilih.
Tampil badge MOBILE atau SERVER.
Tombol Jalan.
Output tampil sesuai tugas: teks, skor, file hasil, log.
Tombol Salin, Simpan, Buka File Hasil, Hapus.
Apabila tugas gagal: tampil alasan, log, tombol Coba Lagi.
11.6 Error litgpt
Tugas tidak valid, model tidak ditemukan, dataset tidak ditemukan, convert gagal, serve gagal start, Python gagal start.
Semua error memakai error code, pesan, dan cara perbaiki.
Semua error masuk log dan Diagnostics.
11.7 Test litgpt
Generate dengan model Qwen2.5-0.5B, output tidak kosong.
Eval dengan dataset kecil, skor dan laporan ada.
Convert model kecil, file hasil ada dan dapat dipakai inference.
Serve jalan dan dapat dipanggil melalui endpoint.
LoRA kecil 5 step, checkpoint ada.
Pretrain di HP ditampilkan badge SERVER dan tidak dipaksa jalan.
12. SPESIFIKASI UI LENGKAP PER LAYAR
12.1 Dashboard
Judul: Forge.
Subjudul: versi app dan versi engine.
Kartu Editor: status, bukti, tombol Buka, tombol Test Cepat.
Kartu Compiler: status, bukti, tombol Buka, tombol Test Cepat.
Kartu Inference: status, bukti, tombol Buka, tombol Test Cepat.
Kartu Trainer: status, bukti, tombol Buka, tombol Test Cepat.
Kartu LitGPT: status, bukti, tombol Buka, tombol Test Cepat.
Kartu Model: jumlah model, total ukuran, tombol Kelola Model.
Kartu Storage: sisa storage, total storage, tombol Bersihkan Cache.
Kartu RAM: RAM tersedia, RAM terpakai.
Tombol Jalankan Semua Test.
Tombol Export Log.
Tombol Diagnostics.
12.2 Editor
Daftar file folder Forge dengan nama, ukuran, waktu ubah.
Tombol Buat File Baru.
Tombol Cari.
Tombol Refresh.
Buka file: tampil isi, edit, simpan, batal.
Tombol Hapus File dengan konfirmasi.
Tombol Salin Path.
Tampil health Service.
Apabila Service mati: tampil FAILED dan tombol Restart Service.
12.3 Inference
Sudah dijelaskan di Bagian 9.
Tambahan: riwayat inference dengan waktu, model, prompt, output, tombol Buka, Hapus.
Tambahan: template prompt: Sapaan, Ringkasan, Penjelasan, Kode.
Tambahan: toggle Simpan Riwayat, default aktif.
12.4 Compiler
Sudah dijelaskan di Bagian 8.
Tambahan: riwayat compile dengan waktu, input, output, ukuran, status.
12.5 Trainer
Sudah dijelaskan di Bagian 10.
Tambahan: template config: LoRA Kecil, LoRA Menengah, Test 5 Step.
Tambahan: grafik loss sederhana dari data nyata.
12.6 LitGPT
Sudah dijelaskan di Bagian 11.
Tambahan: riwayat tugas dengan waktu, tugas, status, file hasil.
12.7 Model Manager
Daftar model dengan nama, ukuran, SHA256, status, backend yang cocok.
Tombol Download dari URL resmi.
Tombol Import dari storage.
Tombol Verifikasi SHA256.
Tombol Hapus dengan konfirmasi.
Tombol Detail dengan tokenizer, arsitektur, ukuran, RAM dibutuhkan.
Progress download dengan persen nyata, pause, resume, batal.
Apabila download gagal: tampil alasan dan tombol Coba Lagi.
Apabila SHA256 tidak cocok: tolak file, tampil SHA yang diharapkan dan SHA yang didapat, tombol Hapus File Rusak.
12.8 Diagnostics
Versi app, versi tiap so, hasil import Python, RAM, storage, ABI, versi Android, model HP, uptime Service.
Status semua engine dengan bukti.
Log terakhir 200 baris dengan filter Error, Warning, Info.
Tombol Jalankan Semua Test.
Tombol Export Log.
Tombol Salin Diagnostics.
Tombol Hapus Log Lama.
Tombol Kirim Laporan Crash opsional dengan izin.
12.9 Settings
Port editor: 8787 default, dapat diubah, restart Service setelah ubah.
Folder model: path default, tombol Ubah, tombol Reset.
Batas RAM inference: slider dan angka.
Batas thread inference: slider dan angka.
Tema: Dark, Light, System.
Bahasa: Indonesia, English.
Toggle Simpan Riwayat.
Toggle Sertakan Prompt di Log, default OFF.
Toggle Notifikasi Training dan Download.
Tombol Hapus Cache.
Tombol Reset Semua Setting dengan konfirmasi.
Tombol Export Config.
Tombol Import Config.
12.10 Licenses
Daftar lisensi dari folder LICENSES.
Daftar lisensi pihak ketiga: Kotlin, Ktor, Chaquopy, numpy, sentencepiece, safetensors, onnxruntime-mobile, Qwen.
Tiap lisensi dapat dibuka dan dibaca penuh.
Tombol Salin Lisensi.
12.11 About
Nama aplikasi: Forge.
Versi app, versi engine, commit, link repo, link release, link privacy, link issues.
Tombol Cek Update.
Tombol Buka Repo.
Tombol Buka Release.
12.12 Onboarding
Halaman 1: penjelasan engine ada di dalam HP, model download terpisah.
Halaman 2: minta permission yang diperlukan.
Halaman 3: buat folder, cek storage, cek RAM.
Halaman 4: pilih model pertama dan download atau lewati.
Tombol Mulai.
Onboarding hanya tampil pertama buka, dapat dibuka lagi dari Settings dan About.
12.13 Help
Cara pakai tiap layar dengan langkah singkat.
Cara download model.
Cara training LoRA kecil.
Cara export log saat lapor bug.
Cara cek update.
13. SPESIFIKASI NAVIGASI, STATE, ERROR, LOADING, EMPTY
13.1 Navigasi
Bottom navigation: Dashboard, Editor, Inference, Compiler, Trainer, LitGPT.
Drawer atau menu atas: Model Manager, Diagnostics, Settings, Licenses, About, Help.
Back button kembali ke layar sebelumnya, tidak menutup aplikasi kecuali di Dashboard.
Deep link internal: dari Dashboard ke layar engine, dari Model Manager ke Inference, dari Compiler ke Inference.
13.2 State
LOADING: tampil progress dan teks sedang apa yang sedang dimuat.
READY: tampil bukti versi, path, backend, dan tombol aksi.
FAILED: tampil alasan, kode error, log terkait, tombol Coba Lagi, tombol Buka Diagnostics.
EMPTY: apabila belum ada model, tampil pesan belum ada model dan tombol Download Model dan tombol Import Model.
EMPTY: apabila belum ada checkpoint, tampil pesan belum ada checkpoint dan tombol Mulai Training.
EMPTY: apabila belum ada riwayat, tampil pesan belum ada riwayat dan tombol Mulai.
13.3 Error
Semua error memakai kode, pesan, dan cara perbaiki.
Error ditampilkan di UI dan ditulis ke log.
Error tidak menutup aplikasi diam-diam.
Error dengan tombol aksi: Coba Lagi, Buka Diagnostics, Pilih Model Lagi, Bersihkan Cache, Restart Service.
13.4 Loading
Semua loading memakai progress nyata dari callback.
Loading tanpa progress nyata dilarang memakai animasi palsu yang mengaku persen.
Loading yang lama menampilkan estimasi waktu apabila dapat dihitung dan tombol Batal.
13.5 Empty dan placeholder
Text field prompt memiliki placeholder contoh prompt.
Text field config memiliki placeholder contoh nilai.
Daftar kosong memiliki pesan dan tombol aksi.
Gambar placeholder tidak dipakai untuk menutupi data yang hilang.
14. SPESIFIKASI DATA, STORAGE, FILE, MODEL, CHECKPOINT, LOG, CONFIG, BACKUP
14.1 Struktur folder
Forge sebagai folder utama di storage aplikasi.
Forge dan models: model GGUF dan safetensors.
Forge dan checkpoints: checkpoint trainer dan litgpt.
Forge dan outputs: output inference, compile, litgpt.
Forge dan outputs dan inference: output inference.
Forge dan outputs dan compile: output compile.
Forge dan outputs dan litgpt: output litgpt.
Forge dan outputs dan trainer: output trainer.
Forge dan logs: log aplikasi, log native, log Python, log crash.
Forge dan configs: config trainer, config litgpt, config aplikasi export.
Forge dan cache: cache download dan cache tokenizer, dapat dihapus.
Forge dan editor: file editor.
14.2 File
Nama file output memakai pola: tipe, tanggal, waktu, model, dan ekstensi.
Contoh: inference_2026-11-07_20-00-00_qwen2-5-0-5b.txt
Contoh: compile_2026-11-07_20-00-00_qwen2-5-0-5b.gguf
Contoh: checkpoint_trainer_2026-11-07_20-00-00_step-50.zip
File lama dapat dihapus dari UI.
File di folder Forge tidak dihapus otomatis kecuali cache dan log lama.
14.3 Model
Model disimpan di folder models.
Model memiliki metadata: nama, ukuran, SHA256, arsitektur, tokenizer, tanggal download, sumber URL.
Metadata disimpan di file models.json di folder models.
Model rusak ditandai dan tidak dipakai.
Model yang SHA256 tidak cocok ditolak dan ditandai rusak.
14.4 Checkpoint
Checkpoint disimpan di folder checkpoints.
Checkpoint memiliki metadata: engine, step, loss, config, waktu, model.
Metadata disimpan di file checkpoints.json.
Checkpoint dapat di-resume, dihapus, dan di-export.
Checkpoint rusak ditandai dan tidak dipakai.
14.5 Log
Log disimpan di folder logs.
Log dibagi: app.log, native.log, python.log, editor.log, crash.log.
Format log: waktu ISO 8601, level, modul, pesan, kode error apabila ada.
Contoh: 2026-11-07T20:00:00+08:00 INFO inference loadModel sukses path=/Forge/models/qwen2.5-0.5b-instruct-q4_k_m.gguf
Rotasi log: file log lama di atas 10 MB dibuat file baru, maksimal 10 file lama.
Log lama di atas 30 hari dapat dihapus dari Settings.
Prompt tidak masuk log kecuali toggle Sertakan Prompt di Log aktif.
API key dan password tidak masuk log.
14.6 Config
Config aplikasi disimpan di DataStore.
Config trainer disimpan sebagai JSON di folder configs.
Config litgpt disimpan sebagai JSON di folder configs.
Config dapat di-export dan di-import.
Config yang tidak valid ditolak dengan pesan dan field yang salah ditandai.
14.7 Backup dan restore
Export Config: menghasilkan file JSON config aplikasi, trainer, dan litgpt.
Export Log: menghasilkan file ZIP log.
Export Checkpoint: menghasilkan file ZIP checkpoint yang dipilih.
Import Config: membaca file JSON, validasi, lalu simpan.
Backup model otomatis dilarang karena terlalu besar.
Restore tidak menghapus model kecuali user memilih.
15. SPESIFIKASI KEAMANAN, PRIVASI, PERMISSION, JARINGAN
15.1 Keamanan
Semua proses lokal secara default.
Prompt, model, dan file user tidak dikirim ke server manapun tanpa izin jelas.
Server lokal hanya 127.0.0.1, tidak 0.0.0.0.
Tidak ada auto-install APK baru diam-diam.
Update dicek dari GitHub Releases, download hanya setelah user menekan tombol.
Keystore dan password tidak di-commit ke repo.
Token dan password tidak disimpan di log.
File sensitif disimpan di storage internal aplikasi, bukan storage publik apabila memungkinkan.
15.2 Privasi
Privacy Policy ditulis di docs dan PRIVACY-POLICY-ID.md.
Privacy Policy ditampilkan di aplikasi dan di link release.
Privacy Policy berisi data apa yang diproses lokal, data apa yang tidak dikirim, log apa yang diexport, izin apa yang diminta dan kenapa.
Laporan crash dikirim hanya apabila user menekan tombol Kirim Laporan.
Prompt tidak masuk log kecuali toggle aktif.
15.3 Permission
Internet: untuk download model dan cek update.
Foreground Service: untuk Service editor dan training background.
Post Notifications: untuk notifikasi Service, training, download, compile.
Media dan storage: untuk import model dan file editor, memakai scoped storage Android 13 ke atas.
Manage External Storage tidak dipakai kecuali benar-benar perlu dan alasan ditulis di listing Play.
Permission diminta saat dibutuhkan, bukan semua di awal.
Apabila permission ditolak, UI menampilkan alasan dan tombol Buka Pengaturan.
15.4 Jaringan
Download model memakai HTTPS.
Cek update memakai HTTPS GitHub Releases.
Tidak ada HTTP tanpa enkripsi untuk download model.
Download mendukung pause, resume, dan verifikasi SHA256.
Timeout download: 30 detik untuk connect, 60 detik untuk baca.
Retry download maksimal 3 kali dengan pesan jelas.
16. SPESIFIKASI PERFORMA, BATERAI, RAM, STORAGE, THREADING, LIFECYCLE
16.1 Performa
Buka aplikasi kurang dari 2 detik di HP RAM 4GB untuk tampil Dashboard.
Service start kurang dari 2 detik.
Python start kurang dari 3 detik.
Health check kurang dari 1 detik.
Inference 128 token kurang dari 30 detik untuk model Qwen2.5-0.5B di HP RAM 4GB.
UI tetap responsif saat inference, compile, training, dan download jalan.
Frame drop dan freeze di main thread dilarang.
16.2 Baterai
Inference memakai thread terbatas.
Training memakai batch kecil.
Download memakai jaringan WiFi apabila memungkinkan, dengan opsi download pakai data seluler yang harus diizinkan user.
Service tidak melakukan polling berlebihan, health check tiap 10 detik.
Apabila baterai di bawah 15 persen, tampil peringatan sebelum training dan compile panjang.
16.3 RAM
Cek RAM sebelum load model, compile, dan training.
RAM dibutuhkan dihitung dari ukuran model ditambah KV cache ditambah overhead.
Apabila RAM tidak cukup, tolak dengan pesan.
Unload model membebaskan RAM dan KV cache.
Cache dibersihkan apabila RAM menipis.
16.4 Storage
Cek storage sebelum download model, compile, dan training.
Apabila storage kurang dari 500 MB, tampil peringatan.
Apabila storage kurang dari 100 MB, tolak download dan training dengan pesan.
Cache dapat dihapus dari Settings.
Log lama dapat dihapus dari Settings.
16.5 Threading
Main thread hanya untuk UI.
Inference, compile, training, download, dan Python jalan di background thread.
Callback ke UI memakai main thread.
Tombol Cancel harus selalu dapat ditekan.
Race condition dihindari dengan satu sumber kebenaran status.
16.6 Lifecycle
Aplikasi pindah background: Service tetap jalan, training tetap jalan, notifikasi tampil progress.
Aplikasi dibunuh sistem: checkpoint tersimpan, status dapat di-resume saat dibuka lagi.
Layar diputar: progress tidak hilang, tidak restart job.
HP restart: Service tidak auto-start kecuali user membuka aplikasi, checkpoint tetap ada.
HP low memory: simpan checkpoint, kurangi thread, tulis log.
17. SPESIFIKASI ANDROID LENGKAP
17.1 Manifest
Permission: Internet, Foreground Service, Post Notifications, media dan storage sesuai scoped storage.
Activity: MainActivity exported true dengan intent MAIN dan LAUNCHER.
Service: ForgeEditorService foregroundServiceType dataSync exported false.
Receiver: apabila perlu untuk download selesai dan boot, dengan permission yang benar.
Application: label Forge, theme DayNight, allowBackup true, supportsRtl false apabila belum siap, usesCleartextTraffic false.
Semua permission dijelaskan di Privacy Policy dan di listing Play.
17.2 Gradle
Plugin: com.android.application, org.jetbrains.kotlin.android, com.chaquopy.python.
Namespace: com.forge.app.
CompileSdk: 34.
MinSdk: 26.
TargetSdk: 34.
ApplicationId: com.forge.app.
VersionCode: 1 untuk rilis pertama production.
VersionName: 3.0.0-production.
ABI: arm64-v8a.
Python: buildPython python3.11.
BuildTypes: release dengan minifyEnabled false untuk rilis pertama agar debug mudah, debug dengan applicationIdSuffix .debug.
SigningConfigs release: storeFile forge-release.keystore, storePassword dari env KEYSTORE_PASSWORD, keyAlias dari env KEY_ALIAS, keyPassword dari env KEY_PASSWORD.
ExternalNativeBuild: cmake path src dan main dan cpp dan CMakeLists.txt.
Dependencies: core-ktx, appcompat, coroutines-android, ktor-server-core, ktor-server-netty atau nanohttpd, chaquopy, lifecycle, datastore.
17.3 CMake
Minimum CMake: 3.22.1.
Standar C++: 17.
Target: forge_inference SHARED, forge_compiler SHARED.
Link: log, android.
Optimasi release: O2.
Strip symbol release, simpan symbol terpisah.
Sumber asli diarahkan dari forge-engine, tidak diduplikasi.
17.4 JNI
LoadLibrary di init Kotlin.
Fungsi JNI memakai nama lengkap paket com.forge.app.
Semua fungsi JNI mengembalikan kode dan error.
Semua fungsi JNI menulis log native.
Semua fungsi JNI tidak crash apabila input null, mengembalikan error.
17.5 Python Chaquopy
Python di-start satu kali.
Requirements mobile di-install dari requirements-mobile.txt.
Panggilan Python melalui PythonBridge.
Exception Python ditangkap dan diubah menjadi error UI.
Python tidak dipakai untuk server, hanya untuk trainer dan litgpt.
17.6 Service dan Broadcast
Service memakai startForeground dengan notifikasi.
Service memakai stopSelf apabila user menekan Stop.
Broadcast untuk download selesai memakai DownloadManager receiver.
Broadcast untuk boot tidak auto-start Service kecuali diperlukan dan diizinkan.
17.7 Notification
Notifikasi Service: Forge Engine Running, tombol Stop, tombol Buka Aplikasi.
Notifikasi Training: progress step dan loss, tombol Pause, tombol Stop.
Notifikasi Download: progress persen, tombol Pause, tombol Batal.
Notifikasi Compile: progress persen, tombol Batal.
Notifikasi memakai channel terpisah: Service, Training, Download, Compile.
Notifikasi tidak memakai suara berlebihan.
17.8 Resources
Strings: semua teks UI di strings.xml, tidak hardcode di Kotlin kecuali log.
Colors: warna Dark, Light, System, primary, error, success, warning.
Themes: Theme.AppCompat.DayNight dan turunannya.
Dimens: margin, padding, radius, text size.
Layout: layout tiap layar dengan nama jelas.
Drawable: ikon vector, background, divider.
Mipmap: launcher icon tiap densitas mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi.
Assets: licenses, privacy, help, contoh config, prompt contoh.
18. SPESIFIKASI BUILD LENGKAP
18.1 Build debug
Perintah: gradlew assembleDebug.
Hasil: APK debug di app dan build dan outputs dan apk dan debug.
APK debug memakai applicationIdSuffix .debug.
APK debug tidak boleh dirilis sebagai production.
Build debug jalan tiap push di CI.
18.2 Build release
Perintah: gradlew assembleRelease.
Hasil: APK release di app dan build dan outputs dan apk dan release.
APK release memakai signing keystore production.
APK release memakai versionCode dan versionName production.
Build release hanya jalan di tag v dan workflow release dan workflow dispatch dengan persetujuan.
18.3 Signing
Keystore dibuat satu kali dengan keytool.
Keystore disimpan di GitHub Secrets sebagai base64.
Workflow release mendecode keystore ke file forge-release.keystore.
Workflow release membaca password dari Secrets.
Keystore tidak di-commit ke repo.
APK release diverifikasi dengan SHA256 dan verify_apk.py.
18.4 Versioning
versionCode naik 1 tiap rilis.
versionName mengikuti semantic versioning.
Changelog wajib untuk tiap versionCode.
APK debug dan APK release tidak bentrok di HP yang sama karena applicationIdSuffix.
18.5 Artifact
Artifact CI debug: APK debug.
Artifact CI release: APK release dan SHA256SUMS.txt.
Artifact release GitHub: APK release, SHA256SUMS.txt, Release Notes.
Artifact Play: AAB release dan mapping dan symbol.
Semua artifact disimpan dengan nama jelas dan tanggal.
19. SPESIFIKASI CI DAN CD LENGKAP
19.1 Workflow debug
Trigger: push, pull request, workflow dispatch.
Langkah: checkout, setup Java 17, setup Android SDK, setup Python 3.11, install NDK dan CMake, check semua engine, build debug, verifikasi APK, upload artifact debug.
Apabila check gagal, build dihentikan dan tidak upload artifact.
Apabila verifikasi APK gagal, build dihentikan.
19.2 Workflow release
Trigger: push tag v dan workflow dispatch.
Langkah: checkout, setup Java 17, setup Android SDK, setup Python 3.11, install NDK dan CMake, check semua engine, decode keystore dari Secrets, build release, verifikasi APK, hitung SHA256, buat GitHub Release publik dengan file APK dan SHA256 dan Release Notes.
Draft false, prerelease false untuk production.
Apabila check gagal, rilis dibatalkan.
Apabila signing gagal, rilis dibatalkan.
19.3 Secrets
ANDROID_KEYSTORE_BASE64.
KEYSTORE_PASSWORD.
KEY_ALIAS.
KEY_PASSWORD.
Secrets tidak ditampilkan di log.
Secrets tidak di-commit ke repo.
19.4 Cache CI
Cache Gradle dipakai agar build lebih cepat.
Cache NDK dipakai apabila memungkinkan.
Cache tidak menyimpan keystore.
20. SPESIFIKASI TESTING LENGKAP
20.1 Checker file
check_all_engines.py cek file JNI, Python, Kotlin, Manifest, CMake, Service ada.
check_all_engines.py cek tidak ada teks palsu READY tanpa load.
check_all_engines.py gagalkan build apabila ada file hilang atau TODO belum diisi.
20.2 Verifier APK
verify_apk.py bongkar APK sebagai ZIP.
verify_apk.py cek lib arm64-v8a dan file so ada.
verify_apk.py cek assets Python ada.
verify_apk.py cek AndroidManifest ada Service.
verify_apk.py cek classes.dex ada.
verify_apk.py cek tidak ada file pyc palsu saja.
20.3 Test fungsional di HP
T-01: aplikasi buka, Service jalan, health 200.
T-02: inference load model kecil, generate 20 token, output tidak kosong, streaming jalan.
T-03: compiler compile model kecil, file output ada, ukuran lebih dari nol.
T-04: trainer mobile LoRA 5 step, checkpoint tersimpan, resume jalan.
T-05: litgpt generate, eval, convert, file output ada.
T-06: editor buat, edit, simpan file lewat Service.
T-07: Model Manager download dan import dengan SHA256, tolak file rusak.
T-08: matikan internet, ulangi T-02 sampai T-06, semua jalan.
T-09: coba model melebihi RAM, ditolak dengan pesan, tidak crash.
T-10: tutup aplikasi paksa, buka lagi, checkpoint dan status pulih.
T-11: rotasi dan background, aplikasi tidak kehilangan progress saat layar diputar dan pindah aplikasi.
T-12: log semua test meninggalkan log di folder logs dan tampil di Diagnostics.
20.4 Test tambahan
T-13: download model 397 MB, pause, resume, verifikasi SHA256 sukses.
T-14: download model rusak, SHA256 tidak cocok, ditolak.
T-15: compile dibatalkan, file parsial dihapus atau ditandai tidak lengkap.
T-16: training dibatalkan, checkpoint terakhir dapat di-resume.
T-17: Service dimatikan paksa, UI tampil FAILED dan tombol Restart Service jalan.
T-18: storage hampir penuh, download dan training ditolak dengan pesan.
T-19: baterai di bawah 15 persen, peringatan tampil sebelum training panjang.
T-20: permission ditolak, UI tampil alasan dan tombol Buka Pengaturan.
T-21: onboarding tampil pertama buka, tidak tampil lagi setelah selesai, dapat dibuka dari Settings.
T-22: tema Dark, Light, System berubah dengan benar.
T-23: bahasa Indonesia dan English berubah dengan benar.
T-24: export log menghasilkan ZIP dan dapat dibuka.
T-25: export config menghasilkan JSON dan dapat di-import.
T-26: cek update dari GitHub Releases tampil changelog dan tombol Download.
T-27: aplikasi jalan di HP RAM 4GB, HP RAM 8GB, emulator x86_64.
T-28: aplikasi jalan offline penuh setelah model terdownload.
20.5 Test unit dan integrasi
Unit test Kotlin untuk FileManager, ModelManager, SettingsManager, Logger, EngineStatusManager.
Unit test Python untuk status trainer dan litgpt dan validasi model.
Integrasi test JNI untuk load, generate, unload, compile.
Integrasi test Service untuk health, models, status, inference, editor.
Semua test unit dan integrasi jalan di CI apabila memungkinkan, dan hasil dicatat.
21. SPESIFIKASI QA LENGKAP
21.1 QA manual
QA dilakukan di minimal tiga perangkat: HP arm64 RAM 4GB, HP arm64 RAM 8GB ke atas, emulator x86_64 untuk UI saja.
QA mengisi CHECKLIST-QA.md dengan nama dan tanggal.
QA tidak menandatangani apabila ada test yang gagal.
QA melampirkan screenshot dan video bukti engine jalan.
QA melampirkan log export apabila ada bug.
21.2 QA crash
Crash native, Kotlin, dan Python ditangkap dan disimpan di folder logs dan crash.
Crash ditampilkan di Diagnostics.
Crash diperbaiki dalam 24 sampai 72 jam pertama setelah rilis.
Patch v3.0.1 dibuat apabila ada crash utama.
Rilis v3.1 dilarang sebelum crash utama beres.
21.3 QA performa
Buka aplikasi, Service start, Python start, health check diukur waktunya.
Inference 128 token diukur waktunya.
Compile model kecil diukur waktunya.
Training 5 step diukur waktunya.
Download 397 MB diukur waktunya di WiFi.
Semua waktu dicatat di QA dan dibandingkan dengan target di Bagian 16.
21.4 QA UX
Semua tombol dapat ditekan dan menghasilkan aksi.
Semua pesan error dapat dibaca dan dimengerti.
Semua loading menampilkan progress nyata.
Semua layar kosong menampilkan pesan dan tombol aksi.
Semua teks tidak terpotong di layar kecil.
Semua warna terbaca di tema Dark dan Light.
22. SPESIFIKASI RILIS GITHUB LENGKAP
Tag: v3.0.0-production.
Nama rilis: Forge REAL APK v3.0.0, Semua System dan Semua Engine, Production.
Isi rilis: APK signed release, bukan debug, SHA256 APK, changelog, cara install, cara pakai, link Privacy Policy, Issues, Repo, Docs, video dan screenshot bukti engine jalan, pernyataan jujur model tidak termasuk di APK dan training full dan distributed mode SERVER.
Cara install: Android 8 ke atas, arm64-v8a, RAM 4GB, storage kosong minimal 2GB.
Cara pakai: buka aplikasi, Model Manager, download GGUF, Inference.
Signing: buat keystore satu kali, simpan keystore dan password di GitHub Secrets, jangan commit keystore dan password ke repo, jangan share keystore.
APK debug dilarang dirilis sebagai production.
Privacy Policy wajib sebelum Play dan sebelum klaim publik.
Privacy Policy berisi data apa yang diproses lokal, data apa yang tidak dikirim, log apa yang diexport, izin apa yang diminta dan kenapa.
Privacy Policy ditulis di docs dan PRIVACY-POLICY-ID.md, ditampilkan di aplikasi, dan link di release.
Release Notes memakai template di scripts dan RELEASE_NOTES_v3.0.0_ID.md.
SHA256 dihitung dengan sha256sum dan disimpan di SHA256SUMS.txt.
Rilis GitHub memakai workflow release, bukan upload manual kecuali darurat.
23. SPESIFIKASI RILIS GOOGLE PLAY LENGKAP
Versi Play: v3.0.1 atau v3.1, setelah satu sampai dua minggu stabil di GitHub.
Bentuk Play: Android App Bundle, bukan APK.
TargetSdk: 34.
Permission: minimal, sesuai kebutuhan, dijelaskan di listing.
Privacy Policy URL: aktif dan dapat dibuka.
Listing: nama aplikasi, deskripsi singkat, deskripsi lengkap, ikon, screenshot HP dan tablet, video demo, kategori, tag, kontak developer.
Content rating: diisi dengan jujur melalui kuesioner Play.
Data safety: diisi dengan jujur, proses lokal, tidak kirim prompt tanpa izin, log export hanya apabila user menekan tombol.
Target audiens: dewasa, bukan anak-anak, apabila ada klaim AI dan data.
Harga: gratis untuk rilis pertama, apabila ada fitur berbayar di versi berikutnya, jelaskan dengan jelas.
Update Play: versionCode naik, changelog diisi, AAB di-upload, review ditunggu.
Penolakan Play: baca alasan penolakan, perbaiki, upload ulang, jangan kirim ulang tanpa perbaikan.
Crash Play: pantau Android Vitals, perbaiki crash utama dalam 24 sampai 72 jam.
24. SPESIFIKASI DOKUMENTASI, README, CHANGELOG, PRIVACY, LICENSES
24.1 README
README berisi judul, deskripsi singkat, fitur utama, cara install Android, cara pakai, cara download model, cara training LoRA kecil, cara export log, link catatan lengkap, link release, link privacy, link issues.
README tidak berisi klaim yang tidak dapat dibuktikan.
README menunjuk ke CATATAN-SUPER-LENGKAP-FINAL.md sebagai acuan utama.
README menunjuk ke docs dan archive untuk catatan lama.
24.2 Changelog
Changelog berisi versi, tanggal, fitur baru, perbaikan, batasan, dan cara update.
Changelog ditulis di RELEASE_NOTES_v3.0.0_ID.md untuk v3.0.0.
Changelog berikutnya ditulis di file release notes versi tersebut.
Changelog jujur tentang mode SERVER dan batasan HP.
24.3 Privacy
Privacy Policy ditulis di docs dan PRIVACY-POLICY-ID.md.
Privacy Policy ditampilkan di aplikasi di layar Licenses dan About dan Onboarding.
Privacy Policy berisi data lokal, data tidak dikirim, log, izin, dan kontak.
24.4 Licenses
Licenses dari folder LICENSES ditampilkan di layar Licenses.
Lisensi pihak ketiga ditampilkan: Kotlin, Ktor, Chaquopy, numpy, sentencepiece, safetensors, onnxruntime-mobile, Qwen.
Lisensi model Qwen Apache 2.0 ditampilkan.
Lisensi yang hilang ditambahkan sebelum rilis.
24.5 Help
Help di aplikasi berisi cara pakai tiap layar.
Help berisi cara download model, cara training, cara export log, cara cek update.
Help memakai bahasa Indonesia yang jelas.
25. SPESIFIKASI OPERASIONAL PASCA RILIS, MAINTENANCE, UPDATE, HOTFIX
25.1 Pantau Issues
Issues GitHub dipantau tiap hari selama dua minggu pertama.
Issues diberi label: bug, crash, inference, compiler, trainer, litgpt, editor, model, UI, docs.
Issues crash diprioritaskan.
Issues dijawab dengan permintaan log export dan diagnostics, bukan tebakan.
25.2 Hotfix
Hotfix memakai branch hotfix dan versi patch.
Hotfix diuji dengan test yang gagal sebelumnya.
Hotfix dirilis sebagai v3.0.1 dengan changelog jelas.
Hotfix tidak menambah fitur baru, hanya memperbaiki bug utama.
25.3 Update
Update dicek dari GitHub Releases.
Update menampilkan changelog dan tombol Download.
Update tidak auto-install diam-diam.
Update Play memakai AAB dan review.
25.4 Maintenance
Dependencies diperbarui tiap bulan, dengan test penuh.
NDK dan CMake diperbarui apabila ada keamanan.
TargetSdk diperbarui mengikuti Play.
Model rekomendasi diperbarui apabila ada model baru yang lebih ringan dan lebih pintar.
Dokumen diperbarui tiap rilis.
25.5 Backup maintainer
Keystore backup di dua tempat aman.
Secrets GitHub dicatat lokasinya, bukan nilainya.
Workflow CI dan CD didokumentasikan.
Cara build manual didokumentasikan di docs dan BUILD.md.
26. DAFTAR KERJA TOTAL FASE 0 SAMPAI FASE 10
Fase 0, persiapan
Clone repo, catat commit, buat branch feat dan real-apk-production, jangan langsung main.
Buka tiap folder forge-engine, hitung file, catat bahasa, catat build system lama, catat requirements.
Buka forge-workbench, catat package.json, catat native-qt6, catat kemampuan build Android di mesin.
Buka .github dan workflows lama, catat apa yang sudah ada.
Download dan bongkar tiga rilis lama, isi tabel bukti rilis lama.
Tentukan UI utama sebagai Kotlin, tulis keputusan di docs.
Tentukan minSdk API 26 Android 8, targetSdk 34, ABI utama arm64-v8a.
Siapkan keystore release, simpan di Secrets, jangan commit ke repo.
Fase 1, struktur Android
Copy paket forge-android ke root repo, jangan timpa forge-engine dan forge-workbench.
Buat AndroidManifest lengkap dengan permission Internet, Foreground Service, Post Notifications, dan media yang sesuai scoped storage Android 13 ke atas.
Buat MainActivity dan navigasi tujuh layar.
Buat ForgeEditorService sebagai Foreground Service dengan notifikasi Forge Engine Running dan tombol Stop.
Buat ForgeFileManager dengan folder Forge, models, checkpoints, outputs, logs, configs, dibuat otomatis saat pertama buka.
Buat EngineStatusManager sebagai satu sumber kebenaran status semua engine.
Buat Diagnostics screen dengan versi app, versi tiap so, hasil import Python, RAM, storage, ABI, versi Android, log terakhir 200 baris, tombol Export Log dan Copy.
Buat Settings dengan port editor, folder model, batas RAM inference, batas thread, tema Dark, Light, System, bahasa, hapus cache, reset.
Buat Licenses screen dengan isi LICENSES dan notice pihak ketiga.
Buat Onboarding pertama dengan penjelasan engine ada di dalam HP dan model download terpisah, minta permission, buat folder, cek storage.
Fase 2, native inference dan compiler
Cari CMakeLists, Makefile, dan build script asli di forge-inference dan forge-compiler. Apabila tidak ada, buat CMakeLists baru yang mengarah ke source asli, jangan duplikasi source asli ke forge-android.
Buat forge_inference_jni.cpp dengan fungsi loadModel, tokenize, generate dengan callback streaming, unloadModel, getInfo, dan getLastError.
Buat forge_compiler_jni.cpp dengan fungsi compileModel, getSupportedTargets, dan getLastError.
Buat ForgeInference.kt dan ForgeCompiler.kt dengan loadLibrary di init, panggil JNI, tangkap error, ubah menjadi state FAILED dengan pesan.
Atur CMake dengan NDK 25, C++17, abiFilters arm64-v8a, strip debug symbol untuk release, simpan symbol terpisah untuk crash report.
Tangani backend GPU dengan cara cek source inference, apabila mendukung Vulkan atau OpenCL atau NNAPI, bundel dan fallback CPU. Apabila tidak mendukung, tulis jujur di UI bahwa GPU tidak didukung source ini dan memakai CPU.
Tangani threading dengan cara inference dilarang di main thread, memakai Coroutine dan Worker, UI tetap responsif, ada tombol Cancel.
Tangani OOM dengan cara cek RAM sebelum load model, tolak dengan pesan jelas apabila model melebihi RAM tersedia, jangan force close.
Test native di emulator dan HP fisik arm64.
Fase 3, Python trainer dan litgpt
Tambahkan Chaquopy di build.gradle.
Buka requirements dan dependencies asli forge-trainer dan forge-litgpt, catat paket yang berat, tentukan mana yang dibundel, mana yang diunduh terpisah, mana yang diganti versi mobile.
Buat forge_trainer_mobile.py dengan fungsi status, listTasks, trainLoRA, pause, resume, getProgress, dan getLastError. Semua memanggil kode asli, bukan contoh.
Buat forge_litgpt_mobile.py dengan fungsi status, generate, evaluate, convert, serve, trainLoRA, dan getLastError.
Buat PythonBridge.kt dengan start Python satu kali, panggil fungsi, tangkap exception Python, tampil di UI dan log.
Atur mode MOBILE dan SERVER, tampilkan di UI dengan badge jelas.
Atur checkpoint dengan simpan tiap N step, bisa resume setelah aplikasi ditutup dan HP restart, jangan hilang.
Test Python di HP fisik, emulator sering gagal untuk Python berat.
Fase 4, system forge-editor menjadi service
Buka source forge-editor, catat bahasa, framework, port default, dan endpoint.
Implementasikan ForgeEditorService dengan start server lokal, expose health, models, editor, sesuai source asli, proxy ke engine apabila source-nya memang proxy.
Buat Editor UI dengan list file folder Forge, buka, edit, simpan, buat file baru, hapus, cari. Semua lewat Service, bukan akses file langsung dari UI.
Buat WebSocket apabila source editor mendukungnya, untuk streaming log dan streaming token.
Buat health check berkala tiap 10 detik, apabila Service mati, UI tampil FAILED dengan tombol Restart Service.
Atur lifecycle dengan Service tetap jalan saat UI ditutup, berhenti bersih saat user tekan Stop atau saat HP low memory, tulis log.
Fase 5, UI semua tampilan membawa engine asli
Dashboard, Editor, Inference, Compiler, Trainer, LitGPT, Model Manager, Diagnostics, Settings, Licenses, About, Onboarding, Help.
Semua layar memanggil engine asli.
Tombol yang tidak berfungsi dihapus atau diberi label Segera Hadir di versi berikutnya.
Layar pajangan dilarang.
Fase 6, data, model, keamanan, privasi
Model Manager dengan SHA256, tolak model rusak dan ukuran tidak cocok.
Scoped storage Android 13 ke atas dengan permission yang benar, jangan pakai cara lama yang ditolak Play.
Prompt, model, dan file user dilarang dikirim ke server manapun tanpa izin jelas. Semua proses lokal secara default. Tulis di Privacy Policy.
Log dilarang menulis API key dan prompt sensitif ke log yang diexport tanpa sensor. Beri toggle Sertakan prompt di log, default OFF.
Backup dengan tombol Export Config dan Export Log, tombol Import Config. Backup model otomatis dilarang karena terlalu besar.
Update dengan cek update dari GitHub Releases, tampilkan changelog, tombol Download APK baru, dilarang auto-install diam-diam.
Crash reporting dengan tangkap crash native, Kotlin, dan Python, simpan ke folder logs dan crash, tampilkan di Diagnostics, beri tombol Kirim Laporan opsional dengan izin.
Fase 7, build, test, QA
Workflow CI: build debug tiap push.
Workflow Release: build release, signing, upload artifact, buat GitHub Release.
Checker: check_all_engines.py, test native, test Python, test Service.
Test di minimal tiga perangkat: HP arm64 RAM 4GB, HP arm64 RAM 8GB ke atas, emulator x86_64 untuk UI saja.
Test offline penuh, test low storage, test low RAM, test rotasi layar, test aplikasi dibunuh sistem lalu dibuka lagi dan resume checkpoint.
Isi CHECKLIST-QA.md, tanda tangani dengan nama dan tanggal sebelum rilis.
Fase 8, rilis publik production
Siapkan Release Notes, Privacy Policy, README update, screenshot dan video demo dengan bukti engine jalan.
Buat GitHub Release publik dengan tag v3.0.0-production, APK signed, SHA256, changelog, link Privacy Policy.
Setelah GitHub stabil, daftarkan ke Google Play dengan listing, ikon, screenshot, Privacy Policy URL, content rating, target API. Klaim AI yang tidak dapat dibuktikan dilarang.
Setelah rilis, pantau Issues, perbaiki crash 24 sampai 72 jam pertama, buat patch v3.0.1 apabila perlu. Rilis v3.1 dilarang sebelum crash utama beres.
Fase 9, Play Store
Siapkan AAB, listing, ikon, screenshot, Privacy Policy URL, content rating, data safety.
Upload AAB, tunggu review, perbaiki apabila ditolak, jangan kirim ulang tanpa perbaikan.
Pantau Android Vitals, perbaiki crash utama.
Fase 10, maintenance
Update dependencies tiap bulan dengan test penuh.
Update NDK dan CMake apabila ada keamanan.
Update TargetSdk mengikuti Play.
Update model rekomendasi apabila ada model baru yang lebih ringan dan lebih pintar.
Update dokumen tiap rilis.
27. DAFTAR FILE WAJIB LENGKAP
File workflow build debug.
File workflow build release.
File build.gradle aplikasi dengan Chaquopy dan CMake.
File CMakeLists.txt.
File forge_inference_jni.cpp yang sudah nyambung kode asli.
File forge_compiler_jni.cpp yang sudah nyambung kode asli.
File forge_trainer_mobile.py yang sudah nyambung kode asli.
File forge_litgpt_mobile.py yang sudah nyambung kode asli.
File forge_model_utils.py.
File requirements-mobile.txt.
File requirements-server.txt.
File MainActivity.kt.
File ForgeApp.kt.
File ForgeEditorService.kt.
File ForgeInference.kt.
File ForgeCompiler.kt.
File PythonBridge.kt.
File EngineStatusManager.kt.
File ForgeFileManager.kt.
File ModelManager.kt.
File Logger.kt.
File DiagnosticsManager.kt.
File SettingsManager.kt.
File PermissionManager.kt.
File NotificationHelper.kt.
File DownloadManager.kt.
File CheckpointManager.kt.
File ErrorHandler.kt.
File DashboardScreen.kt, EditorScreen.kt, InferenceScreen.kt, CompilerScreen.kt, TrainerScreen.kt, LitGPTScreen.kt, ModelManagerScreen.kt, DiagnosticsScreen.kt, SettingsScreen.kt, LicensesScreen.kt, AboutScreen.kt, OnboardingScreen.kt, HelpScreen.kt.
File check_all_engines.py.
File verify_apk.py.
File AndroidManifest.xml dengan permission lengkap.
File strings.xml, colors.xml, themes.xml, dimens.xml.
File layout tiap layar.
File launcher icon tiap densitas.
File assets licenses, privacy, help, contoh config, prompt contoh.
File README.md update.
File CATATAN-SUPER-LENGKAP-FINAL.md.
File RELEASE_NOTES_v3.0.0_ID.md.
File PRIVACY-POLICY-ID.md.
File CHECKLIST-QA.md.
File BUILD.md.
Actions hijau dan artifact APK terdownload.
Checklist uji HP semua centang.
Release notes, privacy, screenshot, video bukti.
28. CHECKLIST AKHIR SEBELUM RILIS
Semua file wajib ada.
Semua TODO JNI dan Python sudah diisi dengan panggilan kode asli.
Semua test T-01 sampai T-28 lulus.
QA ditandatangani dengan nama dan tanggal.
APK diverifikasi dengan verify_apk.py.
APK signed release, bukan debug.
SHA256 cocok.
Release Notes lengkap dan jujur.
Privacy Policy tampil di aplikasi dan link di release.
Screenshot dan video bukti engine jalan.
Licenses lengkap.
Help lengkap.
Onboarding jalan.
Offline jalan.
Checkpoint resume jalan.
Model Manager SHA256 jalan.
Diagnostics jalan.
Settings jalan.
Update check jalan.
Crash log jalan.
Keystore backup dua tempat.
Secrets GitHub terisi.
Workflow debug hijau.
Workflow release hijau di tag percobaan.
29. LAMPIRAN
29.1 Endpoint lengkap
GET health, GET models, GET status, POST inference dan generate, POST compiler dan compile, POST trainer dan start, GET trainer dan status, POST trainer dan pause, POST trainer dan resume, POST trainer dan stop, POST litgpt dan run, GET editor dan files, GET editor dan file, POST editor dan file, DELETE editor dan file, GET logs, GET diagnostics, POST settings dan update, GET settings.
29.2 Model pertama
Nama Qwen2.5-0.5B-Instruct-GGUF, file qwen2.5-0.5b-instruct-q4_k_m.gguf, ukuran sekitar 397 MB, URL https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF, lisensi Apache 2.0.
29.3 Setting test
Prompt Halo, jelaskan dalam 2 kalimat apa itu Forge. Max tokens 128. Temperature 0.7. Threads 4 di HP RAM 4GB, threads 6 di HP RAM 8GB ke atas. Target kurang dari 30 detik.
29.4 Error code
E-1001 model tidak ditemukan.
E-1002 model rusak.
E-1003 SHA256 tidak cocok.
E-1004 RAM tidak cukup.
E-1005 storage penuh.
E-1006 Service mati.
E-1007 Python gagal start.
E-1008 JNI gagal load.
E-1009 config tidak valid.
E-1010 checkpoint rusak.
E-1011 download gagal.
E-1012 permission ditolak.
E-1013 timeout.
E-1014 target tidak didukung.
E-1015 file tidak dapat ditulis.
29.5 Log format
Waktu ISO 8601, level, modul, pesan, kode error apabila ada.
Contoh: 2026-11-07T20:00:00+08:00 INFO inference loadModel sukses path=/Forge/models/qwen2.5-0.5b-instruct-q4_k_m.gguf
29.6 Nama file output
inference_tanggal_waktu_model.txt
compile_tanggal_waktu_model.gguf
checkpoint_trainer_tanggal_waktu_step-50.zip
log_tanggal.txt
config_trainer_tanggal.json
29.7 Penutup
Dokumen ini adalah acuan utama.
Semua keputusan di dokumen ini dipakai apa adanya di seluruh paket.
Semua file di paket ini mengikuti dokumen ini.
Langkah berikutnya adalah mengisi JNI dan Python dengan panggilan ke fungsi asli di forge-engine, lalu build debug, lalu QA, lalu rilis GitHub v3.0.0-production.
