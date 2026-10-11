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
package org.xwiki.extension.index.internal;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.cache.CacheManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.LazySolrCore;
import org.xwiki.search.solr.internal.RemoteSolr;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate that {@link ExtensionIndexStore} can be initialized when the remote Solr server cannot be reached during the
 * startup, and that it still fails to initialize when the embedded extension index core cannot be loaded.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ ExtensionIndexStore.class, LazySolrCore.class })
class ExtensionIndexStoreStartupTest
{
    private static final String UNAVAILABLE_MESSAGE = "The Solr core [extension_index] is not available:"
        + " [SolrException: Connection refused]. Check that the Solr server can be reached and that the core exists on"
        + " it. The core will be requested again when needed.";

    @MockComponent
    private Solr solr;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private SolrConfiguration solrConfiguration;

    @MockComponent
    private SolrUtils solrUtils;

    @MockComponent
    private ExtensionManager extensionManager;

    @MockComponent
    private ExtensionFactory extensionFactory;

    @MockComponent
    private ExtensionIndexSolrUtil extensionIndexSolrUtil;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private ExtensionIndexStore store;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private final XWikiSolrCore core = mock();

    private final SolrClient client = mock();

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        when(this.cacheManager.<SolrExtension>createNewCache(any())).thenReturn(mock());
    }

    @BeforeComponent("solrCannotBeReached")
    void beforeSolrCannotBeReached() throws Exception
    {
        when(this.solrConfiguration.getServerType()).thenReturn(RemoteSolr.TYPE);
        when(this.solr.getCore(ExtensionIndexSolrCoreInitializer.NAME))
            .thenThrow(new SolrException("Connection refused"));
    }

    @BeforeComponent("solrAvailable")
    void beforeSolrAvailable() throws Exception
    {
        when(this.solrConfiguration.getServerType()).thenReturn(RemoteSolr.TYPE);
        when(this.core.getClient()).thenReturn(this.client);
        when(this.solr.getCore(ExtensionIndexSolrCoreInitializer.NAME)).thenReturn(this.core);
    }

    @BeforeComponent("embeddedCoreFailure")
    void beforeEmbeddedCoreFailure() throws Exception
    {
        when(this.solrConfiguration.getServerType()).thenReturn("embedded");
        when(this.solr.getCore(ExtensionIndexSolrCoreInitializer.NAME))
            .thenThrow(new SolrException("Failed to load the core"));
    }

    @Test
    void solrCannotBeReached() throws Exception
    {
        // The store can be initialized even if Solr cannot be reached.
        this.store = this.componentManager.getInstance(ExtensionIndexStore.class);
        assertEquals(1, this.logCapture.size());
        assertEquals(UNAVAILABLE_MESSAGE, this.logCapture.getMessage(0));

        // The use right after fails with the same failure, without contacting the Solr server again (the core is
        // requested again after a delay, see LazySolrCoreTest).
        SolrServerException exception =
            assertThrows(SolrServerException.class, () -> this.store.search(new SolrQuery("*:*")));
        assertEquals("Connection refused", exception.getMessage());
        assertInstanceOf(SolrException.class, exception.getCause());
        verify(this.solr).getCore(ExtensionIndexSolrCoreInitializer.NAME);

        // The failure is logged only once.
        assertEquals(1, this.logCapture.size());
    }

    @Test
    void solrAvailable() throws Exception
    {
        this.store = this.componentManager.getInstance(ExtensionIndexStore.class);

        SolrQuery query = new SolrQuery("*:*");
        QueryResponse response = mock();
        when(this.client.query(query)).thenReturn(response);

        assertSame(response, this.store.search(query));
        this.store.commit();

        verify(this.client).commit();
        verify(this.solr).getCore(ExtensionIndexSolrCoreInitializer.NAME);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void embeddedCoreFailure()
    {
        // With the embedded Solr, a core which cannot be loaded is a configuration error: fail loudly.
        ComponentLookupException exception = assertThrows(ComponentLookupException.class,
            () -> this.componentManager.getInstance(ExtensionIndexStore.class));
        assertEquals("SolrException: Failed to load the core", ExceptionUtils.getRootCauseMessage(exception));
        assertEquals(0, this.logCapture.size());
    }
}
