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
package org.xwiki.notifications.notifiers.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Validate {@link UserEventDispatcher}.
 * 
 * @version $Id$
 */
@ComponentTest
public class UserEventDispatcherTest
{
    @Component(roles = UserEventDispatcher.class)
    @Singleton
    public static class CustomUserEventDispatcher extends UserEventDispatcher
    {
        boolean skipDispatch;

        ReentrantLock lock = new ReentrantLock();

        ReentrantLock nextLock = new ReentrantLock();

        List<Event> dispatched = new ArrayList<>();

        @Override
        public void dispatch(Event event)
        {
            this.dispatched.add(event);

            this.lock.lock();
            if (!this.disposed && !this.skipDispatch) {
                super.dispatch(event);
            }
            this.lock.unlock();
        }

        @Override
        public void dispose() throws ComponentLifecycleException
        {
            super.dispose();

            if (this.lock.isLocked()) {
                this.lock.unlock();
            }
        }
    }

    @MockComponent
    private EventStore store;

    @InjectMockComponents
    private CustomUserEventDispatcher dispatcher;

    private DefaultEvent storeEvent(String id) throws EventStreamException
    {
        return storeEvent(id, new Date());
    }

    private DefaultEvent storeEvent(String id, Date date) throws EventStreamException
    {
        DefaultEvent event = new DefaultEvent();

        event.setId(id);
        event.setDate(date);

        when(this.store.getEvent(id)).thenReturn(Optional.of(event));

        return event;
    }

    private Date nowMinus10Minutes()
    {
        return new Date(System.currentTimeMillis() - (10L * 60 * 1000));
    }

    // Tests

    @Test
    void addEvent() throws InterruptedException, EventStreamException
    {
        this.dispatcher.lock.lock();

        assertEquals(0, this.dispatcher.priorityQueue.size());
        assertEquals(0, this.dispatcher.secondaryQueue.size());

        this.dispatcher.addEvent(storeEvent("event1"));

        // Make sure the thread catched the event added to the queue
        Thread.sleep(100);

        assertEquals(0, this.dispatcher.priorityQueue.size());
        assertEquals(0, this.dispatcher.secondaryQueue.size());
        assertEquals(1, this.dispatcher.dispatched.size());

        this.dispatcher.addEvent(storeEvent("event2"));
        this.dispatcher.addEvent(storeEvent("event3"));

        assertEquals(2, this.dispatcher.priorityQueue.size());
        assertEquals(0, this.dispatcher.secondaryQueue.size());
        assertEquals(1, this.dispatcher.dispatched.size());

        this.dispatcher.addEvent(storeEvent("event4", nowMinus10Minutes()));

        assertEquals(2, this.dispatcher.priorityQueue.size());
        assertEquals(1, this.dispatcher.secondaryQueue.size());
        assertEquals(1, this.dispatcher.dispatched.size());

        this.dispatcher.addEvent(storeEvent("event5").getId(), true);

        assertEquals(2, this.dispatcher.priorityQueue.size());
        assertEquals(2, this.dispatcher.secondaryQueue.size());
        assertEquals(1, this.dispatcher.dispatched.size());

        this.dispatcher.addEvent(storeEvent("event6").getId(), false);

        assertEquals(2, this.dispatcher.priorityQueue.size());
        assertEquals(3, this.dispatcher.secondaryQueue.size());
        assertEquals(1, this.dispatcher.dispatched.size());

        this.dispatcher.skipDispatch = true;
        this.dispatcher.lock.unlock();

        // Make sure the thread have time to dispatch all the events
        Thread.sleep(100);

        assertEquals(0, this.dispatcher.priorityQueue.size());
        assertEquals(0, this.dispatcher.secondaryQueue.size());
        assertEquals(6, this.dispatcher.dispatched.size());
    }
}
