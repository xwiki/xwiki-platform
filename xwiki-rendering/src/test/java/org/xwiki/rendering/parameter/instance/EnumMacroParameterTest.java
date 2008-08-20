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
import org.xwiki.rendering.macro.parameter.descriptor.EnumMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.instance.EnumMacroParameter;

/**
 * Validate {@link EnumMacroParameter}.
 * 
 * @version $Id: $
 */
public class EnumMacroParameterTest extends
    AbstractMacroParameterTest<EnumMacroParameterDescriptor<EnumMacroParameterTest.TestEnum>>
{
    public enum TestEnum
    {
        VALUE1, value2, Value3
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.AbstractXWikiComponentTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.desc = new EnumMacroParameterDescriptor<TestEnum>("name", "desc", TestEnum.VALUE1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parameter.instance.AbstractMacroParameterTest#generateInvalidErrorMessage(java.lang.String)
     */
    protected String generateInvalidErrorMessage(String stringValue)
    {
        return "Invalid value [" + stringValue + "] for parameter \"name\"."
            + " Valid values are (case insensitive) \"VALUE1\", \"value2\" or \"Value3\".";
    }

    public void testGetValue() throws MacroParameterException
    {
        EnumMacroParameter<TestEnum> param = new EnumMacroParameter<TestEnum>(this.desc, "value3");

        assertEquals(TestEnum.Value3, param.getValue());
    }

    public void testGetValueWhenValueInvalid() throws MacroParameterException
    {
        this.desc.setValueHasToBeValid(false);

        EnumMacroParameter<TestEnum> param = new EnumMacroParameter<TestEnum>(this.desc, "a");

        assertEquals(TestEnum.VALUE1, param.getValue());
    }

    public void testGetValueWhenValueInvalidButHasToBeValid()
    {
        this.desc.setValueHasToBeValid(true);

        EnumMacroParameter<TestEnum> param = new EnumMacroParameter<TestEnum>(this.desc, "a");

        try {
            param.getValue();

            fail("Should throw " + MacroParameterException.class + " exception");
        } catch (MacroParameterException expected) {
            assertErrorMessageInvalid("a", expected);
        }
    }
}
