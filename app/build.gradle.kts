import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
}

val properties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "es.us.etsii_go"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "es.us.etsii_go"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AGREGAMOS AQUÍ LA API-KEY
        val apikey = properties.getProperty("API_KEY_GOOGLE") ?: "CLAVE_NO_ENCONTRADA"
        buildConfigField("String", "API_KEY_GOOGLE", "\"$apikey\"")
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

    // ACTIVAMOS LA GENERACIÓN DE LA CLASE BUILDCONFIG
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("com.google.android.libraries.places:places:3.3.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // ----- GPS y HORARIO

    // Para poder hacer zoom en el horario
    implementation(libs.zoomlayout)
    implementation(libs.recyclerview)
    implementation(libs.play.services.location)

    // Para la base de datos
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    annotationProcessor("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")

    // Para el selector de color
    implementation("com.github.yukuku:ambilwarna:2.0.1")

    // Para leer archivos json
    implementation("com.google.code.gson:gson:2.10.1")

    // En vez del pdf, se ve con mejor calidad
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")
}