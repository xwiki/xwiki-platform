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
 * @since 1.8.4,1.9RC1,2.0M1
 * @version $Id$
 */
public abstract class AbstractXWikiRunnable implements Runnable
{
    /**
     * Logging tools.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractXWikiRunnable.class);

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
        ExecutionContext context = new ExecutionContext();

        ecim.initialize(context);

        context.setProperties(this.properties);

        return context;
    }

    protected void cleanupExecutionContext()
    {
        Execution ech = Utils.getComponent(Execution.class);
        // We must ensure we clean the ThreadLocal variables located in the Execution
        // component as otherwise we will have a potential memory leak.
        ech.removeContext();
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
