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
package org.xwiki.rendering.macro.wikibridge;

import java.lang.reflect.Type;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.properties.PropertyGroupDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.stability.Unstable;

/**
 * {@link ParameterDescriptor} for describing wiki macro parameters.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class WikiMacroParameterDescriptor implements ParameterDescriptor
{
    /**
     * Identifier of the parameter.
     * 
     * @since 2.1M1
     */
    private final String id;

    /**
     * Description of the parameter.
     */
    private final String description;

    /**
     * Boolean indicating if the parameter is mandatory.
     */
    private final boolean mandatory;

    /**
     * Default value of the parameter.
     */
    private final Object defaultValue;

    private boolean advanced;
    private boolean displayHidden;
    private boolean deprecated;

    private PropertyGroupDescriptor groupDescriptor;

    /**
     * Type of the parameter.
     */
    private Type parameterType;

    /**
     * Creates a new {@link WikiMacroParameterDescriptor} instance.
     * 
     * @param id parameter identifier.
     * @param description parameter description.
     * @param mandatory if the parameter is mandatory.
     */
    public WikiMacroParameterDescriptor(String id, String description, boolean mandatory)
    {
        this(id, description, mandatory, null);
    }

    /**
     * Creates a new {@link WikiMacroParameterDescriptor} instance.
     *
     * @param id parameter identifier.
     * @param description parameter description.
     * @param mandatory if the parameter is mandatory.
     * @param defaultValue parameter default value.
     * @since 2.3M1
     */
    public WikiMacroParameterDescriptor(String id, String description, boolean mandatory, Object defaultValue)
    {
        this(id, description, mandatory, defaultValue, null);
    }

    /**
     * Creates a new {@link WikiMacroParameterDescriptor} instance.
     *
     * @param id parameter identifier.
     * @param description parameter description.
     * @param mandatory if the parameter is mandatory.
     * @param defaultValue parameter default value.
     * @param parameterType parameter type.
     * @since 10.10RC1
     */
    public WikiMacroParameterDescriptor(String id, String description, boolean mandatory, Object defaultValue,
            Type parameterType)
    {
        this.id = id;
        this.description = description;
        this.mandatory = mandatory;
        this.defaultValue = defaultValue;
        this.parameterType = parameterType;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getName()
    {
        return this.id;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    @Deprecated
    public Class<?> getType()
    {
        Class<?> type = ReflectionUtils.getTypeClass(this.parameterType);
        if (type == null) {
            type = String.class;
        }

        return type;
    }

    @Override
    public Type getParameterType()
    {
        if (this.parameterType != null) {
            return this.parameterType;
        }

        return getType();
    }

    @Override
    public Object getDefaultValue()
    {
        return this.defaultValue;
    }

    @Override
    public boolean isMandatory()
    {
        return mandatory;
    }

    @Override
    public boolean isAdvanced()
    {
        return advanced;
    }

    /**
     * Allows to set whether the parameter is advanced or not. See {@link #isAdvanced()}.
     * @param advanced {@code true} if the parameter is advanced.
     * @return the current instance (builder pattern)
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    @Unstable
    public WikiMacroParameterDescriptor setAdvanced(boolean advanced)
    {
        this.advanced = advanced;
        return this;
    }

    @Override
    public boolean isDisplayHidden()
    {
        return displayHidden;
    }

    /**
     * Allows to set whether the parameter is hidden or not. See {@link #isDisplayHidden()}.
     *
     * @param displayHidden {@code true} if the parameter is hidden.
     * @return the current instance (builder pattern)
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    @Unstable
    public WikiMacroParameterDescriptor setDisplayHidden(boolean displayHidden)
    {
        this.displayHidden = displayHidden;
        return this;
    }

    @Override
    public boolean isDeprecated()
    {
        return deprecated;
    }

    /**
     * Allows to set whether the parameter is deprecated or not. See {@link #isDeprecated()}.
     *
     * @param deprecated {@code true} if the parameter is deprecated.
     * @return the current instance (builder pattern)
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    @Unstable
    public WikiMacroParameterDescriptor setDeprecated(boolean deprecated)
    {
        this.deprecated = deprecated;
        return this;
    }

    @Override
    public PropertyGroupDescriptor getGroupDescriptor()
    {
        return groupDescriptor;
    }

    /**
     * Allows to set the group descriptor of the parameter. See {@link #getGroupDescriptor()}.
     *
     * @param groupDescriptor the group descriptor of the parameter.
     * @return the current instance (builder pattern)
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    @Unstable
    public WikiMacroParameterDescriptor setGroupDescriptor(PropertyGroupDescriptor groupDescriptor)
    {
        this.groupDescriptor = groupDescriptor;
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

        WikiMacroParameterDescriptor that = (WikiMacroParameterDescriptor) o;

        return new EqualsBuilder()
            .append(mandatory, that.mandatory)
            .append(advanced, that.advanced)
            .append(displayHidden, that.displayHidden)
            .append(deprecated, that.deprecated)
            .append(id, that.id)
            .append(description, that.description)
            .append(defaultValue, that.defaultValue)
            .append(groupDescriptor, that.groupDescriptor)
            .append(parameterType, that.parameterType)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 63)
            .append(id)
            .append(description)
            .append(mandatory)
            .append(defaultValue)
            .append(advanced)
            .append(displayHidden)
            .append(deprecated)
            .append(groupDescriptor)
            .append(parameterType)
            .toHashCode();
    }
}
