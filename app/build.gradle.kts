import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.connort6.expensemonitor"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.connort6.expensemonitor"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug") // Replace with your release signing config
        }
    }

    // Specifies one flavor dimension.
    flavorDimensions += "version"
    productFlavors {
        create("beta") {
            dimension = "version"
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
        }
        create("full") {
            dimension = "version"
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(11) // This sets the JDK version for compilation and the JVM target

    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_2) // Or your desired Kotlin language version
        // You can set other compiler options here if needed
        // For example:
        // freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.animation.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.material3)
    implementation(libs.androidx.material3.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.material.icons.extended)
    implementation (libs.charts)
}