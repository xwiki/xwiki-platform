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
package org.xwiki.rendering.macro.parameter.instance;

import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;

/**
 * Base class for macro parameters String values converter.
 * 
 * @param <T> the type of the value after conversion.
 * @version $Id: $
 */
public abstract class AbstractMacroParameter<T> implements MacroParameter<T>
{
    /**
     * Indicate if the value conversion failed.
     */
    protected MacroParameterException error;
    
    /**
     * The macro parameter descriptor.
     */
    private MacroParameterDescriptor<T> parameterClass;

    /**
     * The value as String from parser.
     */
    private String stringValue;

    /**
     * The converted value.
     */
    private T value;

    /**
     * @param parameterClass the macro parameter descriptor.
     * @param stringValue the value as String from parser.
     */
    public AbstractMacroParameter(MacroParameterDescriptor<T> parameterClass, String stringValue)
    {
        this.parameterClass = parameterClass;
        this.stringValue = stringValue;

        this.value = parseValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.instance.MacroParameter#getParameterClass()
     */
    public MacroParameterDescriptor<T> getParameterClass()
    {
        return this.parameterClass;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.instance.MacroParameter#getValueAsString()
     */
    public String getValueAsString()
    {
        return this.stringValue;
    }

    /**
     * Convert the String value.
     * 
     * @return the converted value.
     */
    protected abstract T parseValue();

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.instance.AbstractMacroParameter#getValue()
     */
    public T getValue() throws MacroParameterException
    {
        if (this.error != null && getParameterClass().isValueHasToBeValid()) {
            throw error;
        }

        return this.value;
    }

    /**
     * @return format a standard conversion failure message.
     */
    protected String generateInvalidErrorMessage()
    {
        return "Invalid value [" + getValueAsString() + "] for " + "parameter \"" + getParameterClass() + "\"";
    }
}
