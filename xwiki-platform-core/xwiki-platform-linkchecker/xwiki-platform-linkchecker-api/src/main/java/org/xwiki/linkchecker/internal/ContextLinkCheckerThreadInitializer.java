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
package org.xwiki.linkchecker.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.rendering.transformation.linkchecker.LinkCheckerThreadInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * Initialize the Execution Context so that Event Listeners that listen to
 * {@link org.xwiki.rendering.transformation.linkchecker.InvalidURLEvent} are sure to have a properly set up
 * Execution Context.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named("context")
@Singleton
public class ContextLinkCheckerThreadInitializer implements LinkCheckerThreadInitializer
{
    /**
     * Used to get the Execution Context.
     */
    @Inject
    private Execution execution;

    /**
     * Used to create a new Execution Context from scratch.
     */
    @Inject
    private ExecutionContextManager executionContextManager;

    /**
     * Used to clone the XWiki Context.
     */
    @Inject
    private XWikiStubContextProvider stubContextProvider;

    @Override
    public void initialize()
    {
        ExecutionContext context = this.execution.getContext();
        if (context == null) {
            // Create a clean Execution Context
            context = new ExecutionContext();

            try {
                this.executionContextManager.initialize(context);
            } catch (ExecutionContextException e) {
                throw new RuntimeException("Failed to initialize the Execution Context", e);
            }

            // Bridge with old XWiki Context, required for old code.
            XWikiContext xwikiContext = this.stubContextProvider.createStubContext();
            context.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xwikiContext);

            this.execution.pushContext(context);
        }
    }
}
