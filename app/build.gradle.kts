import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Klucz Gemini API czytany z local.properties (plik nieśledzony przez git, tak jak sdk.dir)
// - nigdy nie trafia do repozytorium ani nie jest wpisany na sztywno w kodzie Kotlin. Brak
// wpisu => pusty string => wywołania API kończą się czytelnym błędem obsłużonym w
// AiReadingRepository, zamiast wycieku klucza w kodzie źródłowym.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        FileInputStream(file).use { load(it) }
    }
}
val tarotApiKey: String = (localProperties.getProperty("TAROT_API_KEY") ?: "").ifBlank {
    System.getenv("TAROT_API_KEY") ?: ""
}

// Bezpiecznik publikacji: wywołania Gemini idą dziś bezpośrednio z klienta (klucz w BuildConfig,
// patrz TODO na górze AiReadingRepository.kt). Domyślnie WŁĄCZONE, żeby nie zepsuć bieżącego
// developmentu/testów, ale to świadoma, widoczna flaga - a nie coś, co "po prostu działa" i łatwo
// przeoczyć przy przejściu na backend/Cloud Function. Ustaw ALLOW_DIRECT_GEMINI_CLIENT=false w
// local.properties, żeby zbudować wariant, w którym ścieżka bezpośredniego klienta jest zablokowana.
val allowDirectGeminiClient: String = (localProperties.getProperty("ALLOW_DIRECT_GEMINI_CLIENT") ?: "true").ifBlank { "true" }

// AdMob: identyfikatory produkcyjne czytane z local.properties (ADMOB_APP_ID, ADMOB_REWARDED_UNIT_ID)
// - jeśli brak wpisu (np. lokalny build kogoś bez dostępu do konta AdMob), spadamy na oficjalne
// testowe ID Google zamiast zostawić puste/nieprawidłowe wartości w Manifeście. Testowe ID są też
// używane wprost dla wariantu debug - nigdy więcej ręcznej podmiany stałych w kodzie Kotlin.
val TEST_ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713"
val TEST_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
val prodAdmobAppId: String = (localProperties.getProperty("ADMOB_APP_ID") ?: "").ifBlank { TEST_ADMOB_APP_ID }
val prodRewardedUnitId: String = (localProperties.getProperty("ADMOB_REWARDED_UNIT_ID") ?: "").ifBlank { TEST_REWARDED_UNIT_ID }

// Podpisywanie wersji release (Play Console) - plik keystore i hasla czytane z
// local.properties (nigdy nie trafiaja do repozytorium, tak jak TAROT_API_KEY powyzej).
val releaseStoreFile: String = localProperties.getProperty("RELEASE_STORE_FILE") ?: ""
val releaseStorePassword: String = localProperties.getProperty("RELEASE_STORE_PASSWORD") ?: ""
val releaseKeyAlias: String = localProperties.getProperty("RELEASE_KEY_ALIAS") ?: ""
val releaseKeyPassword: String = localProperties.getProperty("RELEASE_KEY_PASSWORD") ?: ""
val hasReleaseSigningConfig: Boolean = releaseStoreFile.isNotBlank() && releaseStorePassword.isNotBlank() &&
    releaseKeyAlias.isNotBlank() && releaseKeyPassword.isNotBlank()

android {
    namespace = "com.mazur.tarot"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tarot.mistiq"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "1.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "TAROT_API_KEY", "\"$tarotApiKey\"")
        buildConfigField("boolean", "ALLOW_DIRECT_GEMINI_CLIENT", allowDirectGeminiClient)
        buildConfigField("String", "PROD_ADMOB_APP_ID", "\"$prodAdmobAppId\"")
        buildConfigField("String", "PROD_REWARDED_UNIT_ID", "\"$prodRewardedUnitId\"")
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            manifestPlaceholders["admobAppId"] = prodAdmobAppId
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = TEST_ADMOB_APP_ID
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
}

dependencies {
    // Core / Lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // androidx.fragment nie jest użyte bezpośrednio - to zależność tranzytywna po AdMob/Play
    // Services (play-services-base), które ciągną nieaktualne 1.1.0 (oznaczone przez Google
    // jako outdated w Play Console). Wymuszamy nowszą wersję jawną deklaracją, żeby resolucja
    // Gradle wybrała ją zamiast tranzytywnej.
    implementation("androidx.fragment:fragment-ktx:1.8.4")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Billing
    implementation("com.android.billingclient:billing-ktx:8.3.0")

    // AdMob (Rewarded Ads - alternatywa dla PRO przy jednym zapytaniu do AI)
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // Google Play In-App Review (prośba o ocenę po 3. udanym odczycie)
    implementation("com.google.android.play:review:2.0.1")

    // Gemini API (spersonalizowane odczyty w "Zapytaj Kart")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
