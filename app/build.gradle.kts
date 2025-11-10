plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.healthybite"           // GANTI sesuai paketmu
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.healthybite"   // GANTI sesuai paketmu
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // lokasi (wajib)
    implementation("com.google.android.gms:play-services-location:21.0.1")
}

