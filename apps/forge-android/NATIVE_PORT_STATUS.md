# Native Android port status

This document tracks the port of the uploaded `forge-workbench-fi-ai-eng.zip` into the Android app. The uploaded project is an HTML/React-in-one-file frontend with a Node.js gateway; it is a UI/prototype source, not an Android-native implementation. The Android launcher must remain Jetpack Compose/Kotlin and must not load the uploaded frontend in a WebView.

## Ported into the native launcher in this change

- The primary navigation is **Kode Biasa**, **AI Engineering**, and **Archive / Bongkar**.
- The native screen now follows the uploaded Workbench hierarchy more closely: a vertical activity rail remains on the left, the phone Explorer opens as a drawer, the editor has its file bar and Inline Copilot row, and the bottom panel is inside the editor pane. Wide screens show the Explorer and assistant side-by-side.
- Empty startup state, Explorer/assistant floating controls, and the compact header follow the mobile arrangement in the supplied UI. The `PREVIEW UI` note remains visible so placeholder actions are not confused with working engines.
- AI Engineering categories are exposed in the sidebar. AI Code reuses the native editor and existing Android callbacks rather than copying web/JavaScript code.
- The user-provided Forge and Copilot logo assets are used by the native Compose UI.
- Models exposes the Android document picker, GGUF metadata inspection, and llama.cpp model-load path. End-to-end model loading/generation still needs testing on an arm64 Android device with a compatible GGUF model.
- Archive uses the Android Storage Access Framework and Java ZIP central-directory reader to list entries. It stages at most 512 MiB in app cache and shows at most 2,000 entries; it does not extract, modify, or repackage archives.
- System/Machine display Android build/device identity. Unsupported compiler, training, quantization, serving, and tokenizer actions are not reported as successful operations.

## Not yet implemented as native features

- Project/workspace tree, multi-file tabs, recursive search, source control operations, debugger, language server, build/run, and APK tooling.
- ZIP entry preview, safe extraction, modification, and export.
- Model tensor inspection beyond the existing GGUF metadata reader; actual model conversion, quantization, split, and benchmark operations.
- Model tokenizer encode/decode and accurate token counting.
- Android-native compiler, training/evaluation, and serving pipelines. The uploaded Node gateway's process jobs/adapters are not included in the Android app and are not silently invoked.
- Push notifications and Play updates require their external Firebase/Play configuration; these are not part of the workbench port.

## Validation boundary

CI can prove Kotlin/Gradle/CMake compilation, tests that are present, APK assembly, and APK integrity. Browser screenshots in `screenshots/` are references for the web preview only; they are not screenshots from the native APK. The current sandbox does not have a working Android emulator/device for a native UI capture. Before calling the port complete, validate screenshots/navigation and accessibility on phone/tablet sizes, open/save through SAF, inspect/load a supported GGUF, generate a prompt on arm64 hardware, and test ZIP listing with valid, malformed, oversized, and many-entry archives. Keep unavailable operations clearly labelled until their native implementations and tests exist.
