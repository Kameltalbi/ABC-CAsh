import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun secret(name: String): String? =
    localProperties.getProperty(name) ?: providers.gradleProperty(name).orNull ?: System.getenv(name)

android {
    namespace = "com.abccash.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abccash.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 91
        versionName = "1.20.45"
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${secret("GOOGLE_WEB_CLIENT_ID").orEmpty()}\""
        )
        buildConfigField("boolean", "SIDELOAD_PRO", "false")
    }

    signingConfigs {
        create("release") {
            storeFile = secret("ABC_CASH_RELEASE_STORE_FILE")?.let { file(it) }
            storePassword = secret("ABC_CASH_RELEASE_STORE_PASSWORD")
            keyAlias = secret("ABC_CASH_RELEASE_KEY_ALIAS")
            keyPassword = secret("ABC_CASH_RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            setProperty("archivesBaseName", "AbcCash-V${defaultConfig.versionName}")
        }
        // APK bureau : même clé release, sans minify (évite crash ProGuard/Room au démarrage).
        create("sideload") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            matchingFallbacks += listOf("release", "debug")
            isMinifyEnabled = false
            isShrinkResources = false
            // APK de test hors Play Store : pas de billing, on active Pro pour tester.
            buildConfigField("boolean", "SIDELOAD_PRO", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
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
    val roomVersion = "2.6.1"
    val lifecycleVersion = "2.8.7"

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.navigation:navigation-compose:2.8.7")
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.8")

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.api-client:google-api-client-android:2.7.2")
    implementation("com.google.apis:google-api-services-drive:v3-rev20241027-2.0.0")

    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Google Play Billing Library
    implementation("com.android.billingclient:billing:7.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.json:json:20240303")
}

kapt {
    correctErrorTypes = true
}
