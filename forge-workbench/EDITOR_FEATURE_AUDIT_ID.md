# Audit Fitur Editor Forge Workbench

**Tanggal audit:** 7 November 2026  
**Cakupan:** `forge-workbench/frontend/index.html`, gateway, dan dokumentasi Workbench.  
**Tujuan:** membandingkan alur edit kode Forge dengan kemampuan umum editor VS Code dan mencatat batas implementasi secara jujur.

## Ringkasan

Forge Workbench sudah menyediakan Explorer/workspace lokal, tab edit, penyimpanan, impor/ekspor ZIP, syntax highlighting lokal, pencarian sederhana, serta beberapa API gateway untuk file, Git, run, dan job engine. Namun permukaan editor masih berupa `<textarea>` dengan overlay syntax highlighting—bukan Monaco atau editor IDE penuh. Karena itu, penambahan UI tidak dengan sendirinya menghadirkan IntelliSense, language server, debugger, shell aman, atau kemampuan native.

Perubahan dalam audit ini melengkapi workflow editor dasar di browser: Quick Open, Find/Replace dalam file aktif, Go to Line, navigasi tab, indentasi dan auto-pair dasar, keyboard-operable command palette, preferensi bahasa/syntax tersimpan lokal, dan indikator line/column dinamis. Status Problems/Outline dan keterangan Run/Git diperjelas agar tidak mengesankan fitur yang belum terhubung sedang aktif.

## Update Android Kotlin (7 November 2026)

Setelah audit ini, dibuat proyek terpisah `forge-android/`: shell editor native Kotlin, pemilihan/penyalinan GGUF, serta bridge JNI untuk C API `forge-inference` CPU arm64. Ini proof-of-concept yang harus lulus build NDK dan uji perangkat sebelum dianggap siap; tidak mengubah atau menghapus frontend HTML, dan tidak mengintegrasikan engine Python/Node lain. Rincian build, penggunaan, dan gap editor ada di [`forge-android/README_ID.md`](../forge-android/README_ID.md).

## Sudah tersedia

- Explorer lokal: membuka file/folder, membuat/rename/menghapus file lokal, impor kode/ZIP, ekspor workspace, dan penyimpanan IndexedDB.
- Tab edit dengan dirty state, Save/Save All, penutupan tab yang meminta simpan atau discard, dan proteksi perubahan belum disimpan saat meninggalkan halaman.
- Syntax highlighting lokal untuk pack bahasa yang dibundel, pemilihan bahasa otomatis/manual, tema, dan toggle syntax.
- Cari literal sederhana di workspace lokal, serta gateway terpisah untuk pencarian server.
- Run terbatas melalui gateway dan job inference/compile/training yang bergantung konfigurasi deployment.
- Git status/diff/log/init/commit tersedia sebagai API gateway, tetapi UI Source Control belum memanggil API tersebut.

## Ditambahkan pada pembaruan ini

- **Quick Open (`Ctrl/Cmd+P`)**: filter path file dalam workspace browser lokal, dengan tab aktif dan file yang baru dibuka diprioritaskan.
- **Command Palette (`Ctrl/Cmd+Shift+P`, serta `Ctrl/Cmd+K`)**: daftar perintah aktual dapat difilter dan dipilih dengan panah atas/bawah, Enter, klik, atau Escape. Memuat navigasi file/tab, find/replace, Go to Line, format terbatas, dan perintah AI yang sudah ada.
- **Find (`Ctrl/Cmd+F`) dan Replace (`Ctrl/Cmd+H`)**: pencocokan literal case-insensitive di file aktif, navigasi hasil berikut/sebelumnya, penggantian satu hasil atau semua hasil setelah konfirmasi.
- **Go to Line (`Ctrl/Cmd+G`)** dan navigasi tab berikut/sebelumnya (`Ctrl/Cmd+Tab` / `Ctrl/Cmd+Shift+Tab`).
- **Editing dasar**: Tab/Shift+Tab untuk indent/outdent, Enter mempertahankan indent dasar serta menambah indent setelah `{`, `[`, `(`, dan auto-pair kurung/kutip. Undo/redo tetap memakai kemampuan native browser textarea.
- **Status editor**: posisi baris/kolom, panjang seleksi, encoding UTF-8, dan bahasa aktif dihitung dari tab yang dipilih.
- **Preferensi lokal**: bahasa override, toggle syntax, dan daftar file terbaru disimpan di localStorage.
- **Kejujuran UI**: Problems tidak menampilkan angka palsu; panel menjelaskan diagnostics belum tersedia tanpa language server/compiler. Outline tidak lagi mengklaim simbol telah dimuat. Run diberi label sebagai Run & Jobs, bukan debugger; indikator Git/perangkat juga tidak mengklaim repo/Pixel aktif.

## Kesenjangan dibandingkan workflow VS Code

| Area | Status Workbench | Untuk mendekati VS Code diperlukan |
|---|---|---|
| IntelliSense/completion/hover | Belum tersedia; highlight.js hanya memberi warna sintaks | Migrasi ke editor engine seperti CodeMirror/Monaco dan integrasi language server/toolchain |
| Diagnostics/Problems | Belum ada language server/compiler yang terhubung | LSP atau parser/compiler nyata, transport hasil, navigasi ke file/baris |
| Symbols/Outline/refactor | Belum tersedia; Outline kini menampilkan batasan secara jujur | Parser atau LSP, model simbol, go-to-definition/references/refactor |
| Fitur editor lanjutan | Belum ada multi-cursor, folding, minimap, snippets, semantic tokens, atau history aplikasi | Editor engine khusus; textarea tidak cocok untuk meniru kemampuan ini secara andal |
| Format kode | JSON diformat lokal; bahasa lain hanya normalisasi whitespace | Formatter per bahasa yang dibundel/terpasang dan konfigurasi yang eksplisit |
| Source Control UI | API Git tersedia, UI belum terhubung; push/pull dan stage/unstage tidak ada | Panel yang memakai API status/diff/log/init/commit, review untracked/staged, lalu keputusan aman untuk push/pull/auth |
| Terminal | Panel perintah bawaan, bukan shell/PTy | PTY backend dengan autentikasi, isolasi proses, pembatasan resource, streaming, dan pengamanan path |
| Run & Debug | Run terbatas dan job engine; debugger kode tidak tersedia | Run configurations/task runner dan debug adapter/runtime yang nyata |
| Workspace | Explorer dan pencarian browser menggunakan IndexedDB; gateway bekerja pada filesystem server | Pemilihan workspace jelas, sinkronisasi CRUD, deteksi konflik dan otorisasi |
| Aksesibilitas lanjutan | Label dan semantik dasar ditingkatkan pada kontrol baru | Audit keyboard penuh, tree/tabs semantik, focus trap/return, live region, contrast/reduced-motion |
| Kolaborasi/akun | Preferensi dan file lokal per browser/perangkat | Identitas dan API penyimpanan jika sinkronisasi lintas perangkat dibutuhkan |

## Batasan dan keamanan

1. **Bukan paritas penuh VS Code.** Quick Open, Find/Replace, dan palet ini adalah fitur lokal ringan, bukan pengganti editor engine maupun language server.
2. **Workspace browser dan workspace gateway bukan satu folder yang otomatis tersinkron.** UI tidak boleh mengklaim perubahan lokal sudah masuk ke filesystem server jika gateway write tidak mengonfirmasinya.
3. **Terminal bukan shell sistem.** Perintah panel bawaan terbatas; endpoint `/api/run` memerlukan konfigurasi dan README memperingatkan bahwa eksekusi gateway bukan sandbox penuh.
4. **Jangan tampilkan READY/progress/output contoh sebagai hasil nyata.** Diagnostik, simbol, format, run, dan job harus mencerminkan dukungan implementasi dan backend yang benar-benar ada.
5. **Fitur besar perlu tahap dan keputusan dependensi.** Migrasi Monaco/CodeMirror, LSP, Git penuh, PTY, serta debugging memerlukan evaluasi ukuran bundle, offline Android/WebView, lisensi, keamanan, dan konfigurasi deployment.

## Pintasan yang tersedia

| Pintasan | Tindakan |
|---|---|
| `Ctrl/Cmd+Shift+P` atau `Ctrl/Cmd+K` | Buka Command Palette |
| `Ctrl/Cmd+P` | Quick Open file workspace lokal |
| `Ctrl/Cmd+S` | Simpan file aktif |
| `Ctrl/Cmd+Shift+S` | Simpan semua |
| `Ctrl/Cmd+F` | Cari pada file aktif |
| `Ctrl/Cmd+H` | Ganti pada file aktif |
| `Ctrl/Cmd+G` | Lompat ke baris |
| `Ctrl/Cmd+Tab` / `Ctrl/Cmd+Shift+Tab` | Pindah tab editor |
| `Ctrl/Cmd+W` | Tutup tab aktif (saat fokus pada editor, permintaan konfirmasi tetap berlaku bila ada perubahan) |
| `Tab` / `Shift+Tab` | Indentasi / outdentasi teks terpilih atau baris aktif |

Shortcut mengikuti dukungan browser dan keyboard perangkat; konflik browser/OS dapat berbeda.
