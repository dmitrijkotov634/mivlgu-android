plugins {
    id("com.android.application")
    kotlin("plugin.serialization") version "1.8.10"
    kotlin("android")
}

android {
    compileSdk = 33

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/license.txt")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/notice.txt")
        exclude("META-INF/ASL2.0")
        exclude("META-INF/*.kotlin_module")
    }

    defaultConfig {
        applicationId = "com.wavecat.mivlgu"
        minSdk = 24
        targetSdk = 33
        versionCode = 5
        versionName = "0.6"
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
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0-beta01")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("com.github.bumptech.glide:glide:4.13.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("androidx.preference:preference:1.2.0")
    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("io.ktor:ktor-client-android:2.0.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.0.2")
    implementation("io.ktor:ktor-client-content-negotiation:2.0.2")
    implementation("com.google.api-client:google-api-client:1.32.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation(files("libs/tomcat-util-10.1.7.jar"))
}