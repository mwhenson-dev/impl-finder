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

package dev.mwhenson.implFinder.models

import kotlinx.serialization.Serializable

/**
 * This `data class` represents the Manifest of found implementations that
 * will be packaged as a JSON file in the library so that it can be
 * processed later when the final application is built
 *
 * @param version this is the Version of the manifest. Intended for migrations
 *                in the future, if required
 * @param entries these are the mappings between the [ContractName]s and the
 *                [ImplName]s that have been discovered
 *
 * @author hensonm
 */
@Serializable
data class Manifest(
    val version: Int,
    val entries: Map<ContractName, List<ImplName>>
) {
    @Suppress("UNCHECKED_CAST") // TODO: Is there a better way that isn't slower just to satisfy the compiler
    fun toMutableManifest(): MutableManifest = MutableManifest(version, entries as MutableMap<ContractName, MutableList<ImplName>>)
}

@Serializable
data class MutableManifest(
    val version: Int,
    val entries: MutableMap<ContractName, MutableList<ImplName>>
) {
    fun immutable(): Manifest = Manifest(version, entries)
}

typealias ContractName = String
typealias ImplName = String