plugins {
    alias(libs.plugins.android.library)
}

val screenLogVersion = providers.gradleProperty("screenLogVersion").orElse("1.0.5_SNAPSHOT")

// Keep the exported AAR readable by Kotlin 2.0 consumers.
kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
        apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0
    }
}

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
    // 版本号参与增量检查，避免改版后导出任务被误判为无需执行。
    inputs.property("screenLogVersion", screenLogVersion)
    from(layout.buildDirectory.file("outputs/aar/screen-log-release.aar"))
    into(rootProject.layout.projectDirectory.dir("artifacts"))
    rename { "screen-log-${screenLogVersion.get()}.aar" }
}
