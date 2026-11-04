import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// google-services.json is a secret kept out of the repo. Builds without it, like CI, still
// compile, and the app starts with Firebase disabled, as the desktop app does.
val firebaseConfigured = file("google-services.json").exists()
if (firebaseConfigured) {
    apply(plugin = libs.plugins.googleGmsGoogleServices.get().pluginId)
}

android {
    namespace = "com.peakdevcol.project.hotelmanagement.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // Must match the Android app registered in Firebase, or google-services.json does not apply.
        applicationId = "com.peakdevcol.project.hotelmanagement"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("boolean", "FIREBASE_CONFIGURED", firebaseConfigured.toString())
    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
}
