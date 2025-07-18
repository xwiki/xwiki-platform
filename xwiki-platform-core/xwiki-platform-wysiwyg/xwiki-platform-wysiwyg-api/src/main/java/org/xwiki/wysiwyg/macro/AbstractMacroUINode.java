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
package org.xwiki.wysiwyg.macro;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.stability.Unstable;

/**
 * Abstract representation of a node in {@link MacroDescriptorUI}.
 *
 * @version $Id$
 * @since 17.5.0
 */
@Unstable
public abstract class AbstractMacroUINode
{
    private final MacroUINodeType type;
    private final String id;
    private String name;
    private String description;
    private boolean hidden;
    private boolean mandatory;
    private int order;

    /**
     * Default constructor.
     * @param type the type of the node
     * @param id its identifier
     */
    protected AbstractMacroUINode(MacroUINodeType type, String id)
    {
        this.type = type;
        this.id = id;
        this.order = -1;
    }

    /**
     * @return the key of the node to be identified in {@link MacroDescriptorUI#getParametersMap()}.
     */
    public String getKey()
    {
        return getType().name() + ":" + id;
    }

    /**
     * @return the identifier of the node as defined in the macro descriptor.
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the type of the node.
     */
    public MacroUINodeType getType()
    {
        return type;
    }

    /**
     * @return the translated name of the node.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name see {@link #getName()}.
     * @return the current instance
     * @param <T> the concrete type
     */
    public <T extends AbstractMacroUINode> T setName(String name)
    {
        this.name = name;
        return (T) this;
    }

    /**
     * @return the translated description of the node.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description see {@link #getDescription()}.
     * @return the current instance
     * @param <T> the concrete type
     */
    public <T extends AbstractMacroUINode> T setDescription(String description)
    {
        this.description = description;
        return (T) this;
    }

    /**
     * @return {@code true} if the node should be hidden in the UI.
     */
    public boolean isHidden()
    {
        return hidden;
    }

    /**
     * @param hidden see {@link #isHidden()}.
     * @return the current instance
     * @param <T> the concrete type
     */
    public <T extends AbstractMacroUINode> T setHidden(boolean hidden)
    {
        this.hidden = hidden;
        return (T) this;
    }

    /**
     * @return {@code true} if the node is mandatory (a value needs to be provided).
     */
    public boolean isMandatory()
    {
        return mandatory;
    }

    /**
     * @param mandatory see {@link #isMandatory()}.
     * @return the current instance
     * @param <T> the concrete type
     */
    public <T extends AbstractMacroUINode> T setMandatory(boolean mandatory)
    {
        this.mandatory = mandatory;
        return (T) this;
    }

    /**
     * @return the display order of the node (the lower the value, the higher its priority to display).
     */
    public int getOrder()
    {
        return order;
    }

    /**
     * @param order see {@link #getOrder()}.
     * @return the current instance
     * @param <T> the concrete type
     */
    public <T extends AbstractMacroUINode> T setOrder(int order)
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

        AbstractMacroUINode that = (AbstractMacroUINode) o;

        return new EqualsBuilder()
            .append(hidden, that.hidden)
            .append(mandatory, that.mandatory)
            .append(order, that.order)
            .append(type, that.type)
            .append(id, that.id)
            .append(name, that.name)
            .append(description, that.description)
            .isEquals();
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
