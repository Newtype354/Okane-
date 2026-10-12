plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.okane"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.okane"
        minSdk = 26
        // targetSdk 33: matches the primary test device (Android 13) and keeps
        // runtime behavior rules at the API 33 level. compileSdk stays at the
        // template default because the AndroidX libraries in this project require it.
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 strips unused code from Compose, Room, Hilt and coroutines
            // and drops unused resources: the installed app is a fraction of
            // the unshrunk size. Every library here ships its own keep rules.
            isMinifyEnabled = true
            isShrinkResources = true
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
        compose = true
    }

    // MigrationTestHelper reads the exported schema JSON from the test APK's
    // assets, so the schemas directory has to be packaged into androidTest.
    sourceSets.getByName("androidTest") {
        assets.directories.add("$projectDir/schemas")
    }
}

// Room writes the generated schema JSON here on every build. It is what makes
// a hand-written migration checkable: the CREATE TABLE in Migrations.kt must
// match 3.json exactly, or Room throws on open at runtime.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Coroutines: async work off the main thread (database writes, flows).
    implementation(libs.kotlinx.coroutines.android)

    // Room: local SQLite database. runtime = the library the app uses,
    // compiler (via ksp) = generates the SQL implementation code at build time.
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Hilt: dependency injection. Same split: runtime library + code generator.
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}