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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

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
        Class<?> clazz;
        if (this.parameterType instanceof Class) {
            clazz = (Class) this.parameterType;
        } else if (this.parameterType instanceof ParameterizedType) {
            clazz = (Class) ((ParameterizedType) this.parameterType).getRawType();
        } else {
            clazz = String.class;
        }

        return clazz;
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
}
