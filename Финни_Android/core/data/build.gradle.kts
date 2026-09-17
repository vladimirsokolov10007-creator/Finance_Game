plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.finni.core.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

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
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines)
    api(libs.room.runtime) // тип RoomDatabase в публичных сигнатурах (Hilt-фабрики)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
