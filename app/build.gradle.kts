plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.voiceyanga.citizen"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.voiceyanga.citizen"
        minSdk = 24
        targetSdk = 36
        versionCode = 13
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://voice-yanga-api.vercel.app/api/v1/\"")
            buildConfigField("String", "API_ORIGIN", "\"https://voice-yanga-api.vercel.app\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"https://voice-yanga-api.vercel.app/api/v1/\"")
            buildConfigField("String", "API_ORIGIN", "\"https://voice-yanga-api.vercel.app\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.arch.core.testing)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // Lifecycle / ViewModel (the "VM" in MVVM)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Room (local database)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Retrofit (networking)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // RecyclerView (for complaint lists, later)
    implementation(libs.recyclerview)
    implementation(libs.swiperefreshlayout)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    // WorkManager
    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    annotationProcessor(libs.hilt.work.compiler)
    implementation(libs.glide)
    implementation(libs.glide.okhttp)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation(libs.play.services.location)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    implementation(libs.play.review)
    implementation(libs.play.update)
    implementation(libs.security.crypto)
    implementation(libs.shimmer)
    implementation(libs.biometric)
    implementation(libs.lottie)
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.media3:media3-transformer:1.3.1")
}