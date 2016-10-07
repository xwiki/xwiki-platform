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

import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.job.JobRequestContext;

/**
 * Automatically set various context values before the actual job start.
 * 
 * @version $Id$
 * @since 8.4RC1
 */
@Component
@Named("com.xpn.xwiki.internal.job.JobRequestContextInitializer")
@Singleton
public class JobRequestContextInitializer extends AbstractEventListener
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

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
                    xcontext.setDoc(jobRequestContext.getDocument());
                }

                // Secure document
                if (jobRequestContext.isSDocumentSet()) {
                    xcontext.put(XWikiDocument.CKEY_SDOC, jobRequestContext.getSDocument());
                }
            }
        }
    }
}
