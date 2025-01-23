// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) version "8.7.3" apply false // Updated to a valid version
    alias(libs.plugins.kotlin.android) version "1.9.24" apply false    // Updated Kotlin plugin version
    alias(libs.plugins.google.gms.google.services)version "4.4.2" apply false        // No version specified here; ensure it's managed properly in libs.versions.toml or pluginManagement
}
