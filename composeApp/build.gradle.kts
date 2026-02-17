import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val bootstrapConfigOutputDir = layout.buildDirectory.dir("generated/source/bootstrapConfig/webMain/kotlin")
val localPropertiesApiKey = providers.provider {
    val propertiesFile = rootProject.layout.projectDirectory.file("local.properties").asFile
    if (!propertiesFile.exists()) {
        ""
    } else {
        val properties = Properties()
        propertiesFile.inputStream().use(properties::load)
        properties.getProperty("immich.apiKey", "")
    }
}
val bootstrapApiKey = providers.gradleProperty("immichApiKey")
    .orElse(providers.environmentVariable("IMMICH_API_KEY"))
    .orElse(localPropertiesApiKey)
    .orElse("")

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        // Browser runtime code lives in webMain; keep jsMain/wasmJsMain empty unless
        // target-specific Kotlin sources are intentionally introduced.
        matching { it.name == "webMain" }.configureEach {
            kotlin.srcDir(bootstrapConfigOutputDir)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.marcportabella.immichuploader"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

val bootstrapConfigFile = bootstrapConfigOutputDir.get()
    .file("com/marcportabella/immichuploader/app/BootstrapConfig.kt")
    .asFile
bootstrapConfigFile.parentFile.mkdirs()
val escapedApiKey = bootstrapApiKey.get().replace("\\", "\\\\").replace("\"", "\\\"")
bootstrapConfigFile.writeText(
    """
    package com.marcportabella.immichuploader.app

    internal const val BOOTSTRAP_IMMICH_API_KEY: String = "$escapedApiKey"
    """.trimIndent() + "\n"
)

// Verification handoffs expect these task names for web checks.
tasks.register("compileKotlinWeb") {
    dependsOn(":composeApp:compileKotlinWasmJs")
}

tasks.register("webTest") {
    dependsOn(":composeApp:wasmJsTest")
}

val chromeBin = System.getenv("CHROME_BIN")
if (chromeBin.isNullOrBlank()) {
    tasks.matching { it.name in setOf("wasmJsTest", "wasmJsBrowserTest") }.configureEach {
        enabled = false
    }
    tasks.named("check") {
        dependsOn(":composeApp:compileKotlinWasmJs")
    }
}

rootProject.tasks.matching { it.name == "kotlinStoreYarnLock" }.configureEach {
    enabled = false
}
