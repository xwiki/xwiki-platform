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
package org.xwiki.rendering.parameter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.rendering.macro.AbstractMacroDescriptor;
import org.xwiki.rendering.macro.parameter.DefaultMacroParameters;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.MacroParameterNotSupportedException;
import org.xwiki.rendering.macro.parameter.MacroParameterRequiredException;
import org.xwiki.rendering.macro.parameter.MacroParameters;
import org.xwiki.rendering.macro.parameter.descriptor.BooleanMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.descriptor.EnumMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.descriptor.IntegerMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.descriptor.StringMacroParameterDescriptor;
import org.xwiki.rendering.parameter.instance.EnumMacroParameterTest.TestEnum;
import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;

/**
 * Validate {@link DefaultMacroParameters}.
 * 
 * @version $Id$
 */
public class DefaultMacroParametersTest extends AbstractRenderingTestCase
{
    private TestMacroDescriptor macroDescriptor = new TestMacroDescriptor();

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.AbstractXWikiComponentTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.macroDescriptor = new TestMacroDescriptor();
    }

    public void testCreateMacroParameters() throws MacroParameterException
    {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("int1", "3");
        parameters.put("enum1", "value3");
        parameters.put("boOlean1", "fAlse");
        parameters.put("string1", "string1 value");
        parameters.put("notsupported", "notsupported value");

        MacroParameters macroParameters = this.macroDescriptor.createMacroParameters(parameters);

        assertEquals(macroParameters.<Integer> getParameterValue("int1"), Integer.valueOf(3));
        assertEquals(macroParameters.<TestEnum> getParameterValue("enum1"), TestEnum.Value3);
        assertEquals(macroParameters.<Boolean> getParameterValue("boolean1"), Boolean.valueOf(false));
        assertEquals(macroParameters.<String> getParameterValue("strIng1"), "string1 value");
    }

    /**
     * Validate that an exception is raised when trying to get a not supported parameter.
     */
    public void testGetParameterValueWhenNotSupportedParameter() throws MacroParameterException
    {
        MacroParameters macroParameters =
            this.macroDescriptor.createMacroParameters(Collections.<String, String> emptyMap());

        try {
            macroParameters.getParameterValue("notsupported");

            fail("Should throw " + MacroParameterNotSupportedException.class + " exception");
        } catch (MacroParameterNotSupportedException e) {
            // should throw MacroParameterNotSupportedException exception
        }
    }

    /**
     * Validate that an exception is raised when trying to get a required parameter not loaded.
     */
    public void testGetParameterValueWhenRequiredParameterMissing() throws MacroParameterException
    {
        MacroParameters macroParameters =
            this.macroDescriptor.createMacroParameters(Collections.<String, String> emptyMap());

        try {
            assertEquals(macroParameters.<TestEnum> getParameterValue("enum1"), TestEnum.Value3);

            fail("Should throw " + MacroParameterRequiredException.class + " exception");
        } catch (MacroParameterRequiredException e) {
            // should throw MacroParameterRequiredException exception
        }
    }

    /**
     * Validate that the default value is returned for a not required parameter not loaded.
     */
    public void testGetParameterValueWhenParameterMissing() throws MacroParameterException
    {
        MacroParameters macroParameters =
            this.macroDescriptor.createMacroParameters(Collections.<String, String> emptyMap());

        assertEquals(macroParameters.<Integer> getParameterValue("int1"), Integer.valueOf(5));
    }
}

class TestMacroDescriptor extends AbstractMacroDescriptor<MacroParameters>
{
    public TestMacroDescriptor()
    {
        IntegerMacroParameterDescriptor int1ParamClass = new IntegerMacroParameterDescriptor("int1", "int1 desc", 5);
        registerParameterDescriptor(int1ParamClass);

        EnumMacroParameterDescriptor<TestEnum> enumParamClass =
            new EnumMacroParameterDescriptor<TestEnum>("enum1", "enum1 desc", TestEnum.VALUE1);
        enumParamClass.setRequired(true);
        registerParameterDescriptor(enumParamClass);

        BooleanMacroParameterDescriptor booleanParamClass =
            new BooleanMacroParameterDescriptor("boolean1", "boolean1 desc", true);
        registerParameterDescriptor(booleanParamClass);

        StringMacroParameterDescriptor stringParamClass =
            new StringMacroParameterDescriptor("string1", "string1 desc", "string1");
        stringParamClass.setRequired(true);
        registerParameterDescriptor(stringParamClass);
    }

    public String getDescription()
    {
        return "test macro descriptor";
    }
}
