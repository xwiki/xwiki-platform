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
import java.util.ArrayList;
import java.util.List;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;

/**
 * {@link ParameterDescriptor} for describing wiki macro parameters.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class WikiMacroParameterDescriptor implements ParameterDescriptor
{
    /**
     * Constant for representing parameter type property values.
     *
     * @since 10.10RC1
     */
    public static final List<WikiMacroParameterType> PARAMETER_TYPE_PROPERTY_VALUES =
            new ArrayList<WikiMacroParameterType>()
        {
            {
                add(new WikiMacroParameterType("string", "String", String.class));
                add(new WikiMacroParameterType("boolean", "Boolean", Boolean.class));
                add(new WikiMacroParameterType("enum", "Enum", Enum.class));
                add(new WikiMacroParameterType("document", "Document", DocumentReference.class));
            }
        };

    /**
     * Identifier of the parameter.
     * 
     * @since 2.1M1
     */
    private String id;

    /**
     * Display name of the parameter.
     * 
     * @since 2.1M1
     */
    private String name;

    /**
     * Description of the parameter.
     */
    private String description;

    /**
     * Boolean indicating if the parameter is mandatory.
     */
    private boolean mandatory;

    /**
     * Default value of the parameter.
     */
    private Object defaultValue;

    /**
     * Type of the parameter.
     */
    private String type;

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
        this(id, description, mandatory, defaultValue, PARAMETER_TYPE_PROPERTY_VALUES.get(0).getName());
    }

    /**
     * Creates a new {@link WikiMacroParameterDescriptor} instance.
     *
     * @param id parameter identifier.
     * @param description parameter description.
     * @param mandatory if the parameter is mandatory.
     * @param defaultValue parameter default value.
     * @param type parameter type.
     * @since 10.10RC1
     */
    public WikiMacroParameterDescriptor(String id, String description, boolean mandatory, Object defaultValue,
            String type)
    {
        this.id = id;
        this.description = description;
        this.mandatory = mandatory;
        this.defaultValue = defaultValue;
        this.type = type;
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
    @Deprecated
    public Class< ? > getType()
    {
        return String.class;
    }

    @Override
    public Type getParameterType()
    {
        for (WikiMacroParameterType parameterType : PARAMETER_TYPE_PROPERTY_VALUES) {
            if (parameterType.getName().equals(this.type.toLowerCase())) {
                return parameterType.getType();
            }
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
}
