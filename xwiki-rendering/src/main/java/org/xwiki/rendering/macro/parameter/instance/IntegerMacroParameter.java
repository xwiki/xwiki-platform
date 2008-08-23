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

import org.xwiki.rendering.macro.parameter.descriptor.NumberMacroParameterDescriptor;

/**
 * Convert parameter String value to <code>int</code>, <code>long</code>, <code>float</code> or
 * <code>double</code>.
 * 
 * @version $Id$
 */
public class IntegerMacroParameter extends AbstractNumberMacroParameter<Integer>
{
    /**
     * @param parameterDescriptor the macro parameter descriptor.
     * @param stringValue the value as String from parser.
     */
    public IntegerMacroParameter(NumberMacroParameterDescriptor<Integer> parameterDescriptor, String stringValue)
    {
        super(parameterDescriptor, stringValue);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.instance.AbstractMacroParameter#parseValue()
     */
    @Override
    protected Integer parseValue()
    {
        int value;

        NumberMacroParameterDescriptor<Integer> parameterClass = getNumberParameterDescriptor();

        Integer def = parameterClass.getDefaultValue();

        try {
            value = Integer.valueOf(getValueAsString());

            if (parameterClass.getMinValue() != null && value < parameterClass.getMinValue().intValue()) {
                setErrorTooLow();
                value = parameterClass.isNormalized() ? parameterClass.getMinValue().intValue() : def;
            } else if (parameterClass.getMaxValue() != null && value > parameterClass.getMaxValue().intValue()) {
                setErrorTooHigh();
                value = parameterClass.isNormalized() ? parameterClass.getMaxValue().intValue() : def;
            }
        } catch (NumberFormatException e) {
            setErrorInvalid(e);

            value = def;
        }

        return value;
    }
}
