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
package org.xwiki.mentions.internal.listeners;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.internal.async.MentionsUpdatedRequest;
import org.xwiki.mentions.internal.async.jobs.MentionsUpdateJob;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Listen to entities update. 
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
@Named("MentionsUpdatedEventListener")
public class MentionsUpdatedEventListener extends AbstractEventListener
{
    private static final List<DocumentUpdatedEvent> EVENTS = singletonList(new DocumentUpdatedEvent());

    @Inject
    private Logger logger;

    @Inject
    private JobExecutor jobExecutor;

    /**
     * Default constructor.
     */
    public MentionsUpdatedEventListener()
    {
        super("MentionsUpdatedEventListener", EVENTS);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.logger.debug("Event [{}] received from [{}] with data [{}].",
            DocumentUpdatedEvent.class.getName(), source, data);

        XWikiDocument doc = (XWikiDocument) source;
        XWikiContext ctx = (XWikiContext) data;
        MentionsUpdatedRequest mentionsUpdatedRequest =
            new MentionsUpdatedRequest(doc, doc.getOriginalDocument(), ctx.getUserReference());
        mentionsUpdatedRequest.setVerbose(false);

        try {
            this.jobExecutor.execute(MentionsUpdateJob.ASYNC_REQUEST_TYPE, mentionsUpdatedRequest);
        } catch (JobException e) {
            this.logger.warn(
                "Failed to create a Job for the Event [{}] received from [{}] with data [{}]. Cause: [{}]",
                DocumentUpdatedEvent.class.getName(), source, data, getRootCauseMessage(e));
        }
    }
}
