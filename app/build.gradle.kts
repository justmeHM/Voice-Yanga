plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.voiceyanga.citizen"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.voiceyanga.citizen"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.arch.core.testing)
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
    annotationProcessor(libs.glide) // Glide annotation processor is optional for simple use but good to have
    implementation(libs.play.services.location)
    implementation(libs.firebase.messaging)
    implementation(libs.security.crypto)
    implementation(libs.shimmer)
    implementation(libs.biometric)
    implementation(libs.lottie)
}