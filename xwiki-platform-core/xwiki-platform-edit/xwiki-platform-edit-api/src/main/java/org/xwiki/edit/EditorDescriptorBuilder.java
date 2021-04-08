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
 * Builds an {@link EditorDescriptor} instance.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Role
public interface EditorDescriptorBuilder extends EditorDescriptor
{
    /**
     * Sets the editor id.
     * 
     * @param id the editor id
     * @return this builder
     */
    EditorDescriptorBuilder setId(String id);

    /**
     * Sets the editor name.
     * 
     * @param name the editor name
     * @return this builder
     */
    EditorDescriptorBuilder setName(String name);

    /**
     * Sets the editor description.
     * 
     * @param description the editor description
     * @return this builder
     */
    EditorDescriptorBuilder setDescription(String description);

    /**
     * Sets the editor icon.
     * 
     * @param icon the editor icon
     * @return this builder
     */
    EditorDescriptorBuilder setIcon(String icon);

    /**
     * Sets the editor category.
     * 
     * @param category the editor category
     * @return this builder
     */
    EditorDescriptorBuilder setCategory(String category);

    /**
     * Builds the editor descriptor.
     * 
     * @return the editor descriptor
     */
    EditorDescriptor build();
}
