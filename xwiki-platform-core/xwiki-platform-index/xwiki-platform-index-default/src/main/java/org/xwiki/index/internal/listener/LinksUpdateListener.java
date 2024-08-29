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
package org.xwiki.index.internal.listener;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.TaskManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.xwiki.index.internal.DefaultLinksTaskConsumer.LINKS_TASK_TYPE;

/**
 * Queue links analysis tasks when a document is created or updated.
 *
 * @version $Id$
 * @since 14.2RC1
 * @deprecated link storage and indexing moved to Solr (implemented in xwiki-platform-search-solr-api)
 */
@Component
@Singleton
@Named("LinksUpdateListener")
@Deprecated(since = "14.8RC1")
public class LinksUpdateListener extends AbstractEventListener
{
    @Inject
    private Logger logger;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Inject
    private TaskManager taskManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Default constructor.
     */
    public LinksUpdateListener()
    {
        super("LinksUpdateListener", new DocumentCreatedEvent(), new DocumentUpdatedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = this.contextProvider.get();
        if (!this.remoteObservationManagerContext.isRemoteState() && context.getWiki().hasBacklinks(context)) {
            XWikiDocument doc = (XWikiDocument) source;
            // Note: we display the docId since the task manager logs only display the docId in logs and we want to
            // make the match visually when there's a problem and we have to analyse the logs.
            this.logger.debug("Link analysis task starting for [{}] (docId = [{}])", doc.getDocumentReference(),
                doc.getId());
            this.taskManager.addTask(doc.getDocumentReference().getWikiReference().getName(), doc.getId(),
                LINKS_TASK_TYPE);
        }
    }
}
