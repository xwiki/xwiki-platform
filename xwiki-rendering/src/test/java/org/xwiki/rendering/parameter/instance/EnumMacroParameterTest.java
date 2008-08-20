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
import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;

/**
 * Validate {@link EnumMacroParameter}.
 * 
 * @version $Id: $
 */
public class EnumMacroParameterTest extends AbstractRenderingTestCase
{
    public enum TestEnum
    {
        VALUE1, value2, Value3
    }

    EnumMacroParameterDescriptor<TestEnum> intDesc;

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.AbstractXWikiComponentTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.intDesc = new EnumMacroParameterDescriptor<TestEnum>("name", "desc", TestEnum.VALUE1);
    }

    public void testGetValue() throws MacroParameterException
    {
        EnumMacroParameter<TestEnum> param = new EnumMacroParameter<TestEnum>(this.intDesc, "value3");

        assertEquals(TestEnum.Value3, param.getValue());
    }

    public void testGetValueWhenValueInvalid() throws MacroParameterException
    {
        this.intDesc.setValueHasToBeValid(false);

        EnumMacroParameter<TestEnum> param = new EnumMacroParameter<TestEnum>(this.intDesc, "a");

        assertEquals(TestEnum.VALUE1, param.getValue());
    }

    public void testGetValueWhenValueInvalidButHasTo()
    {
        this.intDesc.setValueHasToBeValid(true);

        EnumMacroParameter<TestEnum> param = new EnumMacroParameter<TestEnum>(this.intDesc, "a");

        try {
            param.getValue();

            fail("Should throw " + MacroParameterException.class + " exception");
        } catch (MacroParameterException e) {
            // should throw MacroParameterException exception
        }
    }
}
