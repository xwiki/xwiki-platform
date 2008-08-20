package org.xwiki.rendering.parameter.instance;

import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.parameter.descriptor.BooleanMacroParameterDescriptor;
import org.xwiki.rendering.macro.parameter.instance.BooleanMacroParameter;
import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;

/**
 * Validate {@link BooleanMacroParameter}.
 * 
 * @version $Id: $
 */
public class BooleanMacroParameterTest extends AbstractRenderingTestCase
{
    BooleanMacroParameterDescriptor intDesc;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.intDesc = new BooleanMacroParameterDescriptor("name", "desc", true);
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

    public void testGetValueWhenValueInvalidButHasTo()
    {
        this.intDesc.setValueHasToBeValid(true);

        BooleanMacroParameter param = new BooleanMacroParameter(this.intDesc, "a");

        try {
            param.getValue();

            fail("Should throw " + MacroParameterException.class + " exception");
        } catch (MacroParameterException e) {
            // should throw MacroParameterException exception
        }
    }
}
