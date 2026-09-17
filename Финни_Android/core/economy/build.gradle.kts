plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    testImplementation(kotlin("test"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}

tasks.test {
    useJUnitPlatform()
    // Windows с русской локалью: JVM читает @argfile в CP1251, а Gradle пишет его в UTF-8.
    // Кириллический путь проекта ломает classpath тестового worker без этих флагов.
    jvmArgs("-Dsun.jnu.encoding=UTF-8", "-Dfile.encoding=UTF-8")
}
