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

import org.gradle.api.Plugin
import org.gradle.api.Project

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
        // TODO: Find all libraries with custom attribute
        // TODO: Process and combine the JSON files
        // TODO: Generate actual Manifest code
    }

    private fun configureJavaLibrary(target: Project) {
        // TODO: Find if JSON has been created, warn if it hasn't
        target.layout.buildDirectory.file("")
        // TODO: Add JSON to final JAR
        // TODO: Add custom attribute for manifest
    }
}
