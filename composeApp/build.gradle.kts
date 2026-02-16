import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
        commonMain.dependencies {
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
