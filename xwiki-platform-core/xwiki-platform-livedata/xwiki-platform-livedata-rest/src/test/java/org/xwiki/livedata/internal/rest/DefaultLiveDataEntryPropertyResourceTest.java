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
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultLiveDataEntryPropertyResource}.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@ComponentTest
class DefaultLiveDataEntryPropertyResourceTest
{
    @InjectMockComponents
    private DefaultLiveDataEntryPropertyResource defaultLiveDataEntryPropertyResource;

    @MockComponent
    private LiveDataSourceManager liveDataSourceManager;

    @MockComponent
    private LiveDataConfigurationResolver<LiveDataConfiguration> defaultLiveDataConfigResolver;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Mock
    private LiveDataSource liveDataSource;

    @Mock
    private LiveDataEntryStore liveDataEntryStore;

    @Mock
    private LiveDataPropertyDescriptorStore liveDataPropertyDescriptorStore;

    @Mock
    private LiveDataPropertyDescriptor liveDataPropertyDescriptor;

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
        UriInfo uriInfo = mock(UriInfo.class);
        FieldUtils.writeField(this.defaultLiveDataEntryPropertyResource, "uriInfo", uriInfo, true);
        when(uriInfo.getQueryParameters()).thenReturn(mock(MultivaluedMap.class));
    }

    @Test
    void getPropertySourceNotFound()
    {
        when(this.liveDataSourceManager.get(new LiveDataQuery.Source("sourceIdTest"), null))
            .thenReturn(Optional.empty());
        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
            () -> this.defaultLiveDataEntryPropertyResource
                .getProperty("sourceIdTest", null, "entryIdTest", "propertyIdTest"));
        assertEquals(NOT_FOUND.getStatusCode(), webApplicationException.getResponse().getStatus());
    }

    @Test
    void getPropertyEntryStoreGetIsEmpty() throws Exception
    {
        when(this.liveDataSourceManager.get(new LiveDataQuery.Source("sourceIdTest"), null))
            .thenReturn(Optional.of(this.liveDataSource));
        when(this.liveDataSource.getEntries()).thenReturn(this.liveDataEntryStore);
        when(this.liveDataEntryStore.get("entryIdTest", "propertyIdTest")).thenReturn(Optional.empty());
        WebApplicationException webApplicationException = assertThrows(WebApplicationException.class,
            () -> this.defaultLiveDataEntryPropertyResource
                .getProperty("sourceIdTest", null, "entryIdTest", "propertyIdTest"));
        assertEquals(NOT_FOUND.getStatusCode(), webApplicationException.getResponse().getStatus());
    }

    @Test
    void getProperty() throws Exception
    {
        when(this.liveDataSourceManager.get(new LiveDataQuery.Source("sourceIdTest"), null))
            .thenReturn(Optional.of(this.liveDataSource));
        when(this.liveDataSource.getEntries()).thenReturn(this.liveDataEntryStore);
        when(this.liveDataEntryStore.get("entryIdTest", "propertyIdTest")).thenReturn(Optional.of("result"));
        Object property = this.defaultLiveDataEntryPropertyResource
            .getProperty("sourceIdTest", null, "entryIdTest", "propertyIdTest");
        assertEquals("result", property);
    }

    @Test
    void setPropertySourceNotFound()
    {
        when(this.liveDataSourceManager.get(new LiveDataQuery.Source("sourceIdTest"), null))
            .thenReturn(Optional.empty());

        WebApplicationException webApplicationException =
            assertThrows(WebApplicationException.class, () -> this.defaultLiveDataEntryPropertyResource
                .setProperty("sourceIdTest", null, "entryIdTest", "propertyIdTest", "valueTest"));
        assertEquals(NOT_FOUND.getStatusCode(), webApplicationException.getResponse().getStatus());
    }

    @Test
    void setPropertyEntryNotFound() throws LiveDataException
    {
        when(this.liveDataSourceManager.get(new LiveDataQuery.Source("sourceIdTest"), null))
            .thenReturn(Optional.of(this.liveDataSource));

        when(this.liveDataSource.getProperties()).thenReturn(this.liveDataPropertyDescriptorStore);
        when(this.liveDataPropertyDescriptorStore.get("propertyIdTest")).thenReturn(Optional.empty());

        Response response = this.defaultLiveDataEntryPropertyResource
            .setProperty("sourceIdTest", null, "entryIdTest", "propertyIdTest", "valueTest");
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void setPropertyEntryNotEditable() throws LiveDataException
    {
        when(this.liveDataSourceManager.get(new LiveDataQuery.Source("sourceIdTest"), null))
            .thenReturn(Optional.of(this.liveDataSource));

        when(this.liveDataSource.getProperties()).thenReturn(this.liveDataPropertyDescriptorStore);
        when(this.liveDataPropertyDescriptorStore.get("propertyIdTest"))
            .thenReturn(Optional.of(this.liveDataPropertyDescriptor));
        when(this.liveDataPropertyDescriptor.isEditable()).thenReturn(false);

        Response response = this.defaultLiveDataEntryPropertyResource
            .setProperty("sourceIdTest", null, "entryIdTest", "propertyIdTest", "valueTest");
        assertEquals(BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void setProperty() throws LiveDataException
    {
        when(this.liveDataSourceManager.get(new LiveDataQuery.Source("sourceIdTest"), null))
            .thenReturn(Optional.of(this.liveDataSource));

        when(this.liveDataSource.getProperties()).thenReturn(this.liveDataPropertyDescriptorStore);
        when(this.liveDataPropertyDescriptorStore.get("propertyIdTest"))
            .thenReturn(Optional.of(this.liveDataPropertyDescriptor));
        when(this.liveDataPropertyDescriptor.isEditable()).thenReturn(true);
        when(this.liveDataSource.getEntries()).thenReturn(this.liveDataEntryStore);
        when(this.liveDataEntryStore.get("entryIdTest", "propertyIdTest"))
            .thenReturn(Optional.of("result"));

        Response response = this.defaultLiveDataEntryPropertyResource
            .setProperty("sourceIdTest", null, "entryIdTest", "propertyIdTest", "valueTest");

        assertEquals(ACCEPTED.getStatusCode(), response.getStatus());
        assertEquals("result", response.getEntity());
        verify(this.liveDataEntryStore).update("entryIdTest", "propertyIdTest", "valueTest");
    }
}