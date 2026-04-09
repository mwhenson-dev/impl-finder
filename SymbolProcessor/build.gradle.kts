plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
}

group = "dev.mwhenson.impl-finder"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Annotations"))
    implementation(libs.kspProcessingApi)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}