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

import org.xwiki.component.annotation.Role;

/**
 * Interface used to customize the {@link EditConfiguration} for a specific data type. It allows you to:
 * <ul>
 * <li>reuse some existing configuration properties that don't follow the conventions established by the
 * {@link EditConfiguration}</li>
 * <li>specify the default editor when there's no one configured</li>
 * </ul>
 * .
 * 
 * @param <D> the type of data that is edited by the editors affected by this configuration
 * @version $Id$
 * @since 8.2RC1
 */
@Role
public interface EditorConfiguration<D>
{
    /**
     * Returns the component hint of the configured default editor or the id of the configured default editor category
     * associated with the data type bound to this configuration. Returns {@code null} if there's no configured default
     * editor or default editor category for the data type bound to this configuration.
     * 
     * @return an editor component hint, or a editor category id or {@code null}
     */
    String getDefaultEditor();

    /**
     * Returns the component hint of the configured default editor within the specified category, or the id of the
     * configured default editor sub-category within the specified category, associated with the data type bound to this
     * configuration. Returns {@code null} if there's no configured default editor or default editor sub-category for
     * the data type bound to this configuration.
     * 
     * @param category some editor category
     * @return an editor component hint, or a editor sub-category id or {@code null}
     */
    String getDefaultEditor(String category);
}
