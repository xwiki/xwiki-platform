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

import org.xwiki.edit.EditorDescriptor;

/**
 * Default {@link EditorDescriptor} implementation.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
public class DefaultEditorDescriptor implements EditorDescriptor
{
    private final String id;

    private final String name;

    private final String description;

    private final String icon;

    private final String category;

    /**
     * Creates a new descriptor for the specified editor.
     * 
     * @param id the editor id (usually the editor component hint)
     * @param name the editor pretty name
     * @param description the editor short description
     * @param icon the editor icon
     * @param category the editor category
     */
    public DefaultEditorDescriptor(String id, String name, String description, String icon, String category)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.category = category;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public String getIcon()
    {
        return this.icon;
    }

    @Override
    public String getCategory()
    {
        return this.category;
    }
}
