package org.xwiki.rendering;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.internal.macro.rss.RssMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.parameter.MacroParameterException;
import org.xwiki.rendering.macro.rss.RssMacroParameters;

/**
 * Unit tests for {@link RssMacro}.
 * 
 * @version $Id$
 * @since 1.9
 */
public class RssMacroFailureConditionsTest extends TestCase
{
    private Macro<RssMacroParameters> macro;
    private RssMacroParameters parameters;
    
    public RssMacroFailureConditionsTest(String name)
    {
        super(name);
        macro = new RssMacro();
        parameters = new RssMacroParameters();
    }

    /**
     * Tests whether the macro throws the appropriate exception 
     * in cases where the required 'feed' parameter is missing.
     */
    public void testRequiredParameterMissing()
    {
        assertNotNull(macro);
        assertNotNull(parameters);
        if (StringUtils.isNotEmpty(parameters.getFeed())) {
            parameters = new RssMacroParameters();
        }
        try {
            macro.execute(parameters, null, null);
            fail("No 'feed' parameter specified, the macro should throw an exception");
        } catch (MacroExecutionException expected) {
            assertTrue("The required 'feed' parameter is missing".equals(expected.getMessage()));
        }
    }        

    /**
     * Tests the macro's behavior when the server hosting the feeds doesn't respond.
     */
    public void testInvalidDocument() throws MacroParameterException
    {
        assertNotNull(macro);
        assertNotNull(parameters);
        
        // We assume that there's no RSS feed at this address.
        parameters.setFeed("http://www.xwiki.org");
        
        try {
            macro.execute(parameters, null, null);
            fail("The macro should throw an 'Invalid document exception'");
        } catch (MacroExecutionException expected) {
            assertTrue(expected.getCause() instanceof IllegalArgumentException); 
            assertEquals("Invalid document", expected.getCause().getMessage());
        }
    }
}
