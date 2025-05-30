plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.tuan1android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tuan1android"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    configurations.all {
        exclude(group = "com.intellij", module = "annotations")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"  // Đảm bảo Kotlin cũng sử dụng JVM 11
    }
}

dependencies {
    implementation (libs.mpandroidchart)
    implementation(libs.converter)
    implementation(libs.guava)
    implementation(libs.compiler)
    //implementation(libs.room)
    implementation(libs.room.ktx)
    //implementation(libs.annotations)
    implementation(libs.room) {exclude (group = "com.intellij", module = "annotations")}
    kapt(libs.compiler)
    implementation(libs.gson)
    implementation(libs.retrofit)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.work.runtime)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
