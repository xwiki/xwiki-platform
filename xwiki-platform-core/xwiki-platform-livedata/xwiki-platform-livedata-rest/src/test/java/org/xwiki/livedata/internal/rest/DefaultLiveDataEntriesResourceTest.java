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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataEntryDescriptor;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.livedata.rest.model.jaxb.Entries;
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultLiveDataEntriesResource}.
 *
 * @version $Id$
 * @since 12.10.9
 * @since 13.4.2
 * @since 13.5.1
 * @since 13.6RC1
 */
@ComponentTest
class DefaultLiveDataEntriesResourceTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMockComponents
    private DefaultLiveDataEntriesResource resource;

    @MockComponent
    private LiveDataResourceContextInitializer contextInitializer;
    
    @MockComponent
    private LiveDataSourceManager liveDataSourceManager;

    @MockComponent
    private LiveDataConfigurationResolver<LiveDataConfiguration> defaultLiveDataConfigResolver;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;
    
    /*
     * Cannot be mocked by annotation because it is needed in the @BeforeComponent phase.
     */
    private XWikiContext xcontext;

    @Mock
    private LiveDataSource liveDataSource;

    @Mock
    private LiveDataEntryStore store;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private LiveDataEntryStore liveDataEntryStore;

    @BeforeComponent
    void configure() throws Exception
    {
        ComponentManager contextComponentManager =
            this.componentManager.registerMockComponent(ComponentManager.class, "context");
        Execution execution = mock(Execution.class);
        when(contextComponentManager.getInstance(Execution.class)).thenReturn(execution);
        ExecutionContext executionContext = new ExecutionContext();
        this.xcontext = mock(XWikiContext.class);
        executionContext.setProperty("xwikicontext", this.xcontext);
        when(execution.getContext()).thenReturn(executionContext);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
    }

    @BeforeEach
    void setUp() throws Exception
    {
        FieldUtils.writeField(this.resource, "uriInfo", this.uriInfo, true);
        when(this.uriInfo.getQueryParameters()).thenReturn(mock(MultivaluedMap.class));
        when(this.uriInfo.getAbsolutePath()).thenReturn(URI.create("https://mywiki"));
        when(this.uriInfo.getBaseUri()).thenReturn(URI.create("https://mywiki"));
        when(this.xcontext.getWikiId()).thenReturn("s1");
    }

    @Test
    void getEntriesWithNamespaceNull() throws Exception
    {
        List<String> properties = Arrays.asList("prop1", null, "prop2");
        List<String> matchAll = Arrays.asList(null, "prop", "other");
        List<String> sort = Arrays.asList("pro2", null);
        List<Boolean> descending = Arrays.asList(true, false, null);

        LiveDataQuery.Source source = new LiveDataQuery.Source("liveTable");
        LiveDataConfiguration config = defaultConfig(source);

        MultivaluedMap<String, String> multivaluedMap = new MultivaluedHashMap<>();
        multivaluedMap.putSingle("filters.age", "18");
        multivaluedMap.putSingle("filters.other", "contains:xwiki:XWiki.Admin");
        multivaluedMap.putSingle("filters.author", ":xwiki:XWiki.Author");
        multivaluedMap.putSingle("notfilter.unused", "abcd");
        when(this.uriInfo.getQueryParameters()).thenReturn(multivaluedMap);
        when(this.defaultLiveDataConfigResolver.resolve(any())).thenReturn(config);
        when(this.liveDataSourceManager.get(source, null)).thenReturn(Optional.of(this.liveDataSource));
        when(this.liveDataSource.getEntries()).thenReturn(this.store);
        when(this.store.get(config.getQuery())).thenReturn(new LiveData());

        Entries entries = this.resource.getEntries("sourceId", null, properties, matchAll, sort, descending, 0, 10);

        assertEquals("{\"links\":["
                + "{\"href\":\"https://mywiki\",\"rel\":\"self\",\"type\":null,\"hrefLang\":null},"
                + "{\"href\":\"https://mywiki/liveData/sources/liveTable\","
                + "\"rel\":\"http://www.xwiki.org/rel/parent\","
                + "\"type\":null,\"hrefLang\":null}],\"entries\":[],\"count\":0,\"offset\":0,\"limit\":10}",
            this.objectMapper.writeValueAsString(entries));

        ArgumentCaptor<LiveDataConfiguration> configCaptor = ArgumentCaptor.forClass(LiveDataConfiguration.class);
        verify(this.defaultLiveDataConfigResolver).resolve(configCaptor.capture());

        assertEquals("{\"query\":{"
            + "\"properties\":[\"prop1\",\"prop2\"],"
            + "\"source\":{\"id\":\"sourceId\"},"
            + "\"filters\":["
            + "{\"property\":\"other\",\"matchAll\":true,\"constraints\":"
            + "[{\"operator\":\"contains\",\"value\":\"xwiki:XWiki.Admin\"}]"
            + "},"
            + "{\"property\":\"author\",\"constraints\":[{\"value\":\"xwiki:XWiki.Author\"}]}],"
            + "\"sort\":[{\"property\":\"pro2\",\"descending\":true}],"
            + "\"offset\":0,"
            + "\"limit\":10}}", this.objectMapper.writeValueAsString(configCaptor.getValue()));

        verify(this.contextInitializer).initialize(null);
    }

    @Test
    void getEntriesWithNamespaceS1() throws Exception
    {
        LiveDataQuery.Source source = new LiveDataQuery.Source("liveTable");
        LiveDataConfiguration config = defaultConfig(source);

        when(this.defaultLiveDataConfigResolver.resolve(any())).thenReturn(config);
        when(this.liveDataSourceManager.get(source, "wiki:s2")).thenReturn(Optional.of(this.liveDataSource));
        when(this.liveDataSource.getEntries()).thenReturn(this.store);
        when(this.store.get(config.getQuery())).thenReturn(new LiveData());

        Entries entries =
            this.resource.getEntries("sourceId", "wiki:s2", emptyList(), emptyList(), emptyList(), emptyList(), 1, 20);

        assertEquals("{\"links\":["
                + "{\"href\":\"https://mywiki\",\"rel\":\"self\",\"type\":null,\"hrefLang\":null},"
                + "{\"href\":\"https://mywiki/liveData/sources/liveTable?namespace=wiki%3As2\","
                + "\"rel\":\"http://www.xwiki.org/rel/parent\","
                + "\"type\":null,\"hrefLang\":null}],\"entries\":[],\"count\":0,\"offset\":1,\"limit\":20}",
            this.objectMapper.writeValueAsString(entries));

        verify(this.contextInitializer).initialize("wiki:s2");
        
    }

    @Test
    void getEntriesDataSourceNotFound() throws Exception
    {
        List<String> properties = emptyList();
        List<String> matchAll = emptyList();
        List<String> sort = emptyList();
        List<Boolean> descending = emptyList();

        LiveDataQuery.Source source = new LiveDataQuery.Source("liveTable");
        LiveDataConfiguration config = defaultConfig(source);

        when(this.defaultLiveDataConfigResolver.resolve(any())).thenReturn(config);
        when(this.liveDataSourceManager.get(source, "wiki:s2")).thenReturn(Optional.empty());

        WebApplicationException exception = assertThrows(WebApplicationException.class,
            () -> this.resource.getEntries("sourceId", "wiki:s2", properties, matchAll, sort, descending, 1, 20));

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), exception.getResponse().getStatus());

        verify(this.contextInitializer).initialize("wiki:s2");
        
    }

    @Test
    void addEntryMissingSource()
    {
        when(this.liveDataSourceManager.get(any(LiveDataQuery.Source.class), isNull())).thenReturn(Optional.empty());
        Entry entry = new Entry();
        WebApplicationException exception =
            assertThrows(WebApplicationException.class, () -> this.resource.addEntry("sourceId", null, entry));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), exception.getResponse().getStatus());
    }
    

    @Test
    void addEntry() throws Exception
    {
        Entry entry = new Entry();
        StringMap value = new StringMap();
        value.put("age", "42");
        entry.setValues(value);

        when(this.liveDataSourceManager.get(any(LiveDataQuery.Source.class), isNull()))
            .thenReturn(Optional.of(this.liveDataSource));
        when(this.liveDataSource.getEntries()).thenReturn(this.liveDataEntryStore);
        when(this.liveDataEntryStore.save(entry.getValues())).thenReturn(Optional.of("entryId"));
        when(this.liveDataEntryStore.get("entryId")).thenReturn(Optional.of(singletonMap("age", "42")));

        Response response = this.resource.addEntry("sourceId", null, entry);
        assertEquals("{\"links\":[{"
                + "\"href\":\"https://mywiki/liveData/sources/sourceId/entries/entryId\","
                + "\"rel\":\"self\",\"type\":null,\"hrefLang\":null},{"
                + "\"href\":\"https://mywiki/liveData/sources/sourceId/entries\","
                + "\"rel\":\"http://www.xwiki.org/rel/parent\","
                + "\"type\":null,\"hrefLang\":null}],\"values\":{\"age\":\"42\"}}",
            this.objectMapper.writeValueAsString(response.getEntity()));
        verify(this.liveDataEntryStore).save(entry.getValues());
    }

    private LiveDataConfiguration defaultConfig(LiveDataQuery.Source source)
    {
        LiveDataConfiguration config = new LiveDataConfiguration();
        LiveDataQuery query = new LiveDataQuery();
        query.setSource(source);
        config.setQuery(query);
        LiveDataMeta meta = new LiveDataMeta();
        LiveDataEntryDescriptor entryDescriptor = new LiveDataEntryDescriptor();
        entryDescriptor.setIdProperty("name");
        meta.setEntryDescriptor(entryDescriptor);
        config.setMeta(meta);
        return config;
    }
}
