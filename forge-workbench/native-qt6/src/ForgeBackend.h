#pragma once

#include <QObject>
#include <QVariantList>
#include <QVariantMap>
#include <QTimer>
#include <QHash>
#include <QJsonObject>
#include <QString>
#include <QNetworkAccessManager>

class ForgeBackend final : public QObject {
    Q_OBJECT
    Q_PROPERTY(QString workspaceRoot READ workspaceRoot WRITE setWorkspaceRoot NOTIFY workspaceRootChanged)
    Q_PROPERTY(QString activeFile READ activeFile NOTIFY activeFileChanged)
    Q_PROPERTY(bool nativeExecutionEnabled READ nativeExecutionEnabled NOTIFY nativeExecutionEnabledChanged)
    Q_PROPERTY(QString appVersion READ appVersion CONSTANT)
    Q_PROPERTY(QString updateStatus READ updateStatus NOTIFY updateStatusChanged)
    Q_PROPERTY(QString latestVersion READ latestVersion NOTIFY latestVersionChanged)
    Q_PROPERTY(int updateProgress READ updateProgress NOTIFY updateProgressChanged)
    Q_PROPERTY(bool updateAvailable READ updateAvailable NOTIFY updateAvailableChanged)

public:
    explicit ForgeBackend(QObject *parent = nullptr);

    QString workspaceRoot() const;
    void setWorkspaceRoot(const QString &root);
    QString activeFile() const;
    bool nativeExecutionEnabled() const;
    QString appVersion() const;
    QString updateStatus() const;
    QString latestVersion() const;
    int updateProgress() const;
    bool updateAvailable() const;

    Q_INVOKABLE QVariantList listFiles(const QString &relativePath = QString()) const;
    Q_INVOKABLE QString readFile(const QString &relativePath);
    Q_INVOKABLE bool writeFile(const QString &relativePath, const QString &content);
    Q_INVOKABLE bool createFile(const QString &relativePath, const QString &content = QString());
    Q_INVOKABLE bool createFolder(const QString &relativePath);
    Q_INVOKABLE bool removePath(const QString &relativePath);
    Q_INVOKABLE QVariantList search(const QString &query, bool regex = false, bool caseSensitive = false) const;
    Q_INVOKABLE QVariantMap status() const;
    Q_INVOKABLE QVariantList engines() const;
    Q_INVOKABLE QVariantMap tokenize(const QString &text) const;
    Q_INVOKABLE QString startJob(const QString &type, const QVariantMap &payload = {});
    Q_INVOKABLE QVariantMap job(const QString &id) const;
    Q_INVOKABLE void cancelJob(const QString &id);
    Q_INVOKABLE void setNativeExecutionEnabled(bool enabled);
    Q_INVOKABLE void openFile(const QString &relativePath);
    Q_INVOKABLE void refresh();
    Q_INVOKABLE void checkForUpdates(bool manual = true);
    Q_INVOKABLE void downloadAndInstallUpdate();

signals:
    void workspaceRootChanged();
    void activeFileChanged();
    void nativeExecutionEnabledChanged();
    void filesChanged();
    void fileOpened(const QString &path, const QString &content);
    void operationMessage(const QString &message, bool error);
    void jobChanged(const QString &id);
    void updateStatusChanged();
    void latestVersionChanged();
    void updateProgressChanged();
    void updateAvailableChanged();

private:
    QString safePath(const QString &relativePath) const;
    QVariantMap classify(const QString &path) const;
    void walk(const QString &absolute, const QString &relative, QVariantList &out) const;
    void saveJobs() const;
    void loadJobs();
    void appendJobLog(QVariantMap &job, const QString &line);
    void setUpdateStatus(const QString &status);
    void handleReleaseResponse(const QByteArray &data, bool manual);
    void installDownloadedApk(const QString &path);

    QString m_workspaceRoot;
    QString m_activeFile;
    bool m_nativeExecutionEnabled = false;
    QHash<QString, QVariantMap> m_jobs;
    QNetworkAccessManager m_network;
    QString m_updateStatus = QStringLiteral("Belum diperiksa");
    QString m_latestVersion;
    QString m_updateApkUrl;
    QString m_updateSha256;
    int m_updateProgress = 0;
    bool m_updateAvailable = false;
};
