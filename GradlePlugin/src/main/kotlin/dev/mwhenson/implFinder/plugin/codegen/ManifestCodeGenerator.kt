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
import java.io.File

/**
 * Turns a resolved [Manifest] into the source of a Kotlin file that can create new instances of the
 * implementations it describes. The generated file declares a single `object` with a `newInstances`
 * function that, given a contract's fully qualified class name, returns a [LazyImplementations]
 * wrapping every implementation the manifest registered against it.
 *
 * [LazyImplementations] is `Iterable`, and none of those implementations are constructed until an
 * iterator reaches them; from that point on, the same [LazyImplementations] always returns the same
 * object for that implementation.
 *
 * Each branch constructs its [LazyImplementations] with the contract itself as the type argument (e.g.
 * `LazyImplementations<com.example.Greeter>(...)`), since every implementation registered against a
 * contract is required to implement it. `newInstances` itself still returns `LazyImplementations<Any>`,
 * since which contract applies isn't known until `contractName` is compared at runtime.
 *
 * Every implementation referenced by the manifest is instantiated with a direct, literal constructor
 * call (e.g. `com.example.MyImpl()`) rather than reflection, so each one is assumed to have a public,
 * no-argument constructor - the same constraint enforced on classes annotated with
 * [dev.mwhenson.implFinder.Implementation].
 *
 * @param packageName the package the generated file will be declared in
 * @param objectName the name of the generated `object`
 *
 * @author mwhenson
 */
class ManifestCodeGenerator(
    private val packageName: String = DEFAULT_PACKAGE_NAME,
    private val objectName: String = DEFAULT_OBJECT_NAME
) {

    /**
     * Renders [manifest] as the full source of a Kotlin file.
     */
    fun generate(manifest: Manifest): String = buildString {
        appendLine("package $packageName")
        appendLine()
        appendLine("import $LAZY_IMPLEMENTATIONS_FQN")
        appendLine()
        appendLine("public object $objectName {")
        appendLine()
        appendLine("    public fun newInstances(contractName: String): LazyImplementations<Any> = when (contractName) {")
        manifest.entries.forEach { (contractName, implNames) ->
            val factories = implNames.joinToString(separator = ", ") { implName -> "{ $implName() }" }
            appendLine("        ${contractName.toKotlinStringLiteral()} -> LazyImplementations<$contractName>(listOf($factories))")
        }
        appendLine("        else -> LazyImplementations<Any>(emptyList())")
        appendLine("    }")
        appendLine("}")
    }

    /**
     * Renders [manifest] as a Kotlin file and writes it beneath [sourceDir], following the standard
     * package-to-directory convention (i.e. dots in [packageName] become nested directories).
     *
     * @return the [File] that was written
     */
    fun generate(manifest: Manifest, sourceDir: File): File {
        val packageDir = File(sourceDir, packageName.replace('.', '/'))
        packageDir.mkdirs()

        val file = File(packageDir, "$objectName.kt")
        file.writeText(generate(manifest))
        return file
    }

    private fun String.toKotlinStringLiteral(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

    companion object {
        const val DEFAULT_PACKAGE_NAME = "dev.mwhenson.implFinder.generated"
        const val DEFAULT_OBJECT_NAME = "GeneratedImplFinder"

        /**
         * Fully qualified name of the [dev.mwhenson.implFinder.runtime.LazyImplementations] class that
         * the generated `newInstances` function returns. It lives in the Annotations module, so whatever
         * module compiles the generated code needs that module on its compile classpath.
         */
        private const val LAZY_IMPLEMENTATIONS_FQN = "dev.mwhenson.implFinder.runtime.LazyImplementations"
    }
}
