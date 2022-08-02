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
package org.xwiki.eventstream.store.solr.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrUtils;

/**
 * Delete all events associated with a deleted wiki.
 *
 * @since 12.6
 * @version $Id$
 */
@Component
@Named(WikiDeletedListener.NAME)
@Singleton
public class WikiDeletedListener extends AbstractEventListener
{
    /**
     * Name of the listener.
     */
    public static final String NAME = "org.xwiki.eventstream.store.solr.internal.WikiDeletedListener";

    @Inject
    private Solr solr;

    @Inject
    private SolrUtils utils;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public WikiDeletedListener()
    {
        super(NAME, new WikiDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        WikiDeletedEvent wikiDeletedEvent = (WikiDeletedEvent) event;

        try {
            SolrClient client = this.solr.getClient(EventsSolrCoreInitializer.NAME);

            client.deleteByQuery(org.xwiki.eventstream.Event.FIELD_WIKI + ':'
                + this.utils.toCompleteFilterQueryString(wikiDeletedEvent.getWikiId()));
            client.commit();
        } catch (Exception e) {
            this.logger.error("Failed to delete events associated with wiki [{}]", wikiDeletedEvent.getWikiId(), e);
        }
    }
}
