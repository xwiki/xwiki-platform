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
package org.xwiki.eventstream.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.eventstream.EventStore;

/**
 * The configuration of the event stream module.
 * 
 * @version $Id$
 * @since 12.4RC1
 */
@Component(roles = EventStreamConfiguration.class)
@Singleton
public class EventStreamConfiguration
{
    private static final String KEY_STORE = "eventstream.store";

    @Inject
    private ConfigurationSource configuration;

    /**
     * @return the hint of the {@link EventStore} implementation to use
     */
    public String getEventStore()
    {
        return this.configuration.getProperty(KEY_STORE, "solr");
    }

    /**
     * @return true if the event store is explicitly set
     */
    public boolean isEventStoreSet()
    {
        return this.configuration.containsKey(KEY_STORE);
    }

    /**
     * @return true if the {@link EventStore} system is enabled
     */
    public boolean isEventStoreEnabled()
    {
        return this.configuration.getProperty("eventstream.store.enabled", true);
    }
}
