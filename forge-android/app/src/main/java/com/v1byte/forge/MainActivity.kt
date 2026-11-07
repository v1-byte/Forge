package com.v1byte.forge

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/** Native Kotlin first slice: local text editing plus real CPU inference through llama.cpp JNI. */
class MainActivity : Activity() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var editor: EditText
    private lateinit var prompt: EditText
    private lateinit var output: TextView
    private lateinit var loadButton: Button
    private lateinit var unloadButton: Button
    private lateinit var generateButton: Button
    private lateinit var nativeEngine: NativeLlamaEngine
    private var nativeReady = false
    private var nativeUnavailableReason = "Library native belum dimuat."
    private var modelFile: File? = null
    private var modelLoaded = false
    private var closing = false

    private val editorFile: File by lazy {
        File(File(filesDir, "workspace").apply { mkdirs() }, "main.kt")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(16, 20, 28)
        window.navigationBarColor = Color.rgb(16, 20, 28)
        window.decorView.systemUiVisibility = 0

        try {
            nativeEngine = NativeLlamaEngine()
            nativeReady = nativeEngine.isAvailable
            nativeUnavailableReason = nativeEngine.unavailableMessage ?: ""
        } catch (error: Throwable) {
            nativeReady = false
            nativeUnavailableReason = error.message ?: error.javaClass.simpleName
        }

        setContentView(buildUi())
        editor.setText(if (editorFile.exists()) editorFile.readText(Charsets.UTF_8) else "// Forge Native Kotlin workspace\nfun main() {\n    println(\"Hello, Forge\")\n}\n")
        refreshButtons()
        if (!nativeReady) {
            setStatus("JNI belum tersedia: $nativeUnavailableReason. Bangun APK dengan NDK atau periksa ABI perangkat.")
        } else {
            setStatus("Native shell aktif. Model dan generation memakai forge-inference/llama.cpp CPU melalui JNI.")
        }
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(16, 20, 28))
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(28))
        }
        scroll.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(dp(16), dp(14) + insets.systemWindowInsetTop, dp(16), dp(28) + insets.systemWindowInsetBottom)
            insets
        }

        root.addView(text("FORGE NATIVE", 12, Color.rgb(112, 190, 255), true))
        root.addView(text("Code Editor · llama.cpp", 24, Color.WHITE, true), spaced())
        root.addView(text("Kotlin UI · Android arm64 · direct JNI · CPU backend", 13, Color.LTGRAY, false), spaced())

        status = text("Memeriksa native runtime…", 13, Color.rgb(185, 210, 196), false).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(28, 38, 42))
        }
        root.addView(status, spaced())

        root.addView(text("WORKSPACE LOKAL · main.kt", 12, Color.rgb(160, 172, 190), true), spaced())
        editor = EditText(this).apply {
            setTextColor(Color.rgb(226, 232, 240))
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.rgb(22, 28, 38))
            typeface = Typeface.MONOSPACE
            textSize = 14f
            gravity = Gravity.TOP or Gravity.START
            minLines = 12
            maxLines = 24
            isVerticalScrollBarEnabled = true
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            setPadding(dp(12), dp(12), dp(12), dp(12))
            hint = "Tulis kode Kotlin lokal…"
        }
        root.addView(editor, spaced())
        root.addView(button("Simpan main.kt") { saveEditorFile() }, spaced())

        root.addView(text("INFERENCE ON-DEVICE · forge-inference", 12, Color.rgb(160, 172, 190), true), spaced())
        root.addView(button("Pilih file model GGUF") { chooseModel() }, spaced())
        loadButton = button("Muat model ke llama.cpp") { loadSelectedModel() }
        root.addView(loadButton, spaced())
        unloadButton = button("Lepaskan model dari memori") { unloadModel() }
        root.addView(unloadButton, spaced())

        root.addView(text("Prompt (completion teks; belum memakai chat template model)", 13, Color.LTGRAY, false), spaced())
        prompt = EditText(this).apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.rgb(22, 28, 38))
            textSize = 15f
            minLines = 3
            maxLines = 6
            gravity = Gravity.TOP or Gravity.START
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(dp(12), dp(12), dp(12), dp(12))
            hint = "Masukkan prompt…"
        }
        root.addView(prompt, spaced())
        generateButton = button("Jalankan completion (maks. 96 token)") { generate() }
        root.addView(generateButton, spaced())
        output = text("Output inference akan tampil di sini.", 14, Color.rgb(226, 232, 240), false).apply {
            typeface = Typeface.MONOSPACE
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(22, 28, 38))
            minHeight = dp(72)
        }
        root.addView(output, spaced())
        root.addView(button("Lisensi open source") { showLicenses() }, spaced())
        root.addView(text("Batas tahap ini: editor native dasar tanpa language server/IntelliSense, multi-cursor, build/debug Kotlin, streaming/cancel, atau akselerasi GPU. Gunakan model GGUF yang muat di RAM perangkat.", 12, Color.rgb(160, 172, 190), false), spaced())
        return scroll
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean): TextView = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun button(label: String, onClick: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun spaced(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(10) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun setStatus(message: String) {
        if (!closing && ::status.isInitialized) status.text = message
    }

    private fun refreshButtons() {
        if (::loadButton.isInitialized) loadButton.isEnabled = nativeReady && modelFile?.exists() == true
        if (::unloadButton.isInitialized) unloadButton.isEnabled = nativeReady && modelLoaded
        if (::generateButton.isInitialized) generateButton.isEnabled = nativeReady && modelLoaded
    }

    private fun showLicenses() {
        try {
            val license = assets.open("forge-inference-license.txt").bufferedReader(Charsets.UTF_8).use { it.readText() }
            android.app.AlertDialog.Builder(this)
                .setTitle("Lisensi forge-inference / llama.cpp")
                .setMessage(license)
                .setPositiveButton("Tutup", null)
                .show()
        } catch (error: IOException) {
            setStatus("File lisensi tidak ditemukan pada APK: ${error.message}")
        }
    }

    private fun saveEditorFile() {
        try {
            editorFile.writeText(editor.text.toString(), Charsets.UTF_8)
            setStatus("Tersimpan lokal: ${editorFile.relativeTo(filesDir).path}")
        } catch (error: IOException) {
            setStatus("Gagal menyimpan file editor: ${error.message}")
        }
    }

    private fun chooseModel() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_MODEL)
    }

    @Deprecated("Storage Access Framework callback retained for a dependency-free Android Activity")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_MODEL || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        val name = displayName(uri)
        if (!name.endsWith(".gguf", ignoreCase = true)) {
            setStatus("File harus berekstensi .gguf. Model split belum didukung pada tahap ini.")
            return
        }
        setStatus("Menyalin $name ke penyimpanan privat aplikasi…")
        modelLoaded = false
        modelFile = null
        refreshButtons()
        worker.execute {
            try {
                if (nativeReady) nativeEngine.unload()
                val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
                val modelsDir = File(filesDir, "models").apply { mkdirs() }
                val target = File(modelsDir, safeName)
                val partial = File(modelsDir, "$safeName.partial")
                contentResolver.openInputStream(uri)?.use { input -> partial.outputStream().buffered().use { outputStream -> input.copyTo(outputStream) } }
                    ?: throw IOException("Tidak dapat membaca model dari file picker.")
                if (target.exists()) target.delete()
                if (!partial.renameTo(target)) throw IOException("Gagal memindahkan model hasil salin.")
                modelFile = target
                runOnUiThread {
                    setStatus("Model disalin lokal: ${target.name} (${target.length() / (1024 * 1024)} MiB). Tekan Muat model untuk memulai.")
                    refreshButtons()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    setStatus("Gagal menyalin model: ${error.message}")
                    refreshButtons()
                }
            }
        }
    }

    private fun displayName(uri: Uri): String {
        var result = "model.gguf"
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            if (cursor?.moveToFirst() == true) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) result = cursor.getString(index) ?: result
            }
        } finally {
            cursor?.close()
        }
        return result
    }

    private fun loadSelectedModel() {
        val file = modelFile?.takeIf { it.exists() } ?: run {
            setStatus("Pilih file GGUF terlebih dahulu.")
            return
        }
        loadButton.isEnabled = false
        generateButton.isEnabled = false
        setStatus("Memuat model ke RAM perangkat. Model besar bisa memerlukan waktu dan memori tinggi…")
        worker.execute {
            try {
                val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
                nativeEngine.loadModel(file.absolutePath, contextSize = 2048, threads = threads)
                modelLoaded = true
                runOnUiThread {
                    setStatus("Model siap melalui forge-inference/llama.cpp C API · GGML CPU · context 2048 · $threads thread.")
                    refreshButtons()
                }
            } catch (error: Throwable) {
                modelLoaded = false
                runOnUiThread {
                    setStatus("Load model gagal: ${error.message ?: error.javaClass.simpleName}")
                    refreshButtons()
                }
            }
        }
    }

    private fun generate() {
        val input = prompt.text.toString()
        if (input.isBlank()) {
            setStatus("Isi prompt sebelum menjalankan inference.")
            return
        }
        generateButton.isEnabled = false
        output.text = "Generation berjalan di worker thread…"
        setStatus("Inference berjalan di perangkat. UI tetap dapat merespons; context dibuat ulang untuk prompt ini.")
        worker.execute {
            try {
                val result = nativeEngine.generate(input, maxTokens = 96)
                runOnUiThread {
                    output.text = result.ifBlank { "Model tidak menghasilkan teks sebelum token akhir." }
                    setStatus("Inference selesai dengan llama.cpp/GGML CPU.")
                    refreshButtons()
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    output.text = "Generation gagal: ${error.message ?: error.javaClass.simpleName}"
                    setStatus("Inference gagal.")
                    refreshButtons()
                }
            }
        }
    }

    private fun unloadModel() {
        generateButton.isEnabled = false
        unloadButton.isEnabled = false
        setStatus("Melepaskan model dan native backend…")
        worker.execute {
            try {
                nativeEngine.unload()
                modelLoaded = false
                runOnUiThread {
                    setStatus("Model dilepaskan. Memuat ulang diperlukan sebelum generation berikutnya.")
                    refreshButtons()
                }
            } catch (error: Throwable) {
                runOnUiThread {
                    setStatus("Unload gagal: ${error.message}")
                    refreshButtons()
                }
            }
        }
    }

    override fun onDestroy() {
        closing = true
        if (nativeReady) {
            worker.execute {
                try { nativeEngine.unload() } catch (_: Throwable) { }
            }
        }
        worker.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_MODEL = 4102
    }
}
