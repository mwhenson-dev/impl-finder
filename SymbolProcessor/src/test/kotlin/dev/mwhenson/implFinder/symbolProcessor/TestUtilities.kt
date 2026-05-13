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

import com.tschuchort.compiletesting.SourceFile
import dev.mwhenson.implFinder.Implementation

class SourceBuilder(val packageName: String, val fileName: String) {
    private val classes = mutableListOf<SourceClass>()
    private val contracts = mutableSetOf<String>()

    fun addClass(className: String, contractNames: List<String>, annotated: Boolean): SourceBuilder {
        contracts.addAll(contractNames)
        classes.add(SourceClass(className, contractNames, annotated))
        return this
    }

    fun createSourceFile(): SourceFile {
        return SourceFile.kotlin(fileName, """
            package $packageName
            ${contracts.sorted().joinToString("\n") { "interface $it" }}
            ${classes.joinToString("\n") { it.asSource() }}
        """.trimIndent())
    }
}

private data class SourceClass(val className: String, val contractNames: List<String>, val annotated: Boolean) {
    fun asSource(): String {
        return """
            ${if (annotated) { "@" + Implementation::class.qualifiedName } else {""} }
            class $className ${ if (contractNames.isEmpty()) {""} else {": " + contractNames.joinToString(", ")}} {
                fun foo() { }
            }
        """.trimIndent()
    }
}