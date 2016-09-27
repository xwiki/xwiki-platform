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
package com.xpn.xwiki.util;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

import com.xpn.xwiki.web.Utils;

/**
 * Base class for any XWiki daemon class. It provide tools to initialize execution context.
 *
 * @since 1.8.4
 * @since 1.9RC1
 * @since 2.0M1
 * @version $Id$
 */
public abstract class AbstractXWikiRunnable implements Runnable
{
    /**
     * Logging tools.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractXWikiRunnable.class);

    private final Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * A reference to the Execution component to be used in {@link #cleanupExecutionContext()} when this thread is
     * stopped. Since we're not inside a component we cannot inject this dependency so we initialize it in
     * {@link #initExecutionContext()}. The reason we keep this reference is because this thread can be stopped after
     * the Component Manager disposes its components so a lookup in {@link #cleanupExecutionContext()} can fail.
     */
    private Execution execution;

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
     * Lets subclasses declare execution context properties.
     *
     * @param executionContext the execution context.
     */
    protected void declareProperties(ExecutionContext executionContext)
    {
    }

    /**
     * Initialize execution context for the current thread.
     *
     * @return the new execution context
     * @throws ExecutionContextException error when try to initialize execution context
     */
    protected ExecutionContext initExecutionContext() throws ExecutionContextException
    {
        // Keep a reference to the Execution component to avoid a lookup in #cleanupExecutionContext() in case this
        // thread is stopped after the Component Manager disposes its components.
        this.execution = Utils.getComponent(Execution.class);

        ExecutionContextManager ecim = Utils.getComponent(ExecutionContextManager.class);
        ExecutionContext context = new ExecutionContext();

        declareProperties(context);

        ecim.initialize(context);

        context.setProperties(this.properties);

        return context;
    }

    protected void cleanupExecutionContext()
    {
        // We must ensure we clean the ThreadLocal variables located in the Execution
        // component as otherwise we will have a potential memory leak.
        this.execution.removeContext();
    }

    @Override
    public final void run()
    {
        try {
            // initialize execution context
            initExecutionContext();
        } catch (ExecutionContextException e) {
            LOGGER.error("Failed to initialize execution context", e);
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
