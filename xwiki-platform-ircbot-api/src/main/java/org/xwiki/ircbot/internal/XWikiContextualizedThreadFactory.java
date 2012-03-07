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
package org.xwiki.ircbot.internal;

import java.util.concurrent.ThreadFactory;

import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * Create new threads that have a proper XWiki Execution Context.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class XWikiContextualizedThreadFactory implements ThreadFactory
{
    private Execution execution;

    private ExecutionContextManager executionContextManager;

    private XWikiStubContextProvider stubContextProvider;

    public XWikiContextualizedThreadFactory(Execution execution,
        ExecutionContextManager executionContextManager,
        XWikiStubContextProvider stubContextProvider)
    {
        this.execution = execution;
        this.executionContextManager = executionContextManager;
        this.stubContextProvider = stubContextProvider;
    }

    private class XWikiContextualizedThread extends Thread
    {
        public XWikiContextualizedThread(Runnable runnable)
        {
            super(runnable);
        }

        @Override
        public void run()
        {
            ExecutionContext context = execution.getContext();
            if (context == null) {
                // Create a clean Execution Context
                context = new ExecutionContext();

                try {
                    executionContextManager.initialize(context);
                } catch (ExecutionContextException e) {
                    throw new RuntimeException("Failed to initialize IRC Bot's execution context", e);
                }

                // Bridge with old XWiki Context, required for old code.
                context.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, stubContextProvider.createStubContext());

                execution.pushContext(context);
            }

            super.run();
        }
    }

    @Override
    public Thread newThread(Runnable runnable)
    {
        return new XWikiContextualizedThread(runnable);
    }
}
