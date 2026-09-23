plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI passes its run number so every new APK installs as an update over the old one.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "com.kruju.habits"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kruju.habits"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    // CI signs with a key from GitHub secrets when one is configured, so new APKs install as updates.
    // Without it, builds fall back to the debug key.
    val keystorePath = System.getenv("SIGNING_KEYSTORE_PATH")
    val keystorePassword = System.getenv("SIGNING_PASSWORD")
    val sharedSigning = if (keystorePath != null && keystorePassword != null) {
        signingConfigs.create("shared") {
            storeFile = file(keystorePath)
            storePassword = keystorePassword
            keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "habits"
            keyPassword = keystorePassword
        }
    } else {
        signingConfigs.getByName("debug")
    }

    buildTypes {
        release {
            // R8 strips the unused parts of the large extended icon set.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = sharedSigning
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
}
