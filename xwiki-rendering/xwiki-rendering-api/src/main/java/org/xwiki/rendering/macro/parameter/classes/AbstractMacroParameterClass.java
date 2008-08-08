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
package org.xwiki.rendering.macro.parameter.classes;

/**
 * Base class for macro parameter descriptor.
 * 
 * @param <T> the type of the value after conversion.
 * @version $Id: $
 */
public abstract class AbstractMacroParameterClass<T> implements MacroParameterClass<T>
{
    /**
     * The name of the parameter.
     */
    private String name;

    /**
     * The description of the parameter.
     */
    private String description;

    /**
     * The default value.
     */
    private T def;

    /**
     * Is parameter required.
     */
    private boolean required;

    /**
     * Indicate if the macro parameter object has to throw exception when value is invalid.
     */
    private boolean valueHasToBeValid = true;

    /**
     * @param name the name of the parameter.
     * @param description the description of the parameter.
     * @param def the default value.
     */
    public AbstractMacroParameterClass(String name, String description, T def)
    {
        this.name = name;
        this.description = description;
        this.def = def;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.classes.MacroParameterClass#getName()
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.classes.MacroParameterClass#getDescription()
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.classes.MacroParameterClass#getDefaultValue()
     */
    public T getDefaultValue()
    {
        return this.def;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.classes.MacroParameterClass#isRequired()
     */
    public boolean isRequired()
    {
        return this.required;
    }

    /**
     * @param required indicate the parameter has to be specified.
     */
    public void setRequired(boolean required)
    {
        this.required = required;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.classes.MacroParameterClass#isValueHasToBeValid()
     */
    public boolean isValueHasToBeValid()
    {
        return this.valueHasToBeValid;
    }

    /**
     * @param isValueHasToBeValid indicate if the macro parameter object has to throw exception when value is invalid.
     */
    public void setValueHasToBeValid(boolean isValueHasToBeValid)
    {
        this.valueHasToBeValid = isValueHasToBeValid;
    }
}
