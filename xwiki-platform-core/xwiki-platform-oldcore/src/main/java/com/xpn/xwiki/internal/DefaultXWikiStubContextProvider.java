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

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponse;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiURLFactory;

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

    @Inject
    private Provider<DocumentReference> defaultDocumentReferenceProvider;

    @Inject
    private Execution execution;

    /**
     * The initial stub XWikiContext.
     */
    private XWikiContext initialContext;

    @Override
    public void initialize(XWikiContext context)
    {
        XWikiContext newContext = context.clone();

        newContext.setCacheDuration(0);

        newContext.setUserReference(null);
        newContext.setLocale(null);
        newContext.setWikiId(context.getMainXWiki());
        newContext.setURLFactory(null);

        // Cleanup
        newContext.flushClassCache();

        // We are sure the context request is a real servlet request
        // So we force the dummy request with the current host
        if (newContext.getRequest() != null) {
            XWikiServletRequestStub initialRequest = new XWikiServletRequestStub(context.getRequest());
            XWikiServletRequest request = new XWikiServletRequest(initialRequest);
            newContext.setRequest(request);
        }

        // Get rid of the real response
        if (newContext.getResponse() != null) {
            XWikiServletResponseStub initialResponse = new XWikiServletResponseStub();
            // anything to keep ?
            XWikiServletResponse response = new XWikiServletResponse(initialResponse);
            newContext.setResponse(response);
        }

        this.initialContext = newContext;

        this.logger.debug("Stub context initialized.");
    }

    @Override
    public XWikiContext createStubContext()
    {
        XWikiContext stubContext;

        if (this.initialContext != null) {
            stubContext = this.initialContext.clone();

            ExecutionContext econtext = this.execution.getContext();

            XWikiContext currentXContext;
            if (econtext != null) {
                // Remember current XWikiContext to restore it
                currentXContext = (XWikiContext) econtext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);

                // Set the context in the execution context in case any part of it's initialization requires it
                // (which might create an infinite loop if there was none already in Provider<XWikiContext> use case
                // for example)
                stubContext.declareInExecutionContext(econtext);
            } else {
                currentXContext = null;
            }

            try {
                initiazeXWikiContext(stubContext);
            } finally {
                if (econtext != null) {
                    // Restore previous XWikiContext
                    if (currentXContext != null) {
                        currentXContext.declareInExecutionContext(econtext);
                    } else {
                        econtext.removeProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
                    }
                }
            }
        } else {
            stubContext = null;
        }

        return stubContext;
    }

    private void initiazeXWikiContext(XWikiContext stubContext)
    {
        // We make sure to not share the same Request instance with several threads
        if (this.initialContext.getRequest() != null) {
            XWikiServletRequestStub stubRequest = new XWikiServletRequestStub(this.initialContext.getRequest());
            XWikiServletRequest request = new XWikiServletRequest(stubRequest);
            stubContext.setRequest(request);

            // Each context is supposed to have a dedicated URL factory
            if (stubContext.getWiki() != null) {
                XWikiURLFactory urlf = stubContext.getWiki().getURLFactoryService()
                    .createURLFactory(XWikiContext.MODE_SERVLET, stubContext);
                stubContext.setURLFactory(urlf);
            }
        }

        // We make sure to not share the same Response instance with several threads
        if (this.initialContext.getResponse() != null) {
            XWikiServletResponseStub stubResponse = new XWikiServletResponseStub();
            XWikiServletResponse response = new XWikiServletResponse(stubResponse);
            stubContext.setResponse(response);
        }

        // We make sure to not share the same document instance with several threads
        stubContext.setDoc(new XWikiDocument(this.defaultDocumentReferenceProvider.get()));
    }
}
