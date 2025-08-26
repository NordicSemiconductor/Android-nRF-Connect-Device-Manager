/*
 * Copyright (c) Runtime Inc., 2017-2018
 * Copyright (c) Intellinium SAS, 2014-2021
 * Copyright (c) Nordic Semiconductor ASA, 2021-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false

    // This plugin is used to generate Dokka documentation.
    alias(libs.plugins.kotlin.dokka) apply false
    // This applies Nordic look & feel to generated Dokka documentation.
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins/blob/main/plugins/src/main/kotlin/NordicDokkaPlugin.kt
    alias(libs.plugins.nordic.dokka) apply true

    // Nordic Gradle Plugins
    // https://github.com/NordicSemiconductor/Android-Gradle-Plugins
    alias(libs.plugins.nordic.application) apply false
    alias(libs.plugins.nordic.library) apply false
    alias(libs.plugins.nordic.hilt) apply false
    alias(libs.plugins.nordic.kotlin.android) apply false
    alias(libs.plugins.nordic.kotlin.jvm) apply false
    alias(libs.plugins.nordic.nexus.android) apply false
    alias(libs.plugins.nordic.nexus.jvm) apply false
}

// Configure main Dokka page
dokka {
    pluginsConfiguration.html {
        homepageLink.set("https://github.com/NordicSemiconductor/Android-nRF-Connect-Device-Manager")
    }
}
