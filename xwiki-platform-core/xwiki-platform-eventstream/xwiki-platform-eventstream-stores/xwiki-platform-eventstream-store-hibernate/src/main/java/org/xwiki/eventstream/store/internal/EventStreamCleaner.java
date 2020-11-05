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
package org.xwiki.eventstream.store.internal;

import java.util.Date;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStream;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

/**
 * Remove old events (according to the configuration) from the event stream.
 *
 * @since 11.1RC1
 * @version $Id$
 */
@Component(roles = EventStreamCleaner.class)
@Singleton
public class EventStreamCleaner
{
    @Inject
    private LegacyEventStreamStoreConfiguration configuration;

    @Inject
    private EventStream eventStream;

    @Inject
    private EventStore eventStore;

    @Inject
    private QueryManager queryManager;

    @Inject
    private Logger logger;

    /**
     * Remove old events (according to the configuration) from the event stream.
     */
    public void clean()
    {
        int days = configuration.getNumberOfDaysToKeep();
        if (days > 0) {
            try {
                Query query = queryManager.createQuery("where event.date < :date", Query.HQL);
                query.bindValue("date", DateUtils.addDays(new Date(), -days));
                for (Event event : eventStream.searchEvents(query)) {
                    this.eventStore.deleteEvent(event);
                }
            } catch (QueryException e) {
                logger.error("Impossible to clean the old events of the event stream.", e);
            }
        }
    }
}
