package org.xwiki.rendering.parameter.instance;

import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.descriptor.IntegerMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.instance.IntegerMacroParameter;
import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;

/**
 * Validate {@link IntegerMacroParameter}.
 * 
 * @version $Id: $
 */
public class IntegerMacroParameterTest extends AbstractRenderingTestCase
{
    IntegerMacroParameterDescriptor intDesc;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.intDesc = new IntegerMacroParameterDescriptor("name", "desc", 5);
    }

    public void testGetValue() throws MacroParameterException
    {
        IntegerMacroParameter param = new IntegerMacroParameter(this.intDesc, "42");

        assertEquals(Integer.valueOf(42), param.getValue());
    }

    public void testGetValueWhenValueInvalid() throws MacroParameterException
    {
        this.intDesc.setValueHasToBeValid(false);

        IntegerMacroParameter param = new IntegerMacroParameter(this.intDesc, "a");

        assertEquals(Integer.valueOf(5), param.getValue());
    }

    public void testGetValueWhenValueInvalidButHasTo()
    {
        this.intDesc.setValueHasToBeValid(true);

        IntegerMacroParameter param = new IntegerMacroParameter(this.intDesc, "a");

        try {
            param.getValue();

            fail("Should throw " + MacroParameterException.class + " exception");
        } catch (MacroParameterException e) {
            // should throw MacroParameterException exception
        }
    }

    public void testGetValueWhenValueInvalidAndNormalized() throws MacroParameterException
    {
        this.intDesc.setValueHasToBeValid(false);
        this.intDesc.setNormalized(true);
        this.intDesc.setMaxValue(6);
        this.intDesc.setMinValue(2);

        IntegerMacroParameter param = new IntegerMacroParameter(this.intDesc, "1");

        assertEquals(Integer.valueOf(2), param.getValue());

        param = new IntegerMacroParameter(this.intDesc, "7");

        assertEquals(Integer.valueOf(6), param.getValue());
    }
}
