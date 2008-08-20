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
import org.xwiki.rendering.macro.parameter.descriptor.BooleanMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.instance.BooleanMacroParameter;

/**
 * Validate {@link BooleanMacroParameter}.
 * 
 * @version $Id: $
 */
public class BooleanMacroParameterTest extends AbstractMacroParameterTest<BooleanMacroParameterDescriptor>
{
    BooleanMacroParameterDescriptor intDesc;

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.AbstractXWikiComponentTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.intDesc = new BooleanMacroParameterDescriptor("name", "desc", true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parameter.instance.AbstractMacroParameterTest#generateInvalidErrorMessage(java.lang.String)
     */
    protected String generateInvalidErrorMessage(String stringValue)
    {
        return "Invalid value [" + stringValue + "] for parameter \"name\"."
            + " Valid values are \"true\" and \"false\" (case insensitive) or \"0\" and \"1\".";
    }

    public void testGetValue() throws MacroParameterException
    {
        BooleanMacroParameter param = new BooleanMacroParameter(this.intDesc, "faLSe");

        assertEquals(Boolean.FALSE, param.getValue());

        param = new BooleanMacroParameter(this.intDesc, "TruE");

        assertEquals(Boolean.TRUE, param.getValue());

        param = new BooleanMacroParameter(this.intDesc, "0");

        assertEquals(Boolean.FALSE, param.getValue());

        param = new BooleanMacroParameter(this.intDesc, "1");

        assertEquals(Boolean.TRUE, param.getValue());
    }

    public void testGetValueWhenValueInvalid() throws MacroParameterException
    {
        this.intDesc.setValueHasToBeValid(false);

        BooleanMacroParameter param = new BooleanMacroParameter(this.intDesc, "a");

        assertEquals(Boolean.TRUE, param.getValue());
    }

    public void testGetValueWhenValueInvalidButHasToBeValid()
    {
        this.intDesc.setValueHasToBeValid(true);

        BooleanMacroParameter param = new BooleanMacroParameter(this.intDesc, "a");

        try {
            param.getValue();

            fail("Should throw " + MacroParameterException.class + " exception");
        } catch (MacroParameterException expected) {
            assertErrorMessageInvalid("a", expected);
        }
    }
}
