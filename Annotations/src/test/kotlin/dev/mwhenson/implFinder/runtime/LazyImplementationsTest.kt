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

package dev.mwhenson.implFinder.runtime

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LazyImplementationsTest {

    @Test
    fun `does not invoke any factory just by being constructed`() {
        var invoked = false
        LazyImplementations(listOf({ invoked = true; Any() }))

        assertFalse(invoked)
    }

    @Test
    fun `does not invoke a factory until the iterator reaches it`() {
        val invoked = mutableListOf<Int>()
        val implementations = LazyImplementations(
            listOf(
                { invoked.add(1); Any() },
                { invoked.add(2); Any() }
            )
        )

        val iterator = implementations.iterator()
        assertTrue(invoked.isEmpty(), "Constructing the iterator shouldn't invoke any factory")

        iterator.next()
        assertEquals(listOf(1), invoked, "Only the first factory should have been invoked")

        iterator.next()
        assertEquals(listOf(1, 2), invoked, "The second factory should now have been invoked too")
    }

    @Test
    fun `always returns the same object for an implementation once it has been inflated`() {
        val implementations = LazyImplementations(listOf({ Any() }))

        val first = implementations.iterator().next()
        val second = implementations.iterator().next()

        assertSame(first, second)
    }

    @Test
    fun `iterates over every implementation in order`() {
        val implementations = LazyImplementations(listOf({ "one" }, { "two" }, { "three" }))

        assertEquals(listOf("one", "two", "three"), implementations.toList())
    }

    @Test
    fun `has no elements when there are no factories`() {
        val implementations = LazyImplementations<Any>(emptyList())

        assertFalse(implementations.iterator().hasNext())
    }
}
