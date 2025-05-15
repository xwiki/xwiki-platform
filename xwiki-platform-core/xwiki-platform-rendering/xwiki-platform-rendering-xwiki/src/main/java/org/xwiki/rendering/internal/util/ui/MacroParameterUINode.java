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
package org.xwiki.rendering.internal.util.ui;

import java.util.SortedSet;
import java.util.TreeSet;

public class MacroParameterUINode
{
    private final MacroParameterUINodeType type;
    private final String id;
    private String name;
    private String description;
    private boolean hidden;
    private boolean mandatory;
    private boolean advanced;
    private boolean deprecated;
    private String displayType;
    private int order;
    private Object defaultValue;
    private boolean caseInsensitive;
    private String editTemplate;
    // FIXME: would be more consistent to maybe only send the list of keys
    private final SortedSet<MacroParameterUINode> children;

    public MacroParameterUINode(MacroParameterUINodeType type, String id)
    {
        this.type = type;
        this.id = id;
        this.name = id;
        this.children = new TreeSet<>(new MacroParameterUINodeComparator());
        this.order = -1;
    }

    public String getKey()
    {
        return getType().name() + ":" + id;
    }

    public String getId()
    {
        return id;
    }

    public MacroParameterUINodeType getType()
    {
        return type;
    }

    public SortedSet<MacroParameterUINode> getChildren()
    {
        return children;
    }

    public MacroParameterUINode addChild(MacroParameterUINode child)
    {
        this.children.add(child);
        return this;
    }

    public String getName()
    {
        return name;
    }

    public MacroParameterUINode setName(String name)
    {
        this.name = name;
        return this;
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public MacroParameterUINode setHidden(boolean hidden)
    {
        this.hidden = hidden;
        return this;
    }

    public boolean isMandatory()
    {
        return mandatory;
    }

    public MacroParameterUINode setMandatory(boolean mandatory)
    {
        this.mandatory = mandatory;
        return this;
    }

    public boolean isAdvanced()
    {
        return advanced;
    }

    public MacroParameterUINode setAdvanced(boolean advanced)
    {
        this.advanced = advanced;
        return this;
    }

    public String getDisplayType()
    {
        return displayType;
    }

    public MacroParameterUINode setDisplayType(String displayType)
    {
        this.displayType = displayType;
        return this;
    }

    public int getOrder()
    {
        return order;
    }

    public MacroParameterUINode setOrder(int order)
    {
        this.order = order;
        return this;
    }

    public boolean isDeprecated()
    {
        return deprecated;
    }

    public MacroParameterUINode setDeprecated(boolean deprecated)
    {
        this.deprecated = deprecated;
        return this;
    }

    public String getDescription()
    {
        return description;
    }

    public MacroParameterUINode setDescription(String description)
    {
        this.description = description;
        return this;
    }

    public Object getDefaultValue()
    {
        return defaultValue;
    }

    public MacroParameterUINode setDefaultValue(Object defaultValue)
    {
        this.defaultValue = defaultValue;
        return this;
    }

    public boolean isCaseInsensitive()
    {
        return caseInsensitive;
    }

    public MacroParameterUINode setCaseInsensitive(boolean caseInsensitive)
    {
        this.caseInsensitive = caseInsensitive;
        return this;
    }

    public String getEditTemplate()
    {
        return editTemplate;
    }

    public MacroParameterUINode setEditTemplate(String editTemplate)
    {
        this.editTemplate = editTemplate;
        return this;
    }

}
