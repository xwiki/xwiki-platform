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

import java.text.MessageFormat;

import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.descriptor.NumberMacroParameterDescriptor;

/**
 * Base class for converting parameter String value to number.
 * 
 * @param <T> the type of number.
 * @version $Id$
 */
public abstract class AbstractNumberMacroParameter<T extends Number> extends AbstractMacroParameter<T>
{
    /**
     * @param parameterDescriptor the macro parameter descriptor.
     * @param stringValue the value as String from parser.
     */
    public AbstractNumberMacroParameter(NumberMacroParameterDescriptor<T> parameterDescriptor, String stringValue)
    {
        super(parameterDescriptor, stringValue);
    }

    /**
     * @return the macro parameter descriptor.
     */
    public NumberMacroParameterDescriptor<T> getNumberParameterDescriptor()
    {
        return (NumberMacroParameterDescriptor<T>) getParameterDescriptor();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.instance.AbstractMacroParameter#generateInvalidErrorMessage()
     */
    protected String generateInvalidErrorMessage()
    {
        StringBuffer errorMessage = new StringBuffer(super.generateInvalidErrorMessage());

        errorMessage.append(" The value must be a number.");

        return errorMessage.toString();
    }

    /**
     * Generate and register error exception.
     */
    protected void setErrorTooHigh()
    {
        StringBuffer errorMessage = new StringBuffer(super.generateInvalidErrorMessage());

        errorMessage.append(MessageFormat.format(" The value is too high. The highest valid value is {0}.",
            getNumberParameterDescriptor().getMaxValue()));

        this.error = new MacroParameterException(errorMessage.toString());
    }

    /**
     * Generate and register error exception.
     */
    protected void setErrorTooLow()
    {
        StringBuffer errorMessage = new StringBuffer(super.generateInvalidErrorMessage());

        errorMessage.append(MessageFormat.format(" The value is too low. The lowest valid value is {0}.",
            getNumberParameterDescriptor().getMinValue()));

        this.error = new MacroParameterException(errorMessage.toString());
    }
}
