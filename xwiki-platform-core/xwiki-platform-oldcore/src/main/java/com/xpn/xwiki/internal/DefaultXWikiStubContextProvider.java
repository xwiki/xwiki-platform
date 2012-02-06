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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponse;
import com.xpn.xwiki.web.XWikiServletResponseStub;

/**
 * Default implementation of XWikiStubContextProvider.
 * 
 * @todo make DefaultXWikiStubContextProvider able to generate a stub context from scratch some way, it will need some
 *       refactor around XWiki class for this to be possible. The current limitation is that without a first request
 *       this provider is unusable.
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Singleton
public class DefaultXWikiStubContextProvider implements XWikiStubContextProvider
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The initial stub XWikiContext.
     */
    private XWikiContext initialContext;

    @Override
    public void initialize(XWikiContext context)
    {
        this.initialContext = (XWikiContext) context.clone();

        this.initialContext.setCacheDuration(0);

        this.initialContext.setUserReference(null);
        this.initialContext.setLanguage(null);
        this.initialContext.setDatabase(context.getMainXWiki());

        // Cleanup
        this.initialContext.flushClassCache();
        this.initialContext.flushArchiveCache();

        // We are sure the context request is a real servlet request
        // So we force the dummy request with the current host
        if (this.initialContext.getRequest() != null) {
            XWikiServletRequestStub initialRequest = new XWikiServletRequestStub();
            initialRequest.setHost(this.initialContext.getRequest().getHeader("x-forwarded-host"));
            initialRequest.setScheme(this.initialContext.getRequest().getScheme());
            XWikiServletRequest request = new XWikiServletRequest(initialRequest);
            this.initialContext.setRequest(request);
        }

        // Get rid of the real response
        if (this.initialContext.getResponse() != null) {
            XWikiServletResponseStub initialResponse = new XWikiServletResponseStub();
            // anything to keep ?
            XWikiServletResponse response = new XWikiServletResponse(initialResponse);
            this.initialContext.setResponse(response);
        }

        this.logger.debug("Stub context initialized.");
    }

    @Override
    public XWikiContext createStubContext()
    {
        XWikiContext stubContext;

        if (this.initialContext != null) {
            stubContext = (XWikiContext) this.initialContext.clone();

            // We make sure to not share the same Request instance with several threads
            if (this.initialContext.getRequest() != null) {
                XWikiServletRequestStub stubRequest = new XWikiServletRequestStub();
                stubRequest.setHost(this.initialContext.getRequest().getHeader("x-forwarded-host"));
                stubRequest.setScheme(this.initialContext.getRequest().getScheme());
                XWikiServletRequest request = new XWikiServletRequest(stubRequest);
                stubContext.setRequest(request);
            }

            // We make sure to not share the same Response instance with several threads
            if (this.initialContext.getResponse() != null) {
                XWikiServletResponseStub stubResponse = new XWikiServletResponseStub();
                XWikiServletResponse response = new XWikiServletResponse(stubResponse);
                stubContext.setResponse(response);
            }

            // We make sure to not share the same document instance with several threads
            stubContext.setDoc(new XWikiDocument());
        } else {
            stubContext = null;
        }

        return stubContext;
    }
}
