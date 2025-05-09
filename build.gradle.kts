
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.3.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id ("androidx.navigation.safeargs") version "2.5.3" apply false
    id ("com.google.gms.google-services") version "4.4.0" apply false
    id ("com.google.firebase.crashlytics") version "2.9.9" apply false
    id ("com.google.firebase.firebase-perf") version "1.4.2" apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1")
    }
}