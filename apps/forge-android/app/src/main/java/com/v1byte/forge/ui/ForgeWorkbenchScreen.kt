package com.v1byte.forge.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

public enum class ForgeWorkspace(val title: String) {
    CODE("Code"), AI_ENGINEERING("AI Engineering"), TRAINING("Training"), ARCHIVE("Archive")
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

public data class ForgeUiState(
    val workspace: ForgeWorkspace = ForgeWorkspace.CODE,
    val activity: ForgeActivity = ForgeActivity.EXPLORER,
    val aiSection: String = "Assistant",
    val fileName: String = "README.md",
    val fileContent: String = "# Forge Workbench\n\nSelect a file or model to get started.\n",
    val modelName: String = "No model selected",
    val modelLoaded: Boolean = false,
    val loading: Boolean = false,
    val chatMessages: List<ForgeChatMessage> = emptyList(),
    val consoleLogs: List<String> = emptyList(),
    val statusText: String = "Ready",
    val updateReady: Boolean = false,
    val updateMessage: String = "",
    val pushConfigured: Boolean = false,
)

@Composable
public fun ForgeWorkbenchScreen(
    state: ForgeUiState,
    onWorkspaceSelected: (ForgeWorkspace) -> Unit,
    onActivitySelected: (ForgeActivity) -> Unit,
    onPickCodeFile: () -> Unit,
    onPickModelFile: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onChatSend: (String) -> Unit,
    onRunAction: (ForgeAction) -> Unit,
    onSaveEditor: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
    onEnableNotifications: () -> Unit,
) {
    var editorText by remember(state.fileName, state.fileContent) { mutableStateOf(state.fileContent) }
    var chatDraft by remember { mutableStateOf("") }
    var bottomTab by remember { mutableStateOf("Problems") }
    var showCompactAssistant by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(ForgeBackground),
    ) {
        val wide = maxWidth >= 1100.dp
        val medium = maxWidth >= 720.dp
        val showExplorer = maxWidth >= 900.dp
        Column(Modifier.fillMaxSize()) {
            ForgeTopBar(
                state = state,
                onWorkspaceSelected = onWorkspaceSelected,
                onRunAction = onRunAction,
                onCheckUpdates = onCheckUpdates,
                onInstallUpdate = onInstallUpdate,
                onEnableNotifications = onEnableNotifications,
                showAssistantToggle = !wide,
                onToggleAssistant = { showCompactAssistant = !showCompactAssistant },
            )
            if (medium) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    ForgeActivityRail(state.activity, onActivitySelected, vertical = true)
                    if (showExplorer) {
                        ForgeExplorer(state, onPickCodeFile, onPickModelFile, Modifier.width(212.dp).fillMaxHeight())
                    }
                    ForgeEditorArea(
                        state = state,
                        editorText = editorText,
                        onEditorTextChanged = { editorText = it },
                        onSave = { onSaveEditor(editorText) },
                        onPickCodeFile = onPickCodeFile,
                        onSendPrompt = onSendPrompt,
                        onRunAction = onRunAction,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
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
            } else {
                ForgeActivityRail(state.activity, onActivitySelected, vertical = false)
                ForgeEditorArea(
                    state = state,
                    editorText = editorText,
                    onEditorTextChanged = { editorText = it },
                    onSave = { onSaveEditor(editorText) },
                    onPickCodeFile = onPickCodeFile,
                    onSendPrompt = onSendPrompt,
                    onRunAction = onRunAction,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            ForgeBottomPanel(state, bottomTab, onTabSelected = { bottomTab = it })
            ForgeStatusBar(state)
        }
        if (!wide && showCompactAssistant) {
            Dialog(onDismissRequest = { showCompactAssistant = false }) {
                Box(
                    Modifier.fillMaxWidth().height(440.dp)
                        .background(ForgeSurface, RoundedCornerShape(10.dp)),
                ) {
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
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ForgeTopBar(
    state: ForgeUiState,
    onWorkspaceSelected: (ForgeWorkspace) -> Unit,
    onRunAction: (ForgeAction) -> Unit,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: () -> Unit,
    onEnableNotifications: () -> Unit,
    showAssistantToggle: Boolean,
    onToggleAssistant: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(ForgeBackground)) {
        Row(
            Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(30.dp).background(
                    Brush.linearGradient(listOf(ForgeOrange, ForgePink)), RoundedCornerShape(8.dp)
                ), contentAlignment = Alignment.Center,
            ) { Text("F", color = ForgeText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
            Column(Modifier.weight(1f)) {
                Text("Forge", color = ForgeText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("AI STUDIO", color = ForgeMutedSecondary, fontSize = 9.sp, letterSpacing = 1.4.sp)
            }
            if (state.modelLoaded) {
                Text("●", color = ForgeGreen, fontSize = 9.sp)
                Text(state.modelName, color = ForgeMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            ForgeTextButton("Updates", onClick = onCheckUpdates, compact = true)
            ForgeTextButton("Notify", onClick = onEnableNotifications, compact = true)
            if (showAssistantToggle) ForgeTextButton("AI Chat", onClick = onToggleAssistant, compact = true)
            ForgeAccentButton("▶ Run", onClick = { onRunAction(ForgeAction.RUN) })
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ForgeWorkspace.values().forEach { workspace ->
                ForgeChip(workspace.title, selected = workspace == state.workspace) { onWorkspaceSelected(workspace) }
            }
        }
        if (state.updateReady) {
            Row(
                Modifier.fillMaxWidth().background(ForgePanel).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (state.updateMessage.isBlank()) "Update available" else state.updateMessage, color = ForgeMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                ForgeTextButton("Install", onClick = onInstallUpdate, compact = true)
            }
        }
        Divider(color = ForgeBorder, thickness = 1.dp)
    }
}

@Composable
private fun ForgeActivityRail(activity: ForgeActivity, onActivitySelected: (ForgeActivity) -> Unit, vertical: Boolean) {
    val rowModifier = if (vertical) Modifier.width(50.dp).fillMaxHeight().padding(vertical = 8.dp) else Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)
    if (vertical) {
        Column(rowModifier.background(ForgeBackground).border(width = 0.5.dp, color = ForgeBorder), horizontalAlignment = Alignment.CenterHorizontally) {
            ForgeActivity.values().forEach { ForgeActivityButton(it, it == activity, onActivitySelected) }
            Spacer(Modifier.weight(1f))
            Text("⚙", color = ForgeMutedSecondary, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
        }
    } else {
        Row(rowModifier.background(ForgeBackground).horizontalScroll(rememberScrollState()).border(width = 0.5.dp, color = ForgeBorder), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ForgeActivity.values().forEach { ForgeActivityButton(it, it == activity, onActivitySelected) }
        }
    }
}

@Composable
private fun ForgeActivityButton(item: ForgeActivity, selected: Boolean, onActivitySelected: (ForgeActivity) -> Unit) {
    Column(
        Modifier.padding(horizontal = 4.dp, vertical = 3.dp).size(width = 52.dp, height = 45.dp)
            .background(if (selected) ForgePanel else ForgeBackground, RoundedCornerShape(8.dp))
            .clickable { onActivitySelected(item) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(item.mark, color = if (selected) ForgeText else ForgeMutedSecondary, fontSize = 18.sp)
        Text(item.title.substringBefore(' '), color = if (selected) ForgeTextSecondary else ForgeMutedSecondary, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun ForgeExplorer(state: ForgeUiState, onPickCodeFile: () -> Unit, onPickModelFile: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(ForgeSurface).border(width = 0.5.dp, color = ForgeBorder).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(state.activity.title.uppercase(), color = ForgeMuted, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.weight(1f))
            ForgeTextButton("+", onClick = onPickCodeFile, compact = true)
        }
        Spacer(Modifier.height(12.dp))
        when (state.activity) {
            ForgeActivity.EXPLORER -> {
                ExplorerRow("⌄", "FORGE WORKSPACE", true)
                ExplorerRow("▸", state.fileName, false, selected = true)
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
) {
    Column(modifier.background(ForgeBackground)) {
        Row(
            Modifier.fillMaxWidth().height(42.dp).background(ForgeSurface).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("◦", color = ForgeOrange, fontSize = 18.sp, modifier = Modifier.padding(end = 7.dp))
            Text(state.fileName, color = ForgeTextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            ForgeTextButton("Ask AI", onClick = { if (editorText.isNotBlank()) onSendPrompt(editorText) }, compact = true)
            ForgeTextButton("Open", onClick = onPickCodeFile, compact = true)
            ForgeTextButton("Save", onClick = onSave, compact = true)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).background(ForgeBackground).padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ForgeAction.values().forEach { action ->
                ForgeChip(action.title, selected = action == ForgeAction.RUN) { onRunAction(action) }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
            if (state.loading) {
                CircularProgressIndicator(color = ForgeOrange, modifier = Modifier.size(26.dp).align(Alignment.TopEnd), strokeWidth = 2.dp)
            }
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
        if (state.updateReady && state.updateMessage.isNotBlank()) {
            Text("Update: ${state.updateMessage}", color = ForgeMuted, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
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
            Text("✦", color = ForgeOrange, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
            Text(state.aiSection, color = ForgeText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
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
                    Text(message.role.replaceFirstChar { it.uppercase() }, color = if (message.role.equals("user", true)) ForgeOrange else ForgeMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
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
    Column(Modifier.fillMaxWidth().height(154.dp).background(ForgeSurface).border(width = 0.5.dp, color = ForgeBorder)) {
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
                "Output", "Terminal" -> if (state.consoleLogs.isEmpty()) {
                    Text("Belum ada output", color = ForgeMutedSecondary, fontSize = 10.sp)
                } else state.consoleLogs.takeLast(40).forEach { Text(it, color = ForgeTextSecondary, fontFamily = FontFamily.Monospace, fontSize = 10.sp, lineHeight = 15.sp) }
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
