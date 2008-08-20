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
package org.xwiki.rendering.parameter.instance;

import org.xwiki.rendering.macro.parameter.descriptor.NumberMacroParameterDescriptor;

/**
 * Base class to validate {@link MacroParameter} with descriptor implementing {@link NumberMacroParameterDescriptor}
 * implementations.
 * 
 * @version $Id: $
 */
public abstract class AbstractNumberMacroParameterTest<T extends NumberMacroParameterDescriptor> extends
    AbstractMacroParameterTest<T>
{
    protected T desc;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parameter.instance.AbstractMacroParameterTest#generateInvalidErrorMessage(java.lang.String)
     */
    protected String generateInvalidErrorMessage(String stringValue)
    {
        return "Invalid value [" + stringValue + "] for parameter \"name\". The value must be a number.";
    }

    protected void assertErrorMessageTooHigh(String stringValue, Throwable expected)
    {
        StringBuffer errorMessage = new StringBuffer(super.generateInvalidErrorMessage(stringValue));

        errorMessage.append(" The value is too high. The highest valid value is " + this.desc.getMaxValue() + ".");

        assertEquals(errorMessage.toString(), expected.getMessage());
    }

    protected void assertErrorMessageTooLow(String stringValue, Throwable expected)
    {
        StringBuffer errorMessage = new StringBuffer(super.generateInvalidErrorMessage(stringValue));

        errorMessage.append(" The value is too low. The lowest valid value is " + this.desc.getMinValue() + ".");

        assertEquals(errorMessage.toString(), expected.getMessage());
    }
}
