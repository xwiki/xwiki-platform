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
public class RssMacroFailureConditionsTests extends TestCase
{
    private Macro<RssMacroParameters> macro;
    private RssMacroParameters parameters;
    
    public RssMacroFailureConditionsTests(String name)
    {
        super(name);
        macro = new RssMacro();
        parameters = new RssMacroParameters();
    }

    /**
     * Tests whether the macro throws the appropriate exception 
     * in cases where the required 'feed' parameter is missing.
     */
    public final void testRequiredParameterMissing()
    {
        assertNotNull(macro);
        assertNotNull(parameters);
        if (StringUtils.isNotEmpty(parameters.getFeed())) {
            parameters = new RssMacroParameters();
        }
        try {
            macro.execute(parameters, null, null);
        } catch (MacroExecutionException ex) {
            assertTrue("The required 'feed' parameter is missing".equals(ex.getMessage()));
            return;
        }
        fail("No 'feed' parameter specified, the macro should throw an exception");
    }
    
    /**
     * Tests the macro's behavior when the server hosting the feeds doesn't respond.
     */
    public final void testConnectionTimeout() throws MacroParameterException
    {
        assertNotNull(macro);
        assertNotNull(parameters);
        
        // We set the feed's URL to an unreachable address.
        parameters.setFeed("http://22.22.22.22");
        
        try {
            macro.execute(parameters, null, null);
        } catch (MacroExecutionException ex) {
            String errorMessage = ex.getMessage();
            assertTrue(errorMessage.startsWith("Connection timeout when trying to reach"));
            return;
        }
        fail("The macro should throw a 'Connection timeout exception'");
    }

    /**
     * Tests the macro's behavior when the server hosting the feeds doesn't respond.
     */
    public final void testInvalidDocument() throws MacroParameterException
    {
        assertNotNull(macro);
        assertNotNull(parameters);
        
        // We assume that there's no RSS feed at this address.
        parameters.setFeed("http://www.xwiki.org");
        
        try {
            macro.execute(parameters, null, null);
        } catch (MacroExecutionException ex) {
            assertTrue(ex.getCause() instanceof IllegalArgumentException); 
            assertTrue("Invalid document".equals(ex.getCause().getMessage()));
            return;
        }
        fail("The macro should throw an 'Ivalid document exception'");
    }
}
