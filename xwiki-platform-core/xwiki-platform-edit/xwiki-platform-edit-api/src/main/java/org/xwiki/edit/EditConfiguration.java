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
package org.xwiki.edit;

import java.lang.reflect.Type;

import org.xwiki.component.annotation.Role;

/**
 * Edit configuration options.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Role
public interface EditConfiguration
{
    /**
     * Returns the component hint of the configured default editor or the id of the configured default editor category
     * associated with the specified data type. Returns {@code null} if there's no configured default editor or default
     * editor category for the specified data type.
     * 
     * @param dataType some data type
     * @return an editor component hint, or a editor category id or {@code null}
     */
    String getDefaultEditor(Type dataType);

    /**
     * Returns the component hint of the configured default editor within the specified category, or the id of the
     * configured default editor sub-category within the specified category, associated with the specified data type.
     * Returns {@code null} if there's no configured default editor or default editor sub-category for the specified
     * data type.
     * 
     * @param dataType some data type
     * @param category some editor category
     * @return an editor component hint, or a editor sub-category id or {@code null}
     */
    String getDefaultEditor(Type dataType, String category);
}
