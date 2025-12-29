import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)

    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    jvm()
    
    sourceSets {
        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            // Koin Android specific
            //implementation(libs.koin.android)

            // SQLDelight Android driver
            implementation(libs.sqldelight.android)

            // Ktor Android engine
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.server.netty)

            // Android security/crypto
            implementation(libs.androidx.security.crypto)

            // Coroutines Android
            implementation(libs.kotlinx.coroutines.android)



        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)

            implementation(libs.navigation.compose)


            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // SQLDelight - common runtime
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)

            // Coroutines
            implementation(libs.kotlinx.coroutines.core)

            // Ktor - WebSocket client/server (common)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.websockets)
            implementation(libs.ktor.serialization)

            // DateTime
            implementation(libs.kotlinx.datetime)

            implementation(libs.material.icons.core)

            implementation(compose.materialIconsExtended)

            implementation(libs.material.kolor)


        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            // SQLDelight JVM driver
            implementation(libs.sqldelight.jvm)

            // Ktor Desktop engine
            implementation(libs.ktor.client.okhttp)
           // implementation(libs.ktor.server.netty)

            // Coroutines Swing (for Desktop UI)
            implementation(libs.kotlinx.coroutines.swing)

        }
    }
}

android {
    namespace = "com.abdat.clipwhisper"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.abdat.clipwhisper"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            //excludes += "/META-INF/INDEX.LIST"
        }
        configurations.all {
            exclude(group = "io.netty")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.abdat.clipwhisper.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb,TargetFormat.Rpm,TargetFormat.Exe)
            packageName = "com.abdat.clipwhisper"
            packageVersion = "1.0.0"
            modules("java.sql", "java.naming", "java.net.http")
            linux {
                packageName = "clipwhisper"
                shortcut = true
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
                rpmLicenseType = "MIT"
                debMaintainer = "abderamanabdat@gmail.com"
            }
            windows {
                packageName = "ClipWhisper"
                dirChooser = true
                perUserInstall = true
                menuGroup = "ClipWhisper"
                upgradeUuid = "85ebee7a-ca5d-4f0b-b44d-ea31ee2d9538"
                shortcut = true
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
            }
        }
    }
}

// SQLDelight configuration
sqldelight {
    databases {
        create("ClipWhisperDatabase") {
            packageName.set("com.abdat.clipwhisper.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
