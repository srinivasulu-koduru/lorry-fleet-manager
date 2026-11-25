import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { isStream ->
            load(isStream)
        }
    }
}
val devBackendHost: String = localProps.getProperty("dev.backend.host") ?: "10.0.2.2"
val devBackendPort: Int = (localProps.getProperty("dev.backend.port") ?: "8080").toInt()

android {
    namespace = "com.lorryowner.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.lorryowner.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PROD_BACKEND_URL", "\"https://bhp-logistics-backend.getvoroa.com/\"")
        buildConfigField("String", "DEV_BACKEND_HOST", "\"$devBackendHost\"")
        buildConfigField("int", "DEV_BACKEND_PORT", "$devBackendPort")
        val mapsApiKey = localProps.getProperty("maps.api.key") ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    signingConfigs {
        create("release") {
            var storeFilePath = providers.gradleProperty("BHP_RELEASE_STORE_FILE").orNull
            var storePass = providers.gradleProperty("BHP_RELEASE_STORE_PASSWORD").orNull
            var keyAl = providers.gradleProperty("BHP_RELEASE_KEY_ALIAS").orNull ?: "bhp-release"
            var keyPass = providers.gradleProperty("BHP_RELEASE_KEY_PASSWORD").orNull

            // Fallback: Read directly from credentials file outside Git if Gradle properties are not set
            if (storeFilePath.isNullOrBlank() || storePass.isNullOrBlank() || keyPass.isNullOrBlank()) {
                val userHome = System.getProperty("user.home")
                val credFile = File(userHome, ".android/keystores/bhp-release-credentials.txt")
                if (credFile.exists()) {
                    credFile.forEachLine { line ->
                        when {
                            line.startsWith("Keystore Password:") -> storePass = line.substringAfter(":").trim()
                            line.startsWith("Key Password:") -> keyPass = line.substringAfter(":").trim()
                            line.startsWith("Keystore Path:") -> {
                                val pathFromCred = line.substringAfter(":").trim()
                                if (pathFromCred.isNotBlank()) storeFilePath = pathFromCred
                            }
                            line.startsWith("Key Alias:") -> {
                                val aliasFromCred = line.substringAfter(":").trim()
                                if (aliasFromCred.isNotBlank()) keyAl = aliasFromCred
                            }
                        }
                    }
                }
            }

            if (storeFilePath.isNullOrBlank()) {
                val defaultKeystore = File(System.getProperty("user.home"), ".android/keystores/bhp-release.jks")
                if (defaultKeystore.exists()) {
                    storeFilePath = defaultKeystore.absolutePath
                }
            }

            if (!storeFilePath.isNullOrBlank()) {
                storeFile = File(storeFilePath)
                storePassword = storePass
                keyAlias = keyAl
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "BACKEND_BASE_URL", "\"https://bhp-logistics-backend.getvoroa.com/\"")
            buildConfigField("boolean", "IS_PRODUCTION", "true")
            isMinifyEnabled = false
        }
        debug {
            buildConfigField("String", "BACKEND_BASE_URL", "\"https://bhp-logistics-backend.getvoroa.com/\"")
            buildConfigField("boolean", "IS_PRODUCTION", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.materialIconsExtended)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.lifecycleViewModelCompose)
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}