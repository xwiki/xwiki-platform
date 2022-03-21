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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.TaskManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.index.internal.DefaultLinksTaskConsumer.LINKS_TASK_TYPE;

/**
 * Automatically index the links (page references, and attachments use) of the wiki at startup.
 *
 * @version $Id$
 * @since 14.2RC1
 */
@Component
@Named(LinksIndexationEventListener.HINT)
@Singleton
public class LinksIndexationEventListener implements EventListener
{
    /**
     * The hint of this listener.
     */
    public static final String HINT = "org.xwiki.index.internal.listener.LinksIndexationEventListener";

    /**
     * The events to listen to that trigger the links indexation.
     */
    private static final List<Event> EVENTS = List.of(new ApplicationReadyEvent(), new WikiReadyEvent());

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Inject
    private TaskManager taskManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return HINT;
    }

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = (XWikiContext) data;

        try {
            if (context.isMainWiki() && context.getWiki().getStore().isXWikiLinkEmpty(context)) {
                innerOnEvent(context);
            }
        } catch (XWikiException e) {
            this.logger.warn(
                "Failed to access the database to count the number of entries in the XWikiLink table. Cause: [{}].",
                getRootCauseMessage(e));
        }
    }

    private void innerOnEvent(XWikiContext context)
    {
        if (!this.remoteObservationManagerContext.isRemoteState() && context.getWiki().hasBacklinks(context)) {
            // We only start the indexation if the wiki has no backlinks. This should only to happen after migration
            // R140200000XWIKI19352, otherwise this would mean a wiki with no links or attachments, which is very
            // unlikely.
            try {
                for (String allId : this.wikiDescriptorManager.getAllIds()) {
                    handleWiki(allId);
                }
            } catch (WikiManagerException e) {
                this.logger.warn("Failed list the wiki ids of the farm. Cause: [{}].", getRootCauseMessage(e));
            }
        }
    }

    private void handleWiki(String wikiId)
    {
        try {
            List<Long> ids =
                this.queryManager.createQuery("SELECT doc.id FROM XWikiDocument doc", Query.HQL)
                    .setWiki(wikiId)
                    .execute();
            for (Long id : ids) {
                this.taskManager.addTask(wikiId, id, LINKS_TASK_TYPE);
            }
        } catch (QueryException e) {
            this.logger.warn("Failed retrieve the list of all the documents for wiki [{}]. Cause: [{}].",
                wikiId, getRootCauseMessage(e));
        }
    }
}
