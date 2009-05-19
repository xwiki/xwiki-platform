package com.xpn.xwiki.util;

import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;

/**
 * Base class for any XWiki daemon class. It provide tools to initialize execution context.
 * 
 * @since 1.8.4,1.9RC1,2.0M1
 * @version $Id$
 */
public abstract class AbstractXWikiRunnable implements Runnable
{
    /**
     * Initialize execution context for the current thread.
     * 
     * @return the new execution context
     * @throws ExecutionContextException error when try to initialize execution context
     */
    protected ExecutionContext initExecutionContext() throws ExecutionContextException
    {
        ExecutionContextManager ecim = (ExecutionContextManager) Utils.getComponent(ExecutionContextManager.ROLE);
        Execution execution = (Execution) Utils.getComponent(Execution.ROLE);

        ExecutionContext ec = new ExecutionContext();

        ecim.initialize(ec);
        execution.setContext(ec);

        return ec;
    }

    /**
     * Initialize execution context for the current thread and put the XWikiContext in it.
     * 
     * @param context the old XWiki context
     * @return the new execution context
     * @throws ExecutionContextException error when try to initialize execution context
     */
    protected ExecutionContext initExecutionContext(XWikiContext context) throws ExecutionContextException
    {
        ExecutionContext ec = initExecutionContext();

        // Bridge with old XWiki Context, required for old code.
        ec.setProperty("xwikicontext", context);

        return ec;
    }

    protected void cleanupExecutionContext()
    {
        Execution ech = (Execution) Utils.getComponent(Execution.ROLE);
        // We must ensure we clean the ThreadLocal variables located in the Execution
        // component as otherwise we will have a potential memory leak.
        ech.removeContext();
    }
}
