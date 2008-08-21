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

import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;

/**
 * Convert parameter String value to provided Enum class entry.
 * 
 * @param <T> the type of the Enum.
 * @version $Id$
 */
public class EnumMacroParameter<T extends Enum<T>> extends AbstractMacroParameter<T>
{
    /**
     * @param parameterDescriptor the macro parameter descriptor.
     * @param stringValue the value as String from parser.
     */
    public EnumMacroParameter(MacroParameterDescriptor<T> parameterDescriptor, String stringValue)
    {
        super(parameterDescriptor, stringValue);
    }

    /**
     * @return the value as provided Enum class entry.
     */
    @Override
    protected T parseValue()
    {
        T def = getParameterDescriptor().getDefaultValue();

        T[] values = ((Class<T>) def.getClass()).getEnumConstants();

        String valueAsString = getValueAsString();
        for (T value : values) {
            if (value.toString().equalsIgnoreCase(valueAsString)) {
                return value;
            }
        }

        setErrorInvalid();

        return def;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.instance.AbstractMacroParameter#generateInvalidErrorMessage()
     */
    @Override
    protected String generateInvalidErrorMessage()
    {
        StringBuffer errorMessage = new StringBuffer(super.generateInvalidErrorMessage());

        errorMessage.append(" Valid values are (case insensitive) ");

        StringBuffer valueList = new StringBuffer();
        T[] constants = ((Class<T>) getParameterDescriptor().getDefaultValue().getClass()).getEnumConstants();

        int index = 1;
        for (T value : constants) {
            if (valueList.length() > 0) {
                if (++index == constants.length) {
                    valueList.append(" or ");
                } else {
                    valueList.append(", ");
                }
            }

            valueList.append('"');
            valueList.append(value);
            valueList.append('"');
        }

        errorMessage.append(valueList);
        errorMessage.append('.');

        return errorMessage.toString();
    }
}
