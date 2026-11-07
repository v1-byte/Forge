plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.v1byte.forge"
    compileSdk = 35
    ndkVersion = "27.2.12479018"

    defaultConfig {
        applicationId = "com.v1byte.forge"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-native-preview"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-fexceptions", "-frtti")
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }


    sourceSets.getByName("main") {
        assets.srcDir(layout.buildDirectory.dir("generated/forge-licenses"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}


val prepareForgeInferenceLicense by tasks.registering(Copy::class) {
    from(rootProject.projectDir.parentFile.resolve("LICENSES/forge-inference__LICENSE"))
    into(layout.buildDirectory.dir("generated/forge-licenses"))
    rename { "forge-inference-license.txt" }
}

tasks.named("preBuild").configure {
    dependsOn(prepareForgeInferenceLicense)
}
