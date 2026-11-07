plugins {
    id("com.android.application")
    id("com.chaquo.python")
}

android {
    namespace = "com.aira.agentwrapper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aira.agentwrapper"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

chaquopy {
    defaultConfig {
        version = "3.13"
    }
}
