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
package com.xpn.xwiki.internal;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * Provide current {@link XWikiContext} or create one from {@link XWikiStubContextProvider} if there is no current one
 * yet.
 *
 * @version $Id$
 * @see ReadOnlyXWikiContextProvider
 */
@Component
@Singleton
public class XWikiContextProvider implements Provider<XWikiContext>
{
    /**
     * Used to create a new {@link XWikiContext}.
     */
    @Inject
    private XWikiStubContextProvider contextProvider;

    /**
     * Used to access current {@link XWikiContext}.
     */
    @Inject
    private Execution execution;

    @Inject
    private Container container;

    @Override
    public XWikiContext get()
    {
        return getXWikiContext();
    }

    /**
     * @return current XWikiContext or new one
     */
    private XWikiContext getXWikiContext()
    {
        XWikiContext xcontext;

        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            xcontext = (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);

            if (xcontext == null) {
                xcontext = this.contextProvider.createStubContext();

                // Set the XWiki context
                this.execution.getContext().setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xcontext);

                // Set the stub request and the response
                if (this.container.getRequest() == null) {
                    this.container.setRequest(new ServletRequest(xcontext.getRequest()));
                }
                if (this.container.getResponse() == null) {
                    this.container.setResponse(new ServletResponse(xcontext.getResponse()));
                }
            }
        } else {
            xcontext = null;
        }

        return xcontext;
    }
}
