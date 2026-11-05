# Sumber teknis

Referensi resmi untuk implementasi native ini:

- Jetpack Compose Gradle/compiler setup: https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
  - Compose compiler memakai plugin Kotlin; dokumentasi saat ini menyarankan BOM `2026.09.00`, tetapi BOM itu memerlukan compileSdk 37/AGP 9. Project ini sementara memakai toolchain Android 36 yang juga dipakai contoh llama.cpp di repo dan Compose BOM `2025.08.00` untuk menghindari ketidakcocokan.
- Google Play In-App Updates Kotlin/Java: https://developer.android.com/guide/playcore/in-app-updates/kotlin-java
  - Library Play Core 2.1.0; flexible update memerlukan persetujuan pengguna. Update yang sudah terunduh meminta pengguna menyetujui restart/install.
- Firebase Cloud Messaging Android setup: https://firebase.google.com/docs/android/setup
  - App harus didaftarkan di project Firebase dengan package ID persis yang digunakan aplikasi; konfigurasi `google-services.json` diletakkan pada modul app.
- Firebase Cloud Messaging Android receive: https://firebase.google.com/docs/cloud-messaging/android/receive-messages
  - Pesan diterima melalui `FirebaseMessagingService`; pesan notification saat app background ditangani notification tray Android.
- Firebase Cloud Messaging Android getting started: https://firebase.google.com/docs/cloud-messaging/android/get-started
  - Android 13+ memerlukan izin notifikasi runtime.
- Android Compose quick start: https://developer.android.com/develop/ui/compose/setup
- Runtime llama.cpp Android yang dirujuk project: https://github.com/ggml-org/llama.cpp/tree/master/examples/llama.android

- AppUpdateManager reference: https://developer.android.com/reference/com/google/android/play/core/appupdate/AppUpdateManager
  - `startUpdateFlowForResult(AppUpdateInfo, ActivityResultLauncher<IntentSenderRequest>, AppUpdateOptions)` mengembalikan boolean apakah flow dimulai.
