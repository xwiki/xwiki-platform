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
package org.xwiki.eventstream.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.script.service.ScriptService;

/**
 * Script services for the Event Stream Module.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Component
@Singleton
@Named("eventstream")
public class EventStreamScriptService implements ScriptService
{
    @Inject
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @Inject
    private EventStore eventStore;

    /**
     * @param allWikis load the descriptors from all the wikis of the farm if true
     * @return the list of the available RecordableEventDescriptors
     * @throws EventStreamException if an error happens
     * @since 9.5.1
     * @since 9.6RC1
     */
    public List<RecordableEventDescriptor> getRecordableEventDescriptors(boolean allWikis) throws EventStreamException
    {
        return recordableEventDescriptorManager.getRecordableEventDescriptors(allWikis);
    }

    /**
     * @param eventType the type of the event
     * @param allWikis load the descriptors from all the wikis of the farm if true
     * @return the corresponding RecordableEventDescriptor or null if no one matches
     * @throws EventStreamException if an error happens
     * @since 9.10RC1
     */
    public RecordableEventDescriptor getDescriptorForEventType(String eventType, boolean allWikis)
        throws EventStreamException
    {
        return recordableEventDescriptorManager.getDescriptorForEventType(eventType, allWikis);
    }

    /**
     * @return the total number of event in the store
     * @throws EventStreamException when failing to query the number of events
     * @since 12.6.1
     * @since 12.7RC1
     */
    public long getEventCount() throws EventStreamException
    {
        return this.eventStore.search(new SimpleEventQuery(0, 0)).getTotalHits();
    }
}
