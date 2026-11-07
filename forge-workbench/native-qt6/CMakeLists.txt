cmake_minimum_required(VERSION 3.21)
project(ForgeWorkbench VERSION 2.1.5 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(QT_QML_GENERATE_QMLLS_INI ON)

find_package(Qt6 6.4 REQUIRED COMPONENTS Core Gui Network Qml Quick QuickControls2)
qt_standard_project_setup(REQUIRES 6.4)

qt_add_executable(ForgeWorkbench
    src/main.cpp
    src/ForgeBackend.cpp
    src/ForgeBackend.h
)

qt_add_qml_module(ForgeWorkbench
    URI Forge.Workbench
    VERSION 1.0
    QML_FILES
        qml/Main.qml
)

target_compile_definitions(ForgeWorkbench PRIVATE FORGE_VERSION="${PROJECT_VERSION}")
math(EXPR FORGE_ANDROID_VERSION_CODE "${PROJECT_VERSION_MAJOR} * 1000000 + ${PROJECT_VERSION_MINOR} * 1000 + ${PROJECT_VERSION_PATCH}")

set_target_properties(ForgeWorkbench PROPERTIES
    MACOSX_BUNDLE TRUE
    WIN32_EXECUTABLE TRUE
    QT_ANDROID_PACKAGE_SOURCE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/android"
    QT_ANDROID_PACKAGE_NAME "com.v1byte.forgeworkbench"
    QT_ANDROID_APP_NAME "Forge Workbench"
    QT_ANDROID_VERSION_NAME "${PROJECT_VERSION}"
    QT_ANDROID_VERSION_CODE "${FORGE_ANDROID_VERSION_CODE}"
    QT_ANDROID_TARGET_SDK_VERSION 34
    QT_ANDROID_SDK_BUILD_TOOLS_REVISION "34.0.0"
)

target_link_libraries(ForgeWorkbench PRIVATE
    Qt6::Core Qt6::Gui Qt6::Network Qt6::Qml Qt6::Quick Qt6::QuickControls2
)

install(TARGETS ForgeWorkbench
    BUNDLE DESTINATION .
    RUNTIME DESTINATION bin
    LIBRARY DESTINATION lib
)
