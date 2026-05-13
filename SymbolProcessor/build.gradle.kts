/*
 *  Copyright (c) 2026 Mathew Henson
 *
 *  This file is part of impl-finder.
 *
 *  impl-finder is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 *  Public License as published by the Free Software Foundation, either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  impl-finder is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 *  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with impl-finder.
 *  If not, see <https://www.gnu.org/licenses/>.
 */

import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("jvm")
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinPluginSerialization)
}

group = "dev.mwhenson.impl-finder"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Annotations"))
    implementation(libs.kspProcessingApi)
    implementation(libs.kotlinxSerialization)

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("dev.zacsweers.kctfork:ksp:0.12.1")
}

tasks.test {
    useJUnitPlatform()
}

// Opt In to some APIs for compiler testing
tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions.optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
}
