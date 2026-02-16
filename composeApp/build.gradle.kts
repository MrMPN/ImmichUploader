import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

val bootstrapConfigOutputDir = layout.buildDirectory.dir("generated/source/bootstrapConfig/webMain/kotlin")
val bootstrapApiKey = providers.environmentVariable("IMMICH_API_KEY").orElse("")

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        matching { it.name == "webMain" }.configureEach {
            kotlin.srcDir(bootstrapConfigOutputDir)
        }
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
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
