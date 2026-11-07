import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import QtQuick.Window
import Forge.Workbench

ApplicationWindow {
    id: window
    visible: true
    width: 1440
    height: 900
    minimumWidth: 1050
    minimumHeight: 650
    title: "Forge AI Studio — AI Engineering Workbench"
    color: theme.bg

    QtObject {
        id: theme
        readonly property color bg: "#08080A"
        readonly property color surface: "#111113"
        readonly property color surface2: "#1A1A1E"
        readonly property color surface3: "#222225"
        readonly property color border: "#1E1E20"
        readonly property color borderHover: "#2A2A2E"
        readonly property color text: "#FAFAFA"
        readonly property color text2: "#E4E4E7"
        readonly property color muted: "#A1A1AA"
        readonly property color muted2: "#71717B"
        readonly property color accent: "#FF6A00"
        readonly property color accentPink: "#FF3D9A"
        readonly property color green: "#22C55E"
        readonly property color yellow: "#FACC15"
        readonly property color red: "#EF4444"
    }

    property string activeView: "code"
    property string currentFile: ""
    property string currentContent: ""
    property bool dirty: false
    property bool sidebarVisible: true
    property bool aiVisible: true
    property bool bottomVisible: true
    property string bottomTab: "Terminal"
    property string editorLanguage: "C++"
    property int editorCursorPosition: 0
    property string toastText: ""
    property color toastColor: theme.green
    property var fileItems: []
    property var searchItems: []
    property var openTabs: []
    property string terminalText: "forge@workspace:~$ Native Forge Workbench ready\nforge@workspace:~$ "

    function refreshFiles() { fileItems = forge.listFiles("") }
    function notify(message, error) { toastText = message; toastColor = error ? theme.red : theme.green; toastTimer.restart() }
    function unsupported(label) { notify(label + " belum tersedia di adapter Qt native; tombol tetap dipertahankan.", true) }
    function openItem(item) {
        if (item.type === "folder") return
        currentFile = item.path
        currentContent = forge.readFile(item.path)
        dirty = false
        if (openTabs.indexOf(item.path) < 0) openTabs = openTabs.concat([item.path])
    }
    function saveCurrent() {
        if (currentFile === "") { notify("Tidak ada file aktif", true); return }
        if (forge.writeFile(currentFile, currentContent)) { dirty = false; notify("Disimpan: " + currentFile, false) }
    }
    function runCurrent() {
        terminalText += "\n$ run " + (currentFile === "" ? "workspace" : currentFile) + "\n[Forge] Adapter C++ native menerima perintah.\n"
        notify("Run dikirim ke adapter native C++", false)
    }
    function closeTab(path) {
        var next = openTabs.filter(function (p) { return p !== path })
        openTabs = next
        if (currentFile === path) { currentFile = next.length ? next[next.length - 1] : ""; currentContent = currentFile ? forge.readFile(currentFile) : ""; dirty = false }
    }

    Component.onCompleted: refreshFiles()
    Timer { id: toastTimer; interval: 3000; onTriggered: toastText = "" }
    Connections {
        target: forge
        function onFilesChanged() { refreshFiles() }
        function onFileOpened(path, content) { currentFile = path; currentContent = content; dirty = false; if (openTabs.indexOf(path) < 0) openTabs = openTabs.concat([path]) }
        function onOperationMessage(message, error) { notify(message, error) }
    }

    component ForgeButton: Button {
        property bool primary: false
        implicitHeight: 28
        leftPadding: 10; rightPadding: 10
        font.pixelSize: 11
        contentItem: Label { text: parent.text; color: parent.primary ? "white" : theme.text2; font.pixelSize: 11; horizontalAlignment: Text.AlignHCenter; verticalAlignment: Text.AlignVCenter; elide: Text.ElideRight }
        background: Rectangle { color: parent.primary ? theme.accent : theme.surface2; border.color: parent.primary ? theme.accent : theme.border; radius: 6 }
    }
    component IconButton: ToolButton {
        implicitWidth: 28; implicitHeight: 28
        contentItem: Label { text: parent.text; color: parent.hovered ? theme.text : theme.muted; font.pixelSize: 14; horizontalAlignment: Text.AlignHCenter; verticalAlignment: Text.AlignVCenter }
        background: Rectangle { color: parent.hovered ? theme.surface2 : "transparent"; radius: 6 }
    }
    component SectionLabel: Label { color: theme.muted2; font.pixelSize: 10; font.bold: true; font.letterSpacing: 1.0 }

    header: Rectangle {
        height: 44; color: theme.bg; border.color: theme.border
        RowLayout { anchors.fill: parent; anchors.leftMargin: 10; anchors.rightMargin: 10; spacing: 10
            RowLayout { spacing: 8
                Label { text: "●"; color: "#FF5F56"; font.pixelSize: 12 }
                Label { text: "●"; color: "#FFBD2E"; font.pixelSize: 12 }
                Label { text: "●"; color: "#27CA40"; font.pixelSize: 12 }
                Rectangle { width: 22; height: 22; radius: 6; color: theme.accent; Label { anchors.centerIn: parent; text: "F"; color: "white"; font.bold: true; font.pixelSize: 13 } }
                Label { text: "forge"; color: theme.text; font.pixelSize: 13; font.bold: true }
                Label { text: "AI Studio"; color: theme.muted; font.pixelSize: 11 }
                Label { text: "PRO"; color: theme.muted; font.pixelSize: 9; padding: 4; background: Rectangle { color: theme.surface2; radius: 5; border.color: theme.border } }
            }
            RowLayout { spacing: 4
                ForgeButton { text: "File"; onClicked: fileMenu.open() }
                ForgeButton { text: "Explorer"; onClicked: sidebarVisible = !sidebarVisible }
            }
            Item { Layout.fillWidth: true }
            Rectangle { visible: window.width > 1150; Layout.preferredWidth: 180; Layout.preferredHeight: 26; color: "#0F0F10"; border.color: theme.border; radius: 6
                RowLayout { anchors.fill: parent; anchors.leftMargin: 10; spacing: 6; Label { text: "●"; color: theme.green; font.pixelSize: 9 } Label { text: forge.nativeExecutionEnabled ? "Native engine online" : "Safe mode"; color: theme.muted2; font.pixelSize: 10 } }
            }
            ForgeButton { text: "▶  Run"; primary: true; onClicked: runCurrent() }
            ForgeButton { text: forge.updateAvailable ? "Update " + forge.latestVersion : "Check update"; onClicked: forge.updateAvailable ? forge.downloadAndInstallUpdate() : forge.checkForUpdates(true) }
            IconButton { text: "☰"; visible: window.width < 1100; onClicked: sidebarVisible = !sidebarVisible }
        }
    }

    RowLayout { anchors.fill: parent; spacing: 0
        Rectangle { Layout.preferredWidth: 64; Layout.fillHeight: true; color: theme.bg; border.color: theme.border
            Column { anchors.top: parent.top; anchors.topMargin: 8; anchors.horizontalCenter: parent.horizontalCenter; spacing: 4
                Repeater { model: [["⌘","Editor Kode","code"],["✦","AI Engine","engine"],["◉","Copilot","copilot"],["⌕","Search","search"],["⑂","Source Control","git"],["▷","Run & Debug","run"]]
                    delegate: Rectangle { width: 56; height: 46; radius: 8; color: activeView === modelData[2] ? theme.surface2 : "transparent"
                        Rectangle { visible: activeView === modelData[2]; width: 3; height: 30; radius: 2; color: theme.accent; anchors.left: parent.left; anchors.verticalCenter: parent.verticalCenter }
                        Column { anchors.centerIn: parent; spacing: 2; Label { text: modelData[0]; color: activeView === modelData[2] ? theme.text : theme.muted2; font.pixelSize: 16; horizontalAlignment: Text.AlignHCenter; width: parent.width } Label { text: modelData[1]; color: activeView === modelData[2] ? theme.text : theme.muted2; font.pixelSize: 8 } }
                        MouseArea { anchors.fill: parent; onClicked: { activeView = modelData[2]; if (modelData[2] === "code") sidebarVisible = true } }
                    }
                }
            }
            Column { anchors.bottom: parent.bottom; anchors.bottomMargin: 12; anchors.horizontalCenter: parent.horizontalCenter; spacing: 8
                IconButton { text: "⌘"; onClicked: commandPalette.open() }
                IconButton { text: "☼"; onClicked: notify("theme: dark Forge", false) }
                Rectangle { width: 28; height: 28; radius: 14; color: theme.accentPink; Label { anchors.centerIn: parent; text: "F"; color: "white"; font.bold: true; font.pixelSize: 12 } }
            }
        }

        Rectangle { visible: sidebarVisible && activeView === "code"; Layout.preferredWidth: 280; Layout.fillHeight: true; color: theme.surface; border.color: theme.border
            ColumnLayout { anchors.fill: parent; spacing: 0
                RowLayout { Layout.fillWidth: true; Layout.preferredHeight: 38; Layout.leftMargin: 10; Layout.rightMargin: 8; Label { text: "EXPLORER"; color: theme.muted; font.pixelSize: 11; font.bold: true; Layout.fillWidth: true } IconButton { text: "↻"; onClicked: refreshFiles() } IconButton { text: "+"; onClicked: forge.createFile("untitled.cpp", "#include <iostream>\n\nint main() {}\n") } IconButton { text: "×"; onClicked: sidebarVisible = false } }
                Rectangle { Layout.fillWidth: true; height: 1; color: theme.border }
                RowLayout { Layout.fillWidth: true; Layout.preferredHeight: 35; Layout.leftMargin: 10; Label { text: "⌄  WORKSPACE"; color: theme.text2; font.pixelSize: 10; font.bold: true; Layout.fillWidth: true } Label { text: "C++ / Qt 6"; color: theme.muted2; font.pixelSize: 9; Layout.rightMargin: 9 } }
                ListView { id: fileView; Layout.fillWidth: true; Layout.fillHeight: true; clip: true; model: fileItems
                    delegate: Item { width: fileView.width; height: 26
                        Rectangle { anchors.fill: parent; anchors.leftMargin: 6; anchors.rightMargin: 6; radius: 5; color: currentFile === modelData.path ? theme.surface3 : "transparent" }
                        Row { anchors.fill: parent; anchors.leftMargin: 12 + Math.min(modelData.path.split("/").length - 1, 3) * 13; spacing: 7; Label { text: modelData.type === "folder" ? "▸" : (modelData.classification && modelData.classification.kind === "code" ? "{}" : "•"); color: modelData.type === "folder" ? theme.accent : theme.muted2; font.pixelSize: 11 } Label { text: modelData.name; color: currentFile === modelData.path ? theme.text : theme.muted; font.pixelSize: 11; elide: Text.ElideRight; width: fileView.width - 65 } }
                        MouseArea { anchors.fill: parent; onDoubleClicked: openItem(modelData) }
                    }
                }
                Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 34; color: theme.surface2; Label { anchors.centerIn: parent; text: "⌄  OUTLINE"; color: theme.muted; font.pixelSize: 10; font.bold: true } }
                Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 155; color: theme.surface
                    ColumnLayout { anchors.fill: parent; anchors.margins: 10; spacing: 5; SectionLabel { text: "TOOLKIT" } Repeater { model: [["＋","New File"],["□","New Folder"],["↥","Open File"],["⌂","Open Folder"],["⇧","Import ZIP"],["{}","Import Code"],["✓","Save"],["⇩","Export ZIP"]]; delegate: ForgeButton { text: modelData[0] + "  " + modelData[1]; Layout.fillWidth: true; onClicked: { if (modelData[1] === "New File") forge.createFile("untitled.cpp", ""); else if (modelData[1] === "New Folder") forge.createFolder("new-folder"); else if (modelData[1] === "Save") saveCurrent(); else unsupported(modelData[1]) } } } }
                }
            }
        }

        Rectangle { Layout.fillWidth: true; Layout.fillHeight: true; color: theme.bg
            ColumnLayout { anchors.fill: parent; spacing: 0
                Loader { Layout.fillWidth: true; Layout.fillHeight: true; sourceComponent: activeView === "code" ? codeView : activeView === "engine" ? engineView : activeView === "copilot" ? copilotView : activeView === "search" ? searchView : activeView === "git" ? gitView : activeView === "run" ? runView : archiveView }
                Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 24; color: theme.surface; border.color: theme.border; RowLayout { anchors.fill: parent; anchors.leftMargin: 12; Label { text: currentFile === "" ? "Ln 1, Col 1" : "Ln " + (editorCursorPosition + 1) + ", Col 1"; color: theme.muted2; font.pixelSize: 10 } Label { text: "UTF-8"; color: theme.muted2; font.pixelSize: 10 } Label { text: editorLanguage; color: theme.muted2; font.pixelSize: 10 } Item { Layout.fillWidth: true } Label { text: forge.nativeExecutionEnabled ? "● C++ backend" : "● C++ backend • safe mode"; color: forge.nativeExecutionEnabled ? theme.green : theme.yellow; font.pixelSize: 10; Layout.rightMargin: 12 } } }
            }
        }

        Rectangle { visible: aiVisible && window.width > 1120; Layout.preferredWidth: 320; Layout.fillHeight: true; color: theme.surface; border.color: theme.border
            ColumnLayout { anchors.fill: parent; spacing: 0
                RowLayout { Layout.fillWidth: true; Layout.preferredHeight: 44; Layout.leftMargin: 12; Layout.rightMargin: 8; Label { text: "✦  COPILOT"; color: theme.text2; font.pixelSize: 11; font.bold: true; Layout.fillWidth: true } IconButton { text: "×"; onClicked: aiVisible = false } }
                Rectangle { Layout.fillWidth: true; height: 1; color: theme.border }
                ComboBox { Layout.fillWidth: true; Layout.margins: 12; model: ["Forge Local / Safe","Forge Compiler","External adapter (disabled)"]; currentIndex: 0; implicitHeight: 34 }
                Rectangle { Layout.fillWidth: true; Layout.margins: 12; height: 92; color: "#0F0F10"; border.color: theme.border; radius: 10; Column { anchors.fill: parent; anchors.margins: 12; spacing: 6; Row { spacing: 8; Rectangle { width: 30; height: 30; radius: 7; color: theme.accent; Label { anchors.centerIn: parent; text: "✦"; color: "white" } } Column { Label { text: "Forge Assistant"; color: theme.text; font.pixelSize: 12; font.bold: true } Label { text: "Native workspace copilot"; color: theme.muted2; font.pixelSize: 10 } } } Rectangle { width: parent.width; height: 4; radius: 2; color: theme.surface3; Rectangle { width: parent.width * .72; height: parent.height; radius: 2; color: theme.accent } } Label { text: forge.nativeExecutionEnabled ? "Native execution enabled" : "Safe mode · no model configured"; color: theme.muted2; font.pixelSize: 9 } } }
                Flickable { Layout.fillWidth: true; Layout.fillHeight: true; contentHeight: chatColumn.height; clip: true; Column { id: chatColumn; width: parent.width; spacing: 12; padding: 12; Row { spacing: 8; Rectangle { width: 28; height: 24; radius: 7; color: theme.accent; Label { anchors.centerIn: parent; text: "✦"; color: "white" } } Rectangle { width: Math.min(220, parent.parent.width - 58); height: 55; radius: 9; color: theme.surface2; border.color: theme.border; Label { anchors.fill: parent; anchors.margins: 9; text: "Ask Forge Copilot anything about your project."; wrapMode: Text.WordWrap; color: theme.text2; font.pixelSize: 11 } } } } }
                ColumnLayout { Layout.fillWidth: true; Layout.margins: 12; spacing: 7; RowLayout { Layout.fillWidth: true; Repeater { model: ["Explain", "Fix", "Review", "Tests"]; delegate: ForgeButton { text: modelData; Layout.fillWidth: true; onClicked: notify(modelData + " queued for native Copilot", false) } } } TextArea { Layout.fillWidth: true; Layout.preferredHeight: 58; placeholderText: "Message Copilot..."; color: theme.text; placeholderTextColor: theme.muted2; wrapMode: TextEdit.Wrap; background: Rectangle { color: "#0F0F10"; border.color: theme.border; radius: 8 } } RowLayout { Layout.fillWidth: true; Label { text: "Native adapter only"; color: theme.muted2; font.pixelSize: 9; Layout.fillWidth: true } ForgeButton { text: "Send  ↑"; primary: true; onClicked: notify("Pesan Copilot disimpan sebagai status UI; adapter chat belum tersedia.", false) } } }
            }
        }
    }

    Component {
        id: codeView
        ColumnLayout { spacing: 0
            Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 38; color: theme.surface; border.color: theme.border
                RowLayout { anchors.fill: parent; spacing: 0
                    RowLayout { Layout.fillWidth: true; Layout.fillHeight: true; spacing: 0
                        Repeater { model: openTabs.length ? openTabs : ["Welcome"] ; delegate: Rectangle { Layout.preferredWidth: 150; Layout.fillHeight: true; color: modelData === currentFile ? theme.bg : theme.surface; border.color: theme.border; RowLayout { anchors.fill: parent; anchors.leftMargin: 10; Label { text: "{}"; color: theme.accent; font.pixelSize: 11 } Label { text: modelData === "" ? "Welcome" : modelData.split("/").pop(); color: theme.text2; font.pixelSize: 11; elide: Text.ElideRight; Layout.fillWidth: true } Label { visible: modelData === currentFile && dirty; text: "●"; color: theme.accent; font.pixelSize: 9 } IconButton { text: "×"; onClicked: closeTab(modelData) } } } }
                    }
                    Repeater { model: [["Save","save"],["⇩","download"],["✦","format"],["▶","run"],["⌕","inspect"]]; delegate: IconButton { text: modelData[0]; ToolTip.visible: hovered; ToolTip.text: modelData[1]; onClicked: { if (modelData[1] === "save") saveCurrent(); else if (modelData[1] === "run") runCurrent(); else unsupported(modelData[1]) } } }
                    ComboBox { Layout.preferredWidth: 80; model: ["C++","Python","JSON","QML","Markdown"]; currentIndex: 0; onActivated: editorLanguage = currentText; implicitHeight: 27 }
                    ForgeButton { text: "Syntax"; onClicked: unsupported("Syntax") }
                    ForgeButton { text: "Plugins"; onClicked: unsupported("Plugins") }
                }
            }
            TextArea { id: editor; Layout.fillWidth: true; Layout.fillHeight: true; Layout.margins: 18; text: currentContent; onTextChanged: { if (focus) { currentContent = text; dirty = true } }
            onCursorPositionChanged: window.editorCursorPosition = cursorPosition; color: theme.text2; selectionColor: theme.accent; selectedTextColor: "white"; wrapMode: TextEdit.NoWrap; font.family: "monospace"; font.pixelSize: 13; background: null; placeholderText: currentFile === "" ? "Buka file dari Explorer untuk mulai mengedit..." : ""; placeholderTextColor: theme.muted2 }
            Rectangle { visible: bottomVisible; Layout.fillWidth: true; Layout.preferredHeight: 210; color: theme.surface; border.color: theme.border
                ColumnLayout { anchors.fill: parent; spacing: 0; RowLayout { Layout.fillWidth: true; Layout.preferredHeight: 36; Layout.leftMargin: 8; spacing: 4; Repeater { model: ["Terminal","Output","Problems","Debug Console"]; delegate: ForgeButton { text: modelData; onClicked: { bottomTab = modelData; bottomVisible = true } } } Item { Layout.fillWidth: true } IconButton { text: "⌄"; onClicked: bottomVisible = false } } Rectangle { Layout.fillWidth: true; height: 1; color: theme.border } ScrollView { Layout.fillWidth: true; Layout.fillHeight: true; TextArea { readOnly: true; text: bottomTab === "Terminal" ? terminalText : "[Forge] " + bottomTab + " panel aktif. Belum ada event native."; color: theme.muted; font.family: "monospace"; font.pixelSize: 11; background: null; wrapMode: TextEdit.Wrap } } }
            }
        }
    }

    Component { id: engineView; ScrollView { clip: true; ColumnLayout { width: parent.width - 48; x: 24; y: 24; spacing: 16; Label { text: "AI Engine"; color: theme.text; font.pixelSize: 24; font.bold: true } Label { text: "Native C++ runtime and model engineering"; color: theme.muted; font.pixelSize: 12 } Flow { Layout.fillWidth: true; spacing: 10; Repeater { model: [["Models","GGUF · Safetensors","◇"],["Runtime","Inference engine","▶"],["Tokenizer","Encode · Decode","⌘"],["Compiler","Build · Convert","⚙"],["Hardware","CPU · GPU","▣"],["Training","LoRA · Fine-tune","◇"]]; delegate: Rectangle { width: 190; height: 104; color: theme.surface; border.color: theme.border; radius: 10; Column { anchors.fill: parent; anchors.margins: 14; spacing: 7; Label { text: modelData[2]; color: theme.accent; font.pixelSize: 19 } Label { text: modelData[0]; color: theme.text; font.pixelSize: 13; font.bold: true } Label { text: modelData[1]; color: theme.muted; font.pixelSize: 10 } } } } } Rectangle { Layout.fillWidth: true; height: 1; color: theme.border } RowLayout { Layout.fillWidth: true; Label { text: "ENGINE STATUS"; color: theme.text; font.pixelSize: 13; font.bold: true; Layout.fillWidth: true } ForgeButton { text: "Refresh"; onClicked: notify("Engine status diperbarui", false) } } Repeater { model: forge.engines(); delegate: RowLayout { Layout.fillWidth: true; Label { text: modelData.available ? "●" : "○"; color: modelData.available ? theme.green : theme.muted2 } Label { text: modelData.id; color: theme.text2; font.pixelSize: 11; Layout.fillWidth: true } Label { text: modelData.available ? "available" : "not configured"; color: theme.muted2; font.pixelSize: 10 } } } } } }
    Component { id: copilotView; ColumnLayout { anchors.margins: 28; spacing: 16; Label { text: "Copilot"; color: theme.text; font.pixelSize: 24; font.bold: true } Label { text: "Native C++ assistant workspace"; color: theme.muted; font.pixelSize: 12 } Flow { Layout.fillWidth: true; spacing: 10; Repeater { model: [["Chat","Ask about code"],["Review","Inspect changes"],["Generate","Create implementation"],["Debug","Find root cause"]]; delegate: Rectangle { width: 210; height: 82; color: theme.surface; border.color: theme.border; radius: 9; Column { anchors.fill: parent; anchors.margins: 12; spacing: 7; Label { text: "✦  " + modelData[0]; color: theme.text; font.bold: true; font.pixelSize: 12 } Label { text: modelData[1]; color: theme.muted; font.pixelSize: 10 } } } } } Item { Layout.fillHeight: true } Label { text: "Open the Copilot panel on the right to continue."; color: theme.muted; Layout.alignment: Qt.AlignHCenter } } }
    Component { id: searchView; ColumnLayout { anchors.margins: 28; spacing: 14; Label { text: "Search workspace"; color: theme.text; font.pixelSize: 24; font.bold: true } RowLayout { Layout.fillWidth: true; TextField { id: query; Layout.fillWidth: true; placeholderText: "Search in workspace..."; color: theme.text; onAccepted: searchItems = forge.search(text, false, false); background: Rectangle { color: theme.surface; border.color: theme.border; radius: 8 } } ForgeButton { text: "⌕ Search"; primary: true; onClicked: searchItems = forge.search(query.text, false, false) } } ListView { Layout.fillWidth: true; Layout.fillHeight: true; model: searchItems; delegate: RowLayout { width: parent.width; height: 36; Label { text: modelData.path + ":" + modelData.line; color: theme.accent; font.pixelSize: 11; Layout.preferredWidth: 230 } Label { text: modelData.text; color: theme.muted; font.pixelSize: 11; elide: Text.ElideRight } } } } }
    Component { id: gitView; ColumnLayout { anchors.margins: 28; spacing: 14; Label { text: "Git"; color: theme.text; font.pixelSize: 24; font.bold: true } Label { text: "Source control actions remain visible while native Git adapter is pending."; color: theme.muted; font.pixelSize: 12 } Repeater { model: ["Changes","Commit","Branches","History","Sync"]; delegate: ForgeButton { text: modelData; Layout.fillWidth: true; onClicked: unsupported("Git " + modelData) } } } }
    Component { id: runView; ColumnLayout { anchors.margins: 28; spacing: 14; Label { text: "Run & Debug"; color: theme.text; font.pixelSize: 24; font.bold: true } Label { text: forge.nativeExecutionEnabled ? "Native execution enabled" : "Safe mode — enable adapter to execute"; color: theme.muted; font.pixelSize: 12 } RowLayout { Layout.fillWidth: true; ForgeButton { text: "▶ Run workspace"; primary: true; onClicked: runCurrent() } ForgeButton { text: "Inspect model"; onClicked: { var id = forge.startJob("inspect-model", {file: currentFile}); notify(id === "" ? "Job gagal dibuat" : "Inspect job dibuat: " + id, id === "") } } ForgeButton { text: "Tokenize"; onClicked: { forge.tokenize(currentContent); notify("Tokenize selesai", false) } } } TextArea { Layout.fillWidth: true; Layout.fillHeight: true; readOnly: true; text: terminalText; color: theme.muted; font.family: "monospace"; background: Rectangle { color: theme.surface; border.color: theme.border; radius: 8 } } } }
    Component { id: archiveView; ColumnLayout { anchors.margins: 28; spacing: 14; Label { text: "Archive"; color: theme.text; font.pixelSize: 24; font.bold: true } Label { text: "Workspace snapshots and ZIP export"; color: theme.muted; font.pixelSize: 12 } Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 100; color: theme.surface; border.color: theme.border; radius: 10; Column { anchors.centerIn: parent; spacing: 8; Label { text: "No archives yet"; color: theme.text2; font.pixelSize: 13; horizontalAlignment: Text.AlignHCenter; width: 220 } Label { text: "Export ZIP keeps its original UI entry point."; color: theme.muted2; font.pixelSize: 10 } } } RowLayout { Layout.fillWidth: true; ForgeButton { text: "Create snapshot"; onClicked: unsupported("Create snapshot") } ForgeButton { text: "Export ZIP"; primary: true; onClicked: unsupported("Export ZIP") } } } }

    Popup { id: fileMenu; x: 95; y: 44; width: 190; padding: 8; background: Rectangle { color: theme.surface2; border.color: theme.borderHover; radius: 8 } Column { width: parent.width; Repeater { model: ["New File","New Folder","Open File","Open Folder","Import ZIP","Import Code","Save","Export ZIP"]; delegate: ForgeButton { text: modelData; width: parent.width; onClicked: { fileMenu.close(); if (modelData === "Save") saveCurrent(); else if (modelData === "New File") forge.createFile("untitled.cpp", ""); else if (modelData === "New Folder") forge.createFolder("new-folder"); else unsupported(modelData) } } } } }
    Dialog { id: commandPalette; title: "Forge Command Palette"; modal: true; standardButtons: Dialog.Cancel; width: 420; contentItem: ColumnLayout { spacing: 10; Label { text: "Run an action"; color: theme.muted } Repeater { model: ["Save current file","Format document","Run workspace","Inspect model","Toggle Copilot"]; delegate: ForgeButton { text: modelData; Layout.fillWidth: true; onClicked: { commandPalette.close(); if (modelData === "Save current file") saveCurrent(); else if (modelData === "Run workspace") runCurrent(); else if (modelData === "Toggle Copilot") aiVisible = !aiVisible; else unsupported(modelData) } } } } }
    Dialog { id: archiveDialog; title: "Archive"; modal: true; standardButtons: Dialog.Close; contentItem: Label { text: "Archive actions are preserved in the Archive view."; color: theme.text2; padding: 18 } }
    Rectangle { visible: toastText !== ""; anchors.right: parent.right; anchors.bottom: parent.bottom; anchors.rightMargin: 18; anchors.bottomMargin: 18; width: Math.min(420, parent.width - 36); height: 42; radius: 8; color: theme.surface2; border.color: toastColor; z: 100; Label { anchors.fill: parent; anchors.margins: 12; text: toastText; color: theme.text2; font.pixelSize: 11; verticalAlignment: Text.AlignVCenter; elide: Text.ElideRight } }
    Rectangle { visible: forge.updateStatus !== "Belum diperiksa"; anchors.left: parent.left; anchors.bottom: parent.bottom; anchors.leftMargin: 78; anchors.bottomMargin: 10; width: Math.min(420, parent.width - 100); height: 28; radius: 6; color: theme.surface2; border.color: forge.updateAvailable ? theme.accent : theme.border; z: 90; RowLayout { anchors.fill: parent; anchors.leftMargin: 10; anchors.rightMargin: 6; Label { text: forge.updateStatus; color: forge.updateAvailable ? theme.accent : theme.muted; font.pixelSize: 10; Layout.fillWidth: true; elide: Text.ElideRight } ProgressBar { visible: forge.updateProgress > 0 && forge.updateProgress < 100; value: forge.updateProgress / 100; Layout.preferredWidth: 80 } } }
}
