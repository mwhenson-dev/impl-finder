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

package dev.mwhenson.implFinder.symbolProcessor

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import com.tschuchort.compiletesting.symbolProcessorProviders
import com.tschuchort.compiletesting.useKsp2
import dev.mwhenson.implFinder.models.Manifest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ImplementationFinderProcessorTest {
    @BeforeEach
    fun setUp() {

    }

    @AfterEach
    fun tearDown() {

    }

    @Test
    fun `Single Class - Single Contract`() {
        val kotlinSource = SourceBuilder("com.example", "Test.kt")
            .addClass("FooClass", listOf("FooInterface"), true)
            .createSourceFile()

        val result = performCompile(listOf(kotlinSource))

        val generatedFiles = result.sourcesGeneratedBySymbolProcessor.toList()
        val manifestJson = generatedFiles.first()

        assertEquals(1, generatedFiles.size, "Only expecting a single file to be made")
        assertEquals("Manifest.json", manifestJson.name, "Expect Manifest.json to be only file")

        val manifest = Json.decodeFromString<Manifest>(manifestJson.readText())
        val expected = Manifest(1, mapOf("com.example.FooInterface" to listOf("com.example.FooClass")))
        assertEquals(expected, manifest, "Manifest wasn't generated correctly")

        assertEquals(ExitCode.OK, result.exitCode)
    }

    @Test
    fun `Single Class - Multiple Contracts`() {
        val kotlinSource = SourceBuilder("com.example", "Test.kt")
            .addClass("FooClass", listOf("FooInterface", "BarInterface"), true)
            .createSourceFile()

        val result = performCompile(listOf(kotlinSource))

        val generatedFiles = result.sourcesGeneratedBySymbolProcessor.toList()
        val manifestJson = generatedFiles.first()

        assertEquals(1, generatedFiles.size, "Only expecting a single file to be made")
        assertEquals("Manifest.json", manifestJson.name, "Expect Manifest.json to be only file")

        val manifest = Json.decodeFromString<Manifest>(manifestJson.readText())
        val expected = Manifest(1, mapOf(
            "com.example.FooInterface" to listOf("com.example.FooClass"),
            "com.example.BarInterface" to listOf("com.example.FooClass"),
        ))
        assertEquals(expected, manifest, "Manifest wasn't generated correctly")

        assertEquals(ExitCode.OK, result.exitCode)
    }

    @Test
    fun `Multiple Classes - Multiple Contracts`() {
        val kotlinSource = SourceBuilder("com.example", "Test.kt")
            .addClass("FooClass", listOf("FooInterface"), true)
            .addClass("BarClass", listOf("BarInterface"), true)
            .createSourceFile()

        val result = performCompile(listOf(kotlinSource))

        val generatedFiles = result.sourcesGeneratedBySymbolProcessor.toList()
        val manifestJson = generatedFiles.first()

        assertEquals(1, generatedFiles.size, "Only expecting a single file to be made")
        assertEquals("Manifest.json", manifestJson.name, "Expect Manifest.json to be only file")

        val manifest = Json.decodeFromString<Manifest>(manifestJson.readText())
        val expected = Manifest(1, mapOf(
            "com.example.FooInterface" to listOf("com.example.FooClass"),
            "com.example.BarInterface" to listOf("com.example.BarClass")
        ))
        assertEquals(expected, manifest, "Manifest wasn't generated correctly")

        assertEquals(ExitCode.OK, result.exitCode)
    }

    @Test
    fun `Multiple Classes - Single Contract`() {
        val kotlinSource = SourceBuilder("com.example", "Test.kt")
            .addClass("FooClass", listOf("FooInterface"), true)
            .addClass("BarClass", listOf("FooInterface"), true)
            .createSourceFile()

        val result = performCompile(listOf(kotlinSource))

        val generatedFiles = result.sourcesGeneratedBySymbolProcessor.toList()
        val manifestJson = generatedFiles.first()

        assertEquals(1, generatedFiles.size, "Only expecting a single file to be made")
        assertEquals("Manifest.json", manifestJson.name, "Expect Manifest.json to be only file")

        val manifest = Json.decodeFromString<Manifest>(manifestJson.readText())
        val expected = Manifest(1, mapOf(
            "com.example.FooInterface" to listOf("com.example.FooClass", "com.example.BarClass")
        ))
        assertEquals(expected, manifest, "Manifest wasn't generated correctly")

        assertEquals(ExitCode.OK, result.exitCode)
    }
}

fun performCompile(sourceFiles: List<SourceFile>): JvmCompilationResult {
    return KotlinCompilation().run {
        useKsp2()
        inheritClassPath = true

        sources = sourceFiles
        symbolProcessorProviders = mutableListOf(ImplementationFinderProcessorProvider())
        compile()
    }
}