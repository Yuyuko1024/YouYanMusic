plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    id("kotlin-parcelize")
}

android {
    namespace = "com.youyuan.music.compose"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.youyuan.music.compose"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    packaging {
        resources {
            excludes += setOf(
                "**/sf_pro.ttf",
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            dexOptions {
                preDexLibraries = true
                dexInProcess = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {

    // Salt UI
    implementation(libs.salt.ui)
    implementation(libs.salt.core)
    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigation.compose.android)
    // Animation
    implementation(libs.androidx.animation.graphics)
    implementation(libs.androidx.animation.graphics.android)
    // AndroidX Media 3
    implementation(libs.androidx.media3.common.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // Squiggly Slider
    implementation(libs.squigglyslider)
    // Pinyin4j
    implementation(libs.pinyin4j)
    // Room database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Room annotation processor
    annotationProcessor(libs.androidx.room.compiler)
    // kapt room annotation processor
    ksp(libs.androidx.room.compiler)
    // javax inject
    implementation(libs.javax.inject)

    // Dagger - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Hilt Navigation Compose
    implementation (libs.androidx.hilt.navigation.compose)

    // Palettes
    implementation(libs.palette)
    implementation(libs.palette.ktx)

    // Accompanist Lyrics
    implementation(libs.lyrics.ui)
    implementation(libs.lyrics.core)

    // AndroidX DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // LazyColumn Scrollbar
    implementation(libs.lazycolumnscrollbar)

    // RenderScript Toolkit
    implementation(libs.renderscrip.toolkit)

    // Gson
    implementation(libs.gson)

    // Retrofit
    implementation(libs.retrofit)

    // XXPermissions
    // 设备兼容框架：https://github.com/getActivity/DeviceCompat
    implementation(libs.devicecompat)
    // 权限请求框架：https://github.com/getActivity/XXPermissions
    implementation(libs.xxpermissions)

    // Retrofit and Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // OkHttp for networking and logging
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Material
    implementation(libs.androidx.material)
    implementation(libs.androidx.material.icons)
    implementation("androidx.compose.material:material")

    // Icons
    implementation(libs.composeIcons.simpleIcons)
    implementation(libs.composeIcons.feather)
    implementation(libs.composeIcons.fontAwesome)
    implementation(libs.composeIcons.tablerIcons)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}