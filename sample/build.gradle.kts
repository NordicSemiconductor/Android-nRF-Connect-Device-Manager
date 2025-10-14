/*
 * Copyright (c) Nordic Semiconductor ASA, 2018-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    alias(libs.plugins.nordic.application)
}

android {
    namespace = "io.runtime.mcumgr.sample"

    defaultConfig {
        applicationId = "no.nordicsemi.android.nrfconnectdevicemanager"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Mcu Mgr
    implementation(project(":mcumgr-ble"))

    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)

    // Dagger
    implementation(libs.dagger)
    implementation(libs.dagger.android)
    implementation(libs.dagger.android.support)
    annotationProcessor(libs.dagger.compiler)
    annotationProcessor(libs.dagger.android.processor)

    // Brings the new BluetoothLeScanner API to older platforms
    implementation(nordic.compat.scanner)

    // Timber & SLF4J
    implementation(libs.slf4j.timber)
    implementation(nordic.log.timber)

    // GSON
    implementation(libs.gson)

    // Test
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    testImplementation(libs.junit4)
}
