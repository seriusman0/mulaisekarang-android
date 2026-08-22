import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services") apply false
    id("com.google.firebase.crashlytics") apply false
}

// Crashlytics needs a real Firebase project's google-services.json (Firebase Console, same
// GCP project as the Google Sign-In OAuth client — see README's Google Sign-In section) — an
// external console step this repo can't automate, mirroring keystore.properties below. Both
// plugins are applied only when that file exists so a fresh checkout still builds.
val hasGoogleServices = rootProject.file("app/google-services.json").exists()
if (hasGoogleServices) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

// Release signing credentials, kept out of the repo — see keystore.properties.example / README.md.
// Absent on machines that only need debug builds; release signingConfig is simply left unusable then.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.mulaisekarang.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mulaisekarang.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"

        // Same Web OAuth client used by the Laravel backend (config/services.php `google.client_id`).
        // Google ID tokens requested on Android must use this as the audience so the backend can verify them.
        // Must be the "Web application" OAuth client type (not "Android") — see mulai-sekarang-auth
        // project, client "Web Client Self Hosted". Using an Android-type client ID here causes
        // DEVELOPER_ERROR (status 10) on legacy GoogleSignIn and a silent cancellation on Credential Manager.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"785712319782-ps89gis6i0uc83kjesjg9k0qtj1nlue8.apps.googleusercontent.com\"",
        )
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Local Laravel dev server (php artisan serve --host=0.0.0.0), reached over Tailscale by default.
            // Override with -PapiBaseUrl=http://10.0.2.2:8001/api/v1/ when testing on an emulator.
            val apiBaseUrl = (project.findProperty("apiBaseUrl") as String?)
                ?: "https://mulaisekarang.com/api/v1/"
            buildConfigField("String", "BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://mulaisekarang.com/api/v1/\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = false
            all {
                it.maxHeapSize = "2g"
                it.testLogging {
                    events("passed", "failed", "skipped")
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    implementation("androidx.browser:browser:1.8.0")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-paging:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.paging:paging-runtime-ktx:3.3.0")
    implementation("androidx.paging:paging-compose:3.3.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    if (hasGoogleServices) {
        implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
        implementation("com.google.firebase:firebase-crashlytics-ktx")
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    testImplementation("androidx.room:room-testing:2.6.1")
}
