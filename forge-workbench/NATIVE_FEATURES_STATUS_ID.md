# Status fitur native — Forge Workbench

Dokumen ini ditambahkan ke arsip Workbench yang Anda kirim. Arsip sumber tetap berisi prototype HTML/React dan Node.js; penambahan dokumen ini **tidak** mengubah atau mengonversi kode sumber di dalam ZIP menjadi Kotlin/Android native.

Berikut fitur yang belum tersedia sebagai fungsi native yang benar-benar berfungsi:

| Area | Yang belum tersedia |
|---|---|
| **Editor & proyek** | Penjelajahan folder/proyek penuh, banyak file/tab, pencarian kode, syntax highlighting, formatter, dan language server. Saat ini utamanya membuka serta menyimpan satu file lewat pemilih file Android. |
| **Run / Build / APK** | Menjalankan kode yang sedang diedit, build proyek, serta tool APK. Tombol **Run** saat ini bukan menjalankan source code; jalurnya mengirim teks editor ke model bila model dimuat. |
| **Git & Debug** | Operasi Git seperti status, commit, push, serta debugger belum dibuat. |
| **GGUF Toolkit** | Inspeksi metadata dasar dan jalur pemuatan model ada di source. **Tensor inspector**, Hex Editor, konversi HF→GGUF, quantize, split, dan benchmark belum tersedia sebagai tool native. |
| **Tokenizer** | Encode/decode dan hitung token akurat dengan tokenizer model belum tersedia. |
| **Training & serving** | LoRA, QLoRA, fine-tuning, evaluasi, checkpoint, serta menjalankan API/server model belum tersedia di APK. |
| **Archive / ZIP** | Perubahan terbaru menambahkan pemilih ZIP native dan daftar isi arsip. Membuka isi file, ekstraksi, mengubah ZIP, dan mengekspor ulang belum tersedia. Batas pemindaian saat ini 512 MB dan 2.000 entri. |
| **Inference lokal** | Jalur JNI llama.cpp dan chat ada di kode, tetapi saya belum membuktikan pemuatan model dan generasi berjalan pada ponsel nyata. Dukungan model, memori, performa, dan kompatibilitas perangkat belum terverifikasi. |

Sebagian tombol/fitur yang terlihat di prototype web **belum berarti mesinnya tersedia**; beberapa memang demo atau hanya UI.

## Status repository saat pengemasan

- Branch: `release/native-android-apk`; commit port UI native: `85c915dd`.
- Pull request #1: **OPEN** — https://github.com/v1-byte/Forge/pull/1
- Pemeriksaan CI untuk commit tersebut: **masih berjalan** pada 5 November 2026 pukul 18:09 WIB — https://github.com/v1-byte/Forge/actions/runs/42007440477

Status CI/PR dapat berubah setelah ZIP ini dibuat; tautan di atas adalah sumber untuk memeriksa kondisi terkini.

## Pembaruan: yang sudah dikerjakan di gateway/web (5 November 2026)

Diuji dengan file GGUF buatan sendiri, repo git sementara, dan eksekusi kode lokal. Ini fitur **gateway Node.js + UI web**, bukan fitur native Kotlin/Android.

| Fitur | Status |
|---|---|
| Inspeksi GGUF (metadata, tensor, tipe quant, vocab) | Ada: `/api/models/gguf` |
| Hex Editor | Hanya **viewer read-only**: `/api/files/hex`. Edit hex belum ada |
| Pencarian kode | Ada: `/api/search` (belum ada panel UI) |
| Git | status, diff, log, init, commit ada. **Push belum ada** |
| Run | Ada untuk js/py/sh bila `FORGE_EXECUTE=1`; tombol Run di UI sudah terhubung. Belum ada build proyek/APK |
| Konversi safetensors→GGUF (F16/F32) | Ada, hanya LlamaForCausalLM. Diuji dengan model llama mini buatan sendiri; **belum diuji memuat di llama.cpp** |
| Quantize Q8_0 | Ada. Galat relatif ±0,4% pada model uji; ukuran turun ±46% |
| Tokenizer dari vocab GGUF | Ada (SPM + BPE), perkiraan; belum dibandingkan dengan llama.cpp |
| Edit hex | Ada via `/api/files/hex-write` dengan backup (belum ada UI) |
| Edit & ekspor ZIP | Ada via `/api/archive/read|write` (belum ada UI) |
| Benchmark | Hanya kecepatan baca disk |
| Logo | Logo katak "forge" dipakai di brand bar, favicon, dan ikon Copilot |

Masih belum ada: versi Kotlin/Android native, syntax highlighting/formatter/language server, debugger, tensor inspector visual, split GGUF, quantize selain Q8_0, konversi arsitektur selain Llama, benchmark inference, training (LoRA/QLoRA), serving model, panel UI untuk fitur-fitur baru, dan bukti inference berjalan di ponsel nyata.
