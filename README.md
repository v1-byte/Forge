# Forge Engine Runtime
> **Sebelum mengerjakan:** baca [Panduan Kontribusi](CONTRIBUTING.md) dan [dokumen acuan utama](docs/CATATAN-SUPER-LENGKAP-FINAL.md).


Forge menyatukan runtime AI dan tooling mesin; payload engine upstream tidak menyertakan UI browser. Aplikasi Workbench tersedia terpisah di `forge-workbench/`, tanpa mengganti backend engine.

Deskripsi teknis lengkap mengenai modul, alur input/output, fitur, hubungan antar-engine, dan batasan integrasi tersedia di [`ENGINE_DESCRIPTIONS.txt`](ENGINE_DESCRIPTIONS.txt).

## Engine

| Engine | Fungsi sistem |
| --- | --- |
| `forge-compiler` | Compiler dan runtime deployment model untuk target CPU/GPU/backend tertentu. |
| `forge-editor` | Server/common backend, HTTP/WebSocket, proxy, health, dan konektivitas editor tanpa browser UI. |
| `forge-inference` | Runtime C/C++ untuk loading model, GGUF, tokenizer, sampling, KV cache, backend CPU/GPU, dan serving. |
| `forge-trainer` | Engine training/fine-tuning Unsloth dengan kernel optimized, quantization, precision, dan distributed training. |
| `forge-litgpt` | Engine Python untuk pretraining, full fine-tuning, LoRA/adapter, generation, evaluasi, conversion, dan serving. |

## Forge Workbench UI

UI Workbench terbaru kini disimpan sebagai subproyek mandiri di [`forge-workbench/`](forge-workbench/). Ini adalah UI browser dan gateway ringan; `forge-engine/forge-editor` tetap dipertahankan sebagai backend code-server/konektivitas.

Jalankan dengan Node.js 18 atau lebih baru:

```bash
cd forge-workbench
npm start
```

Buka `http://localhost:8787`. Detail penggunaan ada di [`forge-workbench/README.md`](forge-workbench/README.md), dan ringkasan perubahan UI ada di [`FORGE_WORKBENCH_UI_NOTES_ID.md`](FORGE_WORKBENCH_UI_NOTES_ID.md).

Versi ini menghapus tombol quick-action di area layar kosong, menambahkan tema Dark/Light/System, syntax highlighting lokal dengan deteksi bahasa, dan pengelola pack bahasa. Plugin DLL/native tidak dieksekusi oleh browser.

Lisensi dan notice pihak ketiga disimpan terpusat di `LICENSES/` untuk memenuhi kewajiban redistribusi; tidak ada UI, tutorial, README upstream, contoh aplikasi, atau test upstream di dalam payload engine.

Port native Qt 6/C++ untuk desktop dan Android tersedia di [`forge-workbench/native-qt6/`](forge-workbench/native-qt6/); UI web lama dan gateway tetap dipertahankan.
