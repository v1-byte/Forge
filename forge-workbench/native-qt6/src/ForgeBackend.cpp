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
#include <QSaveFile>
#include <QCoreApplication>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QVersionNumber>
#include <QUrl>
#ifdef Q_OS_ANDROID
#include <QJniObject>
#endif

namespace {
const QStringList codeExtensions = {"c", "cc", "cpp", "h", "hpp", "py", "js", "ts", "tsx", "jsx", "java", "kt", "rs", "go", "swift", "sh", "json", "yaml", "yml", "toml", "xml", "cmake", "md"};
const QStringList modelExtensions = {"gguf", "safetensors", "bin", "pt", "pth", "ckpt", "onnx"};
const QStringList ignoredDirectories = {".git", "node_modules", "__pycache__", ".venv", "build"};

QString isoNow() { return QDateTime::currentDateTimeUtc().toString(Qt::ISODate); }
QString keyFor(const QString &type) { return QString("FORGE_%1_CMD").arg(type.toUpper().replace('-', '_')); }
QString comparableVersion(QString value) {
    value.remove(QRegularExpression("^[vV]"));
    const int suffix = value.indexOf(QRegularExpression("[^0-9.]"));
    if (suffix >= 0) value.truncate(suffix);
    return value;
}
}

ForgeBackend::ForgeBackend(QObject *parent) : QObject(parent) {
    const QString configured = qEnvironmentVariable("FORGE_WORKSPACE_ROOT");
    m_workspaceRoot = configured.isEmpty()
        ? QDir(QStandardPaths::writableLocation(QStandardPaths::DocumentsLocation)).filePath("ForgeWorkspace")
        : QDir::cleanPath(configured);
    QDir().mkpath(m_workspaceRoot);
    loadJobs();
    QTimer::singleShot(3000, this, [this]() { checkForUpdates(false); });
    auto *updateTimer = new QTimer(this);
    updateTimer->setInterval(6 * 60 * 60 * 1000);
    connect(updateTimer, &QTimer::timeout, this, [this]() { checkForUpdates(false); });
    updateTimer->start();
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
QString ForgeBackend::appVersion() const { return QCoreApplication::applicationVersion(); }
QString ForgeBackend::updateStatus() const { return m_updateStatus; }
QString ForgeBackend::latestVersion() const { return m_latestVersion; }
int ForgeBackend::updateProgress() const { return m_updateProgress; }
bool ForgeBackend::updateAvailable() const { return m_updateAvailable; }

void ForgeBackend::setUpdateStatus(const QString &status) {
    if (m_updateStatus == status) return;
    m_updateStatus = status;
    emit updateStatusChanged();
}

void ForgeBackend::handleReleaseResponse(const QByteArray &data, bool manual) {
    QJsonParseError error;
    const QJsonDocument document = QJsonDocument::fromJson(data, &error);
    if (error.error != QJsonParseError::NoError || !document.isObject()) {
        setUpdateStatus("Metadata update tidak valid");
        if (manual) emit operationMessage(m_updateStatus, true);
        return;
    }
    const QJsonObject release = document.object();
    const QString tag = release.value("tag_name").toString().trimmed();
    const QString currentText = comparableVersion(appVersion());
    const QString latestText = comparableVersion(tag);
    const QVersionNumber current = QVersionNumber::fromString(currentText);
    const QVersionNumber latest = QVersionNumber::fromString(latestText);
    const bool validTag = QRegularExpression("^v[0-9]+\\.[0-9]+\\.[0-9]+$").match(tag).hasMatch();
    if (release.value("draft").toBool() || release.value("prerelease").toBool() || !validTag || current.isNull() || latest.isNull()) {
        if (m_updateAvailable) { m_updateAvailable = false; emit updateAvailableChanged(); }
        m_updateApkUrl.clear();
        m_updateSha256Url.clear();
        m_updateSha256.clear();
        setUpdateStatus("Metadata update tidak valid atau bukan rilis stabil");
        if (manual) emit operationMessage(m_updateStatus, true);
        return;
    }
    if (m_latestVersion != tag) { m_latestVersion = tag; emit latestVersionChanged(); }
    m_updateApkUrl.clear();
    m_updateSha256Url.clear();
    const QString apkName = QString("ForgeWorkbench-%1.apk").arg(tag.mid(1));
    const QString checksumName = apkName + ".sha256";
    for (const QJsonValue &value : release.value("assets").toArray()) {
        const QJsonObject asset = value.toObject();
        const QString name = asset.value("name").toString();
        const QString url = asset.value("browser_download_url").toString();
        const QUrl parsed(url);
        if (name == apkName || name == checksumName) {
            if (!parsed.isValid() || parsed.scheme() != "https" || parsed.host() != "github.com") {
                if (m_updateAvailable) { m_updateAvailable = false; emit updateAvailableChanged(); }
                m_updateApkUrl.clear(); m_updateSha256Url.clear(); m_updateSha256.clear();
                setUpdateStatus("URL asset rilis tidak aman");
                if (manual) emit operationMessage(m_updateStatus, true);
                return;
            }
            if (name == apkName) m_updateApkUrl = url;
            if (name == checksumName) m_updateSha256Url = url;
        }
    }
    const bool newer = QVersionNumber::compare(latest, current) > 0;
    const bool available = newer && !m_updateApkUrl.isEmpty() && !m_updateSha256Url.isEmpty();
    if (m_updateAvailable != available) { m_updateAvailable = available; emit updateAvailableChanged(); }
    if (!available) { m_updateApkUrl.clear(); m_updateSha256Url.clear(); m_updateSha256.clear(); }
    const QString status = available ? QString("Versi %1 tersedia — APK dan checksum tersedia").arg(tag) :
        (newer ? "Rilis baru tidak memiliki pasangan APK/SHA-256 yang valid" : QString("Aplikasi sudah terbaru (%1)").arg(appVersion()));
    setUpdateStatus(status);
    if (manual) emit operationMessage(status, newer && !available);
}

void ForgeBackend::checkForUpdates(bool manual) {
    if (m_updateCheckInProgress) { if (manual) emit operationMessage("Pemeriksaan update sedang berjalan", false); return; }
    m_updateCheckInProgress = true;
    if (m_updateAvailable) { m_updateAvailable = false; emit updateAvailableChanged(); }
    m_updateApkUrl.clear(); m_updateSha256Url.clear(); m_updateSha256.clear();
    setUpdateStatus("Memeriksa update…");
    QNetworkRequest request(QUrl("https://api.github.com/repos/v1-byte/Forge/releases/latest"));
    request.setHeader(QNetworkRequest::UserAgentHeader, "Forge-Workbench-Updater/1.0");
    request.setRawHeader("Accept", "application/vnd.github+json");
    request.setTransferTimeout(30000);
    request.setAttribute(QNetworkRequest::RedirectPolicyAttribute, QNetworkRequest::NoLessSafeRedirectPolicy);
    QNetworkReply *reply = m_network.get(request);
    connect(reply, &QNetworkReply::finished, this, [this, reply, manual]() {
        m_updateCheckInProgress = false;
        const QByteArray data = reply->readAll();
        const int httpStatus = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
        if (reply->error() != QNetworkReply::NoError || httpStatus != 200) {
            if (m_updateAvailable) { m_updateAvailable = false; emit updateAvailableChanged(); }
            setUpdateStatus(QString("Gagal memeriksa update (HTTP %1): %2").arg(httpStatus).arg(reply->errorString()));
            if (manual) emit operationMessage(m_updateStatus, true);
        } else handleReleaseResponse(data, manual);
        reply->deleteLater();
    });
}

void ForgeBackend::downloadAndInstallUpdate() {
    if (m_updateDownloadInProgress) { emit operationMessage("Download update sedang berjalan", false); return; }
    if (!m_updateAvailable || m_updateApkUrl.isEmpty() || m_updateSha256Url.isEmpty()) { emit operationMessage("Update dibatalkan: APK atau checksum SHA-256 tidak tersedia", true); return; }
#ifndef Q_OS_ANDROID
    emit operationMessage("Installer APK hanya tersedia pada Android", true);
    return;
#else
    m_updateDownloadInProgress = true;
    setUpdateStatus("Mengambil checksum resmi release…");
    QNetworkRequest checksumRequest{QUrl(m_updateSha256Url)};
    checksumRequest.setHeader(QNetworkRequest::UserAgentHeader, "Forge-Workbench-Updater/1.0");
    checksumRequest.setTransferTimeout(30000);
    checksumRequest.setAttribute(QNetworkRequest::RedirectPolicyAttribute, QNetworkRequest::NoLessSafeRedirectPolicy);
    QNetworkReply *checksumReply = m_network.get(checksumRequest);
    connect(checksumReply, &QNetworkReply::finished, this, [this, checksumReply]() {
        const QByteArray body = checksumReply->readAll();
        const int status = checksumReply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
        const QRegularExpressionMatch match = QRegularExpression("^\\s*([0-9a-fA-F]{64})(?:\\s+\\*?[^\\r\\n]+)?\\s*$").match(QString::fromUtf8(body));
        if (checksumReply->error() != QNetworkReply::NoError || status != 200 || !match.hasMatch()) {
            m_updateDownloadInProgress = false;
            setUpdateStatus("Gagal memverifikasi checksum release");
            emit operationMessage(QString("Update dibatalkan: checksum tidak dapat diambil/valid (HTTP %1)").arg(status), true);
            checksumReply->deleteLater();
            return;
        }
        m_updateSha256 = match.captured(1).toLower();
        checksumReply->deleteLater();
        downloadVerifiedApk();
    });
#endif
}

void ForgeBackend::downloadVerifiedApk() {
#ifndef Q_OS_ANDROID
    emit operationMessage("Installer APK hanya tersedia pada Android", true);
    return;
#else
    const QString cache = QStandardPaths::writableLocation(QStandardPaths::CacheLocation);
    if (!QDir().mkpath(cache)) { m_updateDownloadInProgress = false; emit operationMessage("Folder cache update tidak dapat dibuat", true); return; }
    const QString apkPath = QDir(cache).filePath("forge-update.apk");
    auto *file = new QSaveFile(apkPath, this);
    if (!file->open(QIODevice::WriteOnly)) { m_updateDownloadInProgress = false; file->deleteLater(); emit operationMessage("Tidak dapat menyiapkan APK update", true); return; }
    m_updateProgress = 0; emit updateProgressChanged(); setUpdateStatus("Mengunduh APK update…");
    QNetworkRequest request{QUrl(m_updateApkUrl)};
    request.setHeader(QNetworkRequest::UserAgentHeader, "Forge-Workbench-Updater/1.0");
    request.setTransferTimeout(120000);
    request.setAttribute(QNetworkRequest::RedirectPolicyAttribute, QNetworkRequest::NoLessSafeRedirectPolicy);
    QNetworkReply *reply = m_network.get(request);
    auto *hash = new QCryptographicHash(QCryptographicHash::Sha256);
    auto consume = [reply, file, hash]() -> bool {
        const QByteArray chunk = reply->readAll();
        if (chunk.isEmpty()) return true;
        constexpr qint64 maxApkBytes = 1024LL * 1024 * 1024;
        if (file->size() < 0 || file->size() + chunk.size() > maxApkBytes || file->write(chunk) != chunk.size()) return false;
        hash->addData(chunk);
        return true;
    };
    connect(reply, &QNetworkReply::readyRead, this, [this, reply, consume]() {
        if (!consume()) { setUpdateStatus("Download dihentikan: ukuran APK melewati batas atau penyimpanan gagal"); reply->abort(); }
    });
    connect(reply, &QNetworkReply::downloadProgress, this, [this](qint64 received, qint64 total) {
        const int progress = total > 0 ? static_cast<int>((received * 100) / total) : 0;
        if (m_updateProgress != progress) { m_updateProgress = progress; emit updateProgressChanged(); }
    });
    connect(reply, &QNetworkReply::finished, this, [this, reply, file, hash, consume, apkPath]() {
        const bool consumed = consume();
        const int httpStatus = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
        const QByteArray expected = m_updateSha256.toLatin1();
        const QByteArray actual = hash->result().toHex();
        const bool digestFormatValid = QRegularExpression("^[0-9a-fA-F]{64}$").match(QString::fromLatin1(expected)).hasMatch();
        const bool responseValid = reply->error() == QNetworkReply::NoError && httpStatus == 200 && consumed && file->size() > 0;
        const bool digestValid = digestFormatValid && actual.compare(expected, Qt::CaseInsensitive) == 0;
        if (!responseValid || !digestValid || !file->commit()) {
            QString reason;
            if (reply->error() != QNetworkReply::NoError) reason = reply->errorString();
            else if (httpStatus != 200) reason = QString("server membalas HTTP %1").arg(httpStatus);
            else if (!consumed) reason = "ukuran APK melewati batas atau penulisan gagal";
            else if (file->size() == 0) reason = "file APK kosong";
            else reason = "SHA-256 tidak cocok atau format checksum salah";
            file->cancelWriting(); m_updateDownloadInProgress = false; m_updateProgress = 0; emit updateProgressChanged();
            setUpdateStatus("Update dibatalkan — verifikasi gagal"); emit operationMessage("APK tidak dipasang: " + reason, true);
            file->deleteLater(); hash->deleteLater(); reply->deleteLater(); return;
        }
        m_updateDownloadInProgress = false; m_updateProgress = 100; emit updateProgressChanged();
        setUpdateStatus("APK terverifikasi SHA-256; Android akan meminta konfirmasi pemasangan");
        installDownloadedApk(apkPath);
        file->deleteLater(); hash->deleteLater(); reply->deleteLater();
    });
#endif
}

void ForgeBackend::installDownloadedApk(const QString &path) {
#ifdef Q_OS_ANDROID
    const QJniObject activity = QNativeInterface::QAndroidApplication::context();
    const QJniObject javaPath = QJniObject::fromString(path);
    QJniObject::callStaticMethod<void>("org/forge/workbench/UpdateInstaller", "install", "(Landroid/content/Context;Ljava/lang/String;)V", activity.object(), javaPath.object());
#else
    Q_UNUSED(path);
#endif
}

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
    const QStringList systemExtensions = {"json", "yaml", "yml", "toml", "xml"};
    const bool system = systemExtensions.contains(suffix) || lower.contains("config") || lower.contains("manifest");
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
