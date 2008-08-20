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

import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.descriptor.IntegerMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.instance.IntegerMacroParameter;

/**
 * Validate {@link IntegerMacroParameter}.
 * 
 * @version $Id: $
 */
public class IntegerMacroParameterTest extends AbstractNumberMacroParameterTest<IntegerMacroParameterDescriptor>
{
    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.AbstractXWikiComponentTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.desc = new IntegerMacroParameterDescriptor("name", "desc", 5);
    }

    public void testGetValue() throws MacroParameterException
    {
        IntegerMacroParameter param = new IntegerMacroParameter(this.desc, "42");

        assertEquals(Integer.valueOf(42), param.getValue());
    }

    public void testGetValueWhenValueInvalid() throws MacroParameterException
    {
        this.desc.setValueHasToBeValid(false);

        IntegerMacroParameter param = new IntegerMacroParameter(this.desc, "a");

        assertEquals(Integer.valueOf(5), param.getValue());
    }

    public void testGetValueWhenValueInvalidButHasToBeValid()
    {
        this.desc.setValueHasToBeValid(true);

        IntegerMacroParameter param = new IntegerMacroParameter(this.desc, "a");

        try {
            param.getValue();

            fail("Should throw " + MacroParameterException.class + " exception");
        } catch (MacroParameterException expected) {
            assertErrorMessageInvalid("a", expected);
        }
    }

    public void testGetValueWhenValueTooLowButHasToBeValid()
    {
        this.desc.setValueHasToBeValid(true);
        this.desc.setMinValue(2);

        IntegerMacroParameter param = new IntegerMacroParameter(this.desc, "1");

        try {
            param.getValue();

            fail("Should throw " + MacroParameterException.class + " exception");
        } catch (MacroParameterException expected) {
            assertErrorMessageTooLow("1", expected);
        }
    }

    public void testGetValueWhenValueTooHighButHasToBeValid()
    {
        this.desc.setValueHasToBeValid(true);
        this.desc.setMaxValue(6);

        IntegerMacroParameter param = new IntegerMacroParameter(this.desc, "7");

        try {
            param.getValue();

            fail("Should throw " + MacroParameterException.class + " exception");
        } catch (MacroParameterException expected) {
            assertErrorMessageTooHigh("7", expected);
        }
    }

    public void testGetValueWhenValueInvalidAndNormalized() throws MacroParameterException
    {
        this.desc.setValueHasToBeValid(false);
        this.desc.setNormalized(true);
        this.desc.setMinValue(2);
        this.desc.setMaxValue(6);

        IntegerMacroParameter param = new IntegerMacroParameter(this.desc, "1");

        assertEquals(Integer.valueOf(2), param.getValue());

        param = new IntegerMacroParameter(this.desc, "7");

        assertEquals(Integer.valueOf(6), param.getValue());
    }
}
