#include <QGuiApplication>
#include <QQmlApplicationEngine>
#include <QQmlContext>
#include <QQuickStyle>
#include <QDir>
#include "ForgeBackend.h"

int main(int argc, char *argv[]) {
    QGuiApplication app(argc, argv);
    app.setApplicationName("Forge Workbench");
    app.setOrganizationName("Forge");
    app.setApplicationVersion("2.1.0");
    QQuickStyle::setStyle("Basic");

    ForgeBackend backend;
    QQmlApplicationEngine engine;
    engine.rootContext()->setContextProperty("forge", &backend);
    engine.loadFromModule("Forge.Workbench", "Main");
    if (engine.rootObjects().isEmpty()) return 1;
    return app.exec();
}
