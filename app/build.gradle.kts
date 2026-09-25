plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

providers.environmentVariable("ESPBRIDGE_BUILD_DIR").orNull
    ?.takeIf { it.isNotBlank() }
    ?.let { layout.buildDirectory.set(file(it)) }

val hasFirebaseConfig = file("google-services.json").exists()
val otaManifestUrl = providers.gradleProperty("ESPBRIDGE_OTA_MANIFEST_URL")
    .orElse(providers.environmentVariable("ESPBRIDGE_OTA_MANIFEST_URL"))
    .getOrElse("")
val otaPublicKey = providers.gradleProperty("ESPBRIDGE_OTA_PUBLIC_KEY_B64")
    .orElse(providers.environmentVariable("ESPBRIDGE_OTA_PUBLIC_KEY_B64"))
    .getOrElse("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEk3v1MoQ51dePMgLP6RF5sxLF7ApABCOmmcPDg8+M7kTnBJMKWWFoHhDzV/J07HVKAHGtkIGC5cZ+qjf4yE3/Aw==")
val releaseStoreFile = providers.environmentVariable("ESPBRIDGE_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("ESPBRIDGE_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ESPBRIDGE_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ESPBRIDGE_RELEASE_KEY_PASSWORD").orNull
val releaseSigningReady = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }
fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
if (hasFirebaseConfig) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.irsyadlabs.espbridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.irsyadlabs.espbridge"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.4.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        buildConfigField("boolean", "FIREBASE_CONFIG_PRESENT", hasFirebaseConfig.toString())
        buildConfigField("String", "DEFAULT_BLE_SERVICE_UUID", "\"7c9e0001-6f2f-4d4d-9f25-0d7fd4f0a001\"")
        buildConfigField("String", "OTA_MANIFEST_URL", buildConfigString(otaManifestUrl))
        buildConfigField("String", "OTA_PUBLIC_KEY_B64", buildConfigString(otaPublicKey))
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "Xichi-v${defaultConfig.versionName}-${name}.apk"
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    options.isFork = true
    options.forkOptions.memoryInitialSize = "32m"
    options.forkOptions.memoryMaximumSize = "128m"
    options.forkOptions.jvmArgs = listOf(
        "-XX:+UseSerialGC",
        "-XX:MaxMetaspaceSize=128m",
        "-XX:ReservedCodeCacheSize=64m",
        "-Xss512k"
    )
}

tasks.withType<Test>().configureEach {
    minHeapSize = "32m"
    maxHeapSize = "192m"
    jvmArgs(
        "-XX:+UseSerialGC",
        "-XX:MaxMetaspaceSize=128m",
        "-XX:ReservedCodeCacheSize=64m",
        "-Xss512k"
    )
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // CameraX & ML Kit for QR Scanning
    val cameraVersion = "1.4.0"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-common")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
