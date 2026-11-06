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
    minimumWidth: 960
    minimumHeight: 620
    title: "Forge AI Studio — AI Engineering Workbench"
    color: Theme.bg

    QtObject {
        id: Theme
        readonly property color bg: "#08080A"
        readonly property color surface: "#111113"
        readonly property color surface2: "#1A1A1E"
        readonly property color surface3: "#222225"
        readonly property color border: "#1E1E20"
        readonly property color text: "#FAFAFA"
        readonly property color text2: "#E4E4E7"
        readonly property color muted: "#A1A1AA"
        readonly property color muted2: "#71717B"
        readonly property color accent: "#FF6A00"
        readonly property color green: "#22C55E"
        readonly property color red: "#EF4444"
    }

    property string activeView: "code"
    property string currentFile: ""
    property string currentContent: ""
    property bool dirty: false
    property bool sidebarVisible: true
    property var fileItems: []
    property var searchItems: []

    function refreshFiles() { fileItems = forge.listFiles("") }
    function iconFor(item) { return item.type === "folder" ? "▸" : (item.classification?.kind === "code" ? "{}" : "•") }
    function openItem(item) {
        if (item.type === "folder") return
        currentFile = item.path
        currentContent = forge.readFile(item.path)
        dirty = false
    }

    Component.onCompleted: refreshFiles()

    Connections {
        target: forge
        function onFilesChanged() { refreshFiles() }
        function onFileOpened(path, content) { currentFile = path; currentContent = content; dirty = false }
        function onOperationMessage(message, error) { toast.text = message; toast.color = error ? Theme.red : Theme.green; toastTimer.restart() }
    }

    Timer { id: toastTimer; interval: 2800; onTriggered: toast.text = "" }

    header: Rectangle {
        height: 44
        color: Theme.bg
        border.color: Theme.border
        RowLayout {
            anchors.fill: parent
            anchors.leftMargin: 10
            anchors.rightMargin: 10
            spacing: 10
            RowLayout {
                spacing: 8
                Label { text: "●"; color: "#FF5F56"; font.pixelSize: 12 }
                Label { text: "●"; color: "#FFBD2E"; font.pixelSize: 12 }
                Label { text: "●"; color: "#27CA40"; font.pixelSize: 12 }
                Label { text: "forge"; color: Theme.text; font.pixelSize: 13; font.bold: true }
                Label { text: "AI Studio"; color: Theme.muted; font.pixelSize: 11 }
                Label { text: "PRO"; color: Theme.muted; font.pixelSize: 9; padding: 4; background: Rectangle { color: Theme.surface2; radius: 5; border.color: Theme.border } }
            }
            Item { Layout.fillWidth: true }
            Rectangle {
                visible: window.width > 1050
                Layout.preferredWidth: 170; Layout.preferredHeight: 26
                color: "#0F0F10"; border.color: Theme.border; radius: 6
                RowLayout { anchors.fill: parent; anchors.leftMargin: 10; spacing: 6
                    Label { text: "●"; color: Theme.green; font.pixelSize: 9 }
                    Label { text: forge.nativeExecutionEnabled ? "Native engine online" : "Safe mode"; color: Theme.muted2; font.pixelSize: 10 }
                }
            }
            Button {
                text: "▶  Run"
                Layout.preferredHeight: 30
                background: Rectangle { color: Theme.accent; radius: 8 }
                contentItem: Label { text: parent.text; color: "white"; horizontalAlignment: Text.AlignHCenter; verticalAlignment: Text.AlignVCenter; font.pixelSize: 11 }
                onClicked: toast.text = "Run menggunakan adapter C++ native"; onPressed: toastTimer.restart()
            }
            ToolButton { text: "☰"; visible: window.width < 900; onClicked: sidebarVisible = !sidebarVisible }
        }
    }

    RowLayout {
        anchors.fill: parent
        spacing: 0

        Rectangle {
            Layout.preferredWidth: 64; Layout.fillHeight: true
            color: Theme.bg; border.color: Theme.border
            Column {
                anchors.top: parent.top; anchors.topMargin: 8; anchors.horizontalCenter: parent.horizontalCenter; spacing: 4
                Repeater {
                    model: [ ["⌘", "Code", "code"], ["✦", "AI Engine", "engine"], ["◉", "Copilot", "copilot"], ["⌕", "Search", "search"] ]
                    delegate: Rectangle {
                        width: 56; height: 48; radius: 8
                        color: activeView === modelData[2] ? Theme.surface2 : "transparent"
                        Rectangle { visible: activeView === modelData[2]; width: 3; height: 32; radius: 2; color: Theme.accent; anchors.left: parent.left; anchors.verticalCenter: parent.verticalCenter }
                        Column { anchors.centerIn: parent; spacing: 2; Label { text: modelData[0]; color: activeView === modelData[2] ? Theme.text : Theme.muted2; font.pixelSize: 17; horizontalAlignment: Text.AlignHCenter; width: parent.width } Label { text: modelData[1]; color: activeView === modelData[2] ? Theme.text : Theme.muted2; font.pixelSize: 8 } }
                        MouseArea { anchors.fill: parent; onClicked: { activeView = modelData[2]; if (modelData[2] === "code") sidebarVisible = true } }
                    }
                }
                Rectangle { width: 34; height: 1; color: Theme.border; anchors.horizontalCenter: parent.horizontalCenter; anchors.topMargin: 8 }
                Repeater {
                    model: [ ["⑂", "Source", "source"], ["▷", "Debug", "debug"], ["▣", "Archive", "archive"] ]
                    delegate: Rectangle {
                        width: 56; height: 42; radius: 8; color: activeView === modelData[2] ? Theme.surface2 : "transparent"
                        Column { anchors.centerIn: parent; spacing: 2; Label { text: modelData[0]; color: Theme.muted2; font.pixelSize: 15; horizontalAlignment: Text.AlignHCenter; width: parent.width } Label { text: modelData[1]; color: Theme.muted2; font.pixelSize: 8 } }
                        MouseArea { anchors.fill: parent; onClicked: activeView = modelData[2] }
                    }
                }
            }
            Label { anchors.bottom: parent.bottom; anchors.bottomMargin: 16; anchors.horizontalCenter: parent.horizontalCenter; text: "⚙"; color: Theme.muted2; font.pixelSize: 18 }
        }

        Rectangle {
            visible: sidebarVisible && activeView === "code"
            Layout.preferredWidth: 260; Layout.fillHeight: true
            color: Theme.surface; border.color: Theme.border
            ColumnLayout { anchors.fill: parent; spacing: 0
                RowLayout { Layout.fillWidth: true; Layout.preferredHeight: 46; Layout.leftMargin: 16; Layout.rightMargin: 10
                    Label { text: "EXPLORER"; color: Theme.muted; font.pixelSize: 10; font.bold: true; Layout.fillWidth: true }
                    ToolButton { text: "↻"; onClicked: refreshFiles(); contentItem: Label { text: parent.text; color: Theme.muted; font.pixelSize: 16 } }
                    ToolButton { text: "+"; onClicked: forge.createFile("untitled.cpp", "#include <iostream>\n\nint main() {}\n"); contentItem: Label { text: parent.text; color: Theme.muted; font.pixelSize: 18 } }
                }
                Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 1; color: Theme.border }
                RowLayout { Layout.fillWidth: true; Layout.preferredHeight: 34; Layout.leftMargin: 12; spacing: 5
                    Label { text: "⌄"; color: Theme.muted2 }
                    Label { text: "WORKSPACE"; color: Theme.text2; font.pixelSize: 10; font.bold: true }
                    Label { text: "C++ / Qt 6"; color: Theme.muted2; font.pixelSize: 9; Layout.fillWidth: true; horizontalAlignment: Text.AlignRight; Layout.rightMargin: 8 }
                }
                ListView {
                    id: fileView; Layout.fillWidth: true; Layout.fillHeight: true; clip: true; model: fileItems
                    delegate: Item {
                        width: fileView.width; height: 28
                        Row { anchors.fill: parent; anchors.leftMargin: 14 + Math.min(modelData.path.split("/").length - 1, 3) * 12; spacing: 8
                            Label { text: iconFor(modelData); color: modelData.type === "folder" ? Theme.accent : Theme.muted2; font.pixelSize: 12; anchors.verticalCenter: parent.verticalCenter }
                            Label { text: modelData.name; color: currentFile === modelData.path ? Theme.text : Theme.muted; font.pixelSize: 11; elide: Text.ElideRight; width: fileView.width - 55; anchors.verticalCenter: parent.verticalCenter }
                        }
                        MouseArea { anchors.fill: parent; onDoubleClicked: openItem(modelData) }
                        Rectangle { anchors.fill: parent; color: Theme.surface2; opacity: currentFile === modelData.path ? .75 : 0; z: -1 }
                    }
                }
                Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 34; color: Theme.surface2
                    Label { anchors.verticalCenter: parent.verticalCenter; anchors.leftMargin: 14; anchors.left: parent.left; text: "⌄  OUTLINE"; color: Theme.muted; font.pixelSize: 10; font.bold: true }
                }
            }
        }

        Rectangle {
            Layout.fillWidth: true; Layout.fillHeight: true; color: Theme.bg
            ColumnLayout {
                anchors.fill: parent; spacing: 0
                Loader { Layout.fillWidth: true; Layout.fillHeight: true; sourceComponent: activeView === "code" ? codeView : activeView === "engine" ? engineView : activeView === "copilot" ? copilotView : activeView === "search" ? searchView : archiveView }
                Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 24; color: Theme.surface; border.color: Theme.border
                    RowLayout { anchors.fill: parent; anchors.leftMargin: 12; spacing: 16
                        Label { text: "Ln 1, Col 1"; color: Theme.muted2; font.pixelSize: 10 }
                        Label { text: "UTF-8"; color: Theme.muted2; font.pixelSize: 10 }
                        Label { text: "C++"; color: Theme.muted2; font.pixelSize: 10 }
                        Item { Layout.fillWidth: true }
                        Label { text: forge.nativeExecutionEnabled ? "C++ backend" : "C++ backend • safe mode"; color: Theme.green; font.pixelSize: 10; Layout.rightMargin: 12 }
                    }
                }
            }
        }
    }

    Component {
        id: codeView
        ColumnLayout { spacing: 0
            Rectangle { Layout.fillWidth: true; Layout.preferredHeight: 36; color: Theme.surface; border.color: Theme.border
                RowLayout { anchors.fill: parent; spacing: 0
                    Rectangle { Layout.preferredWidth: 190; Layout.fillHeight: true; color: Theme.bg; border.color: Theme.border
                        RowLayout { anchors.fill: parent; anchors.leftMargin: 12; spacing: 8
                            Label { text: "{ }"; color: Theme.accent; font.pixelSize: 12 }
                            Label { text: currentFile === "" ? "Welcome" : currentFile.split("/").pop(); color: Theme.text2; font.pixelSize: 11; Layout.fillWidth: true; elide: Text.ElideRight }
                            Label { visible: dirty; text: "●"; color: Theme.accent; font.pixelSize: 10 }
                            ToolButton { text: "×"; onClicked: { currentFile = ""; currentContent = "" } }
                        }
                    }
                    Item { Layout.fillWidth: true }
                    Button { text: "Save"; visible: currentFile !== ""; flat: true; onClicked: { if (forge.writeFile(currentFile, currentContent)) dirty = false } }
                }
            }
            TextArea {
                id: editor; Layout.fillWidth: true; Layout.fillHeight: true; Layout.margins: 18
                text: currentContent; onTextChanged: { if (focus) { currentContent = text; dirty = true } }
                color: Theme.text2; selectionColor: Theme.accent; selectedTextColor: "white"; wrapMode: TextEdit.NoWrap
                font.family: "monospace"; font.pixelSize: 13; background: null; placeholderText: "Buka file dari Explorer untuk mulai mengedit..."; placeholderTextColor: Theme.muted2
            }
        }
    }

    Component {
        id: engineView
        Flickable { contentWidth: width; contentHeight: engineColumn.height; clip: true
            ColumnLayout { id: engineColumn; width: parent.width; spacing: 16; anchors.margins: 26
                Label { text: "AI Engine"; color: Theme.text; font.pixelSize: 24; font.bold: true }
                Label { text: "Native C++ runtime and model engineering"; color: Theme.muted; font.pixelSize: 12 }
                Flow { Layout.fillWidth: true; spacing: 12
                    Repeater { model: [ ["Models", "GGUF · Safetensors", "♢"], ["Runtime", "Inference engine", "▶"], ["Tokenizer", "Encode · Decode", "⌘"], ["Compiler", "Build · Convert", "⚙"], ["Hardware", "CPU · GPU", "▣"], ["Training", "LoRA · Fine-tune", "◇"] ]
                        delegate: Rectangle { width: 210; height: 112; color: Theme.surface; border.color: Theme.border; radius: 10
                            Column { anchors.fill: parent; anchors.margins: 16; spacing: 8; Label { text: modelData[2]; color: Theme.accent; font.pixelSize: 20 }; Label { text: modelData[0]; color: Theme.text; font.pixelSize: 13; font.bold: true }; Label { text: modelData[1]; color: Theme.muted; font.pixelSize: 10 } }
                        }
                    }
                }
                Rectangle { Layout.fillWidth: true; height: 1; color: Theme.border }
                Label { text: "Engine status"; color: Theme.text; font.pixelSize: 14; font.bold: true }
                Repeater { model: forge.engines(); delegate: RowLayout { Layout.fillWidth: true; spacing: 10; Label { text: modelData.available ? "●" : "○"; color: modelData.available ? Theme.green : Theme.muted2 }; Label { text: modelData.id; color: Theme.text2; font.pixelSize: 11; Layout.fillWidth: true }; Label { text: modelData.available ? "available" : "not configured"; color: Theme.muted2; font.pixelSize: 10 } } }
            }
        }
    }

    Component { id: copilotView; ColumnLayout { anchors.margins: 28; spacing: 16; Label { text: "Copilot"; color: Theme.text; font.pixelSize: 24; font.bold: true }; Label { text: "Native C++ assistant workspace"; color: Theme.muted; font.pixelSize: 12 }; Item { Layout.fillHeight: true }; Label { text: "Ask Forge Copilot anything about your project."; color: Theme.muted; Layout.alignment: Qt.AlignHCenter }; RowLayout { Layout.fillWidth: true; TextField { Layout.fillWidth: true; placeholderText: "Message Copilot..."; color: Theme.text; background: Rectangle { color: Theme.surface; border.color: Theme.border; radius: 8 } }; Button { text: "Send"; background: Rectangle { color: Theme.accent; radius: 8 } } } } }

    Component { id: searchView; ColumnLayout { anchors.margins: 28; spacing: 14; Label { text: "Search"; color: Theme.text; font.pixelSize: 24; font.bold: true }; RowLayout { Layout.fillWidth: true; TextField { id: query; Layout.fillWidth: true; placeholderText: "Search in workspace..."; onAccepted: searchItems = forge.search(text, false, false); color: Theme.text; background: Rectangle { color: Theme.surface; border.color: Theme.border; radius: 8 } }; Button { text: "⌕"; onClicked: searchItems = forge.search(query.text, false, false) } }; ListView { Layout.fillWidth: true; Layout.fillHeight: true; model: searchItems; delegate: RowLayout { width: parent.width; height: 38; Label { text: modelData.path + ":" + modelData.line; color: Theme.accent; font.pixelSize: 11; Layout.preferredWidth: 220 }; Label { text: modelData.text; color: Theme.muted; font.pixelSize: 11; elide: Text.ElideRight } } } } }

    Component { id: archiveView; ColumnLayout { anchors.margins: 28; spacing: 12; Label { text: "Archive"; color: Theme.text; font.pixelSize: 24; font.bold: true }; Label { text: "ZIP workspace editor"; color: Theme.muted; font.pixelSize: 12 }; Rectangle { Layout.fillWidth: true; Layout.fillHeight: true; color: Theme.surface; border.color: Theme.border; radius: 10; Label { anchors.centerIn: parent; text: "Buka file .zip dari Explorer untuk meninjau isinya"; color: Theme.muted2; font.pixelSize: 12 } } } }

    Label { id: toast; anchors.bottom: parent.bottom; anchors.bottomMargin: 38; anchors.horizontalCenter: parent.horizontalCenter; color: Theme.green; font.pixelSize: 11; z: 100 }
}
