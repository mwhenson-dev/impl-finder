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
import dev.mwhenson.implFinder.models.Manifest
import dev.mwhenson.implFinder.models.MutableManifest
import dev.mwhenson.implFinder.plugin.attr.Attributes
import kotlinx.serialization.json.Json
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.bundling.Jar
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
        // TODO: Generate actual Manifest code

        // The `java-library` plugin applies the `java` plugin internally, which fires this same callback
        // for library projects before `java-library` itself is registered as applied. Deferring to
        // afterEvaluate lets us reliably tell the two apart, and also ensures all dependencies declared
        // anywhere in the project's build script have been added before we resolve against them.
        target.afterEvaluate {
            if (target.plugins.hasPlugin("java-library")) return@afterEvaluate

            configureJavaApplicationManifestMerging(target)
        }
    }

    private fun configureJavaApplicationManifestMerging(target: Project) {
        // Artifacts are requested with the "jar" artifact type so that, for project dependencies, Gradle
        // builds (and depends on) the actual jar rather than substituting the compiled-classes directory
        // it would otherwise prefer for a compile/runtime classpath.
        val manifestArtifacts = target.provider {
            target.configurations
                .filter { it.isCanBeResolved }
                .map { config ->
                    config.incoming.artifactView { view ->
                        view.lenient(true)
                        view.attributes { attrs ->
                            attrs.attribute(Attributes.CONTAINS_MANIFEST, "true")
                            attrs.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
                        }
                    }.artifacts
                }
        }

        val manifestEntryDir = MANIFEST_JSON_ENTRY.substringBeforeLast("/")
        val manifestEntryName = MANIFEST_JSON_ENTRY.substringAfterLast("/")
        val mergedManifestFile = target.layout.buildDirectory.file("generated/implFinder/$manifestEntryName")

        val mergeManifestsTask = target.tasks.register("mergeImplFinderManifests") { task ->
            task.inputs.files(manifestArtifacts.map { collections -> collections.map { it.artifactFiles } })
            task.outputs.file(mergedManifestFile)

            task.doLast {
                // The same artifact is commonly resolvable from more than one configuration
                // (e.g. compileClasspath and runtimeClasspath both pulling in the same project
                // dependency), so dedupe by artifact identity before merging.
                val manifests = manifestArtifacts.get()
                    .flatMap { it.artifacts }
                    .distinctBy { it.id }
                    .mapNotNull { extractManifestJson(it.file) }
                    .map { Json.decodeFromString<Manifest>(it.readText()) }

                val output = mergedManifestFile.get().asFile
                output.parentFile.mkdirs()
                output.writeText(Json.encodeToString(combineManifests(manifests)))
            }
        }

        target.tasks.named("jar", Jar::class.java) { jar ->
            jar.dependsOn(mergeManifestsTask)
            jar.from(mergedManifestFile) { spec -> spec.into(manifestEntryDir) }
        }
    }

    private fun combineManifests(manifests: List<Manifest>): Manifest {
        val combined = MutableManifest(version = 1, entries = mutableMapOf())
        manifests.forEach { manifest ->
            manifest.entries.forEach { (contract, implementations) ->
                combined.entries.getOrPut(contract) { mutableListOf() }.addAll(implementations)
            }
        }
        return combined.immutable()
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
