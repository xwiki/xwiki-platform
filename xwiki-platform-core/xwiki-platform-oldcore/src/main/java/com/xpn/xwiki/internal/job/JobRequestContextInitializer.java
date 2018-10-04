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
package com.xpn.xwiki.internal.job;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.concurrent.ContextStore;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.job.JobRequestContext;
import com.xpn.xwiki.web.XWikiServletRequestStub;

/**
 * Automatically set various context values before the actual job start.
 * 
 * @version $Id$
 * @since 8.4RC1
 * @deprecated since 10.9RC1, use {@link ContextStore} instead
 */
@Component
@Named("com.xpn.xwiki.internal.job.JobRequestContextInitializer")
@Singleton
@Deprecated
public class JobRequestContextInitializer extends AbstractEventListener
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    /**
     * The default constructor.
     */
    public JobRequestContextInitializer()
    {
        super(JobRequestContextInitializer.class.getName(), new JobStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        JobStartedEvent jobStartedEvent = (JobStartedEvent) event;

        JobRequestContext jobRequestContext = jobStartedEvent.getRequest().getProperty(JobRequestContext.KEY);

        if (jobRequestContext != null) {
            XWikiContext xcontext = this.xcontextProvider.get();

            if (xcontext != null) {
                // Wiki id
                if (jobRequestContext.isWikiIdSet()) {
                    xcontext.setWikiId(jobRequestContext.getWikiId());
                }

                // User
                if (jobRequestContext.isUserReferenceSet()) {
                    xcontext.setUserReference(jobRequestContext.getUserReference());
                }

                // Document
                if (jobRequestContext.isDocumentSet()) {
                    XWikiDocument document = getDocument(jobRequestContext, xcontext);
                    xcontext.setDoc(document);
                }

                // Secure document
                if (jobRequestContext.isSDocumentSet()) {
                    XWikiDocument sdocument = getSDocument(jobRequestContext, xcontext);
                    xcontext.put(XWikiDocument.CKEY_SDOC, sdocument);
                }

                // Request
                if (jobRequestContext.isRequestSet()) {
                    xcontext.setRequest(new XWikiServletRequestStub(jobRequestContext.getRequestURL(),
                        jobRequestContext.getRequestParameters()));
                }
            }
        }
    }

    private XWikiDocument getDocument(JobRequestContext jobRequestContext, XWikiContext xcontext)
    {
        if (jobRequestContext.getDocument() != null) {
            return jobRequestContext.getDocument();
        } else if (jobRequestContext.getDocumentReference() != null) {
            try {
                return xcontext.getWiki().getDocument(jobRequestContext.getDocumentReference(), xcontext);
            } catch (XWikiException e) {
                this.logger.error("Failed to get document with reference [{}]",
                    jobRequestContext.getDocumentReference(), e);
            }
        }

        return null;
    }

    private XWikiDocument getSDocument(JobRequestContext jobRequestContext, XWikiContext xcontext)
    {
        if (jobRequestContext.getSDocument() != null) {
            return jobRequestContext.getSDocument();
        } else if (jobRequestContext.getSDocumentReference() != null) {
            try {
                return xcontext.getWiki().getDocument(jobRequestContext.getSDocumentReference(), xcontext);
            } catch (XWikiException e) {
                this.logger.error("Failed to get secure document with reference [{}]",
                    jobRequestContext.getSDocumentReference(), e);
            }
        }

        return null;
    }
}
