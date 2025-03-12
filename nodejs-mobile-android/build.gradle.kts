plugins {
    id("com.android.library")
}

android {
    namespace = "com.nodejs.mobile"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
            java.srcDirs("src/main/java")
            jniLibs.srcDirs("libnode/bin/")
            resources.srcDirs("src/main/resources")
        }
    }
} 