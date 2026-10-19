plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.kifpool.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kifpool.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.5.2"
    }

    signingConfigs {
        create("release") {
            // فایل keystore عمداً در مخزن گیت قرار ندارد (مخزن عمومی است)
            // اگر keystore را در app/ قرار دهید، نسخهٔ release خودکار امضا می‌شود
            val ksFile = file("kifpool-release.keystore")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = "kifpool2024"
                keyAlias = "kifpool"
                keyPassword = "kifpool2024"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // بدون keystore موجود، خروجی unsigned تولید می‌شود (build خطا نمی‌دهد)
            if (file("kifpool-release.keystore").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
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

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room (local database)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Biometric (اثر انگشت) + WorkManager (چک روزانه یادآورها)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
