/*
 This file is part of impl-finder.

 impl-finder is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 Public License as published by the Free Software Foundation, either version 2.1 of the License,
 or (at your option) any later version.

 impl-finder is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 for more details.

 You should have received a copy of the GNU Lesser General Public License along with Foobar.
 If not, see <https://www.gnu.org/licenses/>.
*/

package dev.mwhenson.implFinder;

/**
 * If you have an implementation that needs to be able to be found, you should annotated
 * it with this annotation. When processed, that will cause your class to be noted and
 * eventually added as an implementation for any interfaces that your class directly implements
 *
 * TODO: Do we need the ability to specify specific interfaces as an argument here?
 *
 * @author mwhenson
 */
annotation class Implementation