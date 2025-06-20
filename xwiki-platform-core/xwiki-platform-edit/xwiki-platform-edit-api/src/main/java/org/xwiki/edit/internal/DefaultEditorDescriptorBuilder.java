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

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.edit.EditorDescriptor;
import org.xwiki.edit.EditorDescriptorBuilder;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Builds an {@link EditorDescriptor} instance.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultEditorDescriptorBuilder implements EditorDescriptorBuilder
{
    private String id;

    private String name;

    private String description;

    private String icon;

    private String category;

    @Inject
    private ContextualLocalizationManager localization;

    @Override
    public DefaultEditorDescriptorBuilder setId(String id)
    {
        this.id = id;
        return this;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public DefaultEditorDescriptorBuilder setName(String name)
    {
        this.name = name;
        return this;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public DefaultEditorDescriptorBuilder setDescription(String description)
    {
        this.description = description;
        return this;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public DefaultEditorDescriptorBuilder setIcon(String icon)
    {
        this.icon = icon;
        return this;
    }

    @Override
    public String getIcon()
    {
        return this.icon;
    }

    @Override
    public DefaultEditorDescriptorBuilder setCategory(String category)
    {
        this.category = category;
        return this;
    }

    @Override
    public String getCategory()
    {
        return this.category;
    }

    @Override
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
