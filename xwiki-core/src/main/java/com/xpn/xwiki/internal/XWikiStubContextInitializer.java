package com.xpn.xwiki.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * An automatic XWikiContext stub injecter for ExecutionContext for daemons unable to create a proper XWikiContext (no
 * real request information or not even know about XWikiContext like components).
 * 
 * @see XWikiStubContextProvider
 * @version $Id$
 * @since 2.0M3
 */
@Component("XWikiStubContextInitializer")
public class XWikiStubContextInitializer implements ExecutionContextInitializer
{
    /**
     * Generate stub XWikiContext.
     */
    @Requirement
    private XWikiStubContextProvider stubContextProvider;

    /**
     * Indicate if a valid XWikiContext has already been provided to {@link XWikiStubContextProvider}.
     */
    boolean initialized = false;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.context.ExecutionContextInitializer#initialize(org.xwiki.context.ExecutionContext)
     */
    public void initialize(ExecutionContext context) throws ExecutionContextException
    {
        XWikiContext xcontext = (XWikiContext) context.getProperty("xwikicontext");

        if (!this.initialized) {
            if (xcontext != null) {
                // initialize the XWikiStubContextProvider with the context of the first request
                this.stubContextProvider.initialize(xcontext);

                this.initialized = true;
            }
        } else if (xcontext == null) {
            context.setProperty("xwikicontext", this.stubContextProvider.createStubContext());
        }
    }
}
