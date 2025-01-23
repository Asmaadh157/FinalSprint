plugins {
    id("com.android.application") version "8.7.3" apply true // Valid AGP version
    id("org.jetbrains.kotlin.android") version "1.9.24" apply true // Valid Kotlin version
    id("com.google.gms.google-services") version "4.4.2" apply true // Explicit Google Services version
}

android {
    namespace = "com.example.gestiondesproject"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gestiondesproject"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // Enable View Binding
    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Core libraries
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1") // Updated AppCompat version
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.activity:activity-ktx:1.6.1") // Updated Activity KTX version
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation (libs.mpandroidchart)

    // Firebase dependencies
    implementation("com.google.firebase:firebase-auth-ktx:21.1.0") // Firebase Authentication
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")


    implementation(libs.androidx.recyclerview)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.gridlayout) // Firebase Realtime Database


    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

