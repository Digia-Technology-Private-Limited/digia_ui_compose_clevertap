plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}


version = "1.0.0-beta.1"

android {
    namespace = "com.digia.cleverTap"
    compileSdk = 36

    defaultConfig {
        minSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.clevertap.android.sdk)
    implementation(libs.digia.ui.compose)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])
                groupId = "com.digia"
                artifactId = "clevertap"
                version = version
            }
        }
    }
}