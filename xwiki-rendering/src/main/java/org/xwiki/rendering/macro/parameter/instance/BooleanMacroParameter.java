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
 * Convert parameter String value to <code>boolean</code>.
 * 
 * @version $Id$
 */
public class BooleanMacroParameter extends AbstractMacroParameter<Boolean>
{
    /**
     * @param parameterDescriptor the macro parameter descriptor.
     * @param stringValue the value as String from parser.
     */
    public BooleanMacroParameter(MacroParameterDescriptor<Boolean> parameterDescriptor, String stringValue)
    {
        super(parameterDescriptor, stringValue);
    }

    /**
     * Convert the String value in <code>boolean</code> value.
     * 
     * @return the value as <code>boolean</code>.
     */
    protected Boolean parseValue()
    {
        boolean bValue;

        if (getValueAsString().equalsIgnoreCase("true") || getValueAsString().equals("1")) {
            bValue = true;
        } else if (getValueAsString().equalsIgnoreCase("false") || getValueAsString().equals("0")) {
            bValue = false;
        } else {
            setErrorInvalid();

            bValue = getParameterDescriptor().getDefaultValue();
        }

        return bValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.parameter.instance.AbstractMacroParameter#generateInvalidErrorMessage()
     */
    protected String generateInvalidErrorMessage()
    {
        StringBuffer errorMessage = new StringBuffer(super.generateInvalidErrorMessage());

        errorMessage.append(" Valid values are \"true\" and \"false\" (case insensitive) or \"0\" and \"1\".");

        return errorMessage.toString();
    }
}
