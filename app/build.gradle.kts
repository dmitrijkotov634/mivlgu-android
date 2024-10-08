plugins {
    id("com.android.application")
    id("kotlin-parcelize")

    kotlin("plugin.serialization") version "1.9.22"
    kotlin("android")
    kotlin("kapt")

    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    compileSdk = 34

    configurations {
        create("cleanedAnnotations")
        implementation {
            exclude(group = "org.jetbrains", module = "annotations-java5")
        }
    }

    defaultConfig {
        applicationId = "com.wavecat.mivlgu"
        minSdk = 25
        targetSdk = 34
        versionCode = 38
        versionName = "1.12"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }

    namespace = "com.wavecat.mivlgu"
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("org.jsoup:jsoup:1.15.4")
    implementation("androidx.preference:preference:1.2.1")
    implementation("io.ktor:ktor-client-core-jvm:2.3.6")
    implementation("io.ktor:ktor-client-android-jvm:2.3.6")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.6")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.6")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:syntax-highlight:4.6.2")
    implementation("io.noties.markwon:recycler-table:4.6.2")
    implementation("io.noties.markwon:ext-latex:4.6.2")
    implementation("io.noties.markwon:inline-parser:4.6.2")
    implementation("io.noties.markwon:image-glide:4.6.2")
    implementation("io.noties.markwon:image:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-core-ktx:2.8.4")
    implementation("androidx.work:work-runtime:2.9.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    kapt("io.noties:prism4j-bundler:2.0.0")
    implementation("io.noties:prism4j:2.0.0")
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-analytics-ktx:22.1.0")
    implementation("com.google.firebase:firebase-crashlytics-ktx:19.0.3")
    implementation("ru.rustore.sdk:remoteconfig:0.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("ru.rustore.sdk:billingclient:5.1.1")
}