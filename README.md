# Forge Engine Runtime

Forge menyatukan runtime AI dan tooling mesin tanpa UI bawaan upstream.

## Engine

- `forge-engine/forge-compiler`: compiler/runtime deployment untuk membangun dan menargetkan model ke backend perangkat.
- `forge-engine/forge-editor`: server/common engine untuk menjalankan editor Forge tanpa browser UI upstream.
- `forge-engine/forge-inference`: engine inference C/C++ untuk memuat model, tokenisasi, sampling, GGUF, backend GPU/CPU, dan serving.
- `forge-engine/forge-trainer`: engine training/fine-tuning Unsloth.
- `forge-engine/forge-litgpt`: engine Python untuk konfigurasi model, tokenisasi, pretraining, fine-tuning, generation, evaluasi, konversi checkpoint, dan serving.

Lisensi dan notice pihak ketiga disimpan terpusat di `LICENSES/` untuk memenuhi kewajiban redistribusi; tidak ada UI, tutorial, README upstream, contoh aplikasi, atau test upstream di dalam payload engine.
