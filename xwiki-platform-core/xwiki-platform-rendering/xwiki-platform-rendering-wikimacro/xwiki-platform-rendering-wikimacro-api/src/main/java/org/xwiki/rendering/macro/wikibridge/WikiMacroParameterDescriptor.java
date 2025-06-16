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
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.properties.PropertyGroupDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * {@link ParameterDescriptor} for describing wiki macro parameters.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class WikiMacroParameterDescriptor implements ParameterDescriptor
{
    /**
     * Constant to be used in the parameters map of the constructor to define whether the parameter is advanced or
     * not: the accepted value should be a boolean or its serialized value.
     * @see #isAdvanced()
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    @Unstable
    public static final String ADVANCED_PARAMETER_NAME = "advanced";

    /**
     * Constant to be used in the parameters map of the constructor to define whether the parameter is hidden or
     * not: the accepted value should be a boolean or its serialized value.
     * @see #isDisplayHidden()
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    @Unstable
    public static final String HIDDEN_PARAMETER_NAME = "hidden";

    /**
     * Constant to be used in the parameters map of the constructor to define whether the parameter is deprecated or
     * not: the accepted value should be a boolean or its serialized value.
     * @see #isDeprecated()
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    @Unstable
    public static final String DEPRECATED_PARAMETER_NAME = "deprecated";

    /**
     * Constant to be used in the parameters map of the constructor to define the group: the accepted value should be
     * of type {@link PropertyGroupDescriptor}.
     * @see #getGroupDescriptor()
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    @Unstable
    public static final String GROUP_PARAMETER_NAME = "group";

    /**
     * Constant to be used in the parameters map of the constructor to define the order: the accepted value should be
     * of type {@link Integer}.
     * @see #getOrder()
     * @since 17.5.0RC1
     */
    @Unstable
    public static final String ORDER_PARAMETER_NAME = "order";

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

    /**
     * Type of the parameter.
     */
    private final Type parameterType;

    private final boolean advanced;
    private final boolean displayHidden;
    private final boolean deprecated;
    private final int order;

    private final PropertyGroupDescriptor groupDescriptor;

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
        this(id, description, mandatory, defaultValue, parameterType, Map.of());
    }

    /**
     * Creates a new {@link WikiMacroParameterDescriptor} instance.
     *
     * @param id parameter identifier.
     * @param description parameter description.
     * @param mandatory if the parameter is mandatory.
     * @param defaultValue parameter default value.
     * @param parameterType parameter type.
     * @param parameters a map of other parameters of the descriptor. See also the documented constants of this class
     * to know the value allowed in that map.
     * @since 17.3.0RC1
     * @since 16.10.6
     */
    @Unstable
    public WikiMacroParameterDescriptor(String id, String description, boolean mandatory, Object defaultValue,
        Type parameterType, Map<String, Object> parameters)
    {
        this.id = id;
        this.description = description;
        this.mandatory = mandatory;
        this.defaultValue = defaultValue;
        this.parameterType = parameterType;
        this.advanced = Boolean.parseBoolean(String.valueOf(parameters.get(ADVANCED_PARAMETER_NAME)));
        this.displayHidden = Boolean.parseBoolean(String.valueOf(parameters.get(HIDDEN_PARAMETER_NAME)));
        this.deprecated = Boolean.parseBoolean(String.valueOf(parameters.get(DEPRECATED_PARAMETER_NAME)));
        if (parameters.get(GROUP_PARAMETER_NAME) instanceof PropertyGroupDescriptor localGroupDescriptor) {
            this.groupDescriptor = localGroupDescriptor;
        } else {
            this.groupDescriptor = null;
        }
        this.order = (int) parameters.getOrDefault(ORDER_PARAMETER_NAME, -1);
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

    @Override
    public boolean isDisplayHidden()
    {
        return displayHidden;
    }

    @Override
    public boolean isDeprecated()
    {
        return deprecated;
    }

    @Override
    public PropertyGroupDescriptor getGroupDescriptor()
    {
        return groupDescriptor;
    }

    @Override
    public int getOrder()
    {
        return this.order;
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

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("id", id)
            .append("description", description)
            .append("mandatory", mandatory)
            .append("defaultValue", defaultValue)
            .append("parameterType", parameterType)
            .append(ADVANCED_PARAMETER_NAME, advanced)
            .append("displayHidden", displayHidden)
            .append(DEPRECATED_PARAMETER_NAME, deprecated)
            .append("groupDescriptor", groupDescriptor)
            .toString();
    }
}
