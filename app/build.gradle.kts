plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.blissless.animeclient"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.blissless.animeclient"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release")
            storePassword = "lucaacul9"
            keyAlias = "key0"
            keyPassword = "lucaacul9"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "src/main/keepRules/rules.keep" // <--- ADDED THIS LINE!
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.material.v1110)
    // For parsing the JSON from the extension
    implementation(libs.gson)
    // For running network calls in the background
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.okhttp)
}