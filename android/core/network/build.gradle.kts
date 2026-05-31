plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

val defaultBuyPilotBaseUrl = "http://10.0.2.2:8000"

fun String.asBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun loadRootDotEnv(): Map<String, String> {
    val projectRoot = rootProject.projectDir.parentFile ?: rootProject.projectDir
    val envFile = projectRoot.resolve(".env")
    if (!envFile.isFile) return emptyMap()

    return envFile
        .readLines()
        .asSequence()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val separatorIndex = line.indexOf('=')
            if (separatorIndex <= 0) return@mapNotNull null

            val key = line.substring(0, separatorIndex).trim()
            val rawValue = line.substring(separatorIndex + 1).trim()
            val value = rawValue
                .removeSurrounding("\"")
                .removeSurrounding("'")
            key.takeIf { it.isNotEmpty() }?.let { it to value }
        }
        .toMap()
}

val buyPilotBaseUrl = providers
    .environmentVariable("BUY_PILOT_BASE_URL")
    .orNull
    ?.takeIf { it.isNotBlank() }
    ?: loadRootDotEnv()["BUY_PILOT_BASE_URL"]?.takeIf { it.isNotBlank() }
    ?: defaultBuyPilotBaseUrl

android {
    namespace = "com.buypilot.core.network"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BUY_PILOT_BASE_URL", buyPilotBaseUrl.asBuildConfigString())
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))

    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
