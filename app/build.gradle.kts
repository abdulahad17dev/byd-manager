plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.byd.vehiclecontrol"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.byd.vehiclecontrol"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildFeatures {
        aidl = true
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

//    packagingOptions {
//        resources {
//            excludes += setOf(
//                "android/**",
//                "androidx/**",
//                "com/android/**",
//                "dalvik/**",
//                "java/**",
//                "javax/**",
//                "org/apache/**",
//                "org/json/**",
//                "org/w3c/**",
//                "org/xml/**",
//                "org/xmlpull/**"
//            )
//        }
//    }
}

dependencies {
    compileOnly(files("libs/android.jar"))
    implementation("com.tananaev:adblib:1.3")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}