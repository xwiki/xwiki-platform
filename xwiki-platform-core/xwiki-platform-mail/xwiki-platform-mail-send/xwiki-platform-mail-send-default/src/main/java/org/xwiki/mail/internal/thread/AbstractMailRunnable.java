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
package org.xwiki.mail.internal.thread;

import javax.inject.Inject;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.mail.MailSenderConfiguration;

import com.xpn.xwiki.XWikiContext;

/**
 * Common code that sets up a XWiki Context in a Thread.
 *
 * @version $Id$
 * @since 6.4
 */
public abstract class AbstractMailRunnable implements MailRunnable
{
    @Inject
    protected Logger logger;

    @Inject
    protected MailSenderConfiguration configuration;

    /**
     * Allows to stop this thread, used in {@link #stopProcessing()}.
     */
    protected volatile boolean shouldStop;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private Execution execution;

    @Inject
    private ExecutionContextManager executionContextManager;

    protected void prepareContext(ExecutionContext executionContext) throws ExecutionContextException
    {
        // Isolate the context when sending a mail by cloning it
        ExecutionContext clonedExecutionContext = this.executionContextManager.clone(executionContext);

        // The above clone just creates and initializes an empty XWiki Context, so it needs special handling.
        XWikiContext xwikiContext = (XWikiContext) executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        // This is still a shallow clone, but at least for stuff like wikiID and userReference it gets the job done.
        XWikiContext clonedXWikiContext = xwikiContext.clone();
        clonedXWikiContext.setUserReference(xwikiContext.getUserReference());
        clonedXWikiContext.setWikiId(xwikiContext.getWikiId());
        clonedXWikiContext.setWiki(xwikiContext.getWiki());
        clonedXWikiContext.setLocale(xwikiContext.getLocale());
        clonedXWikiContext.setMainXWiki(xwikiContext.getMainXWiki());
        // FIXME: We probably need a deeper cloning of XWikiContext and we probably need it in a better place.
        clonedExecutionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, clonedXWikiContext);

        try {
            this.execution.setContext(clonedExecutionContext);
        } catch (Exception e) {
            // If inheritance fails, we will get an unchecked exception here. So we'll wrap it in an
            // ExecutionContextException.
            throw new ExecutionContextException("Failed to set the execution context.", e);
        }
    }

    protected void removeContext()
    {
        this.execution.removeContext();
    }

    @Override
    public void stopProcessing()
    {
        this.shouldStop = true;
    }
}
