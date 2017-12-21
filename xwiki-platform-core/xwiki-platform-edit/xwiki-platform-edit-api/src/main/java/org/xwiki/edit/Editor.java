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

import java.util.Map;

import org.xwiki.component.annotation.Role;

/**
 * Base interface for editors.
 * 
 * @param <D> the type of data that can be edited by this editor
 * @version $Id$
 * @since 8.2RC1
 */
@Role
public interface Editor<D>
{
    /**
     * @return this editor's descriptor
     */
    EditorDescriptor getDescriptor();

    /**
     * Generates the HTML code needed to edit the given data.
     * 
     * @param data the data to edit
     * @param parameters editor parameters
     * @return the HTML code that displays the editor
     * @throws EditException if the editor fails to be rendered
     */
    String render(D data, Map<String, Object> parameters) throws EditException;
}
