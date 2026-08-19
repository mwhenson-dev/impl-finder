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

package dev.mwhenson.implFinder.plugin

import com.google.devtools.ksp.gradle.KspGradleSubplugin
import dev.mwhenson.implFinder.plugin.attr.Attributes
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import java.io.File
import java.util.jar.JarFile

private const val MANIFEST_JSON_ENTRY = "dev/mwhenson/implFinder/manifest/Manifest.json"

class ImplFinderPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.run {
            withId("com.android.application") { configureAndroidApplication(target) }
            withId("com.android.library") { configureAndroidLibrary(target) }
            withId("java") { configureJavaApplication(target) }
            withId("java-library") { configureJavaLibrary(target) }
        }

    }

    private fun configureAndroidApplication(target: Project) {
        // TODO: Set up Android
        // TODO: Disable standard Java tasks

        // TODO: Find all libraries with custom attribute
        // TODO: Process and combine the JSON files
        // TODO: Generate actual Manifest code
    }

    private fun configureAndroidLibrary(target: Project) {
        // TODO: Set up Android
        // TODO: Disable standard Java tasks

        // TODO: Find if JSON has been created, warn if it hasn't
        // TODO: Add JSON to final JAR
        // TODO: Add custom attribute for manifest
    }

    private fun configureJavaApplication(target: Project) {
        // TODO: Process and combine the JSON files
        // TODO: Generate actual Manifest code

        val sourceSets = target.extensions.getByType(JavaPluginExtension::class.java).sourceSets

        sourceSets.configureEach { sourceSet ->
            val manifestArtifacts = listOf(
                sourceSet.compileClasspathConfigurationName,
                sourceSet.runtimeClasspathConfigurationName,
            )
                .map { configurationName -> target.configurations.getByName(configurationName) }
                .map { configuration ->
                    configuration.incoming
                        .artifactView { view ->
                            view.lenient(true)
                            view.attributes.attribute(Attributes.CONTAINS_MANIFEST, "true")
                        }
                        .artifacts
                        .resolvedArtifacts
                }
                .let { perConfiguration ->
                    target.provider { perConfiguration.flatMap { it.get() }.distinctBy { it.id } }
                }

            val manifestJsonFiles: Provider<List<File>> = manifestArtifacts.map { artifacts ->
                artifacts.mapNotNull { artifact -> extractManifestJson(artifact.file) }
            }
        }
    }

    private fun extractManifestJson(jarFile: File): File? =
        JarFile(jarFile).use { jar ->
            val entry = jar.getEntry(MANIFEST_JSON_ENTRY) ?: return@use null
            val extracted = File.createTempFile("impl-finder-manifest-", ".json")
            extracted.deleteOnExit()
            jar.getInputStream(entry).use { input -> extracted.outputStream().use { output -> input.copyTo(output) } }
            extracted
        }

    private fun configureJavaLibrary(target: Project) {
        // TODO: Is there a better way to do this.
        // TODO: Give this a unique name and ensure it gets added properly

        // KSP's own Gradle plugin already wires its resource output directory into `processResources`
        // (see KspGradleSubplugin.applyToCompilation), so it doesn't need to be added here too - doing so
        // causes a duplicate-entry failure once com.google.devtools.ksp is actually applied.
        val manifestResources = KspGradleSubplugin.getKspResourceOutputDir(target, "main", "")
        val manifestFile = manifestResources.map { it.file("dev/mwhenson/implFinder/manifest/Manifest.json") }

        // TODO: I don't like tying this to `jar`, so let's find a better way to deal with this
//        target.tasks.getByName("jar").doFirst {
//            if (!manifestFile.get().asFile.exists()) {
//                target.logger.warn(
//                    "impl-finder: no Manifest.json was found for project '${target.path}'. " +
//                        "No implementations from this library will be discoverable at runtime."
//                )
//            }
//        }

        target.configurations.getByName("apiElements").attributes.attribute(Attributes.CONTAINS_MANIFEST, "true")
    }
}
