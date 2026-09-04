plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.utar.ucycle"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.utar.ucycle"
        minSdk = 26
        targetSdk = 36

        // Bump versionCode every time you send a new APK to the group,
        // otherwise Android refuses to install it over the old one.
        versionCode = 13
        versionName = "2.2"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment:1.8.3")
    implementation("androidx.activity:activity:1.9.2")
    implementation("androidx.core:core:1.13.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Photos are stored as Base64 inside Firestore, so no Firebase Storage
    // (and no Blaze billing plan) is needed.
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
}
