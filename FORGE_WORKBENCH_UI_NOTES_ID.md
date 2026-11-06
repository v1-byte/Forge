# Catatan Integrasi UI Forge Workbench

**Tanggal:** 2026-11-06
**Branch target:** `main`
**Baseline repo saat integrasi:** `9ef1199a` (`Document Forge engine capabilities`)

## Keputusan struktur

Pada branch `main` sebelum integrasi tidak ada UI Workbench browser mandiri. `forge-engine/forge-editor` berisi backend/server code-server dan konektivitas editor. Karena itu, UI baru ditempatkan di `forge-workbench/` sebagai subproyek terpisah; source engine dan backend asli tidak ditimpa.

## Perubahan UI

- Menghapus kumpulan tombol Open File, Import ZIP, Import Code, New File, dan Open Folder dari layar kosong. Akses file tetap melalui menu File dan Explorer.
- Menambahkan pemilih tema Dark, Light, dan System; pilihan disimpan pada perangkat.
- Menambahkan language selector dengan deteksi otomatis dan syntax highlighting lokal.
- Menambahkan tombol Syntax On/Off dan pengelola pack bahasa bawaan yang dapat diaktifkan/nonaktifkan.
- Bundel Highlight.js v11.11.1 beserta lisensinya disertakan di `forge-workbench/frontend/vendor/`.
- Pack tersedia untuk JavaScript/TypeScript, Python, HTML/XML, CSS/SCSS, JSON/YAML, C/C++, Java/Kotlin, Shell/Bash, Markdown, dan SQL.

## Batas dukungan plugin

Editor berjalan di browser, sehingga plugin biner `.dll`/native tidak dimuat langsung. Penggunaan plugin eksternal memerlukan host/API plugin berbasis web yang tepercaya. UI hanya menyatakan kemampuan yang tersedia dalam build ini.

## Validasi

- `npm run check` pada `forge-workbench/`.
- Uji UI browser untuk pilihan tema dan penyimpanannya, penghapusan quick-actions, syntax highlighting Python, toggle syntax, serta aktivasi pack plugin.
- Gateway `/health` dan aset `/vendor/highlight.min.js` diperiksa sebelum publikasi.
