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

package dev.mwhenson.implFinder.plugin

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipFile

class ImplFinderPluginTest {

    @TempDir
    lateinit var testDir: File
    lateinit var settingsFile: File
    lateinit var buildFile: File
    lateinit var runner: GradleRunner

    @BeforeEach
    fun setup() {
        settingsFile = File(testDir, "settings.gradle.kts")
        settingsFile.writeText(
            """
            rootProject.name = "test-project"
            """.trimIndent()
        )

        buildFile = File(testDir, "build.gradle.kts")
        buildFile.writeText(
            """
            plugins {
                kotlin("jvm")  
                id("java-library")
                id("com.google.devtools.ksp")
                id("dev.mwhenson.impl-finder")
            }
            """.trimIndent()
        )

        buildFile.appendText(
            """
              project.tasks.register("printAppliedPlugins") {
                group = "help"
                  description = "Prints all plugins applied to this project"
                  doLast {
                    project.plugins.forEach { println(it::class.java.name) }
                  }
              }
              """.trimIndent()
        )

        runner = GradleRunner.create()
            .withProjectDir(testDir)
            .withPluginClasspath()
            .withDebug(true)
            .forwardOutput()
    }

    @Test
    fun `plugin applies successfully to a java-library project`() {
        val result = runner
            .withArguments("printAppliedPlugins")
            .build()

        assertTrue(result.output.contains("org.gradle.api.plugins.JavaLibraryPlugin"))
        assertTrue(result.output.contains("dev.mwhenson.implFinder.plugin.ImplFinderPlugin"))
    }

    @Test
    fun `manifest json is packaged into the produced jar`() {
        val manifestJson = """{"version":1,"entries":{}}"""
        val manifestDir = File(testDir, "build/generated/ksp/main/resources/dev/mwhenson/implFinder/manifest")
        manifestDir.mkdirs()
        File(manifestDir, "Manifest.json").writeText(manifestJson)

        val result = runner
            .withArguments("jar")
            .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))

        val jarFile = File(testDir, "build/libs").listFiles { file -> file.extension == "jar" }?.first()
        assertNotNull(jarFile, "Expected a jar to be produced in build/libs")

        ZipFile(jarFile!!).use { zip ->
            val entry = zip.getEntry("dev/mwhenson/implFinder/manifest/Manifest.json")
            assertNotNull(entry, "Expected Manifest.json to be present in the produced jar")

            val actualContent = zip.getInputStream(entry).bufferedReader().use { it.readText() }
            assertEquals(manifestJson, actualContent)
        }
    }

    @Test
    fun `generated jar's apiElements variant is marked with the CONTAINS_MANIFEST attribute`() {
        val result = runner
            .withArguments("outgoingVariants", "--variant", "apiElements")
            .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        assertTrue(
            result.output.contains(Regex(".*dev\\.mwhenson\\.implFinder\\.manifest\\s*= true")),
            "Expected the apiElements variant to declare the CONTAINS_MANIFEST attribute"
        )
        assertTrue(
            result.output.contains(".jar (artifactType = jar)"),
            "Expected the apiElements variant's artifact to be the produced jar"
        )
    }
}