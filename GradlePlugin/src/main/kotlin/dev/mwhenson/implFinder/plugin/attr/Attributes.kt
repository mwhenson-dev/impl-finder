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

package dev.mwhenson.implFinder.plugin.attr

import org.gradle.api.attributes.Attribute

private const val ATTR_NAME = "dev.mwhenson.implFinder.manifest"

/**
 * Contains Attributes that can be added to artifacts generated so that Gradle
 * has an idea of what we contain (and then lets us filter based on that later)
 *
 * @author mwhenson
 */
object Attributes {

    /**
     * This [Attribute] denotes that an artifact contains an impl-finder Manifest
     */
    val CONTAINS_MANIFEST = Attribute.of(ATTR_NAME, String::class.java)
}