plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.buypilot.core.model"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// SSE protocol guard: force protocol test to run on every assemble
// This makes Kotlin-side drift detection as hard as Python's import-time guard
tasks.matching { it.name.startsWith("assemble") }.configureEach {
    dependsOn("testDebugUnitTest")
}

dependencies {
    implementation(project(":core:common"))
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
