plugins {
    alias(libs.plugins.android.library)
}

val screenLogVersion = providers.gradleProperty("screenLogVersion").orElse("1.0.0_SNAPSHOT")

android {
    namespace = "com.hexin.screenlog"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit)
}

tasks.register<Copy>("exportReleaseAar") {
    group = "distribution"
    description = "Builds and exports the versioned release AAR to the root artifacts directory."
    dependsOn("bundleReleaseAar")
    from(layout.buildDirectory.file("outputs/aar/screen-log-release.aar"))
    into(rootProject.layout.projectDirectory.dir("artifacts"))
    rename { "screen-log-${screenLogVersion.get()}.aar" }
}
