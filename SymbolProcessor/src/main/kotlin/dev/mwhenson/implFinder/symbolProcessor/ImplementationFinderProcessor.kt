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

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import dev.mwhenson.implFinder.models.MutableManifest
import kotlinx.serialization.json.Json
import java.io.OutputStream

/**
 * TODO: Write Docs
 * TODO: Make incremental
 */
class ImplementationFinderProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    private val mutableManifest = MutableManifest(1, mutableMapOf())

    // 1 process() function
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val declarations = resolver
            .getSymbolsWithAnnotation("dev.mwhenson.implFinder.Implementation")
            .filter { it.validate() }
            .filterIsInstance<KSClassDeclaration>()

        if (declarations.none()) return emptyList()

        declarations.forEach { it.accept(ImplementationFinderVisitor(), Unit) }

        // TODO: Track dependencies by changing manifest to store declarations
        // TODO: Figure out duplicated file names. Should be able to prevent merging in final library somehow
        val file = codeGenerator.createNewFile(
            Dependencies(true, *declarations.mapNotNull { it.containingFile }.toList().toTypedArray()),
            "dev.mwhenson.implFinder.manifest",
            "Manifest",
            "json")

        val jsonString = Json.encodeToString(mutableManifest)
        file.write(jsonString)

        return emptyList()
    }

    // 2 Visitor 
    inner class ImplementationFinderVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val qualifiedClassName = classDeclaration.qualifiedName?.asString()
            if (qualifiedClassName.isNullOrBlank()) {
                logger.warn("$classDeclaration has no qualified name, bailing")
                return
            }

            val interfaces = classDeclaration.superTypes
                .map { it.resolve() }
                .filter { (it.declaration as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE }

            interfaces.forEach { iface ->
                val list = mutableManifest.entries.getOrPut(iface.declaration.qualifiedName!!.asString(), { mutableListOf() })
                list.add(qualifiedClassName)
            }
        }
    }
}

// Utility function for writing string to OutputStream
fun OutputStream.write(string: String): Unit {
    this.write(string.toByteArray())
}