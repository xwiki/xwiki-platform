/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.internal.model;

import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Special Resolver to parse Space Reference representation of the type {@code space1.space2.space3} and to resolve
 * it into a List of Strings. This is useful when refactoring old API taking a String that used to represent a single
 * space and that, now that Nested Spaces has been implemented, can represent one or several spaces.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Role
public interface LegacySpaceResolver
{
    /**
     * @param spaceReferenceRepresentation the string representing one or several Nested Spaces
     * @return the several space names represented as a List
     */
    List<String> resolve(String spaceReferenceRepresentation);
}
