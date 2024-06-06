import com.android.sdklib.AndroidVersion.VersionCodes.UPSIDE_DOWN_CAKE

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.pitchmasterbeta"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nbarkoch.pitchmaster"
        minSdk = 24
        targetSdk = UPSIDE_DOWN_CAKE
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")


    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(files("libs/TarsosDSP-Android-2.4.jar"))

    /*Amazon Libraries*/
    val awsVersion = "2.73.0"
    implementation("com.amazonaws:aws-android-sdk-s3:$awsVersion")
    implementation("com.amazonaws:aws-android-sdk-cognito:2.16.1")
    implementation("com.amazonaws:aws-android-sdk-cognitoidentityprovider:$awsVersion")
    implementation("com.amazonaws:aws-android-sdk-lambda:$awsVersion")
    implementation("com.amazonaws:aws-android-sdk-core:$awsVersion")

    implementation("androidx.compose.material:material")
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.navigation:navigation-compose:2.8.0-beta02")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}