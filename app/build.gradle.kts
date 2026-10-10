import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

// API settings. Override per machine with -P<name>=<value> or in ~/.gradle/gradle.properties.
// 10.0.2.2 is the host machine seen from the Android emulator; 5000 is the port of the contract's local server.
val debugApiBaseUrl = providers.gradleProperty("hiposim.apiBaseUrl.debug").getOrElse("http://10.0.2.2:5000/")
val releaseApiBaseUrlPlaceholder = "https://api.hiposim.invalid/"
val releaseApiBaseUrl = providers.gradleProperty("hiposim.apiBaseUrl.release").getOrElse(releaseApiBaseUrlPlaceholder)

// true: repositories answer with in-memory fakes shaped like the contract. false: they call the API.
// Stays true by default until the API host (hiposim-platform) exists.
val useFakeApi = providers.gradleProperty("hiposim.useFakeApi").getOrElse("true").toBoolean()

// true: the list of agencies is the demo agency of the backend seed, kept in memory, even when the rest of the app
// talks to the real API. Only needed against a backend older than GET /api/agencies.
val useFakeAgencies = providers.gradleProperty("hiposim.useFakeAgencies").getOrElse("false").toBoolean()

// Release signing. The keystore and its passwords live OUTSIDE the repository: a properties file with storeFile,
// storePassword, keyAlias and keyPassword, located by -Phiposim.signing.propertiesFile=<path> (for example in
// ~/.gradle/gradle.properties) or by the HIPOSIM_SIGNING_PROPERTIES environment variable. Without it the release
// APK is built unsigned, which is what CI does. See docs/firebase-app-distribution.md.
val signingPropertiesFile =
    providers
        .gradleProperty("hiposim.signing.propertiesFile")
        .orElse(providers.environmentVariable("HIPOSIM_SIGNING_PROPERTIES"))
        .map { file(it) }
        .orNull
val signingProperties =
    signingPropertiesFile?.takeIf { it.isFile }?.let { propertiesFile ->
        Properties().apply { propertiesFile.inputStream().use { load(it) } }
    }

val appVersionName = "0.1.0"

// "demo" answers from memory and works without a server; "api" calls the API at API_BASE_URL.
base {
    archivesName.set("hiposim-$appVersionName-${if (useFakeApi) "demo" else "api"}")
}

android {
    namespace = "com.auracode.hiposim"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.auracode.hiposim"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = appVersionName

        testInstrumentationRunner = "com.auracode.hiposim.HiltTestRunner"

        buildConfigField("boolean", "USE_FAKE_API", useFakeApi.toString())
        buildConfigField("boolean", "USE_FAKE_AGENCIES", useFakeAgencies.toString())
    }

    signingConfigs {
        signingProperties?.let { properties ->
            create("release") {
                // A relative storeFile is resolved from the folder of the properties file.
                storeFile = signingPropertiesFile!!.parentFile.resolve(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
    }
}

// A release that calls the API must know where the API is: no placeholder and no cleartext.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    val callsTheApi = !useFakeApi
    val url = releaseApiBaseUrl
    val placeholder = releaseApiBaseUrlPlaceholder
    doFirst {
        if (callsTheApi && (url == placeholder || !url.startsWith("https://") || !url.endsWith("/"))) {
            throw GradleException(
                "A release with hiposim.useFakeApi=false needs -Phiposim.apiBaseUrl.release=https://<host>/ " +
                    "(HTTPS, ending in a slash). Current value: $url",
            )
        }
    }
}

ktlint {
    android.set(false)
    ignoreFailures.set(false)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
