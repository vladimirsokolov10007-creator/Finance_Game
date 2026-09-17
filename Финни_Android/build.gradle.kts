plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// JVM на Windows читает @argfile тестовых worker в платформенной кодировке (CP1251),
// а Gradle пишет его в UTF-8: classpath с кириллическими сегментами пути проекта
// ломает загрузку классов тестов. Перенаправляем build-директории в ASCII-путь.
// Переопределить можно: ./gradlew -Pfinni.build.root=D:/builds
val finniBuildRoot = providers.gradleProperty("finni.build.root")
    .orElse("C:/Users/vovas/.finni_build")

subprojects {
    layout.buildDirectory.set(
        layout.dir(
            finniBuildRoot.map { root ->
                file("$root/${project.path.trim(':').replace(':', '/')}")
            }
        )
    )
}
