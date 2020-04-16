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
import java.util.HashSet;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.eventstream.EqualEventQuery;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.search.solr.test.SolrComponentList;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test the initialization of the Solr instance.
 * 
 * @version $Id$
 */
@SolrComponentList
@ComponentTest
@ComponentList(EventsSolrCoreInitializer.class)
public class EventStoreTest
{
    @XWikiTempDir
    private File permanentDirectory;

    private ConfigurationSource mockXWikiProperties;

    private Environment mockEnvironment;

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
        DefaultEvent event = new DefaultEvent();

        event.setId(id);

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
        assertNull(this.eventStore.getEvent("id"));

        DefaultEvent event = event("id");

        this.eventStore.saveEvent(event);

        Event storedEvent = this.eventStore.getEvent("id");
        assertNotNull(storedEvent);

        this.eventStore.deleteEvent(event);

        assertNull(this.eventStore.getEvent("id"));

        this.eventStore.saveEvent(event);

        assertNotNull(this.eventStore.getEvent("id"));

        this.eventStore.deleteEvent("id");

        assertNull(this.eventStore.getEvent("id"));
    }

    @Test
    public void search() throws EventStreamException
    {
        DefaultEvent event1 = event("id1");
        DefaultEvent event2 = event("id2");
        DefaultEvent event3 = event("id3");
        DefaultEvent event4 = event("id4");

        this.eventStore.saveEvent(event1);
        this.eventStore.saveEvent(event2);
        this.eventStore.saveEvent(event3);
        this.eventStore.saveEvent(event4);

        assertSearch(Arrays.asList(event1, event2, event3, event4), new EqualEventQuery());
        assertSearch(Arrays.asList(event1), new EqualEventQuery("id", "id1"));
    }
}
