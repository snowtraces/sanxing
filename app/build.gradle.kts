plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.snowtraces.sanxing"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.snowtraces.sanxing"
        minSdk = 24
        targetSdk = 24
        versionCode = 2
        versionName = "0.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    applicationVariants.all {
        outputs.all {
            val outputImpl = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            outputImpl?.outputFileName = "sanxing-v${versionName}-${buildType.name}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = "21" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    lint {
        // V1 is a locally installed APK with the reference client's target SDK.
        disable += "ExpiredTargetSdkVersion"
    }
    packaging {
        resources {
            pickFirsts += listOf(
                "lib/arm64-v8a/libonnxruntime.so",
                "lib/armeabi-v7a/libonnxruntime.so",
                "lib/x86/libonnxruntime.so",
                "lib/x86_64/libonnxruntime.so"
            )
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")
    implementation(platform("androidx.compose:compose-bom:2023.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("com.github.k2-fsa:sherpa-onnx:v2.0.1") {
        exclude(group = "com.github.k2-fsa.sherpa-onnx", module = "sherpa-onnx-jvm")
        exclude(group = "com.github.k2-fsa.sherpa-onnx", module = "sherpa-onnx-native-lib-linux-aarch64")
        exclude(group = "com.github.k2-fsa.sherpa-onnx", module = "sherpa-onnx-native-lib-linux-x64")
        exclude(group = "com.github.k2-fsa.sherpa-onnx", module = "sherpa-onnx-native-lib-osx-aarch64")
        exclude(group = "com.github.k2-fsa.sherpa-onnx", module = "sherpa-onnx-native-lib-osx-x64")
        exclude(group = "com.github.k2-fsa.sherpa-onnx", module = "sherpa-onnx-native-lib-win-arm64")
        exclude(group = "com.github.k2-fsa.sherpa-onnx", module = "sherpa-onnx-native-lib-win-x64")
    }
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20231013")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
