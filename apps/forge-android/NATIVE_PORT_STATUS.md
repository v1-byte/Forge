# Native Android port status

This document tracks the port of the uploaded `forge-workbench-fi-ai-eng.zip` into the Android app. The uploaded project is an HTML/React-in-one-file frontend with a Node.js gateway; it is a UI/prototype source, not an Android-native implementation. The Android launcher must remain Jetpack Compose/Kotlin and must not load the uploaded frontend in a WebView.

## Ported into the native launcher in this change

- The primary navigation is now **Kode Biasa**, **AI Engineering**, and **Archive / Bongkar**.
- AI Engineering has native Compose sections: AI Code, Models, System, Tokens, Engines, Machine, and Training.
- AI Code reuses the native editor and existing Android callbacks rather than copying web/JavaScript code.
- Models screen exposes the existing Android document picker, GGUF metadata inspection, and llama.cpp model-load path. These are source-connected operations; end-to-end model loading/generation still needs testing on a physical arm64 Android device with a compatible GGUF model.
- Archive uses the Android Storage Access Framework and Java ZIP central-directory reader to list archive entries. It stages at most 512 MiB in app cache and shows at most 2,000 entries. It does not extract, modify, or repackage archives.
- System/Machine display Android build/device identity supplied by the native app. Tokens, unsupported engines, and training explicitly state their current limitations.
- The UI does not report fake success for compiler, training, quantization, serving, or tokenizer actions.

## Not yet implemented as native features

- Project/workspace tree, multi-file tabs, recursive search, source control operations, debugger, language server, build/run, and APK tooling.
- ZIP entry preview, safe extraction, modification, and export.
- Model tensor inspection beyond the existing GGUF metadata reader; actual model conversion, quantization, split, and benchmark operations.
- Model tokenizer encode/decode and accurate token counting.
- Android-native compiler, training/evaluation, and serving pipelines. The uploaded Node gateway's process jobs/adapters are not included in the Android app and are not silently invoked.
- Push notifications and Play updates require their external Firebase/Play configuration; these are not part of the workbench port.

## Validation boundary

CI can prove Kotlin/Gradle/CMake compilation, tests that are present, APK assembly, and APK integrity. It cannot prove the UI matches every web detail or that inference works on a device. Before calling the port complete, test navigation and accessibility on phone/tablet sizes, open/save through SAF, inspect/load a supported GGUF, generate a prompt on arm64 hardware, and test ZIP listing with valid, malformed, oversized, and many-entry archives. Keep unavailable operations disabled or clearly labelled until their native implementations and tests exist.
