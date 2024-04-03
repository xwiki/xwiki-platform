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
package org.xwiki.livedata.internal.rest;

import java.net.URI;
import java.util.HashMap;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataEntryDescriptor;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.livedata.rest.model.jaxb.Entry;
import org.xwiki.livedata.rest.model.jaxb.StringMap;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWikiContext;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultLiveDataEntryResource}.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@ComponentTest
class DefaultLiveDataEntryResourceTest
{
    @InjectMockComponents
    private DefaultLiveDataEntryResource defaultLiveDataEntryPropertyResource;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    protected LiveDataSourceManager liveDataSourceManager;

    @MockComponent
    protected LiveDataConfigurationResolver<LiveDataConfiguration> defaultLiveDataConfigResolver;

    @Mock
    private LiveDataSource liveDataSource;

    @Mock
    private LiveDataEntryStore liveDataEntryStore;

    @Mock
    private LiveDataPropertyDescriptorStore liveDataPropertyDescriptorStore;

    @Mock
    private LiveDataPropertyDescriptor liveDataPropertyDescriptor;

    @Mock
    private UriInfo uriInfo;

    private final LiveDataQuery.Source source = new LiveDataQuery.Source("sourceIdTest");

    @BeforeComponent
    void configure() throws Exception
    {
        ComponentManager contextComponentManager =
            this.componentManager.registerMockComponent(ComponentManager.class, "context");
        Execution execution = mock(Execution.class);
        when(contextComponentManager.getInstance(Execution.class)).thenReturn(execution);
        ExecutionContext executionContext = new ExecutionContext();
        XWikiContext xcontext = mock(XWikiContext.class);
        executionContext.setProperty("xwikicontext", xcontext);
        when(execution.getContext()).thenReturn(executionContext);
    }

    @BeforeEach
    void setUp() throws Exception
    {

        FieldUtils.writeField(this.defaultLiveDataEntryPropertyResource, "uriInfo", uriInfo, true);
        when(uriInfo.getQueryParameters()).thenReturn(mock(MultivaluedMap.class));

        LiveDataQuery query = new LiveDataQuery();
        query.setSource(this.source);
        LiveDataConfiguration liveDataConfiguration = new LiveDataConfiguration();
        liveDataConfiguration.setQuery(query);
        LiveDataMeta meta = new LiveDataMeta();
        LiveDataEntryDescriptor entryDescriptor = new LiveDataEntryDescriptor();
        entryDescriptor.setIdProperty("idPropertyTest");
        meta.setEntryDescriptor(entryDescriptor);
        liveDataConfiguration.setMeta(meta);

        when(this.defaultLiveDataConfigResolver.resolve(any())).thenReturn(liveDataConfiguration);
    }

    @Test
    void updateEntrySourceMissing()
    {
        Entry entry = new Entry();
        when(this.liveDataSourceManager.get(this.source, null)).thenReturn(Optional.empty());

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
            () -> this.defaultLiveDataEntryPropertyResource.updateEntry("sourceIdTest", null, "entryIdTest", entry));
        assertEquals(NOT_FOUND.getStatusCode(), webApplicationException.getResponse().getStatus());
    }

    @Test
    void updateEntrySaveFail() throws Exception
    {
        Entry entry = new Entry();
        entry.setValues(new StringMap());
        when(this.liveDataSourceManager.get(this.source, null)).thenReturn(Optional.of(this.liveDataSource));
        when(this.liveDataSource.getEntries()).thenReturn(this.liveDataEntryStore);
        when(this.liveDataEntryStore.save(entry.getValues())).thenReturn(Optional.empty());

        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
            () -> this.defaultLiveDataEntryPropertyResource.updateEntry("sourceIdTest", null, "entryIdTest", entry));
        assertEquals(NOT_FOUND.getStatusCode(), webApplicationException.getResponse().getStatus());
        verify(this.liveDataEntryStore).save(entry.getValues());
        assertEquals("entryIdTest", entry.getValues().get("idPropertyTest"));
    }

    @Test
    void updateEntry() throws Exception
    {
        Entry entry = new Entry();
        entry.setValues(new StringMap());
        when(this.liveDataSourceManager.get(this.source, null)).thenReturn(Optional.of(this.liveDataSource));
        when(this.liveDataSource.getEntries()).thenReturn(this.liveDataEntryStore);
        when(this.liveDataEntryStore.save(entry.getValues())).thenReturn(Optional.of("sourceIdTest"));
        when(this.liveDataEntryStore.get("sourceIdTest")).thenReturn(Optional.of(new HashMap<>()));
        when(this.uriInfo.getBaseUri()).thenReturn(URI.create("http://test.org/"));

        Response response =
            this.defaultLiveDataEntryPropertyResource.updateEntry("sourceIdTest", null, "entryIdTest", entry);
        assertEquals(ACCEPTED.getStatusCode(), response.getStatus());
        assertEquals(
            "{\"links\":[{\"href\":\"http://test.org/liveData/sources/sourceIdTest/entries/sourceIdTest\","
                + "\"rel\":\"self\",\"type\":null,\"hrefLang\":null},"
                + "{\"href\":\"http://test.org/liveData/sources/sourceIdTest/entries\","
                + "\"rel\":\"http://www.xwiki.org/rel/parent\",\"type\":null,\"hrefLang\":null}],\"values\":{}}",
            new ObjectMapper().writeValueAsString(response.getEntity()));
        verify(this.liveDataEntryStore).save(entry.getValues());
        assertEquals("entryIdTest", entry.getValues().get("idPropertyTest"));
    }
}