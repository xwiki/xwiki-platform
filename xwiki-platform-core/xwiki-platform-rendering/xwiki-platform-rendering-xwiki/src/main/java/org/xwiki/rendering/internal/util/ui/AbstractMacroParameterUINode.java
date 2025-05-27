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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public abstract class AbstractMacroParameterUINode
{
    private final MacroParameterUINodeType type;
    private final String id;
    private String name;
    private String description;
    private boolean hidden;
    private boolean mandatory;
    private int order;

    protected AbstractMacroParameterUINode(MacroParameterUINodeType type, String id)
    {
        this.type = type;
        this.id = id;
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

    public String getName()
    {
        return name;
    }

    public <T extends AbstractMacroParameterUINode> T setName(String name)
    {
        this.name = name;
        return (T) this;
    }

    public String getDescription()
    {
        return description;
    }

    public <T extends AbstractMacroParameterUINode> T setDescription(String description)
    {
        this.description = description;
        return (T) this;
    }

    public boolean isHidden()
    {
        return hidden;
    }

    public <T extends AbstractMacroParameterUINode> T setHidden(boolean hidden)
    {
        this.hidden = hidden;
        return (T) this;
    }

    public boolean isMandatory()
    {
        return mandatory;
    }

    public <T extends AbstractMacroParameterUINode> T setMandatory(boolean mandatory)
    {
        this.mandatory = mandatory;
        return (T) this;
    }

    public int getOrder()
    {
        return order;
    }

    public <T extends AbstractMacroParameterUINode> T setOrder(int order)
    {
        this.order = order;
        return (T) this;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractMacroParameterUINode that = (AbstractMacroParameterUINode) o;

        return new EqualsBuilder().append(hidden, that.hidden)
            .append(mandatory, that.mandatory).append(order, that.order).append(type, that.type).append(id, that.id)
            .append(name, that.name).append(description, that.description).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 21)
            .append(type)
            .append(id)
            .append(name)
            .append(description)
            .append(hidden)
            .append(mandatory)
            .append(order)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("type", type)
            .append("id", id)
            .append("name", name)
            .append("description", description)
            .append("hidden", hidden)
            .append("mandatory", mandatory)
            .append("order", order)
            .toString();
    }
}
