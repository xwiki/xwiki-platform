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
package org.xwiki.edit.internal;

import java.lang.reflect.Type;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.edit.Editor;
import org.xwiki.stability.Unstable;

/**
 * A source of {@link Editor}s.
 * <p>
 * NOTE: This interface is used to discover editors that are not defined as components. For instance we need to discover
 * editors defined in wiki pages. We will probably drop this interface once we add support for automatic creation of
 * editor components from non-component editor sources.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Role
@Unstable
public interface EditorSource
{
    /**
     * @param dataType the data type
     * @param <D> the data type
     * @return the list of editors that can edit the specified type of data
     */
    <D> List<Editor<D>> getEditors(Type dataType);

    /**
     * @param dataType the data type
     * @param hint the {@link Editor} role hint
     * @param <D> the data type
     * @return an editor that can edit the specified data type and which has the given {@link Editor} role hint, or
     *         {@code null} if no such editor can be found
     */
    <D> Editor<D> getEditor(Type dataType, String hint);
}
