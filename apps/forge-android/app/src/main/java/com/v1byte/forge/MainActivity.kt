package com.v1byte.forge

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.lifecycleScope
import com.arm.aichat.gguf.GgufMetadataReader
import com.v1byte.forge.engine.ForgeInferenceRepository
import com.v1byte.forge.engine.ForgeInferenceRepository.State as InferenceState
import com.v1byte.forge.notifications.enablePushNotifications
import com.v1byte.forge.notifications.isPushConfigured
import com.v1byte.forge.ui.ForgeAction
import com.v1byte.forge.ui.ForgeActivity
import com.v1byte.forge.ui.ForgeAiSection
import com.v1byte.forge.ui.ForgeArchiveEntry
import com.v1byte.forge.ui.ForgeChatMessage
import com.v1byte.forge.ui.ForgeTheme
import com.v1byte.forge.ui.ForgeUiState
import com.v1byte.forge.ui.ForgeWorkbenchScreen
import com.v1byte.forge.ui.ForgeWorkspace
import com.v1byte.forge.updates.PlayUpdateController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

class MainActivity : ComponentActivity() {
    private var uiState by mutableStateOf(
        ForgeUiState(
            workspace = ForgeWorkspace.CODE,
            fileName = "",
            fileContent = "",
            statusText = "Workspace kosong • pilih file atau model untuk mulai",
            deviceSummary = "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} • ABI ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}",
        ),
    )

    private lateinit var inferenceRepository: ForgeInferenceRepository
    private lateinit var updateController: PlayUpdateController
    private var selectedCodeUri: Uri? = null
    private var pendingSaveText: String? = null
    private var inspectModelOnly = false

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        updateController?.onUpdateFlowResult(result.resultCode)
    }

    private val openCodeFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) readCodeFile(uri)
    }

    private val createCodeFile = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        val content = pendingSaveText
        pendingSaveText = null
        if (uri != null && content != null) writeCodeFile(uri, content)
    }

    private val openModelFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) importAndLoadModel(uri)
    }

    private val openArchiveFile = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) readArchive(uri)
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) enableConfiguredPush()
        else updateStatus("Izin notifikasi ditolak; push tidak akan ditampilkan.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inferenceRepository = ForgeInferenceRepository(applicationContext)
        updateController = PlayUpdateController(this, updateLauncher)
        uiState = uiState.copy(pushConfigured = isPushConfigured(applicationContext))

        setContent {
            val updates by updateController.state.collectAsState()
            val inference by inferenceRepository.state.collectAsState()
            val engineHasModel = inference is InferenceState.ModelReady || inference is InferenceState.Generating
            val currentState = uiState.copy(
                modelLoaded = uiState.modelLoaded || engineHasModel,
                modelName = if (uiState.modelLoaded) uiState.modelName else if (engineHasModel) "Model loaded" else uiState.modelName,
                loading = uiState.loading || inference is InferenceState.Initializing ||
                    inference is InferenceState.LoadingModel || inference is InferenceState.Generating,
                updateReady = updates.updateReady,
                updateMessage = updates.updateMessage.orEmpty(),
                pushConfigured = isPushConfigured(applicationContext),
                statusText = when (inference) {
                    is InferenceState.Error -> "Runtime error: ${(inference as InferenceState.Error).cause.message ?: "unknown"}"
                    is InferenceState.ModelReady -> "Model loaded • llama.cpp"
                    is InferenceState.Generating -> "Generating locally • llama.cpp"
                    else -> uiState.statusText
                },
            )

            ForgeTheme {
                ForgeWorkbenchScreen(
                    state = currentState,
                    onWorkspaceSelected = { workspace -> uiState = uiState.copy(workspace = workspace) },
                    onAiSectionSelected = { section -> uiState = uiState.copy(aiSection = section) },
                    onActivitySelected = { activity -> uiState = uiState.copy(activity = activity) },
                    onPickCodeFile = ::launchCodePicker,
                    onPickModelFile = ::launchModelPicker,
                    onNewFile = {
                        selectedCodeUri = null
                        pendingSaveText = null
                        uiState = uiState.copy(
                            fileName = "untitled.txt",
                            fileContent = "",
                            statusText = "File baru • belum disimpan",
                        )
                    },
                    onCloseFile = {
                        selectedCodeUri = null
                        pendingSaveText = null
                        uiState = uiState.copy(fileName = "", fileContent = "", statusText = "Workspace kosong")
                    },
                    onOpenFolder = { updateStatus("Pemilih folder belum tersedia; gunakan Open File untuk memilih berkas.") },
                    onSendPrompt = ::sendPrompt,
                    onChatSend = ::sendPrompt,
                    onRunAction = ::runAction,
                    onSaveEditor = ::saveCodeFile,
                    onOpenArchive = ::launchArchivePicker,
                    onCheckUpdates = { updateController.checkForUpdate(autoStart = true) },
                    onInstallUpdate = { updateController.completeUpdate() },
                    onEnableNotifications = ::requestPushPermission,
                )
            }
        }

        // This check uses Play metadata. A sideloaded/debug APK normally has no update available.
        updateController.checkForUpdate(autoStart = true)
    }

    override fun onResume() {
        super.onResume()
        if (::updateController.isInitialized) updateController.onResume()
    }

    override fun onDestroy() {
        if (::updateController.isInitialized) updateController.close()
        super.onDestroy()
    }

    private fun launchCodePicker() {
        openCodeFile.launch(
            arrayOf(
                "text/*",
                "application/json",
                "application/xml",
                "application/javascript",
                "application/x-yaml",
                "application/toml",
            ),
        )
    }

    private fun launchModelPicker() {
        inspectModelOnly = false
        openModelFile.launch(arrayOf("application/octet-stream", "application/*", "*/*"))
    }

    private fun launchArchivePicker() {
        openArchiveFile.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
    }

    private fun readArchive(uri: Uri) {
        retainSafAccess(uri, allowWrite = false)
        uiState = uiState.copy(loading = true, statusText = "Membaca daftar ZIP…")
        lifecycleScope.launch {
            try {
                val displayName = queryDisplayName(uri) ?: "archive.zip"
                val entries = withContext(Dispatchers.IO) {
                    val staged = File(cacheDir, "forge-archive-${System.nanoTime()}.zip")
                    try {
                        val input = contentResolver.openInputStream(uri)
                            ?: throw IOException("Tidak dapat membaca arsip")
                        input.use { source ->
                            staged.outputStream().buffered().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var total = 0L
                                while (true) {
                                    val count = source.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    if (total > MAX_ARCHIVE_BYTES) {
                                        throw IOException("ZIP melebihi batas impor 512 MB")
                                    }
                                    output.write(buffer, 0, count)
                                }
                            }
                        }
                        ZipFile(staged).use { zip ->
                            val result = ArrayList<ForgeArchiveEntry>()
                            val iterator = zip.entries()
                            while (iterator.hasMoreElements() && result.size < MAX_ARCHIVE_ENTRIES) {
                                val entry = iterator.nextElement()
                                result += ForgeArchiveEntry(
                                    name = entry.name,
                                    isDirectory = entry.isDirectory,
                                    sizeBytes = entry.size.coerceAtLeast(0L),
                                )
                            }
                            result
                        }
                    } finally {
                        staged.delete()
                    }
                }
                uiState = uiState.copy(
                    archiveFileName = displayName,
                    archiveEntries = entries,
                    loading = false,
                    statusText = "Daftar ZIP dibaca lokal • ${entries.size} entries ditampilkan",
                )
                appendConsole("ZIP indexed: $displayName (${entries.size} entries)")
            } catch (error: Exception) {
                uiState = uiState.copy(loading = false, archiveEntries = emptyList())
                updateStatus("Gagal membaca ZIP: ${error.message ?: "format tidak valid"}")
                appendConsole("ZIP read failed: ${error.message ?: error.javaClass.simpleName}")
            }
        }
    }

    private fun launchModelInspectionPicker() {
        inspectModelOnly = true
        openModelFile.launch(arrayOf("application/octet-stream", "application/*", "*/*"))
    }

    private fun readCodeFile(uri: Uri) {
        retainSafAccess(uri, allowWrite = true)
        uiState = uiState.copy(loading = true, statusText = "Membuka file…")
        lifecycleScope.launch {
            try {
                val name = queryDisplayName(uri) ?: "untitled.txt"
                val content = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IOException("Tidak dapat membuka file")
                }
                selectedCodeUri = uri
                uiState = uiState.copy(
                    fileName = name,
                    fileContent = content,
                    loading = false,
                    statusText = "File dibuka dari penyimpanan perangkat",
                )
                appendConsole("Opened $name")
            } catch (error: Exception) {
                uiState = uiState.copy(loading = false)
                updateStatus("Gagal membuka file: ${error.message ?: "kesalahan baca"}")
            }
        }
    }

    private fun saveCodeFile(content: String) {
        val uri = selectedCodeUri
        if (uri == null) {
            pendingSaveText = content
            createCodeFile.launch(uiState.fileName.ifBlank { "untitled.txt" })
        } else {
            writeCodeFile(uri, content)
        }
    }

    private fun writeCodeFile(uri: Uri, content: String) {
        retainSafAccess(uri, allowWrite = true)
        uiState = uiState.copy(loading = true, statusText = "Menyimpan file…")
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(content) }
                        ?: throw IOException("Tidak dapat menyimpan ke lokasi ini")
                }
                selectedCodeUri = uri
                uiState = uiState.copy(fileContent = content, loading = false, statusText = "File tersimpan")
                appendConsole("Saved ${uiState.fileName}")
            } catch (error: Exception) {
                uiState = uiState.copy(loading = false)
                updateStatus("Gagal menyimpan file: ${error.message ?: "kesalahan tulis"}")
            }
        }
    }

    private fun importAndLoadModel(uri: Uri) {
        retainSafAccess(uri, allowWrite = false)
        val inspectionOnly = inspectModelOnly
        inspectModelOnly = false
        uiState = uiState.copy(loading = true, statusText = "Memeriksa model GGUF…")
        lifecycleScope.launch {
            try {
                val displayName = queryDisplayName(uri) ?: "model.gguf"
                require(displayName.endsWith(".gguf", ignoreCase = true)) {
                    "Pilih file model berformat GGUF."
                }
                val metadataSummary = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        GgufMetadataReader.create().readStructuredMetadata(input).toString()
                    } ?: "Metadata GGUF tidak dapat dibaca"
                }
                appendConsole("GGUF metadata: ${metadataSummary.take(1600)}")
                if (inspectionOnly) {
                    uiState = uiState.copy(
                        loading = false,
                        statusText = "Metadata GGUF berhasil dibaca; model belum dimuat.",
                    )
                    return@launch
                }

                val destination = withContext(Dispatchers.IO) {
                    val modelsDirectory = File(filesDir, "models").apply { mkdirs() }
                    val safeName = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(180)
                    val target = File(modelsDirectory, safeName)
                    contentResolver.openInputStream(uri)?.use { input ->
                        target.outputStream().buffered().use { output -> input.copyTo(output) }
                    } ?: throw IOException("Tidak dapat membaca model")
                    target
                }
                inferenceRepository.loadModel(destination.absolutePath)
                uiState = uiState.copy(
                    modelName = displayName,
                    modelLoaded = true,
                    loading = false,
                    statusText = "Model lokal aktif • llama.cpp",
                )
                appendConsole("Model loaded: ${destination.name}")
            } catch (error: Exception) {
                uiState = uiState.copy(loading = false, modelLoaded = false)
                updateStatus("Model gagal dimuat: ${error.message ?: "kesalahan inference"}")
                appendConsole("Model load failed: ${error.message ?: error.javaClass.simpleName}")
            }
        }
    }

    private fun sendPrompt(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty()) {
            updateStatus("Masukkan prompt terlebih dahulu.")
            return
        }
        if (!uiState.modelLoaded) {
            updateStatus("Pilih model GGUF sebelum menjalankan inference lokal.")
            launchModelPicker()
            return
        }
        if (uiState.loading) {
            updateStatus("Inference sedang berjalan; tunggu sampai selesai.")
            return
        }

        uiState = uiState.copy(
            chatMessages = uiState.chatMessages + ForgeChatMessage("user", prompt) + ForgeChatMessage("assistant", ""),
            loading = true,
            statusText = "Generating locally • llama.cpp",
        )
        lifecycleScope.launch {
            try {
                inferenceRepository.generate(prompt).collect { token ->
                    val messages = uiState.chatMessages.toMutableList()
                    val lastIndex = messages.lastIndex
                    if (lastIndex >= 0 && messages[lastIndex].role.equals("assistant", ignoreCase = true)) {
                        messages[lastIndex] = messages[lastIndex].copy(content = messages[lastIndex].content + token)
                        uiState = uiState.copy(chatMessages = messages)
                    }
                }
                uiState = uiState.copy(loading = false, statusText = "Inference selesai • lokal")
                appendConsole("Inference completed")
            } catch (cancelled: CancellationException) {
                uiState = uiState.copy(loading = false, statusText = "Inference dibatalkan")
                throw cancelled
            } catch (error: Exception) {
                uiState = uiState.copy(loading = false, statusText = "Inference error: ${error.message ?: "gagal"}")
                appendConsole("Inference failed: ${error.message ?: error.javaClass.simpleName}")
            }
        }
    }

    private fun runAction(action: ForgeAction) {
        when (action) {
            ForgeAction.OPEN_MODEL -> launchModelPicker()
            ForgeAction.INSPECT_GGUF -> launchModelInspectionPicker()
            ForgeAction.RUN -> {
                if (uiState.modelLoaded) sendPrompt(uiState.fileContent)
                else launchModelPicker()
            }
            ForgeAction.BUILD -> updateStatus("Build kode belum dijalankan. Aplikasi tidak mengeksekusi shell command dari editor.")
            ForgeAction.COMPILE -> updateStatus("Compiler Android/MLC belum dihubungkan sebagai pipeline aplikasi.")
            ForgeAction.TRAIN_LORA, ForgeAction.TRAIN_QLORA, ForgeAction.FULL_FINE_TUNE -> updateStatus("Training memerlukan backend workstation/GPU; fitur ini belum berjalan di perangkat.")
            ForgeAction.QUANTIZE -> updateStatus("Quantization belum dieksekusi di perangkat ini; simpan salinan model sebelum menggunakan tool eksternal.")
            ForgeAction.SERVE -> updateStatus("Local API server belum diaktifkan; tidak ada server yang berjalan.")
            ForgeAction.TOKENIZER -> updateStatus("Tokenizer inspector belum disambungkan ke tokenizer model.")
        }
        appendConsole("Action: ${action.title}")
    }

    private fun requestPushPermission() {
        if (!isPushConfigured(applicationContext)) {
            updateStatus("Firebase belum disiapkan. Tambahkan google-services.json untuk mengaktifkan push.")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enableConfiguredPush()
        }
    }

    private fun enableConfiguredPush() {
        val enabled = enablePushNotifications(applicationContext)
        uiState = uiState.copy(
            pushConfigured = isPushConfigured(applicationContext),
            statusText = if (enabled) "FCM siap menerima notifikasi" else "FCM tidak dapat diaktifkan pada build ini",
        )
    }

    private fun updateStatus(message: String) {
        uiState = uiState.copy(statusText = message)
    }

    private fun appendConsole(message: String) {
        uiState = uiState.copy(consoleLogs = (uiState.consoleLogs + message).takeLast(120))
    }

    private fun retainSafAccess(uri: Uri, allowWrite: Boolean) {
        val readFlag = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val requestedFlags = readFlag or if (allowWrite) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0
        try {
            contentResolver.takePersistableUriPermission(uri, requestedFlags)
        } catch (_: SecurityException) {
            if (allowWrite) {
                runCatching { contentResolver.takePersistableUriPermission(uri, readFlag) }
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? = contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }

    private companion object {
        const val MAX_ARCHIVE_BYTES = 512L * 1024L * 1024L
        const val MAX_ARCHIVE_ENTRIES = 2_000
    }
}
