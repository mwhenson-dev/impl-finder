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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ManifestCodeGeneratorTest {

    private val generator = ManifestCodeGenerator()

    @Test
    fun `generates a when branch per contract that constructs each of its implementations`() {
        val manifest = Manifest(
            version = 1,
            entries = mapOf(
                "com.example.Contract" to listOf("com.example.Impl1", "com.example.Impl2")
            )
        )

        val source = generator.generate(manifest)

        assertTrue(
            source.contains(
                "\"com.example.Contract\" -> LazyImplementations<com.example.Contract>(listOf({ com.example.Impl1() }, { com.example.Impl2() }))"
            ),
            "Expected a branch constructing both implementations, got:\n$source"
        )
    }

    @Test
    fun `generates one branch per contract when the manifest has multiple entries`() {
        val manifest = Manifest(
            version = 1,
            entries = mapOf(
                "com.example.ContractOne" to listOf("com.example.ImplOne"),
                "com.example.ContractTwo" to listOf("com.example.ImplTwo")
            )
        )

        val source = generator.generate(manifest)

        assertTrue(source.contains("\"com.example.ContractOne\" -> LazyImplementations<com.example.ContractOne>(listOf({ com.example.ImplOne() }))"))
        assertTrue(source.contains("\"com.example.ContractTwo\" -> LazyImplementations<com.example.ContractTwo>(listOf({ com.example.ImplTwo() }))"))
    }

    @Test
    fun `falls back to an empty, already-lazy collection for unknown contracts`() {
        val manifest = Manifest(version = 1, entries = emptyMap())

        val source = generator.generate(manifest)

        assertTrue(source.contains("else -> LazyImplementations<Any>(emptyList())"))
    }

    @Test
    fun `types each branch's LazyImplementations as its own contract, and the function as Any`() {
        val manifest = Manifest(version = 1, entries = mapOf("com.example.Contract" to listOf("com.example.Impl")))

        val source = generator.generate(manifest)

        assertTrue(source.contains("public fun newInstances(contractName: String): LazyImplementations<Any> = when (contractName) {"))
        assertTrue(source.contains("LazyImplementations<com.example.Contract>(listOf({ com.example.Impl() }))"))
    }

    @Test
    fun `imports LazyImplementations rather than declaring it`() {
        val manifest = Manifest(version = 1, entries = emptyMap())

        val source = generator.generate(manifest)

        assertTrue(source.contains("import dev.mwhenson.implFinder.runtime.LazyImplementations"))
        assertTrue(
            !source.contains("class LazyImplementations"),
            "Expected LazyImplementations to be imported, not declared, got:\n$source"
        )
    }

    @Test
    fun `declares the configured package and object name`() {
        val generator = ManifestCodeGenerator(packageName = "com.example.generated", objectName = "MyRegistry")

        val source = generator.generate(Manifest(version = 1, entries = emptyMap()))

        assertTrue(source.contains("package com.example.generated"))
        assertTrue(source.contains("object MyRegistry {"))
    }

    @Test
    fun `writes the generated source beneath the package's directory structure`(@TempDir tempDir: File) {
        val generator = ManifestCodeGenerator(packageName = "com.example.generated", objectName = "MyRegistry")
        val manifest = Manifest(version = 1, entries = mapOf("com.example.Contract" to listOf("com.example.Impl")))

        val written = generator.generate(manifest, tempDir)

        val expectedFile = File(tempDir, "com/example/generated/MyRegistry.kt")
        assertEquals(expectedFile, written)
        assertTrue(expectedFile.exists())
        assertEquals(generator.generate(manifest), expectedFile.readText())
    }
}
