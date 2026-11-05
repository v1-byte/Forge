# Spesifikasi UI Forge Android Native

**Status:** spesifikasi implementasi awal untuk branch `native-android`.

**Implementasi utama:** `app/src/main/java/com/v1byte/forge/ui/ForgeWorkbenchScreen.kt` dan `ForgeTheme.kt`.

**Catatan verifikasi:** dokumen ini menjelaskan kode saat ini. APK belum berhasil dibangun atau diperiksa di perangkat karena Android SDK belum tersedia di lingkungan build.

## 1. Tujuan dan prinsip desain

Aplikasi menggunakan Kotlin dan Jetpack Compose; UI web dari workbench yang diberikan menjadi acuan visual, bukan runtime aplikasi. Struktur dark IDE dipertahankan: identitas Forge, pilihan workspace, activity bar, area editor, panel AI, panel output bawah, dan status bar. Pada ponsel, panel yang tidak cukup ruang dipindahkan ke dialog atau baris horizontal, bukan dipaksa menjadi layout desktop yang mengecil.

Dokumen ini membedakan **bagian yang sudah terhubung ke callback aplikasi** dari **bagian yang masih berupa tampilan/placeholder**. Label tombol tidak berarti mesin build, trainer, server, atau Git sudah berjalan.

## 2. Susunan layar

Urutan utama dari atas ke bawah:

1. **Top bar (54 dp):** ikon huruf F, nama Forge/AI Studio, indikator model saat model aktif, tombol `Updates`, `Notify`, akses `AI Chat` pada layar yang tidak lebar, dan tombol aksen `Run`.
2. **Workspace selector:** chip `Kode Biasa`, `AI Engineering`, dan `Archive / Bongkar`. Aksi training ada di bagian `Training` dalam AI Engineering; chip mengganti workspace, bukan memuat pipeline terpisah.
3. **Catatan UI (selalu tampil):** label `PREVIEW UI` dan teks bahwa sebagian kontrol masih berupa tampilan serta belum semua engine native terhubung.
4. **Area kerja:** activity bar, panel Explorer bila ruang cukup, editor, serta panel assistant sesuai breakpoint.
5. **Bottom panel (154 dp):** tab `Problems`, `Output`, `Terminal`, `Inference`, dan `Trainer`.
6. **Status bar (24 dp):** status operasi terakhir, label FCM bila Firebase terkonfigurasi, dan identitas Forge Workbench.

```text
┌──────────────────────────────────────────────────────────────┐
│ F  Forge / AI Studio      model?   Updates Notify AI Chat Run │
│ Code   AI Engineering   Training   Archive                    │
├──────┬───────────────┬───────────────────┬───────────────────┤
│Activity│ Explorer    │ Editor / actions  │ AI Assistant      │
│ bar    │ (opsional)   │                   │ (layar lebar)     │
├──────┴───────────────┴───────────────────┴───────────────────┤
│ Problems | Output | Terminal | Inference | Trainer            │
├──────────────────────────────────────────────────────────────┤
│ Status                                                     Forge│
└──────────────────────────────────────────────────────────────┘
```

Diagram menunjukkan susunan konseptual layar lebar. Di ponsel, activity bar horizontal dan assistant dibuka sebagai dialog.

## 3. Perilaku responsif

Breakpoint ditentukan berdasarkan lebar Compose dalam dp:

| Lebar | Susunan |
|---|---|
| Di bawah 720 dp | Activity bar horizontal; Explorer samping disembunyikan; editor memakai ruang utama; `AI Chat` membuka dialog assistant. |
| 720–899 dp | Activity bar vertikal; editor memakai ruang utama tanpa Explorer samping; assistant tetap melalui dialog. |
| 900–1099 dp | Activity bar vertikal, Explorer 212 dp, editor; assistant tetap melalui dialog. |
| 1100 dp atau lebih | Activity bar vertikal, Explorer 212 dp, editor, dan assistant 300 dp tampil berdampingan. Tombol dialog `AI Chat` tidak ditampilkan. |

Workspace selector dan daftar action dapat digulir horizontal. Pada mode kompak, dialog assistant setinggi 440 dp dan dapat ditutup dengan tombol Back atau sentuhan di luar dialog.

## 4. Komponen dan fungsi

### 4.1 Top bar dan workspace

- `Updates` meminta pemeriksaan pembaruan Google Play. Jika update fleksibel sudah diunduh, banner menampilkan pesan dan tombol `Install` untuk melanjutkan alur Play.
- `Notify` memulai permintaan izin notifikasi Android bila Firebase tersedia. Tanpa konfigurasi Firebase, status memberi tahu bahwa `google-services.json` belum dipasang.
- `AI Chat` membuka assistant pada lebar di bawah 1100 dp.
- Banner `PREVIEW UI` selalu menjelaskan bahwa sebagian kontrol masih hanya tampilan dan belum semua engine native terhubung.
- `Run` meneruskan isi editor sebagai prompt ke model lokal bila model sudah dimuat. Tombol ini **tidak menjalankan kode editor**.
- Nama model dan indikator hijau hanya tampil bila state aplikasi menyatakan model telah aktif.

### 4.2 Activity bar dan Explorer

Activity bar menyediakan `Explorer`, `Search`, `Source Control`, `Debug`, dan `Models`. Memilih item mengganti isi panel Explorer; tidak otomatis menghubungkan layanan eksternal.

- **Explorer:** menunjukkan file teks yang sedang dibuka dan nama model; tombol tambah membuka pemilih file Android. Ini belum merupakan pohon proyek/folder yang dapat ditelusuri.
- **Models:** menampilkan status model dan menyediakan pemilih file GGUF.
- **Search:** bidang pencarian masih berupa UI; indeks dan pencarian lintas file belum disambungkan.
- **Source Control:** saat ini hanya memberi tahu bahwa Git remote belum dihubungkan. Belum ada autentikasi Git, status perubahan, commit, atau push dari aplikasi.
- **Debug:** teks bantuan saja; belum ada debugger atau breakpoint.

### 4.3 Editor

Editor memakai `BasicTextField` Compose dengan font monospace dan area gulir. Header berisi nama file serta:

- `Open`: memilih satu file teks melalui Android Storage Access Framework (SAF). MIME type yang diminta meliputi teks, JSON, XML, JavaScript, YAML, dan TOML.
- `Save`: menulis kembali ke URI terpilih; jika belum ada tujuan file, Android meminta lokasi/nama file baru.
- `Ask AI`: mengirim teks editor sebagai prompt. Respons diproses oleh model lokal yang aktif.

Editor saat ini adalah editor teks dasar: belum ada tab banyak file, line number, syntax highlighting, autocomplete, navigasi simbol, atau pembukaan satu folder proyek. Penyimpanan juga bergantung pada izin tulis yang diberikan penyedia dokumen Android.

### 4.4 Action bar

Daftar action dapat digulir mendatar. Status setiap action:

| Action | Perilaku saat ini |
|---|---|
| `Run` | Mengirim isi editor ke inference lokal jika model aktif; tidak menjalankan source code sebagai program. |
| `Open model` | Membuka pemilih file, menerima file berekstensi `.gguf`, menyalin ke penyimpanan privat aplikasi, lalu meminta llama.cpp memuatnya. |
| `Inspect GGUF` | Membaca metadata GGUF dan menulis ringkasannya ke panel output; tidak memuat model hanya untuk inspeksi. |
| `Build` | Menampilkan keterangan bahwa eksekusi shell/build dari editor belum diaktifkan. |
| `Compile` | Pipeline compiler Android/MLC belum disambungkan. |
| `Train LoRA`, `Train QLoRA`, `Full fine-tune` | Belum berjalan di perangkat; memerlukan backend workstation/GPU. |
| `Quantize` | Belum dieksekusi oleh aplikasi. |
| `Serve` | Belum menjalankan local API server. |
| `Tokenizer` | Inspector tokenizer belum dihubungkan. |

### 4.5 Model lokal dan AI assistant

File model harus berformat GGUF dan arsitekturnya harus didukung oleh runtime llama.cpp yang disertakan. Aplikasi menyalin file ke direktori privat `files/models/`; sediakan ruang penyimpanan yang cukup. Setelah model aktif, prompt dari `AI Chat`, `Ask AI`, atau action `Run` dikirim ke repository inference native dan token respons ditampilkan dalam percakapan.

`AI Chat` memakai panel kanan pada layar lebar dan dialog pada ukuran yang lebih kecil. Tombol saran seperti `Explain this file` dan `Review my code` hanya mengisi draft; pengguna tetap mengirimkannya. Assistant ini memakai model lokal, bukan layanan AI cloud.

### 4.6 Bottom panel dan status

- **Problems:** saat ini menampilkan teks statis bahwa belum ada data masalah.
- **Output / Terminal:** menampilkan log aplikasi (buka/simpan file, metadata GGUF, inference); belum merupakan terminal shell.
- **Inference:** menampilkan model aktif atau petunjuk memilih model.
- **Trainer:** keterangan bahwa training didelegasikan ke host app; tidak ada job training yang dijalankan.
- **Status bar:** menampilkan status terakhir. `FCM configured` berarti konfigurasi Firebase ditemukan, bukan koneksi Git.

## 5. Token visual

Palet berasal dari `ForgeTheme.kt` dan menggunakan skema Material 3 gelap.

| Token | Warna | Penggunaan |
|---|---|---|
| `ForgeBackground` | `#08080A` | Latar utama dan editor |
| `ForgeSurface` | `#111113` | Panel/sidebar |
| `ForgePanel` | `#1A1A1E` | Permukaan sekunder dan bubble assistant |
| `ForgePanelRaised` | `#222225` | Elemen terpilih/permukaan terangkat |
| `ForgeBorder` | `#1E1E20` | Garis pemisah dan outline |
| `ForgeText` | `#FAFAFA` | Teks utama dan logo |
| `ForgeTextSecondary` | `#E4E4E7` | Teks isi/editor |
| `ForgeMuted` | `#A1A1AA` | Teks sekunder |
| `ForgeMutedSecondary` | `#71717B` | Label dan teks redup |
| `ForgeOrange` | `#FF6A00` | Aksen utama, seleksi, indikator |
| `ForgePink` | `#FF3D9A` | Aksen gradien bersama oranye |
| `ForgeGreen` | `#22C55E` | Status siap/model aktif |
| `ForgeYellow` | `#FACC15` | Status peringatan |
| `ForgeRed` | `#EF4444` | Error |

Teks editor dan log memakai monospace; label antarmuka kecil memakai tipografi Compose/Material 3. Chip terpilih memiliki outline oranye transparan, tombol utama memakai gradien oranye, dan komponen memakai sudut membulat.

## 6. Konfigurasi eksternal yang memengaruhi UI

- **Firebase:** package ID harus terdaftar dalam project Firebase dan `google-services.json` harus diletakkan di `app/`. Tanpa itu, penerimaan dan pengiriman FCM belum dapat diuji. Integrasi saat ini belum mendaftarkan token ke backend aplikasi.
- **Google Play updates:** memerlukan listing, versi lebih baru di Play testing track, serta application ID dan signing yang sama. Alur flexible update meminta persetujuan pengguna; pemasangan senyap tidak dilakukan.
- **Package sementara:** `com.v1byte.forge`; tetapkan package final sebelum rilis Play/Firebase.

## 7. Checklist status UI

- [x] UI Compose native; tidak memakai WebView.
- [x] Identitas dark Forge, workspaces, activity rail, editor, panel bawah, assistant, dan status bar.
- [x] Responsif untuk ponsel dan layar lebar; assistant tersedia pada kedua mode.
- [x] Pilih/baca/simpan satu file teks melalui SAF.
- [x] Pilih file GGUF, inspeksi metadata, dan jalur inference lokal llama.cpp.
- [x] Kontrol UI update Google Play dan permission/penerima FCM dengan konfigurasi eksternal.
- [ ] Screenshot dan pemeriksaan visual hasil render APK di perangkat Android nyata. Screenshot preview web tidak dianggap bukti render native.
- [ ] Build APK berhasil; toolchain Android belum tersedia saat pemeriksaan terakhir.
- [ ] Pohon file proyek, pencarian, Git, debugger, terminal shell, build/run source code.
- [ ] Integrasi nyata compiler, trainer, quantization, API serving, dan tokenizer editor.

## 8. File implementasi terkait

- `app/src/main/java/com/v1byte/forge/ui/ForgeWorkbenchScreen.kt` — komponen dan layout Compose.
- `app/src/main/java/com/v1byte/forge/ui/ForgeTheme.kt` — palet/tema.
- `app/src/main/java/com/v1byte/forge/MainActivity.kt` — callback file, model/inference, update, dan permission push.
- `app/src/main/java/com/v1byte/forge/engine/ForgeInferenceRepository.kt` — facade untuk runtime llama.cpp.
- `app/src/main/java/com/v1byte/forge/updates/PlayUpdateController.kt` — controller Play flexible update.
- `app/src/main/java/com/v1byte/forge/notifications/` — service dan helper FCM.
