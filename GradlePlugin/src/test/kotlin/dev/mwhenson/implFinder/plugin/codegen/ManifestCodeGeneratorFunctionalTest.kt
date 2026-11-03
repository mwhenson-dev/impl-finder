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

package dev.mwhenson.implFinder.plugin.codegen

import dev.mwhenson.implFinder.models.Manifest
import dev.mwhenson.implFinder.plugin.functional.APP_PLUGINS
import dev.mwhenson.implFinder.plugin.functional.configureLibraryProjectAndGenerateSources
import dev.mwhenson.implFinder.plugin.functional.generateInterfaceProject
import dev.mwhenson.implFinder.plugin.functional.generateProject
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Exercises [ManifestCodeGenerator] against a [Manifest] produced by a real multi-module Gradle build - two
 * library modules and an application module, wired together exactly as [dev.mwhenson.implFinder.plugin.ImplFinderPlugin]
 * wires real consumers - rather than a hand-written [Manifest]. Where [ManifestCodeGeneratorTest] checks that
 * the generator renders plausible-looking source from manifests it's handed directly, this proves the source
 * it renders for a *real, cross-module merged* manifest actually compiles and behaves correctly: both
 * implementations, contributed by two separate libraries, are constructed and run from the application module.
 *
 * @author mwhenson
 */
class ManifestCodeGeneratorFunctionalTest {

    @TempDir
    lateinit var testDir: File

    private val repoRoot: File = File("..").canonicalFile

    @BeforeEach
    fun setup() {
        check(File(repoRoot, "Annotations").isDirectory) {
            "Expected to find the Annotations project at $repoRoot; is the GradlePlugin test working directory not the project directory?"
        }
    }

    @Test
    fun `generates code that constructs implementations contributed by two libraries and runs them from an application`() {
        val (root, _, rootSettings) = generateProject("root", testDir, emptyList())

        val ifaceDir = File(testDir, "interface").also { it.mkdirs() }
        val oneDir = File(testDir, "one").also { it.mkdirs() }
        val twoDir = File(testDir, "two").also { it.mkdirs() }
        val appDir = File(testDir, "app").also { it.mkdirs() }

        generateInterfaceProject("interface", ifaceDir)
        val (_, oneProject, _) = generateProject("one", oneDir)
        val (_, twoProject, _) = generateProject("two", twoDir)
        val (_, appProject, _) = generateProject("app", appDir, APP_PLUGINS)

        rootSettings.appendText(
            """
            rootProject.name = "root-project"
            include("interface")
            include("one")
            include("two")
            include("app")
            """.trimIndent()
        )

        configureLibraryProjectAndGenerateSources("one", oneDir, oneProject)
        configureLibraryProjectAndGenerateSources("two", twoDir, twoProject)

        appProject.appendText(
            """
            dependencies {
                implementation(project(":interface"))
                implementation(project(":one"))
                implementation(project(":two"))
                implementation("dev.mwhenson.impl-finder:Annotations")
            }

            application {
                mainClass.set("com.example.MainKt")
            }
            """.trimIndent()
        )

        val firstBuild = root.build()
        assertTrue(firstBuild.output.contains("BUILD SUCCESSFUL"), firstBuild.output)

        // This is the exact same merged manifest ImplFinderPlugin.configureJavaApplicationManifestMerging
        // produces for a real application module - i.e. the two libraries' manifests, combined.
        val mergedManifestFile = File(appDir, "build/generated/implFinder/Manifest.json")
        assertTrue(mergedManifestFile.exists(), "Expected a merged manifest at $mergedManifestFile")

        val manifest = Json.decodeFromString<Manifest>(mergedManifestFile.readText())
        val implementations = manifest.entries["com.example.Greeter"].orEmpty()
        assertTrue(
            implementations.containsAll(listOf("com.example.GreeterOne", "com.example.GreeterTwo")),
            "Expected the merged manifest to contain implementations from both libraries, got: $implementations"
        )

        val appSourceDir = File(appDir, "src/main/kotlin")
        ManifestCodeGenerator().generate(manifest, appSourceDir)

        File(appSourceDir, "com/example/Main.kt").apply { parentFile.mkdirs() }.writeText(
            """
            package com.example

            import dev.mwhenson.implFinder.generated.GeneratedImplFinder

            fun main() {
                val greetings = GeneratedImplFinder.newInstances("com.example.Greeter")
                    .map { (it as Greeter).greet() }
                    .sorted()
                println(greetings.joinToString(","))
            }
            """.trimIndent()
        )

        val runResult = root.withArguments(":app:run").build()

        assertTrue(runResult.output.contains("BUILD SUCCESSFUL"), runResult.output)
        assertTrue(
            runResult.output.contains("Hello From One,Hello From Two"),
            "Expected the generated code to construct and run both libraries' implementations, got:\n${runResult.output}"
        )
    }
}
