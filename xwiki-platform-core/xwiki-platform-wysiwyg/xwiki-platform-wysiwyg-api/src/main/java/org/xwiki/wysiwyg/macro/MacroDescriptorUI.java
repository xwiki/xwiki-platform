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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.stability.Unstable;

/**
 * Representation of a macro descriptor used for the configuration UI.
 *
 * @version $Id$
 * @since 17.5.0
 */
@Unstable
public class MacroDescriptorUI
{
    private final String id;
    private String name;
    private String description;
    private boolean supportsInlineMode;
    private List<String> mandatoryNodes;
    private List<String> optionalNodes;
    private Map<String, AbstractMacroUINode> parametersMap;

    /**
     * Default constructor.
     * @param id the identifier of the macro.
     */
    public MacroDescriptorUI(String id)
    {
        this.id = id;
    }

    /**
     * @return the ordered list of mandatory nodes keys to be found in {@link #getParametersMap()}.
     */
    public List<String> getMandatoryNodes()
    {
        return mandatoryNodes;
    }

    /**
     * @param mandatoryNodes see {@link #getMandatoryNodes()}
     * @return the current instance
     */
    public MacroDescriptorUI setMandatoryNodes(List<String> mandatoryNodes)
    {
        this.mandatoryNodes = mandatoryNodes;
        return this;
    }

    /**
     * @return the ordered list of optional nodes keys to be found in {@link #getParametersMap()}.
     */
    public List<String> getOptionalNodes()
    {
        return optionalNodes;
    }

    /**
     * @param optionalNodes see {@link #getOptionalNodes()}
     * @return the current instance
     */
    public MacroDescriptorUI setOptionalNodes(List<String> optionalNodes)
    {
        this.optionalNodes = optionalNodes;
        return this;
    }

    /**
     * @return the full map of parameters, groups and features of the descriptor, each index is computed by
     * parameters themselves with {@link AbstractMacroUINode#getKey()}.
     */
    public Map<String, AbstractMacroUINode> getParametersMap()
    {
        return parametersMap;
    }

    /**
     * @param parametersMap see {@link #getParametersMap()}
     * @return the current instance
     */
    public MacroDescriptorUI setParametersMap(Map<String, AbstractMacroUINode> parametersMap)
    {
        this.parametersMap = parametersMap;
        return this;
    }

    /**
     * @return the id of the macro
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return the translated description of the macro
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description see {@link #getDescription()}
     * @return the current instance
     */
    public MacroDescriptorUI setDescription(String description)
    {
        this.description = description;
        return this;
    }

    /**
     * @return whether the macro supports inline mode as defined by the descriptor.
     */
    public boolean isSupportsInlineMode()
    {
        return supportsInlineMode;
    }

    /**
     * @param supportsInlineMode see {@link #isSupportsInlineMode()}
     * @return the current instance
     */
    public MacroDescriptorUI setSupportsInlineMode(boolean supportsInlineMode)
    {
        this.supportsInlineMode = supportsInlineMode;
        return this;
    }

    /**
     * @return the translated name of the macro.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name see {@link #getName()}.
     * @return the current instance
     */
    public MacroDescriptorUI setName(String name)
    {
        this.name = name;
        return this;
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

        MacroDescriptorUI that = (MacroDescriptorUI) o;

        return new EqualsBuilder()
            .append(supportsInlineMode, that.supportsInlineMode)
            .append(id, that.id)
            .append(name, that.name)
            .append(description, that.description)
            .append(mandatoryNodes, that.mandatoryNodes)
            .append(optionalNodes, that.optionalNodes)
            .append(parametersMap, that.parametersMap)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 63)
            .append(id)
            .append(name)
            .append(description)
            .append(supportsInlineMode)
            .append(mandatoryNodes)
            .append(optionalNodes)
            .append(parametersMap)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("name", name)
            .append("description", description)
            .append("supportsInlineMode", supportsInlineMode)
            .append("mandatoryNodes", mandatoryNodes)
            .append("optionalNodes", optionalNodes)
            .append("parametersMap", parametersMap)
            .toString();
    }
}
