# Forge Engine Runtime

Forge menyatukan runtime AI dan tooling mesin tanpa UI bawaan upstream.

Deskripsi teknis lengkap mengenai modul, alur input/output, fitur, hubungan antar-engine, dan batasan integrasi tersedia di [`ENGINE_DESCRIPTIONS.txt`](ENGINE_DESCRIPTIONS.txt).

## Engine

| Engine | Fungsi sistem |
| --- | --- |
| `forge-compiler` | Compiler dan runtime deployment model untuk target CPU/GPU/backend tertentu. |
| `forge-editor` | Server/common backend, HTTP/WebSocket, proxy, health, dan konektivitas editor tanpa browser UI. |
| `forge-inference` | Runtime C/C++ untuk loading model, GGUF, tokenizer, sampling, KV cache, backend CPU/GPU, dan serving. |
| `forge-trainer` | Engine training/fine-tuning Unsloth dengan kernel optimized, quantization, precision, dan distributed training. |
| `forge-litgpt` | Engine Python untuk pretraining, full fine-tuning, LoRA/adapter, generation, evaluasi, conversion, dan serving. |

Lisensi dan notice pihak ketiga disimpan terpusat di `LICENSES/` untuk memenuhi kewajiban redistribusi; tidak ada UI, tutorial, README upstream, contoh aplikasi, atau test upstream di dalam payload engine.
