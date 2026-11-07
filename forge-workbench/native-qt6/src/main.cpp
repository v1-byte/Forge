#include <QGuiApplication>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQuickStyle>
#include <QDir>
#include "ForgeBackend.h"

#ifndef FORGE_VERSION
#define FORGE_VERSION "0.0.0"
#endif

int main(int argc, char *argv[]) {
    QGuiApplication app(argc, argv);
    app.setApplicationName("Forge Workbench");
    app.setOrganizationName("Forge");
    app.setApplicationVersion(FORGE_VERSION);
    QQuickStyle::setStyle("Basic");

    ForgeBackend backend;
    QQmlApplicationEngine engine;
    engine.rootContext()->setContextProperty("forge", &backend);
    engine.loadFromModule("Forge.Workbench", "Main");
    if (engine.rootObjects().isEmpty()) return 1;
    return app.exec();
}
