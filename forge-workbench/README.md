# Forge Workbench

AI Studio — Model & Inference workbench (frontend + lightweight gateway).

## Cara menjalankan

```bash
# 1. Masuk ke folder
cd forge-workbench

# 2. Jalankan (Node.js 18+ diperlukan)
node gateway/server/index.js

# atau
npm start
```

Buka browser: **http://localhost:8787**

Port default: `8787`
Bisa diganti dengan environment variable: `PORT=3000 node gateway/server/index.js`

## Struktur

- `frontend/` — UI React (single HTML + Babel)
- `gateway/server/` — HTTP server sederhana (API + static files)
- `config/` — engine-map & workbench manifest
- `adapters/`, `contracts/` — konfigurasi engine

## Catatan

Server sekarang sudah melayani file statis (`/assets/...`) sehingga logo dan resource lain bisa dimuat.


## Pengelompokan UI Forge
Workbench menempatkan empat pintasan utama di activity rail kiri: Editor Kode, AI Engine, Copilot, dan Search. Source Control serta Run & Debug dikelompokkan sebagai alat sekunder. Tombol workspace ganda dan Copilot mengambang dihapus. Theme menyediakan pilihan Dark, Light, dan System yang disimpan di perangkat.
- **Kode Biasa** — editor file dan proyek. Search, Source Control, Run, dan aksi format memakai navigasi/toolbar yang sudah ada, bukan tombol duplikat di panel.
- **AI Engine** — satu workspace dengan bagian Models, Runtime, Tokenizer, Compiler, Hardware, dan Training. Bagian ini memetakan tugas ke `forge-inference`, `forge-compiler`, `forge-trainer`, dan `forge-litgpt` sesuai perannya.
- **Copilot** — panel chat terpisah yang dibuka dari tombol Copilot pada activity rail kiri.
- **Search** — pencarian isi file workspace terpisah dari pengelolaan model.
- **Editor profesional** — bahasa otomatis/manual dan syntax highlighting lokal untuk JavaScript/TypeScript, Python, HTML/XML, CSS/SCSS, JSON/YAML, C/C++, Java/Kotlin, Bash, Markdown, dan SQL.
- **Editor Plugins** — aktif/nonaktifkan pack bahasa bawaan; pack terpilih disimpan di perangkat.

Plugin `.dll`/native tidak berjalan langsung di browser. Plugin eksternal memerlukan host/API plugin tepercaya.
- **Archive / Bongkar** — workspace ZIP tersendiri untuk meninjau atau mengedit entry arsip.

`forge-editor` adalah server konektivitas HTTP/WebSocket/proxy, bukan UI editor visual atau mesin AI. Pengaturan lanjutan dan job AI memerlukan engine native, model, backend, serta adapter yang sesuai; menu UI tidak mengklaim job selesai bila komponen tersebut tidak tersedia.

## Backend API dan status implementasi

Gateway sekarang menyediakan baseline operasional untuk Workbench:

- `GET /health` — health check.
- `GET /api/status` — status CPU, memory, GPU/backend, engine, dan workspace.
- `GET /api/files?path=...` — daftar file workspace dengan klasifikasi.
- `GET /api/files/read?path=...` — membaca file teks.
- `POST /api/files/write` — menyimpan file kode; penulisan binary diblokir.
- `GET /api/models/inspect?path=...` — inspeksi ukuran/format model dengan penanda binary.
- `POST /api/tokenizer` — endpoint tokenizer baseline dan token count.
- `POST /api/jobs` — membuat job inference, compile, convert, quantize, serve, training, evaluate, atau inspect.
- `GET /api/jobs/:id` — progress, log, result, dan status job.
- `POST /api/jobs/:id/cancel` — membatalkan job.
- `GET /api/models/gguf?path=...&tensors=500` — baca header GGUF sungguhan: versi, arsitektur, metadata, daftar tensor (nama, dimensi, tipe quant), ukuran vocab.
- `GET /api/files/hex?path=...&offset=0&length=512` — hex viewer read-only (maks 4096 byte per permintaan).
- `GET /api/search?path=&q=...&regex=1&case=1` — pencarian kode di workspace (lewati node_modules/.git/binary/file >1 MB).
- `GET /api/git/status|diff|log?path=...` dan `POST /api/git/init|commit` — operasi Git lokal tanpa shell dan dengan hooks dimatikan. Push belum ada.
- `POST /api/run` — jalankan `.js/.mjs/.py/.sh` (dari `path` atau `code`+`language`). **Hanya aktif dengan `FORGE_EXECUTE=1`**, timeout 10 dtk (`FORGE_RUN_TIMEOUT_MS`), output dibatasi 100 KB. Ini bukan sandbox penuh; jangan buka ke jaringan publik tanpa isolasi.

- `POST /api/models/convert` `{path, output, type: f16|f32, name?, vocab?}` — safetensors → GGUF. Hanya `LlamaForCausalLM`, hanya butuh `python3` + `numpy` (`gateway/tools/forge_gguf.py`). Butuh `FORGE_EXECUTE=1`. `vocab:true` (tokenizer BPE) eksperimental.
- `POST /api/models/quantize` `{input, output}` — GGUF F32/F16 → Q8_0 (tensor 2D dengan dimensi pertama kelipatan 32; norm tetap F32). Butuh `FORGE_EXECUTE=1`.
- `POST /api/tokenizer/gguf` `{path, text?, ids?}` — encode/decode memakai vocab di dalam GGUF (SPM dan BPE byte-level). **Perkiraan**: belum dibandingkan dengan llama.cpp.
- `POST /api/files/hex-write` `{path, offset, hex, confirm:true}` — tulis byte (file tidak boleh membesar), backup `.bak-<waktu>` otomatis.
- `POST /api/archive/read` `{archive, entry}` dan `POST /api/archive/write` `{archive, entry, content, output?}` — baca/ubah satu entry ZIP; hasil disimpan ke ZIP baru (default `*.edited.zip`), arsip asli tidak diubah.
- `POST /api/models/benchmark` `{path}` — kecepatan baca disk saja, bukan benchmark inference.

Tombol **Run** di UI memakai `/api/run` dengan isi tab aktif. Tanpa `FORGE_EXECUTE=1` UI menampilkan pesan bahwa eksekusi dimatikan.

Jalankan dengan Node.js 18+:

```bash
FORGE_WORKSPACE_ROOT=./workspaces \
FORGE_ENGINE_ROOT=../forge-engine \
node gateway/server/index.js
```

`FORGE_EXECUTE=1` hanya menandai engine native sebagai diizinkan. Engine asli tetap harus dipasang atau diarahkan melalui `FORGE_ENGINE_ROOT`, dan adapter native spesifik masih perlu ditambahkan untuk setiap tool compiler/inference/trainer. Tanpa engine tersebut, job berjalan sebagai **safe preview/fallback** dan tidak mengklaim telah menjalankan training atau inference model sungguhan.

Gateway menerapkan path confinement pada workspace, membatasi ukuran body JSON, membatasi pembacaan file besar, dan menolak penulisan binary lewat endpoint file. Untuk produksi masih diperlukan autentikasi, sandbox proses, rate limit, audit log persisten, dan database job.


## Strict native mode

Workbench sekarang berjalan dalam strict-native mode secara default. Job inference, compile, convert, quantize, serve, training, dan evaluate akan berstatus **failed** jika engine source/build atau command native belum tersedia. Tidak ada lagi fallback yang dilaporkan sebagai sukses.

Contoh konfigurasi native:

```bash
FORGE_ENGINE_ROOT=/absolute/path/to/Forge/forge-engine \
FORGE_EXECUTE=1 \
FORGE_NATIVE_STRICT=1 \
FORGE_INFERENCE_CMD="/absolute/path/to/llama-cli --model $MODEL_PATH --prompt $PROMPT" \
node gateway/server/index.js
```

Command native harus disediakan oleh administrator deployment dan dijalankan di workspace yang telah diisolasi. Source engine saja belum cukup: inference membutuhkan binary build dan model, compiler membutuhkan TVM/FFI/toolchain, sedangkan trainer membutuhkan PyTorch, Transformers, Triton, dan dependency GPU yang sesuai.


## File manager di UI (Open / Save / Import / Export)

Menu **File** di topbar (juga di HP) dan baris tombol di atas Explorer sekarang benar-benar berfungsi:

- **Open File** (Ctrl+O) — buka satu atau banyak file dari perangkat; teks bisa diedit, biner ditampilkan read-only dan bisa diunduh.
- **Open Folder** — muat seluruh folder (node_modules dan .git dilewati).
- **Import ZIP** — ekstrak ZIP ke Explorer (struktur folder dipertahankan, file biner utuh).
- **Import Code** — tempel kode, beri nama file, langsung masuk Explorer dan terbuka di editor.
- **New File / New Folder / Rename / Hapus** — lewat tombol ＋, menu ⋯ di tiap baris, atau klik kanan.
- **Save** (tombol Save, Ctrl+S) dan **Save All** (Ctrl+Shift+S) — menyimpan ke workspace lokal (IndexedDB, bertahan setelah reload) dan, bila gateway aktif, juga ke folder workspace server.
- **Unduh file / Export Workspace → ZIP** — simpan hasil ke perangkat. Export memakai isi editor terbaru.
- **Bongkar ZIP** — buka isi ZIP tanpa mengekstrak, edit, Save ke dalam ZIP, lalu **Unduh ZIP** hasil edit atau **Ekstrak** ke Explorer.

Workspace lokal bekerja tanpa gateway. Library JSZip dimuat dari CDN, jadi perlu internet sekali (atau simpan jszip.min.js lokal untuk APK offline). Unduhan memakai tautan `<a download>`; di WebView Android perlu `DownloadListener`/`WebChromeClient.onShowFileChooser` agar pemilih file dan unduhan berjalan.
