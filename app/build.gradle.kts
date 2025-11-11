import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "me.miguelantonyortegasanta.tradumemo"
    compileSdk = 34   // 35/34 are safer; 36 todavía no es estándar

    defaultConfig {
        applicationId = "me.miguelantonyortegasanta.tradumemo"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --- Leer GOOGLE_API_KEY desde local.properties ---
        val localProps = project.rootProject.file("local.properties")
        val props = Properties()
        if (localProps.exists()) {
            props.load(localProps.inputStream())
        }
        val googleKey = props.getProperty("GOOGLE_API_KEY") ?: ""
        buildConfigField("String", "GOOGLE_API_KEY", "\"$googleKey\"")
        val openRouterKey = props.getProperty("OPENROUTER_API_KEY") ?: ""
        buildConfigField("String", "OPENROUTER_API_KEY", "\"$openRouterKey\"")
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
    kotlinOptions {
        jvmTarget = "11"
    }
    composeOptions {
        // Works with Kotlin 1.9.22 + Compose BOM 2024.x
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

dependencies {
    // --- Core / Compose básicos ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.firebase.auth.ktx.v2310)
    implementation(libs.firebase.common.ktx.v2100)
    implementation(libs.firebase.firestore.ktx)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // --- Networking / coroutines para Google STT REST ---
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
}
