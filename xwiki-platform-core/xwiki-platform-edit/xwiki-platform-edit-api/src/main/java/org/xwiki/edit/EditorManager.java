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
import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Manages the available {@link Editor}s.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Role
public interface EditorManager
{
    /**
     * @param dataType the data type
     * @param <D> the data type
     * @return the list of editors that can edit the specified type of data
     */
    <D> List<Editor<D>> getEditors(Type dataType);

    /**
     * @param dataType the data type
     * @param category the editor category
     * @param <D> the data type
     * @return the list of editors that have the specified category and which are associated with the given data type
     */
    <D> List<Editor<D>> getEditors(Type dataType, String category);

    /**
     * @param dataType the data type
     * @param hint the {@link Editor} component role hint
     * @param <D> the data type
     * @return an editor that can edit the specified data type and which has the given {@link Editor} component role
     *         hint, or {@code null} if no such editor can be found
     */
    <D> Editor<D> getEditor(Type dataType, String hint);

    /**
     * @param dataType the data type
     * @param <D> the data type
     * @return the configured default editor associated with the specified data type
     */
    <D> Editor<D> getDefaultEditor(Type dataType);

    /**
     * @param dataType the data type
     * @param category the editor category
     * @param <D> the data type
     * @return the configured default editor that has the specified category and which is associated with the given data
     *         type
     */
    <D> Editor<D> getDefaultEditor(Type dataType, String category);
}
