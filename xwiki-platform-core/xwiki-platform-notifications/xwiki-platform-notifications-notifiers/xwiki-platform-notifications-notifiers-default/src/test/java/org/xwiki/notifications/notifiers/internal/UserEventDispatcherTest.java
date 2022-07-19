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
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Named;
import javax.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserException;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.group.GroupManager;
import org.xwiki.user.internal.document.DocumentUserReference;

import static ch.qos.logback.classic.Level.WARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link UserEventDispatcher}.
 * 
 * @version $Id$
 */
@ComponentTest
class UserEventDispatcherTest
{
    @Component(roles = UserEventDispatcher.class)
    @Singleton
    public static class CustomUserEventDispatcher extends UserEventDispatcher
    {
        boolean skipDispatch;

        ReentrantLock lock = new ReentrantLock();

        CountDownLatch count = new CountDownLatch(0);

        List<Event> dispatched = new ArrayList<>();

        @Override
        public void dispatch(Event event)
        {
            this.dispatched.add(event);
            this.count.countDown();

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

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @MockComponent
    private GroupManager groupManager;

    @MockComponent
    private UserManager userManager;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @InjectMockComponents
    private CustomUserEventDispatcher dispatcher;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private DefaultEvent storeEvent(String id) throws EventStreamException
    {
        return storeEvent(id, new Date());
    }

    private DefaultEvent storeEvent(String id, Date date) throws EventStreamException
    {
        DefaultEvent event = initEvent(id, date);

        when(this.store.getEvent(id)).thenReturn(Optional.of(event));

        return event;
    }

    private DefaultEvent initEvent(String id, Date date)
    {
        DefaultEvent event = new DefaultEvent();

        event.setId(id);
        event.setDate(date);
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

        this.dispatcher.count = new CountDownLatch(1);

        this.dispatcher.addEvent(storeEvent("event1"));

        // Make sure the thread catched the event added to the queue
        this.dispatcher.count.await(1, TimeUnit.SECONDS);

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
        this.dispatcher.count = new CountDownLatch(5);
        this.dispatcher.lock.unlock();

        // Make sure the thread have time to dispatch all the events
        this.dispatcher.count.await(1, TimeUnit.SECONDS);

        assertEquals(0, this.dispatcher.priorityQueue.size());
        assertEquals(0, this.dispatcher.secondaryQueue.size());
        assertEquals(6, this.dispatcher.dispatched.size());
    }

    @Test
    void dispatchTargetExistingUser() throws Exception
    {
        DefaultEvent event = initEvent("event0", new Date());
        WikiReference wikiReference = new WikiReference("xwiki");
        event.setWiki(wikiReference);
        event.setTarget(Set.of("xwiki:XWiki.U1"));
        DocumentReference userDocumentReference = new DocumentReference("xwiki", "XWiki", "U1");
        DocumentUserReference documentUserReference = new DocumentUserReference(userDocumentReference, true);

        when(this.resolver.resolve("xwiki:XWiki.U1", wikiReference)).thenReturn(userDocumentReference);
        when(this.documentReferenceUserReferenceResolver.resolve(userDocumentReference))
            .thenReturn(documentUserReference);
        when(this.userManager.exists(documentUserReference)).thenReturn(true);
        when(this.entityReferenceSerializer.serialize(userDocumentReference)).thenReturn("xwiki:XWiki.U1");
        when(this.store.search(any())).thenReturn(mock(EventSearchResult.class));

        this.dispatcher.dispatch(event);

        verify(this.store).prefilterEvent(event);
    }

    @Test
    void dispatchTargetUserExistsError() throws Exception
    {
        DefaultEvent event = initEvent("event0", new Date());
        WikiReference wikiReference = new WikiReference("xwiki");
        event.setWiki(wikiReference);
        event.setTarget(Set.of("xwiki:XWiki.U1"));
        DocumentReference userDocumentReference = new DocumentReference("xwiki", "XWiki", "U1");
        DocumentUserReference documentUserReference = new DocumentUserReference(userDocumentReference, true);

        when(this.resolver.resolve("xwiki:XWiki.U1", wikiReference)).thenReturn(userDocumentReference);
        when(this.documentReferenceUserReferenceResolver.resolve(userDocumentReference))
            .thenReturn(documentUserReference);
        when(this.userManager.exists(documentUserReference)).thenThrow(UserException.class);

        this.dispatcher.dispatch(event);

        verify(this.store).prefilterEvent(event);
        assertEquals("Failed to verify if user [reference = [xwiki:XWiki.U1]] exists. Cause: [UserException: ]",
            this.logCapture.getMessage(0));
        assertEquals(WARN, this.logCapture.getLogEvent(0).getLevel());
    }
}
