pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ForgeAndroid"
include(":app", ":llamaRuntime")
project(":llamaRuntime").projectDir = file("../../forge-engine/forge-inference/examples/llama.android/lib")
