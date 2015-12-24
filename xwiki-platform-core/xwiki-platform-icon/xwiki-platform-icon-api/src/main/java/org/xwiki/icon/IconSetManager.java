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
package org.xwiki.icon;

import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Component to get the icon sets from the Wiki instance (from the WAR of from a wiki page).
 *
 * @since 6.2M1
 * @version $Id$
 */
@Role
public interface IconSetManager
{
    /**
     * Get the icon set defined in the preferences of the wiki.
     * @return the current icon set
     * @throws IconException if problems occur
     */
    IconSet getCurrentIconSet() throws IconException;

    /**
     * Get the default icon set (from the WAR).
     * @return the default icon set
     * @throws IconException if problem occur
     */
    IconSet getDefaultIconSet() throws IconException;

    /**
     * Get icon set by name.
     * @param name of the icon set
     * @return the icon set corresponding to the name, or null if it does not exist
     * @throws IconException if problem occur
     *
     * @since 6.3RC1
     */
    IconSet getIconSet(String name) throws IconException;

    /**
     * Get the name of all the icon sets present in the current wiki.
     *
     * @return the list of the name of the icon sets present in the current wiki.
     * @throws IconException if problem occur
     *
     * @since 6.4M1
     */
    List<String> getIconSetNames() throws IconException;
}
