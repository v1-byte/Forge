#include "ForgeBackend.h"

#include <QDir>
#include <QDirIterator>
#include <QFile>
#include <QFileInfo>
#include <QJsonArray>
#include <QJsonDocument>
#include <QRegularExpression>
#include <QStandardPaths>
#include <QUuid>
#include <QDateTime>
#include <QSysInfo>
#include <QThread>
#include <QCryptographicHash>

namespace {
const QStringList codeExtensions = {"c", "cc", "cpp", "h", "hpp", "py", "js", "ts", "tsx", "jsx", "java", "kt", "rs", "go", "swift", "sh", "json", "yaml", "yml", "toml", "xml", "cmake", "md"};
const QStringList modelExtensions = {"gguf", "safetensors", "bin", "pt", "pth", "ckpt", "onnx"};
const QStringList ignoredDirectories = {".git", "node_modules", "__pycache__", ".venv", "build"};

QString isoNow() { return QDateTime::currentDateTimeUtc().toString(Qt::ISODate); }
QString keyFor(const QString &type) { return QString("FORGE_%1_CMD").arg(type.toUpper().replace('-', '_')); }
}

ForgeBackend::ForgeBackend(QObject *parent) : QObject(parent) {
    const QString configured = qEnvironmentVariable("FORGE_WORKSPACE_ROOT");
    m_workspaceRoot = configured.isEmpty()
        ? QDir(QStandardPaths::writableLocation(QStandardPaths::DocumentsLocation)).filePath("ForgeWorkspace")
        : QDir::cleanPath(configured);
    QDir().mkpath(m_workspaceRoot);
    loadJobs();
}

QString ForgeBackend::workspaceRoot() const { return m_workspaceRoot; }

void ForgeBackend::setWorkspaceRoot(const QString &root) {
    const QString cleaned = QDir::cleanPath(root.trimmed());
    if (cleaned.isEmpty() || cleaned == m_workspaceRoot) return;
    if (!QDir().mkpath(cleaned)) {
        emit operationMessage(tr("Tidak dapat membuka workspace"), true);
        return;
    }
    m_workspaceRoot = cleaned;
    emit workspaceRootChanged();
    emit filesChanged();
}

QString ForgeBackend::activeFile() const { return m_activeFile; }
bool ForgeBackend::nativeExecutionEnabled() const { return m_nativeExecutionEnabled; }

QString ForgeBackend::safePath(const QString &relativePath) const {
    const QString base = QDir(m_workspaceRoot).absolutePath();
    const QString requested = QDir(base).absoluteFilePath(relativePath);
    const QString clean = QDir::cleanPath(requested);
    if (clean == base || clean.startsWith(base + QDir::separator())) return clean;
    return {};
}

QVariantMap ForgeBackend::classify(const QString &path) const {
    const QString lower = path.toLower();
    const QString suffix = QFileInfo(lower).suffix();
    const bool model = modelExtensions.contains(suffix);
    const bool token = lower.contains("token") || lower.contains("vocab") || lower.contains("sentencepiece");
    const bool system = {"json", "yaml", "yml", "toml", "xml"}.contains(suffix) || lower.contains("config") || lower.contains("manifest");
    QVariantMap result;
    result["kind"] = token ? "token" : model ? "binary" : codeExtensions.contains(suffix) ? "code" : system ? "config" : "file";
    result["group"] = model || token ? "machine" : system ? "system" : "file";
    return result;
}

void ForgeBackend::walk(const QString &absolute, const QString &relative, QVariantList &out) const {
    QDir dir(absolute);
    const QFileInfoList entries = dir.entryInfoList(QDir::AllEntries | QDir::NoDotAndDotDot, QDir::DirsFirst | QDir::Name);
    for (const QFileInfo &entry : entries) {
        if (entry.isDir() && ignoredDirectories.contains(entry.fileName())) continue;
        const QString rel = relative.isEmpty() ? entry.fileName() : relative + "/" + entry.fileName();
        QVariantMap item;
        item["name"] = entry.fileName();
        item["path"] = rel;
        item["type"] = entry.isDir() ? "folder" : "file";
        if (entry.isFile()) {
            item["size"] = entry.size();
            item["classification"] = classify(rel);
        }
        out.append(item);
        if (entry.isDir()) walk(entry.absoluteFilePath(), rel, out);
    }
}

QVariantList ForgeBackend::listFiles(const QString &relativePath) const {
    const QString path = safePath(relativePath);
    QVariantList files;
    if (!path.isEmpty() && QFileInfo(path).isDir()) walk(path, relativePath, files);
    return files;
}

QString ForgeBackend::readFile(const QString &relativePath) {
    const QString path = safePath(relativePath);
    QFile file(path);
    if (path.isEmpty() || !file.exists() || !file.open(QIODevice::ReadOnly)) {
        emit operationMessage(tr("File tidak ditemukan: %1").arg(relativePath), true);
        return {};
    }
    if (file.size() > 5 * 1024 * 1024) {
        emit operationMessage(tr("File terlalu besar untuk dibuka di editor"), true);
        return {};
    }
    m_activeFile = relativePath;
    emit activeFileChanged();
    const QString content = QString::fromUtf8(file.readAll());
    emit fileOpened(relativePath, content);
    return content;
}

bool ForgeBackend::writeFile(const QString &relativePath, const QString &content) {
    const QString path = safePath(relativePath);
    if (path.isEmpty()) return false;
    const QString suffix = QFileInfo(path).suffix().toLower();
    if (!codeExtensions.contains(suffix) && !m_nativeExecutionEnabled) {
        emit operationMessage(tr("Penulisan binary diblokir; aktifkan adapter khusus"), true);
        return false;
    }
    QDir().mkpath(QFileInfo(path).absolutePath());
    QFile file(path);
    if (!file.open(QIODevice::WriteOnly | QIODevice::Truncate)) return false;
    file.write(content.toUtf8());
    emit filesChanged();
    emit operationMessage(tr("Disimpan: %1").arg(relativePath), false);
    return true;
}

bool ForgeBackend::createFile(const QString &relativePath, const QString &content) { return writeFile(relativePath, content); }

bool ForgeBackend::createFolder(const QString &relativePath) {
    const QString path = safePath(relativePath);
    const bool ok = !path.isEmpty() && QDir().mkpath(path);
    if (ok) emit filesChanged();
    return ok;
}

bool ForgeBackend::removePath(const QString &relativePath) {
    const QString path = safePath(relativePath);
    if (path.isEmpty() || path == m_workspaceRoot) return false;
    QFileInfo info(path);
    const bool ok = info.isDir() ? QDir(path).removeRecursively() : QFile::remove(path);
    if (ok) emit filesChanged();
    return ok;
}

QVariantList ForgeBackend::search(const QString &query, bool regex, bool caseSensitive) const {
    QVariantList results;
    if (query.isEmpty()) return results;
    const QRegularExpression::PatternOptions options = caseSensitive ? QRegularExpression::NoPatternOption : QRegularExpression::CaseInsensitiveOption;
    const QRegularExpression expression(regex ? query : QRegularExpression::escape(query), options);
    QDirIterator it(m_workspaceRoot, QDir::Files, QDirIterator::Subdirectories);
    while (it.hasNext() && results.size() < 500) {
        const QString filePath = it.next();
        const QFileInfo info(filePath);
        if (ignoredDirectories.contains(info.dir().dirName()) || info.size() > 1024 * 1024) continue;
        QFile file(filePath);
        if (!file.open(QIODevice::ReadOnly)) continue;
        const QString text = QString::fromUtf8(file.readAll());
        if (text.contains(QChar('\0'))) continue;
        const QStringList lines = text.split('\n');
        for (int i = 0; i < lines.size() && results.size() < 500; ++i) {
            if (expression.match(lines.at(i)).hasMatch()) {
                QVariantMap hit;
                hit["path"] = QDir(m_workspaceRoot).relativeFilePath(filePath);
                hit["line"] = i + 1;
                hit["text"] = lines.at(i).trimmed();
                results.append(hit);
            }
        }
    }
    return results;
}

QVariantMap ForgeBackend::status() const {
    QVariantMap hardware;
    hardware["platform"] = QSysInfo::productType();
    hardware["architecture"] = QSysInfo::currentCpuArchitecture();
    hardware["cpu"] = QSysInfo::currentCpuArchitecture();
    hardware["native"] = m_nativeExecutionEnabled;
    QVariantMap result;
    result["ok"] = true;
    result["workspaceRoot"] = m_workspaceRoot;
    result["hardware"] = hardware;
    result["strictNative"] = true;
    return result;
}

QVariantList ForgeBackend::engines() const {
    QVariantList result;
    const QStringList ids = {"forge-compiler", "forge-editor", "forge-inference", "forge-trainer", "forge-litgpt"};
    const QString root = qEnvironmentVariable("FORGE_ENGINE_ROOT", QDir(m_workspaceRoot).filePath("../forge-engine"));
    for (const QString &id : ids) {
        QVariantMap engine;
        engine["id"] = id;
        engine["path"] = QDir(root).filePath(id);
        engine["available"] = QFileInfo::exists(engine["path"].toString());
        engine["executable"] = m_nativeExecutionEnabled;
        result.append(engine);
    }
    return result;
}

QVariantMap ForgeBackend::tokenize(const QString &text) const {
    QVariantList tokens;
    const QVector<uint> codepoints = text.toUcs4();
    for (int i = 0; i < codepoints.size(); ++i) {
        QVariantMap token;
        token["id"] = static_cast<quint32>(codepoints.at(i));
        token["text"] = QString::fromUcs4(&codepoints.at(i), 1);
        token["index"] = i;
        tokens.append(token);
    }
    return {{"ok", true}, {"mode", "encode"}, {"tokenCount", tokens.size()}, {"tokens", tokens}, {"text", text}};
}

QString ForgeBackend::startJob(const QString &type, const QVariantMap &payload) {
    static const QStringList allowed = {"inspect-model", "tokenize", "inference", "compile", "quantize", "serve", "training", "evaluate", "convert"};
    if (!allowed.contains(type)) return {};
    const QString id = QUuid::createUuid().toString(QUuid::WithoutBraces);
    QVariantMap created{{"id", id}, {"type", type}, {"payload", payload}, {"status", "failed"}, {"progress", 0}, {"createdAt", isoNow()}};
    created["logs"] = QStringList{QString("[%1] Native C++ job dibuat").arg(isoNow())};
    created["result"] = QVariantMap{{"native", m_nativeExecutionEnabled}, {"message", m_nativeExecutionEnabled ? "Menunggu adapter native C++" : "Native execution belum diaktifkan"}};
    m_jobs.insert(id, created);
    saveJobs();
    emit jobChanged(id);
    return id;
}

QVariantMap ForgeBackend::job(const QString &id) const { return m_jobs.value(id); }
void ForgeBackend::cancelJob(const QString &id) { if (m_jobs.contains(id)) { auto value = m_jobs.value(id); value["status"] = "cancelled"; m_jobs[id] = value; saveJobs(); emit jobChanged(id); } }
void ForgeBackend::setNativeExecutionEnabled(bool enabled) { if (m_nativeExecutionEnabled == enabled) return; m_nativeExecutionEnabled = enabled; emit nativeExecutionEnabledChanged(); }
void ForgeBackend::openFile(const QString &relativePath) { readFile(relativePath); }
void ForgeBackend::refresh() { emit filesChanged(); }

void ForgeBackend::saveJobs() const {
    const QString path = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
    QDir().mkpath(path);
    QFile file(QDir(path).filePath("jobs.json"));
    if (!file.open(QIODevice::WriteOnly)) return;
    QJsonArray array;
    for (const auto &job : m_jobs) array.append(QJsonObject::fromVariantMap(job));
    file.write(QJsonDocument(array).toJson(QJsonDocument::Compact));
}

void ForgeBackend::loadJobs() {
    QFile file(QDir(QStandardPaths::writableLocation(QStandardPaths::AppDataLocation)).filePath("jobs.json"));
    if (!file.open(QIODevice::ReadOnly)) return;
    const QJsonArray array = QJsonDocument::fromJson(file.readAll()).array();
    for (const auto &value : array) m_jobs.insert(value.toObject().value("id").toString(), value.toObject().toVariantMap());
}

void ForgeBackend::appendJobLog(QVariantMap &, const QString &) {}
