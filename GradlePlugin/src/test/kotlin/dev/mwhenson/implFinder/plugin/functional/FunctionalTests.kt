/*
 * 2026 Mathew Henson
 *
 * This file is part of impl-finder.
 *
 * impl-finder is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 2.1 of the License,
 * or (at your option) any later version.
 *
 * impl-finder is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with impl-finder.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 */

package dev.mwhenson.implFinder.plugin.functional

import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

// All of these are just to make things a bit more readable and checkable
private typealias BuildFile = File
private typealias SettingsFile = File
private typealias TestProject = Triple<GradleRunner, BuildFile, SettingsFile>
val TestProject.runner get() = this.first
val TestProject.buildFile get() = this.second
val TestProject.settingsFile get() = this.third

class FunctionalTests {
    @TempDir
    lateinit var testDir: File

    @BeforeEach
    fun setup() {
        check(File(repoRoot, "Annotations").isDirectory) {
            "Expected to find the Annotations project at $repoRoot; is the GradlePlugin test working directory not the project directory?"
        }
    }

    @Test
    fun `Combine Multiple Projects into Single Application`() {
        val (root, rootProject, rootSettings) = generateProject("root", testDir, emptyList())

        val ifaceDir = File(testDir, "interface").also { it.mkdirs() }
        val oneDir = File(testDir, "one").also { it.mkdirs() }
        val twoDir = File(testDir, "two").also { it.mkdirs() }
        val appDir = File(testDir, "app").also { it.mkdirs() }

        val (iface, ifaceProject, ifaceSettings) = generateInterfaceProject("interface", ifaceDir)
        val (one, oneProject, oneSettings) = generateProject("one", oneDir)
        val (two, twoProject, twoSettings) = generateProject("two", twoDir)
        val (app, appProject, appSettings) = generateProject("app", appDir, APP_PLUGINS)

        rootSettings.appendText("""
            rootProject.name = "root-project"
            include("interface")
            include("one")
            include("two")
            include("app")
        """.trimIndent())

        configureLibraryProjectAndGenerateSources("one", oneDir, oneProject)
        configureLibraryProjectAndGenerateSources("two", twoDir, twoProject)

        appProject.appendText("""
            dependencies {
                implementation(project(":interface"))
                implementation(project(":one"))
                implementation(project(":two"))
            }
        """.trimIndent())

        val appBuild = root.build()
        assertTrue(appBuild.output.contains("BUILD SUCCESSFUL"))
    }
}

fun configureLibraryProjectAndGenerateSources(name: String, testDir: File, projectFile: File) {
    projectFile.appendText("""
        dependencies {
            implementation(project(":interface"))
            implementation("dev.mwhenson.impl-finder:Annotations")
            ksp("dev.mwhenson.impl-finder:SymbolProcessor")
        }
    """.trimIndent())

    val capName = name.capitalized()
    val sourceDir = File(testDir, "src/main/kotlin/com/example")
    sourceDir.mkdirs()
    File(sourceDir, "Greeter$capName.kt").writeText(
        """
            package com.example
            
            import dev.mwhenson.implFinder.Implementation

            @Implementation
            class Greeter$capName : Greeter {
                override fun greet() = "Hello From $capName"
            }
        """.trimIndent()
    )
}

fun generateInterfaceProject(name: String, testDir: File): TestProject {
    val sourceDir = File(testDir, "src/main/kotlin/com/example")
    sourceDir.mkdirs()
    File(sourceDir, "Greeter.kt").writeText(
        """
            package com.example

            interface Greeter {
                fun greet(): String
            }
            """.trimIndent()
    )

    val project = generateProject(name, testDir, listOf("java-library"))

    project.buildFile.appendText("""
        dependencies {
            implementation("dev.mwhenson.impl-finder:Annotations")
        }
    """.trimIndent())

    return project
}

val LIBRARY_PLUGINS = listOf("java-library", "com.google.devtools.ksp", "dev.mwhenson.impl-finder")
val APP_PLUGINS = listOf("application", "com.google.devtools.ksp", "dev.mwhenson.impl-finder")
private val repoRoot: File = File("..").canonicalFile

fun generateProject(name: String, testDir: File, plugins: List<String> = LIBRARY_PLUGINS): TestProject {
    val settingsFile = File(testDir, "settings.gradle.kts")
    settingsFile.writeText(
        """
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                    google()
                }
            }
            
            includeBuild("${repoRoot.invariantSeparatorsPath}")
            
        """.trimIndent()
    )

    val buildFile = File(testDir, "build.gradle.kts")
    buildFile.writeText(
        """
        plugins {
            kotlin("jvm")  
            ${ plugins.joinToString("\n") { "id(\"$it\")" } }
        }
        
        """.trimIndent()
    )

    val runner = GradleRunner.create().apply {
        withProjectDir(testDir)
        withPluginClasspath()
        withDebug(true)
        withArguments("build")
        forwardOutput()
    }

    return TestProject(runner, buildFile, settingsFile)
}
