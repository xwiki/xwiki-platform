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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.SimpleEventQuery;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.converter.Converter;
import org.xwiki.search.solr.test.SolrComponentList;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test the initialization of the Solr instance.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList(EventsSolrCoreInitializer.class)
@SolrComponentList
public class EventStoreTest
{
    @XWikiTempDir
    private File permanentDirectory;

    private ConfigurationSource mockXWikiProperties;

    private Environment mockEnvironment;

    @MockComponent
    private Converter<DocumentReference> converter;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockComponents
    private SolrEventStore eventStore;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.mockXWikiProperties =
            this.componentManager.registerMockComponent(ConfigurationSource.class, "xwikiproperties");
        this.mockEnvironment = this.componentManager.registerMockComponent(Environment.class);
        when(this.mockXWikiProperties.getProperty(anyString(), anyString())).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArgument(1);
            }
        });

        when(this.mockEnvironment.getPermanentDirectory()).thenReturn(this.permanentDirectory);
        FileUtils.deleteDirectory(this.permanentDirectory);
        this.permanentDirectory.mkdirs();
    }

    private DefaultEvent event(String id)
    {
        return event(id, null);
    }

    private DefaultEvent event(String id, Date date)
    {
        DefaultEvent event = new DefaultEvent();

        event.setId(id);
        event.setDate(date);

        return event;
    }

    private void assertSearch(Collection<Event> expected, EventQuery query) throws EventStreamException
    {
        EventSearchResult result = this.eventStore.search(query);

        assertEquals(new HashSet<>(expected), result.stream().collect(Collectors.toSet()));
    }

    // Tests

    @Test
    public void saveGetDeleteEvent() throws Exception
    {
        assertFalse(this.eventStore.getEvent("id").isPresent());

        DefaultEvent event = event("id");

        this.eventStore.saveEvent(event).get();

        Optional<Event> storedEvent = this.eventStore.getEvent("id");
        assertTrue(storedEvent.isPresent());

        this.eventStore.deleteEvent(event).get();

        assertFalse(this.eventStore.getEvent("id").isPresent());

        this.eventStore.saveEvent(event).get();

        assertEquals(event, this.eventStore.getEvent("id").get());

        Optional<Event> deleted = this.eventStore.deleteEvent("id").get();

        assertEquals(event, deleted.get());

        assertFalse(this.eventStore.getEvent("id").isPresent());
    }

    @Test
    public void search() throws EventStreamException, InterruptedException, ExecutionException
    {
        DefaultEvent event1 = event("id1");
        DefaultEvent event2 = event("id2");
        DefaultEvent event3 = event("id3");
        DefaultEvent event4 = event("id4");

        event1.setHidden(true);
        event2.setHidden(true);
        event3.setHidden(false);
        event4.setHidden(false);

        event1.setUser(new DocumentReference("wiki", "space", "user1"));
        when(this.converter.convert(DocumentReference.class, "wiki:space.user1")).thenReturn(event1.getUser());
        when(this.converter.convert(String.class, event1.getUser())).thenReturn("wiki:space.user1");
        event2.setUser(new DocumentReference("wiki", "space", "user2"));
        when(this.converter.convert(DocumentReference.class, "wiki:space.user2")).thenReturn(event2.getUser());
        when(this.converter.convert(String.class, event2.getUser())).thenReturn("wiki:space.user2");
        event3.setUser(new DocumentReference("wiki", "space", "user3"));
        when(this.converter.convert(DocumentReference.class, "wiki:space.user3")).thenReturn(event3.getUser());
        when(this.converter.convert(String.class, event3.getUser())).thenReturn("wiki:space.user3");
        event4.setUser(new DocumentReference("wiki", "space", "user4"));
        when(this.converter.convert(DocumentReference.class, "wiki:space.user4")).thenReturn(event4.getUser());
        when(this.converter.convert(String.class, event4.getUser())).thenReturn("wiki:space.user4");

        this.eventStore.saveEvent(event1).get();
        this.eventStore.saveEvent(event2).get();
        this.eventStore.saveEvent(event3).get();
        this.eventStore.saveEvent(event4).get();

        assertSearch(Arrays.asList(event1, event2, event3, event4), new SimpleEventQuery());

        SimpleEventQuery query = new SimpleEventQuery();
        query.eq(Event.FIELD_ID, "id1");
        assertSearch(Arrays.asList(event1), query);

        query.eq(Event.FIELD_ID, "id2");
        assertSearch(Arrays.asList(), query);

        assertSearch(Arrays.asList(event1, event2), new SimpleEventQuery(Event.FIELD_HIDDEN, true));

        assertSearch(Arrays.asList(event4), new SimpleEventQuery(Event.FIELD_USER, event4.getUser()));
    }

    @Test
    public void searchDate() throws EventStreamException, InterruptedException, ExecutionException
    {
        Date date0 = new Date(0);
        Date date10 = new Date(10);
        Date date20 = new Date(20);
        Date date30 = new Date(30);
        Date date40 = new Date(40);

        DefaultEvent event10 = event("id10", date10);
        DefaultEvent event30 = event("id30", date30);

        this.eventStore.saveEvent(event10).get();
        this.eventStore.saveEvent(event30).get();

        SimpleEventQuery query = new SimpleEventQuery();
        query.after(date0);

        assertSearch(Arrays.asList(event10, event30), query);

        query = new SimpleEventQuery();
        query.after(date10);

        assertSearch(Arrays.asList(event30), query);

        query = new SimpleEventQuery();
        query.after(date20);

        assertSearch(Arrays.asList(event30), query);

        query = new SimpleEventQuery();
        query.after(date30);

        assertSearch(Arrays.asList(), query);

        query = new SimpleEventQuery();
        query.after(date40);

        assertSearch(Arrays.asList(), query);

        query = new SimpleEventQuery();
        query.before(date0);

        assertSearch(Arrays.asList(), query);

        query = new SimpleEventQuery();
        query.before(date10);

        assertSearch(Arrays.asList(), query);

        query = new SimpleEventQuery();
        query.before(date20);

        assertSearch(Arrays.asList(event10), query);

        query = new SimpleEventQuery();
        query.before(date30);

        assertSearch(Arrays.asList(event10), query);

        query = new SimpleEventQuery();
        query.before(date40);

        assertSearch(Arrays.asList(event10, event30), query);
    }
}
