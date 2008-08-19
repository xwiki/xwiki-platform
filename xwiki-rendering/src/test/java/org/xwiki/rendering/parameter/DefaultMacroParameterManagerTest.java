package org.xwiki.rendering.parameter;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.rendering.macro.parameter.DefaultMacroParameterManager;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.MacroParameterNotSupportedException;
import org.xwiki.rendering.macro.parameter.MacroParameterRequiredException;
import org.xwiki.rendering.macro.parameter.descriptor.BooleanMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.descriptor.EnumMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.descriptor.IntegerMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.descriptor.StringMacroParameterDescriptor;
import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;

/**
 * Validate {@link DefaultMacroParameterManager}.
 * 
 * @version $Id$
 */
public class DefaultMacroParameterManagerTest extends AbstractRenderingTestCase
{
    DefaultMacroParameterManager dmpm = new DefaultMacroParameterManager();

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

        IntegerMacroParameterDescriptor int1ParamClass = new IntegerMacroParameterDescriptor("int1", "int1 desc", 5);
        this.dmpm.registerParameterDescriptor(int1ParamClass);

        EnumMacroParameterDescriptor<TestEnum> enumParamClass =
            new EnumMacroParameterDescriptor<TestEnum>("enum1", "enum1 desc", TestEnum.VALUE1);
        enumParamClass.setRequired(true);
        this.dmpm.registerParameterDescriptor(enumParamClass);

        BooleanMacroParameterDescriptor booleanParamClass =
            new BooleanMacroParameterDescriptor("boolean1", "boolean1 desc", true);
        this.dmpm.registerParameterDescriptor(booleanParamClass);

        StringMacroParameterDescriptor stringParamClass =
            new StringMacroParameterDescriptor("string1", "string1 desc", "string1");
        stringParamClass.setRequired(true);
        this.dmpm.registerParameterDescriptor(stringParamClass);
    }

    public void testLoad() throws MacroParameterException
    {
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("int1", "3");
        parameters.put("enum1", "value3");
        parameters.put("boOlean1", "fAlse");
        parameters.put("string1", "string1 value");
        parameters.put("notsupported", "notsupported value");

        this.dmpm.load(parameters);

        assertEquals(this.dmpm.<Integer> getParameterValue("int1"), Integer.valueOf(3));
        assertEquals(this.dmpm.<TestEnum> getParameterValue("enum1"), TestEnum.Value3);
        assertEquals(this.dmpm.<Boolean> getParameterValue("boolean1"), Boolean.valueOf(false));
        assertEquals(this.dmpm.<String> getParameterValue("strIng1"), "string1 value");
    }

    /**
     * Validate that an exception is raised when trying to get a not supported parameter.
     */
    public void testMacroParameterNotSupportedException() throws MacroParameterException
    {
        try {
            this.dmpm.getParameterValue("notsupported");

            fail("Should throw " + MacroParameterNotSupportedException.class + " exception");
        } catch (MacroParameterNotSupportedException e) {
            // should throw MacroParameterNotSupportedException exception
        }
    }

    /**
     * Validate that an exception is raised when trying to get a required parameter not loaded.
     */
    public void testMacroParameterRequiredException() throws MacroParameterException
    {
        try {
            assertEquals(this.dmpm.<TestEnum> getParameterValue("enum1"), TestEnum.Value3);

            fail("Should throw " + MacroParameterRequiredException.class + " exception");
        } catch (MacroParameterRequiredException e) {
            // should throw MacroParameterRequiredException exception
        }
    }

    /**
     * Validate that the default value is returned for a not required parameter not loaded.
     */
    public void testDefaultValue() throws MacroParameterException
    {
        assertEquals(this.dmpm.<Integer> getParameterValue("int1"), Integer.valueOf(5));
    }
}
