import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.zenimmersive.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zenimmersive.android"
        minSdk = 25
        //noinspection EditedTargetSdkVersion
        targetSdk = 36
        versionCode = 73
        versionName = "0.0.18"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        multiDexEnabled = true


        var formattedDate = SimpleDateFormat("ddMMYYYY-HHmm").format(Date())
        var formattedVersion = "$versionName"
        formattedVersion = formattedVersion.replace(".", "")
        setProperty(
            "archivesBaseName",
            "Zen-Immersive_Phase-1-" + formattedVersion + "_" + formattedDate
        )

        // Add Build config variable
        val buildTimeMillis = System.currentTimeMillis().toString()
        buildConfigField("String", "BUILD_TIME_MILLIS", "\"$buildTimeMillis\"")
    }



    signingConfigs {
        create("release") {
            storeFile = file(providers.gradleProperty("RELEASE_STORE_FILE").get())
            storePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").get()
        }
        getByName("debug") {
            storeFile = file(providers.gradleProperty("RELEASE_STORE_FILE").get())
            storePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.material)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)

    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.androidx.multidex)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //SP DP
    implementation(libs.intuit.sdp)
    implementation(libs.intuit.ssp)

    //Retrofit
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.converter)
    implementation(libs.squareup.okhttp3)
    implementation(libs.google.gson)


    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    annotationProcessor(libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)


    implementation(libs.androidx.swiperefreshlayout)


    implementation("com.github.skydoves:colorpickerview:2.3.0")

    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-exoplayer-dash:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")

    implementation("com.hbb20:ccp:2.7.3")

    implementation("androidx.mediarouter:mediarouter:1.8.1")
    implementation("com.google.android.gms:play-services-cast-framework:22.2.0")

    implementation("com.github.bumptech.glide:glide:5.0.5")
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")

    // Otp view
    implementation("io.github.chaosleung:pinview:1.4.4")

    //Google Auth
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-crashlytics-ndk")

    // In App Purchase
    implementation("com.android.billingclient:billing:8.0.0")
    implementation("com.google.android.play:integrity:1.5.0")

}