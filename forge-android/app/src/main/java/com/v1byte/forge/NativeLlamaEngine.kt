package com.v1byte.forge

/** Thin Kotlin facade over the vendored llama.cpp C API through JNI. */
class NativeLlamaEngine {
    private val libraryLoadError: String? = try {
        System.loadLibrary("forge_inference_jni")
        null
    } catch (error: UnsatisfiedLinkError) {
        error.message ?: "Native library forge_inference_jni belum tersedia."
    }

    val isAvailable: Boolean
        get() = libraryLoadError == null

    val unavailableMessage: String?
        get() = libraryLoadError

    fun loadModel(path: String, contextSize: Int = 2048, threads: Int = 2) {
        requireLibrary()
        check(nativeLoadModel(path, contextSize, threads)) { "Model tidak dapat dimuat." }
    }

    fun generate(prompt: String, maxTokens: Int = 96): String {
        requireLibrary()
        require(prompt.isNotBlank()) { "Prompt tidak boleh kosong." }
        require(maxTokens in 1..512) { "Batas output harus antara 1 dan 512 token." }
        return nativeGenerate(prompt, maxTokens)
    }

    fun unload() {
        if (libraryLoadError == null) nativeUnload()
    }

    private fun requireLibrary() {
        libraryLoadError?.let {
            throw IllegalStateException("Library inference Android belum dimuat: $it")
        }
    }

    private external fun nativeLoadModel(path: String, contextSize: Int, threads: Int): Boolean
    private external fun nativeGenerate(prompt: String, maxTokens: Int): String
    private external fun nativeUnload()
}
