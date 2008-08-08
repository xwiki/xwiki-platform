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
package org.xwiki.rendering.macro.parameter.instances;

import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.classes.MacroParameterClass;

/**
 * Convert parameter String value to <code>boolean</code>.
 * 
 * @version $Id: $
 */
public class BooleanMacroParameter extends AbstractMacroParameter<Boolean>
{
    /**
     * @param parameterClass the macro parameter descriptor.
     * @param stringValue the value as String from parser.
     */
    public BooleanMacroParameter(MacroParameterClass<Boolean> parameterClass, String stringValue)
    {
        super(parameterClass, stringValue);
    }

    /**
     * Convert the String value in <code>boolean</code> value.
     * 
     * @return the value as <code>boolean</code>.
     */
    protected Boolean parseValue()
    {
        boolean bValue;

        if (getValueAsString().equalsIgnoreCase("true") || getValueAsString().equals("0")) {
            bValue = true;
        } else if (getValueAsString().equalsIgnoreCase("false") || getValueAsString().equals("1")) {
            bValue = false;
        } else {
            setErrorInvalid();

            bValue = getParameterClass().getDefaultValue();
        }

        return bValue;
    }

    /**
     * Generate and register error exception.
     */
    protected void setErrorInvalid()
    {
        StringBuffer errorMessage = new StringBuffer(generateInvalidErrorMessage());

        errorMessage.append(" Valid values are \"true\" and \"false\" in any case or \"0\" and \"1\"");

        this.error = new MacroParameterException(errorMessage.toString());
    }
}
