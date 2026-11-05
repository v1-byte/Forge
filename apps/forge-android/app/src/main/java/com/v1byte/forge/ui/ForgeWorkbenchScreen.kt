package com.v1byte.forge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.v1byte.forge.R

public enum class ForgeWorkspace(val title: String) {
    CODE("Kode Biasa"), AI_ENGINEERING("AI Engineering"), ARCHIVE("Archive / Bongkar")
}

public enum class ForgeAiSection(val title: String) {
    AI_CODE("AI Code"), MODELS("Models"), SYSTEM("System"), TOKENS("Tokens"),
    ENGINES("Engines"), MACHINE("Machine"), TRAINING("Training")
}

public enum class ForgeActivity(val title: String, val mark: String) {
    EXPLORER("Explorer", "▤"), SEARCH("Search", "⌕"), SOURCE_CONTROL("Source Control", "⑂"),
    DEBUG("Debug", "◉"), MODELS("Models", "◇")
}

public enum class ForgeAction(val title: String) {
    RUN("Run"), BUILD("Build"), INSPECT_GGUF("Inspect GGUF"), QUANTIZE("Quantize"),
    COMPILE("Compile"), TRAIN_LORA("Train LoRA"), TRAIN_QLORA("Train QLoRA"),
    FULL_FINE_TUNE("Full fine-tune"), OPEN_MODEL("Open model"), SERVE("Serve"), TOKENIZER("Tokenizer")
}

public data class ForgeChatMessage(val role: String, val content: String)

public data class ForgeArchiveEntry(val name: String, val isDirectory: Boolean, val sizeBytes: Long)

public data class ForgeUiState(
    val workspace: ForgeWorkspace = ForgeWorkspace.CODE,
    val activity: ForgeActivity = ForgeActivity.EXPLORER,
    val aiSection: ForgeAiSection = ForgeAiSection.AI_CODE,
    val fileName: String = "",
    val fileContent: String = "",
    val modelName: String = "No model selected",
    val modelLoaded: Boolean = false,
    val loading: Boolean = false,
    val chatMessages: List<ForgeChatMessage> = emptyList(),
    val consoleLogs: List<String> = emptyList(),
    val statusText: String = "Ready",
    val updateReady: Boolean = false,
    val updateMessage: String = "",
    val pushConfigured: Boolean = false,
    val archiveFileName: String = "",
    val archiveEntries: List<ForgeArchiveEntry> = emptyList(),
    val deviceSummary: String = "",
)

@Composable
public fun ForgeWorkbenchScreen(
    state: ForgeUiState,
    onWorkspaceSelected: (ForgeWorkspace) -> Unit,
    onAiSectionSelected: (ForgeAiSection) -> Unit,
    onActivitySelected: (ForgeActivity) -> Unit,
    onPickCodeFile: () -> Unit,
    onPickModelFile: () -> Unit,
    onNewFile: () -> Unit,
    onCloseFile: () -> Unit,
    onOpenFolder: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onChatSend: (String) -> Unit,
    onRunAction: (ForgeAction) -> Unit,
    onSaveEditor: (String) -> Unit,
    onOpenArchive: () -> Unit,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
    onEnableNotifications: () -> Unit,
) {
    var editorText by remember(state.fileName, state.fileContent) { mutableStateOf(state.fileContent) }
    var chatDraft by remember { mutableStateOf("") }
    var bottomTab by remember { mutableStateOf("Terminal") }
    var showCompactAssistant by remember { mutableStateOf(false) }
    var showExplorerDrawer by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(ForgeBackground),
    ) {
        val compact = maxWidth < 768.dp
        val wide = maxWidth >= 1100.dp
        val showDesktopExplorer = maxWidth >= 768.dp
        Column(Modifier.fillMaxSize()) {
            ForgeTopBar(
                state = state,
                compact = compact,
                showAssistantMenuItem = !wide,
                onWorkspaceSelected = onWorkspaceSelected,
                onActivitySelected = onActivitySelected,
                onPickCodeFile = onPickCodeFile,
                onOpenArchive = onOpenArchive,
                onNewFile = onNewFile,
                onRunAction = onRunAction,
                onCheckUpdates = onCheckUpdates,
                onInstallUpdate = onInstallUpdate,
                onEnableNotifications = onEnableNotifications,
                onToggleAssistant = { showCompactAssistant = !showCompactAssistant },
            )
            ForgePreviewNote()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Row(Modifier.fillMaxSize()) {
                    ForgeActivityRail(
                        activity = state.activity,
                        onActivitySelected = { activity ->
                            onActivitySelected(activity)
                            if (!showDesktopExplorer) {
                                showExplorerDrawer = !(showExplorerDrawer && state.activity == activity)
                            }
                        },
                        vertical = true,
                    )
                    if (showDesktopExplorer && state.workspace != ForgeWorkspace.ARCHIVE) {
                        ForgeWorkspaceSidebar(
                            state = state,
                            onPickCodeFile = onPickCodeFile,
                            onPickModelFile = onPickModelFile,
                            onNewFile = onNewFile,
                            onAiSectionSelected = onAiSectionSelected,
                            onRunAction = onRunAction,
                            modifier = Modifier.width(260.dp).fillMaxHeight(),
                        )
                    }
                    if (state.workspace == ForgeWorkspace.ARCHIVE) {
                        ForgeArchiveWorkspace(
                            state = state,
                            onOpenArchive = onOpenArchive,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    } else {
                        Column(Modifier.weight(1f).fillMaxHeight()) {
                            ForgeEditorArea(
                                state = state,
                                editorText = editorText,
                                onEditorTextChanged = { editorText = it },
                                onSave = { onSaveEditor(editorText) },
                                onPickCodeFile = onPickCodeFile,
                                onSendPrompt = onSendPrompt,
                                onRunAction = onRunAction,
                                onOpenArchive = onOpenArchive,
                                onStartNewFile = onNewFile,
                                onOpenExplorer = { showExplorerDrawer = true },
                                onCloseFile = onCloseFile,
                                onOpenFolder = onOpenFolder,
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                            )
                            ForgeBottomPanel(state, bottomTab, onTabSelected = { bottomTab = it })
                        }
                        if (wide) {
                            ForgeAssistantPanel(
                                state = state,
                                chatDraft = chatDraft,
                                onDraftChanged = { chatDraft = it },
                                onSend = {
                                    val prompt = chatDraft.trim()
                                    if (prompt.isNotEmpty()) {
                                        onChatSend(prompt)
                                        chatDraft = ""
                                    }
                                },
                                modifier = Modifier.width(300.dp).fillMaxHeight(),
                            )
                        }
                    }
                }
                if (showExplorerDrawer && !showDesktopExplorer && state.workspace != ForgeWorkspace.ARCHIVE) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                            .clickable { showExplorerDrawer = false }.zIndex(2f),
                    )
                    ForgeWorkspaceSidebar(
                        state = state,
                        onPickCodeFile = { showExplorerDrawer = false; onPickCodeFile() },
                        onPickModelFile = { showExplorerDrawer = false; onPickModelFile() },
                        onNewFile = { showExplorerDrawer = false; onNewFile() },
                        onAiSectionSelected = onAiSectionSelected,
                        onRunAction = onRunAction,
                        onCloseDrawer = { showExplorerDrawer = false },
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 50.dp)
                            .width(280.dp).fillMaxHeight().zIndex(3f),
                    )
                }
                if (!showDesktopExplorer && state.workspace != ForgeWorkspace.ARCHIVE) {
                    ForgeFloatingButton(
                        label = "Buka Explorer",
                        onClick = { showExplorerDrawer = true },
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 10.dp),
                    )
                }
                if (!wide && state.workspace != ForgeWorkspace.ARCHIVE) {
                    ForgeFloatingButton(
                        copilot = true,
                        label = "AI Chat",
                        onClick = { showCompactAssistant = true },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 10.dp),
                    )
                }
                if (!wide && showCompactAssistant && state.workspace != ForgeWorkspace.ARCHIVE) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.60f))
                            .clickable { showCompactAssistant = false }.zIndex(4f),
                    )
                    ForgeAssistantPanel(
                        state = state,
                        chatDraft = chatDraft,
                        onDraftChanged = { chatDraft = it },
                        onSend = {
                            val prompt = chatDraft.trim()
                            if (prompt.isNotEmpty()) {
                                onChatSend(prompt)
                                chatDraft = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(vertical = 10.dp)
                            .width(if (compact) (maxWidth - 28.dp) else 380.dp).fillMaxHeight(0.96f).zIndex(5f),
                    )
                }
            }
            ForgeStatusBar(state)
        }
    }
}

@Composable
private fun ForgeTopBar(
    state: ForgeUiState,
    compact: Boolean,
    showAssistantMenuItem: Boolean,
    onWorkspaceSelected: (ForgeWorkspace) -> Unit,
    onActivitySelected: (ForgeActivity) -> Unit,
    onPickCodeFile: () -> Unit,
    onOpenArchive: () -> Unit,
    onNewFile: () -> Unit,
    onRunAction: (ForgeAction) -> Unit,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
    onEnableNotifications: () -> Unit,
    onToggleAssistant: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().background(ForgeBackground)) {
        Row(
            Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            if (compact) ForgeTextButton("‹", onClick = { onActivitySelected(ForgeActivity.EXPLORER) }, compact = true)
            Image(
                painter = painterResource(R.drawable.forge_logo),
                contentDescription = "Forge",
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(if (compact) 17.dp else 20.dp).aspectRatio(400f / 218f),
            )
            if (compact) {
                Text(
                    state.activity.title.lowercase(),
                    color = ForgeTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                ForgeFileMenuButton(onPickCodeFile, onNewFile, onOpenArchive)
                Box {
                    ForgeTextButton("⋮", onClick = { showMenu = true }, compact = true)
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        ForgeWorkspace.values().forEach { workspace ->
                            DropdownMenuItem(
                                text = { Text(workspace.title) },
                                onClick = { onWorkspaceSelected(workspace); showMenu = false },
                            )
                        }
                        DropdownMenuItem(text = { Text("Updates") }, onClick = { onCheckUpdates(); showMenu = false })
                        DropdownMenuItem(text = { Text("Notify") }, onClick = { onEnableNotifications(); showMenu = false })
                        if (showAssistantMenuItem) DropdownMenuItem(text = { Text("AI Chat") }, onClick = { onToggleAssistant(); showMenu = false })
                        if (state.updateReady) DropdownMenuItem(text = { Text("Install update") }, onClick = { onInstallUpdate(); showMenu = false })
                    }
                }
                ForgeAccentButton("▶ Run", onClick = { onRunAction(ForgeAction.RUN) })
            } else {
                Text("AI Studio", color = ForgeMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    ForgeFileMenuButton(onPickCodeFile, onNewFile, onOpenArchive)
                ForgeTextButton("Explorer", onClick = { onActivitySelected(ForgeActivity.EXPLORER) }, compact = true)
                ForgeWorkspace.values().forEach { workspace ->
                    ForgeChip(workspace.title, selected = workspace == state.workspace) { onWorkspaceSelected(workspace) }
                }
                if (state.modelLoaded) {
                    Text("●", color = ForgeGreen, fontSize = 9.sp)
                    Text(state.modelName, color = ForgeMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(
                    Modifier.background(ForgePanel, RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ForgeCopilotLogo(height = 14.dp, modifier = Modifier.padding(end = 5.dp))
                    Text("Copilot • Forge Edition", color = ForgeMuted, fontSize = 9.sp, maxLines = 1)
                }
                Box {
                    ForgeTextButton("⋮", onClick = { showMenu = true }, compact = true)
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Updates") }, onClick = { onCheckUpdates(); showMenu = false })
                        DropdownMenuItem(text = { Text("Notify") }, onClick = { onEnableNotifications(); showMenu = false })
                        if (showAssistantMenuItem) DropdownMenuItem(text = { Text("AI Chat") }, onClick = { onToggleAssistant(); showMenu = false })
                        if (state.updateReady) DropdownMenuItem(text = { Text("Install update") }, onClick = { onInstallUpdate(); showMenu = false })
                    }
                }
                ForgeAccentButton("▶ Run", onClick = { onRunAction(ForgeAction.RUN) })
            }
        }
        Divider(color = ForgeBorder, thickness = 1.dp)
    }
}

@Composable
private fun ForgeFileMenuButton(onPickCodeFile: () -> Unit, onNewFile: () -> Unit, onOpenArchive: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ForgeTextButton("File ▾", onClick = { expanded = true }, compact = true)
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Open File") }, onClick = { onPickCodeFile(); expanded = false })
            DropdownMenuItem(text = { Text("New File") }, onClick = { onNewFile(); expanded = false })
            DropdownMenuItem(text = { Text("Import ZIP") }, onClick = { onOpenArchive(); expanded = false })
        }
    }
}

@Composable
private fun ForgePreviewNote() {
    Row(
        Modifier.fillMaxWidth().background(ForgePanel).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("PREVIEW UI", color = ForgeOrange, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(
            "Catatan: beberapa kontrol masih tampilan; engine native belum semuanya terhubung.",
            color = ForgeMuted,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ForgeAiEngineeringWorkspace(
    state: ForgeUiState,
    editorText: String,
    onEditorTextChanged: (String) -> Unit,
    onAiSectionSelected: (ForgeAiSection) -> Unit,
    onPickCodeFile: () -> Unit,
    onPickModelFile: () -> Unit,
    onSaveEditor: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onRunAction: (ForgeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(ForgeBackground)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ForgeAiSection.values().forEach { section ->
                ForgeChip(section.title, selected = section == state.aiSection) { onAiSectionSelected(section) }
            }
        }
        Divider(color = ForgeBorder)
        when (state.aiSection) {
            ForgeAiSection.AI_CODE -> ForgeEditorArea(
                state = state,
                editorText = editorText,
                onEditorTextChanged = onEditorTextChanged,
                onSave = onSaveEditor,
                onPickCodeFile = onPickCodeFile,
                onSendPrompt = onSendPrompt,
                onRunAction = onRunAction,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            ForgeAiSection.MODELS -> Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ForgeInfoCard(
                    title = "GGUF Toolkit",
                    subtitle = if (state.modelLoaded) "Model loaded: ${state.modelName}" else "Model belum dipilih",
                    body = "Android file picker dan metadata reader tersedia. Model inference berjalan lewat JNI llama.cpp bila file GGUF kompatibel dan perangkat memiliki memori cukup.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ForgeAccentButton("Pilih model GGUF", onClick = onPickModelFile)
                    ForgeTextButton("Inspect GGUF", onClick = { onRunAction(ForgeAction.INSPECT_GGUF) })
                }
                ForgeInfoCard(
                    title = "Batas fungsi saat ini",
                    subtitle = "Import/convert, quantize, split, dan bench belum diimplementasikan sebagai operasi native Android.",
                    body = "Tombol tool tersebut tidak akan menampilkan hasil sukses palsu. Untuk menjalankan inference, muat model GGUF lalu gunakan panel AI Code.",
                )
            }
            ForgeAiSection.SYSTEM -> NativeInfoPage(
                title = "System",
                subtitle = "Konfigurasi sistem aplikasi native",
                body = "${state.deviceSummary}\n\nFile model disimpan di ruang privat aplikasi. Akses file kode dan model menggunakan Android Storage Access Framework.",
            )
            ForgeAiSection.TOKENS -> NativeInfoPage(
                title = "Tokens",
                subtitle = "Tokenizer belum tersambung ke tokenizer model",
                body = "Belum ada encode/decode atau token-count native yang dapat divalidasi. Tidak ada hitungan token contoh yang akan ditampilkan sebagai hasil nyata.",
            )
            ForgeAiSection.ENGINES -> NativeInfoPage(
                title = "Engines",
                subtitle = "Integrasi inference Android",
                body = "Source aplikasi menautkan wrapper JNI llama.cpp untuk ABI arm64-v8a. Build CI memverifikasi kompilasi APK; pemuatan model dan generasi tetap perlu diuji pada perangkat Android dengan model yang didukung.",
            )
            ForgeAiSection.MACHINE -> NativeInfoPage(
                title = "Machine",
                subtitle = "Informasi perangkat Android",
                body = state.deviceSummary,
            )
            ForgeAiSection.TRAINING -> NativeInfoPage(
                title = "Training",
                subtitle = "Training model belum tersedia di perangkat ini",
                body = "LoRA, QLoRA, full fine-tune, evaluasi, dan serving membutuhkan runtime/backend tambahan yang belum dibundel atau diuji sebagai fungsi Android native.",
            )
        }
    }
}

@Composable
private fun ForgeArchiveWorkspace(
    state: ForgeUiState,
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(ForgeBackground).padding(14.dp)) {
        ForgeInfoCard(
            title = "Archive / Bongkar",
            subtitle = if (state.archiveFileName.isBlank()) "Belum ada ZIP dibuka" else state.archiveFileName,
            body = "Pilih ZIP untuk membaca daftar entry secara lokal. Layar ini hanya menampilkan isi arsip; ekstraksi dan penyuntingan ZIP belum diaktifkan.",
        )
        ForgeAccentButton("Buka file ZIP", onClick = onOpenArchive)
        Spacer(Modifier.height(10.dp))
        if (state.archiveEntries.isEmpty()) {
            Text("Entry arsip akan ditampilkan setelah file ZIP dipilih.", color = ForgeMuted, fontSize = 11.sp)
        } else {
            Text("${state.archiveEntries.size} entries", color = ForgeMuted, fontSize = 10.sp)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                state.archiveEntries.forEach { entry ->
                    Row(
                        Modifier.fillMaxWidth().border(0.5.dp, ForgeBorder, RoundedCornerShape(5.dp)).padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(if (entry.isDirectory) "▸" else "•", color = ForgeOrange, fontSize = 12.sp)
                        Text(entry.name, color = ForgeTextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f).padding(start = 7.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!entry.isDirectory) Text(formatBytes(entry.sizeBytes), color = ForgeMutedSecondary, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NativeInfoPage(title: String, subtitle: String, body: String) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        ForgeInfoCard(title = title, subtitle = subtitle, body = body)
    }
}

@Composable
private fun ForgeInfoCard(title: String, subtitle: String, body: String) {
    Column(
        Modifier.fillMaxWidth().background(ForgeSurface, RoundedCornerShape(9.dp))
            .border(0.5.dp, ForgeBorder, RoundedCornerShape(9.dp)).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(title, color = ForgeText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = ForgeOrange, fontSize = 11.sp)
        Text(body, color = ForgeMuted, fontSize = 11.sp, lineHeight = 16.sp)
    }
}

private fun formatBytes(value: Long): String = when {
    value < 1024 -> "$value B"
    value < 1024 * 1024 -> "${value / 1024} KB"
    else -> "${value / (1024 * 1024)} MB"
}

@Composable
private fun ForgeWorkspaceSidebar(
    state: ForgeUiState,
    onPickCodeFile: () -> Unit,
    onPickModelFile: () -> Unit,
    onNewFile: () -> Unit,
    onAiSectionSelected: (ForgeAiSection) -> Unit,
    onRunAction: (ForgeAction) -> Unit,
    onCloseDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (state.workspace == ForgeWorkspace.AI_ENGINEERING) {
        Column(modifier.background(ForgeSurface).border(0.5.dp, ForgeBorder).padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("AI ENGINEERING", color = ForgeMuted, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                if (onCloseDrawer != null) ForgeTextButton("×", onClick = onCloseDrawer, compact = true)
                ForgeTextButton("+", onClick = onNewFile, compact = true)
            }
            Spacer(Modifier.height(10.dp))
            ForgeAiSection.values().forEach { section ->
                Text(
                    section.title,
                    color = if (section == state.aiSection) ForgeText else ForgeMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                        .background(if (section == state.aiSection) ForgePanelRaised else ForgeSurface, RoundedCornerShape(5.dp))
                        .clickable { onAiSectionSelected(section) }
                        .padding(horizontal = 9.dp, vertical = 8.dp),
                )
            }
            Divider(color = ForgeBorder, modifier = Modifier.padding(vertical = 9.dp))
            when (state.aiSection) {
                ForgeAiSection.AI_CODE -> {
                    Text("FORGE WORKSPACE", color = ForgeMutedSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    ForgeTextButton("Open code file", onClick = onPickCodeFile)
                    ForgeTextButton("Run / Build", onClick = { onRunAction(ForgeAction.BUILD) })
                }
                ForgeAiSection.MODELS -> {
                    Text("MODEL REGISTRY", color = ForgeMutedSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(state.modelName, color = ForgeTextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (state.modelLoaded) "● Loaded" else "○ No model selected", color = if (state.modelLoaded) ForgeGreen else ForgeMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp))
                    ForgeTextButton("Buka isi model", onClick = onPickModelFile)
                }
                ForgeAiSection.TRAINING -> {
                    Text("MODEL • QUANT • TRAIN", color = ForgeMutedSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    ForgeChip("Train LoRA", false) { onRunAction(ForgeAction.TRAIN_LORA) }
                    ForgeChip("Train QLoRA", false) { onRunAction(ForgeAction.TRAIN_QLORA) }
                    ForgeChip("Full fine-tune", false) { onRunAction(ForgeAction.FULL_FINE_TUNE) }
                }
                else -> {
                    Text(state.aiSection.title.uppercase(), color = ForgeMutedSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Panel ini mengikuti kategori AI Engineering pada UI Workbench.", color = ForgeMuted, fontSize = 10.sp, lineHeight = 15.sp)
                }
            }
        }
    } else {
        ForgeExplorer(state, onPickCodeFile, onPickModelFile, onNewFile, onCloseDrawer, modifier)
    }
}

@Composable
private fun ForgeActivityRail(activity: ForgeActivity, onActivitySelected: (ForgeActivity) -> Unit, vertical: Boolean) {
    val rowModifier = if (vertical) Modifier.width(50.dp).fillMaxHeight() else Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)
    if (vertical) {
        Column(rowModifier.background(ForgeBackground).border(width = 0.5.dp, color = ForgeBorder), horizontalAlignment = Alignment.CenterHorizontally) {
            ForgeActivity.values().forEach { ForgeActivityButton(it, it == activity, onActivitySelected, showLabel = false) }
            Spacer(Modifier.weight(1f))
            Text("⌘K", color = ForgeMutedSecondary, fontSize = 9.sp, modifier = Modifier.padding(vertical = 7.dp))
            Box(Modifier.padding(bottom = 8.dp).size(28.dp).background(Brush.linearGradient(listOf(ForgeOrange, ForgePink)), CircleShape), contentAlignment = Alignment.Center) {
                Text("F", color = ForgeText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Row(rowModifier.background(ForgeBackground).horizontalScroll(rememberScrollState()).border(width = 0.5.dp, color = ForgeBorder), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ForgeActivity.values().forEach { ForgeActivityButton(it, it == activity, onActivitySelected, showLabel = true) }
        }
    }
}

@Composable
private fun ForgeActivityButton(item: ForgeActivity, selected: Boolean, onActivitySelected: (ForgeActivity) -> Unit, showLabel: Boolean) {
    Column(
        Modifier.padding(horizontal = 4.dp, vertical = 3.dp).size(width = 42.dp, height = if (showLabel) 45.dp else 42.dp)
            .background(if (selected) ForgePanel else ForgeBackground, RoundedCornerShape(8.dp))
            .clickable { onActivitySelected(item) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(item.mark, color = if (selected) ForgeText else ForgeMutedSecondary, fontSize = 18.sp)
        if (showLabel) Text(item.title.substringBefore(' '), color = if (selected) ForgeTextSecondary else ForgeMutedSecondary, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun ForgeExplorer(
    state: ForgeUiState,
    onPickCodeFile: () -> Unit,
    onPickModelFile: () -> Unit,
    onNewFile: () -> Unit,
    onCloseDrawer: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(ForgeSurface).border(width = 0.5.dp, color = ForgeBorder).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.activity.title.uppercase(), color = ForgeMuted, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            if (onCloseDrawer != null) ForgeTextButton("×", onClick = onCloseDrawer, compact = true)
            ForgeTextButton("+", onClick = onNewFile, compact = true)
        }
        Spacer(Modifier.height(12.dp))
        when (state.activity) {
            ForgeActivity.EXPLORER -> {
                ExplorerRow("⌄", "FORGE WORKSPACE", true)
                if (state.fileName.isBlank()) {
                    Text("Workspace masih kosong", color = ForgeMutedSecondary, fontSize = 10.sp, modifier = Modifier.padding(start = 17.dp, top = 6.dp))
                    ForgeTextButton("Open File", onClick = onPickCodeFile)
                } else {
                    ExplorerRow("▸", state.fileName, false, selected = true)
                }
                ExplorerRow("▸", "models", true)
                ExplorerRow("◇", state.modelName, false)
            }
            ForgeActivity.MODELS -> {
                ExplorerRow("◇", "Models", true)
                ExplorerRow("•", state.modelName, false, selected = state.modelLoaded)
                ForgeTextButton("Open model file", onClick = onPickModelFile)
            }
            ForgeActivity.SEARCH -> {
                ForgeInput("Search files", "")
                Text("Pencarian file lokal", color = ForgeMutedSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
            ForgeActivity.SOURCE_CONTROL -> {
                Text("Git remote belum dihubungkan", color = ForgeMuted, fontSize = 11.sp)
                ForgeTextButton("Open code file", onClick = onPickCodeFile)
            }
            ForgeActivity.DEBUG -> Text("Gunakan action bar untuk menjalankan atau membangun melalui host app.", color = ForgeMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(18.dp))
        Divider(color = ForgeBorder)
        Text("MODEL", color = ForgeMutedSecondary, fontSize = 9.sp, letterSpacing = 1.sp, modifier = Modifier.padding(vertical = 10.dp))
        Text(state.modelName, color = ForgeTextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(if (state.modelLoaded) "● Loaded" else "○ Not loaded", color = if (state.modelLoaded) ForgeGreen else ForgeMutedSecondary, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        ForgeTextButton("Choose model", onClick = onPickModelFile)
    }
}

@Composable
private fun ExplorerRow(mark: String, label: String, muted: Boolean, selected: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().background(if (selected) ForgePanelRaised else ForgeSurface, RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(mark, color = ForgeMutedSecondary, fontSize = 12.sp, modifier = Modifier.padding(end = 7.dp))
        Text(label, color = if (muted) ForgeMuted else ForgeTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ForgeEditorArea(
    state: ForgeUiState,
    editorText: String,
    onEditorTextChanged: (String) -> Unit,
    onSave: () -> Unit,
    onPickCodeFile: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onRunAction: (ForgeAction) -> Unit,
    modifier: Modifier = Modifier,
    onOpenArchive: () -> Unit = {},
    onStartNewFile: () -> Unit = {},
    onOpenExplorer: () -> Unit = {},
    onCloseFile: () -> Unit = {},
    onOpenFolder: () -> Unit = {},
) {
    var inlinePrompt by remember { mutableStateOf("") }
    val workspaceEmpty = state.fileName.isBlank() && editorText.isBlank()
    Column(modifier.background(ForgeBackground)) {
        Row(
            Modifier.fillMaxWidth().height(34.dp).background(ForgeSurface).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("◇", color = ForgeOrange, fontSize = 14.sp, modifier = Modifier.padding(end = 7.dp))
            Text(state.fileName.ifBlank { "Forge workspace" }, color = ForgeTextSecondary, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            ForgeTextButton("Save", onClick = onSave, compact = true)
            ForgeTextButton("All", onClick = onSave, compact = true)
            ForgeTextButton("×", onClick = onCloseFile, compact = true)
        }
        Row(
            Modifier.fillMaxWidth().height(42.dp).background(ForgeBackground).padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ForgeCopilotLogo(height = 13.dp)
            Text("Inline", color = ForgeMuted, fontSize = 9.sp)
            ForgeInput("Ask Copilot… (/explain, /fix, /decompile)", inlinePrompt, { inlinePrompt = it }, Modifier.weight(1f))
            ForgeAccentButton("Send", onClick = {
                if (inlinePrompt.isNotBlank()) {
                    onSendPrompt(inlinePrompt.trim())
                    inlinePrompt = ""
                }
            })
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)) {
            if (state.loading) {
                CircularProgressIndicator(color = ForgeOrange, modifier = Modifier.size(26.dp).align(Alignment.TopEnd), strokeWidth = 2.dp)
            }
            if (workspaceEmpty) {
                ForgeEmptyWorkspace(
                    onOpenFile = onPickCodeFile,
                    onOpenArchive = onOpenArchive,
                    onNewFile = onStartNewFile,
                    onOpenExplorer = onOpenExplorer,
                    onOpenFolder = onOpenFolder,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                BasicTextField(
                    value = editorText,
                    onValueChange = onEditorTextChanged,
                    modifier = Modifier.fillMaxSize().background(ForgeBackground).verticalScroll(rememberScrollState()),
                    textStyle = TextStyle(color = ForgeTextSecondary, fontSize = 13.sp, lineHeight = 20.sp, fontFamily = FontFamily.Monospace),
                    decorationBox = { inner ->
                        Box {
                            if (editorText.isEmpty()) Text("Mulai mengetik…", color = ForgeMutedSecondary, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            inner()
                        }
                    },
                )
            }
        }
        if (state.updateReady && state.updateMessage.isNotBlank()) {
            Text("Update: ${state.updateMessage}", color = ForgeMuted, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun ForgeEmptyWorkspace(
    onOpenFile: () -> Unit,
    onOpenArchive: () -> Unit,
    onNewFile: () -> Unit,
    onOpenExplorer: () -> Unit,
    onOpenFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(64.dp).background(Brush.linearGradient(listOf(ForgeOrange, ForgePink)), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ForgeCopilotLogo(height = 23.dp)
        }
        Spacer(Modifier.height(12.dp))
        Text("Forge AI Engineering", color = ForgeText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Tidak ada file terbuka. Buka file, impor ZIP atau tempel kode untuk mulai mengedit.",
            color = ForgeMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeEmptyActionButton("Open File", primary = true, onClick = onOpenFile, modifier = Modifier.weight(1f))
            ForgeEmptyActionButton("Import ZIP", onClick = onOpenArchive, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeEmptyActionButton("Import Code", onClick = onOpenFile, modifier = Modifier.weight(1f))
            ForgeEmptyActionButton("New File", onClick = onNewFile, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeEmptyActionButton("Open Folder", onClick = onOpenFolder, modifier = Modifier.weight(1f))
            ForgeEmptyActionButton("Open Explorer", onClick = onOpenExplorer, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ForgeEmptyActionButton(label: String, primary: Boolean = false, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        label,
        color = if (primary) Color(0xFF111113) else ForgeTextSecondary,
        fontSize = 10.sp,
        fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1,
        modifier = modifier.padding(vertical = 4.dp)
            .background(if (primary) ForgeText else ForgePanel, RoundedCornerShape(7.dp))
            .border(0.5.dp, ForgeBorder, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 9.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
private fun ForgeCopilotLogo(height: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.copilot_logo),
        contentDescription = "Logo Copilot",
        contentScale = ContentScale.Fit,
        modifier = modifier.height(height).aspectRatio(2f),
    )
}

@Composable
private fun ForgeFloatingButton(
    copilot: Boolean = false,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (copilot) {
        Box(
            modifier.size(46.dp).background(Brush.linearGradient(listOf(ForgeOrange, ForgePink)), CircleShape)
                .border(0.5.dp, ForgeBorder, CircleShape).clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            ForgeCopilotLogo(height = 21.dp)
        }
    } else {
        Row(
            modifier.background(ForgePanel, RoundedCornerShape(20.dp)).border(0.5.dp, ForgeBorder, RoundedCornerShape(20.dp))
                .clickable(onClick = onClick).padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(36.dp).background(ForgePanelRaised, CircleShape), contentAlignment = Alignment.Center) {
                Text("▤", color = ForgeOrange, fontSize = 16.sp)
            }
            Text(label, color = ForgeTextSecondary, fontSize = 9.sp, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun ForgeAssistantPanel(
    state: ForgeUiState,
    chatDraft: String,
    onDraftChanged: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.background(ForgeSurface).border(width = 0.5.dp, color = ForgeBorder)) {
        Row(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            ForgeCopilotLogo(height = 18.dp, modifier = Modifier.padding(end = 8.dp))
            Text(state.aiSection.title, color = ForgeText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("AI", color = ForgeMutedSecondary, fontSize = 10.sp)
        }
        Divider(color = ForgeBorder)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.chatMessages.isEmpty()) {
                Text("What are you building?", color = ForgeTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("Ask about code, models, or your current workflow.", color = ForgeMuted, fontSize = 11.sp, lineHeight = 16.sp)
                ForgeChip("Explain this file", selected = false) { onDraftChanged("Explain this file") }
                ForgeChip("Review my code", selected = false) { onDraftChanged("Review my code") }
            }
            state.chatMessages.forEach { message ->
                Column(Modifier.fillMaxWidth().background(if (message.role.equals("user", true)) ForgePanelRaised else ForgePanel, RoundedCornerShape(8.dp)).padding(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!message.role.equals("user", true)) ForgeCopilotLogo(height = 11.dp, modifier = Modifier.padding(end = 5.dp))
                        Text(message.role.replaceFirstChar { it.uppercase() }, color = if (message.role.equals("user", true)) ForgeOrange else ForgeMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(message.content, color = ForgeTextSecondary, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            ForgeInput("Message Forge…", chatDraft, onDraftChanged, Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            ForgeAccentButton("↑", onClick = onSend)
        }
    }
}

@Composable
private fun ForgeBottomPanel(state: ForgeUiState, selectedTab: String, onTabSelected: (String) -> Unit) {
    val tabs = listOf("Problems", "Output", "Terminal", "Inference", "Trainer")
    Column(Modifier.fillMaxWidth().height(220.dp).background(ForgeSurface).border(width = 0.5.dp, color = ForgeBorder)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            tabs.forEach { tab ->
                Column(Modifier.clickable { onTabSelected(tab) }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tab, color = if (selectedTab == tab) ForgeText else ForgeMutedSecondary, fontSize = 10.sp)
                    if (selectedTab == tab) Spacer(Modifier.padding(top = 5.dp).width(24.dp).height(2.dp).background(ForgeOrange, CircleShape))
                }
            }
        }
        Divider(color = ForgeBorder)
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 7.dp)) {
            when (selectedTab) {
                "Problems" -> Text("No problem data reported", color = ForgeMutedSecondary, fontSize = 10.sp)
                "Output" -> if (state.consoleLogs.isEmpty()) {
                    Text("Belum ada output", color = ForgeMutedSecondary, fontSize = 10.sp)
                } else state.consoleLogs.takeLast(40).forEach { Text(it, color = ForgeTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 15.sp) }
                "Terminal" -> Column(Modifier.fillMaxWidth()) {
                    if (state.consoleLogs.isEmpty()) {
                        Text("Terminal preview • shell host belum diaktifkan", color = ForgeMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    } else {
                        state.consoleLogs.takeLast(20).forEach { Text(it, color = ForgeTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 15.sp) }
                    }
                    Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("›", color = ForgeOrange, fontSize = 14.sp)
                        Text(" forge$", color = ForgeMutedSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
                "Inference" -> Text(if (state.modelLoaded) "Model: ${state.modelName}" else "Pilih model untuk memulai inference.", color = ForgeMuted, fontSize = 10.sp)
                "Trainer" -> Text("Training actions are delegated to the host app.", color = ForgeMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ForgeStatusBar(state: ForgeUiState) {
    Row(
        Modifier.fillMaxWidth().height(24.dp).background(Color(0xFF101012)).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("●", color = ForgeGreen, fontSize = 8.sp)
        Text("  ${state.statusText}", color = ForgeMuted, fontSize = 9.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (state.pushConfigured) Text("FCM configured", color = ForgeMutedSecondary, fontSize = 9.sp)
        Text("  Forge Workbench", color = ForgeMutedSecondary, fontSize = 9.sp)
    }
}

@Composable
private fun ForgeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) ForgeText else ForgeMuted,
        fontSize = 10.sp,
        maxLines = 1,
        modifier = Modifier.background(if (selected) ForgePanelRaised else ForgeSurface, RoundedCornerShape(6.dp))
            .border(0.5.dp, if (selected) ForgeOrange.copy(alpha = 0.42f) else ForgeBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 6.dp),
    )
}

@Composable
private fun ForgeTextButton(label: String, onClick: () -> Unit, compact: Boolean = false) {
    Text(
        label,
        color = ForgeMuted,
        fontSize = if (compact) 10.sp else 11.sp,
        maxLines = 1,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = if (compact) 5.dp else 7.dp),
    )
}

@Composable
private fun ForgeAccentButton(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = ForgeText,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        modifier = Modifier.background(Brush.linearGradient(listOf(ForgeOrange, Color(0xFFFF8A00))), RoundedCornerShape(7.dp))
            .clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 8.dp),
    )
}

@Composable
private fun ForgeInput(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.background(ForgePanel, RoundedCornerShape(7.dp)).padding(horizontal = 9.dp, vertical = 8.dp),
        singleLine = true,
        textStyle = TextStyle(color = ForgeTextSecondary, fontSize = 11.sp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = ForgeMutedSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                inner()
            }
        },
    )
}
