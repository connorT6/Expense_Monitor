// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    kotlin("jvm") version "2.2.0" // Or your Kotlin version
    kotlin("plugin.serialization") version "2.2.0"
    alias(libs.plugins.google.firebase.crashlytics) apply false // Or your Kotlin version
}