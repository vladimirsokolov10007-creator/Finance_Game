import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "ru.finni.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ru.finni.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 5
        versionName = "0.5.0"
    }

    // Подпись релиза: параметры в keystore.properties (не коммитить!)
    val keystoreProps = Properties()
    val ksPropsFile = rootProject.file("keystore.properties")
    if (ksPropsFile.exists()) {
        ksPropsFile.inputStream().use { stream -> keystoreProps.load(stream) }
    }
    signingConfigs {
        create("release") {
            storeFile = ksPropsFile.let { rootProject.file(keystoreProps.getProperty("storeFile", "finni-release.jks")) }
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:economy"))
    implementation(project(":core:data"))
    implementation(project(":core:design"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
}
