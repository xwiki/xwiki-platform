package com.xpn.xwiki.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

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
     * Logging tools.
     */
    private static final Log LOG = LogFactory.getLog(AbstractXWikiRunnable.class);

    private Map<String, Object> properties = new HashMap<String, Object>();

    protected AbstractXWikiRunnable()
    {

    }

    /**
     * @param propertyName the name of the property to put in the initialized context
     * @param propertyValue the value of the property to put in the initialized context
     */
    protected AbstractXWikiRunnable(String propertyName, Object propertyValue)
    {
        this.properties.put(propertyName, propertyValue);
    }

    /**
     * @param properties properties to put in the initialized context
     */
    protected AbstractXWikiRunnable(Map<String, Object> properties)
    {
        this.properties.putAll(properties);
    }

    /**
     * Initialize execution context for the current thread.
     * 
     * @return the new execution context
     * @throws ExecutionContextException error when try to initialize execution context
     */
    protected ExecutionContext initExecutionContext() throws ExecutionContextException
    {
        ExecutionContextManager ecim = Utils.getComponent(ExecutionContextManager.class);
        Execution execution = Utils.getComponent(Execution.class);

        ExecutionContext ec = new ExecutionContext();

        ecim.initialize(ec);

        ec.setProperties(this.properties);

        execution.setContext(ec);

        return ec;
    }

    protected void cleanupExecutionContext()
    {
        Execution ech = Utils.getComponent(Execution.class);
        // We must ensure we clean the ThreadLocal variables located in the Execution
        // component as otherwise we will have a potential memory leak.
        ech.removeContext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Runnable#run()
     */
    public final void run()
    {
        try {
            // initialize execution context
            initExecutionContext();
        } catch (ExecutionContextException e) {
            LOG.error("Failed to initialize execution context", e);
            return;
        }

        try {
            // call run
            runInternal();
        } finally {
            // cleanup execution context
            cleanupExecutionContext();
        }
    }

    protected abstract void runInternal();
}
