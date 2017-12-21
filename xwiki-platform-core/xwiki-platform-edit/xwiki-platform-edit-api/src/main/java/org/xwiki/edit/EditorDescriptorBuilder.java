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

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.edit.internal.DefaultEditorDescriptor;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Builds an {@link EditorDescriptor} instance.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component(roles = EditorDescriptorBuilder.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class EditorDescriptorBuilder implements EditorDescriptor
{
    private String id;

    private String name;

    private String description;

    private String icon;

    private String category;

    @Inject
    private ContextualLocalizationManager localization;

    /**
     * Sets the editor id.
     * 
     * @param id the editor id
     * @return this builder
     */
    public EditorDescriptorBuilder setId(String id)
    {
        this.id = id;
        return this;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    /**
     * Sets the editor name.
     * 
     * @param name the editor name
     * @return this builder
     */
    public EditorDescriptorBuilder setName(String name)
    {
        this.name = name;
        return this;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    /**
     * Sets the editor description.
     * 
     * @param description the editor description
     * @return this builder
     */
    public EditorDescriptorBuilder setDescription(String description)
    {
        this.description = description;
        return this;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    /**
     * Sets the editor icon.
     * 
     * @param icon the editor icon
     * @return this builder
     */
    public EditorDescriptorBuilder setIcon(String icon)
    {
        this.icon = icon;
        return this;
    }

    @Override
    public String getIcon()
    {
        return this.icon;
    }

    /**
     * Sets the editor category.
     * 
     * @param category the editor category
     * @return this builder
     */
    public EditorDescriptorBuilder setCategory(String category)
    {
        this.category = category;
        return this;
    }

    @Override
    public String getCategory()
    {
        return this.category;
    }

    /**
     * Builds the editor descriptor.
     * 
     * @return the editor descriptor
     */
    public EditorDescriptor build()
    {
        if (StringUtils.isEmpty(this.id)) {
            throw new RuntimeException("The editor id is mandatory.");
        }
        if (StringUtils.isEmpty(this.name)) {
            this.name = this.localization.getTranslationPlain(String.format("edit.editor.%s.name", this.id));
            if (StringUtils.isEmpty(this.name)) {
                this.name = this.id;
            }
        }
        if (this.description == null) {
            this.description =
                this.localization.getTranslationPlain(String.format("edit.editor.%s.description", this.id));
        }
        return new DefaultEditorDescriptor(this.id, this.name, this.description, this.icon, this.category);
    }
}
