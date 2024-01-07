plugins {
    id("com.android.application")
    id("kotlin-parcelize")

    kotlin("plugin.serialization") version "1.8.10"
    kotlin("android")
    kotlin("kapt")

    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
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
        minSdk = 24
        targetSdk = 34
        versionCode = 20
        versionName = "1.6"
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
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
    implementation("androidx.lifecycle:lifecycle-livedata-core-ktx:2.6.2")
    kapt("io.noties:prism4j-bundler:2.0.0")
    implementation("io.noties:prism4j:2.0.0")
    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
    implementation("com.google.firebase:firebase-analytics-ktx:21.5.0")
    implementation("com.google.firebase:firebase-crashlytics-ktx:18.6.0")
    implementation("ru.rustore.sdk:remoteconfig:0.0.1")
}