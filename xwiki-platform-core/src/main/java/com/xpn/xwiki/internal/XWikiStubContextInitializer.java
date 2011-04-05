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
     * {@inheritDoc}
     * 
     * @see org.xwiki.context.ExecutionContextInitializer#initialize(org.xwiki.context.ExecutionContext)
     */
    public void initialize(ExecutionContext context) throws ExecutionContextException
    {
        XWikiContext xcontext = (XWikiContext) context.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);

        if (xcontext == null) {
            // if the XWikiContext is not provided in the Execution context it mean the Execution context is being
            // initialized by a daemon thread
            XWikiContext stubContext = this.stubContextProvider.createStubContext();

            if (stubContext != null) {
                // the stub context has been properly initialized, we inject it in the Execution context
                context.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, stubContext);
            }
        }
    }
}
