# Spesifikasi UI Forge Android Native

**Acuan visual:** `preview/index.html`, salinan UI terbaru dari ZIP Workbench.

**Implementasi APK:** Kotlin + Jetpack Compose di `app/src/main/java/com/v1byte/forge/ui/ForgeWorkbenchScreen.kt`. UI preview web dan APK native adalah dua runtime terpisah; APK tidak memuat frontend melalui WebView.

## Struktur layar

Susunan utama mengikuti UI Workbench: header ringkas, catatan `PREVIEW UI`, activity rail vertikal di kiri, pane editor di tengah, pane AI di kanan pada layar lebar, panel bawah di dalam pane editor, dan status bar. Tampilan awal dibuka sebagai workspace kosong—bukan dengan file README contoh.

| Ukuran layar | Tata letak native |
|---|---|
| Ponsel, di bawah 768 dp | Header 44 dp; activity rail vertikal 50 dp tetap di kiri; Explorer ditutup sebagai drawer sampai dibuka lewat ikon/FAB; editor dan panel bawah berada dalam pane yang sama; Copilot dibuka sebagai panel overlay. |
| 768–899 dp | Activity rail, Explorer 260 dp, dan editor tersusun berdampingan; assistant dibuka sebagai overlay. |
| 900–1099 dp | Activity rail, Explorer 260 dp, dan editor tersusun berdampingan; AI dibuka sebagai overlay. |
| 1100 dp ke atas | Activity rail, Explorer 260 dp, editor, dan assistant 300 dp terlihat berdampingan. |

Header menampilkan wordmark Forge, konteks aktivitas dan menu File/Run pada layar ponsel. Pada layar lebar tersedia pilihan workspace `Kode Biasa`, `AI Engineering`, dan `Archive / Bongkar`. Pada ponsel, pilihan workspace berada di menu overflow supaya area editor tetap menyerupai UI mobile ZIP. Banner `PREVIEW UI` di bawah header menyatakan bahwa sebagian kontrol masih berupa tampilan dan belum semua engine native terhubung.

Pane editor mengikuti urutan UI Workbench: bar file/tab, input Inline Copilot, area editor atau empty state, lalu panel bawah. Empty state menampilkan logo Copilot yang dikirim pengguna, keterangan Forge AI Engineering, dan aksi Open File, Import ZIP, Import Code, New File, Open Folder, dan Open Explorer. Panel bawah (220 dp) berisi Problems, Output, Terminal, Inference, dan Trainer; tab awalnya Terminal dan letaknya hanya di bawah pane editor, bukan membentang di bawah seluruh aplikasi. Panel assistant menggunakan logo Copilot; pada ponsel panel itu tampil sebagai overlay dari tombol Copilot.

## Interaksi native dan batas fungsinya

| Kontrol | Implementasi saat ini |
|---|---|
| `Open File` / `Import Code` | Membuka satu file teks melalui Android Storage Access Framework. |
| `New File` | Membuka editor kosong; penyimpanan pertama meminta lokasi/nama file melalui SAF. |
| `Open Folder` | Menampilkan status bahwa pemilih folder belum tersedia; gunakan Open File untuk memilih berkas. |
| `Import ZIP` / `Archive / Bongkar` | Membaca dan menampilkan daftar entry ZIP lokal; belum mengekstrak atau mengedit arsip. |
| `Save` | Menyimpan teks ke URI yang dipilih atau meminta lokasi baru. |
| `Open model` / `Inspect GGUF` | Memilih GGUF, membaca metadata; pemuatan model diteruskan ke runtime llama.cpp bila format didukung. |
| `Ask AI` / Copilot | Memakai model lokal yang aktif; jika tidak ada model, inference tidak berjalan. |
| `Run` | Mengirim teks editor sebagai prompt ke inference lokal; bukan mengeksekusi kode editor. |
| `Terminal`, `Build`, trainer, Git, Debug | Tampilan/placeholder saja; shell terminal, build dari editor, training, autentikasi Git, dan debugger belum diaktifkan. |
| `Updates` / `Notify` | Akses update Google Play dan izin notifikasi Android; masing-masing tetap memerlukan konfigurasi layanan terkait. |

Banner preview dan status tiap komponen mencegah tampilan tombol disalahartikan sebagai engine yang sudah berjalan. Detail koneksi dan batas fitur dapat dibaca di `README.md` dan `NATIVE_PORT_STATUS.md`.

## Visual dan aset

Tema tetap dark Forge, menggunakan aksen oranye/pink. Wordmark Forge pada header memakai `app/src/main/res/drawable-nodpi/forge_logo.png`; logo Copilot pada empty state, status/panel assistant memakai `app/src/main/res/drawable-nodpi/copilot_logo.png`.

File `screenshots/ui-preview-mobile.png` dan `screenshots/ui-preview-desktop.png` adalah screenshot **preview browser** untuk frontend HTML. Gambar tersebut bukan capture dari APK native. Validasi visual native memerlukan render pada emulator/perangkat Android; build CI hanya memvalidasi test dan kompilasi paket.
