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
package org.xwiki.annotation.maintainer;

import java.util.Collection;

import org.xwiki.component.annotation.Role;

/**
 * Defines the interface of a service providing the differences between two strings.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Role
public interface DiffService
{
    /**
     * Returns the differences between the previous content and the current content, to be implemented depending on the
     * algorithm used to get the differences, or the granulation, etc.
     * 
     * @param previous the previous content
     * @param current the current content
     * @return the collection of differences between the old content and the new content
     */
    Collection<XDelta> getDifferences(String previous, String current);
}
