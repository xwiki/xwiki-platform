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
package org.xwiki.search.solr.internal;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.search.solr.internal.api.SolrInstance;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate that {@link SolrClientInstance} (and so the components which depend on it, like the event listeners) can be
 * initialized when the remote Solr server cannot be reached during the startup, and that it still fails to initialize
 * when the embedded search core cannot be loaded.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ SolrClientInstance.class, LazySolrCore.class })
class SolrClientInstanceStartupTest
{
    private static final String UNAVAILABLE_MESSAGE = "The Solr core [search] is not available: [SolrException:"
        + " Connection refused]. Check that the Solr server can be reached and that the core exists on it. The core"
        + " will be requested again when needed.";

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private Solr solr;

    @MockComponent
    private SolrConfiguration configuration;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeComponent("solrCannotBeReached")
    void beforeSolrCannotBeReached() throws Exception
    {
        when(this.configuration.getServerType()).thenReturn(RemoteSolr.TYPE);
        when(this.solr.getCore(SolrClientInstance.CORE_NAME)).thenThrow(new SolrException("Connection refused"));
    }

    @BeforeComponent("embeddedCoreFailure")
    void beforeEmbeddedCoreFailure() throws Exception
    {
        when(this.configuration.getServerType()).thenReturn("embedded");
        when(this.solr.getCore(SolrClientInstance.CORE_NAME)).thenThrow(new SolrException("Failed to load the core"));
    }

    @Test
    void solrCannotBeReached() throws Exception
    {
        // The Solr instance can be initialized even if Solr cannot be reached.
        SolrInstance solrInstance = this.componentManager.getInstance(SolrInstance.class);

        assertEquals(1, this.logCapture.size());
        assertEquals(UNAVAILABLE_MESSAGE, this.logCapture.getMessage(0));

        // The use right after fails with the same failure, without contacting the Solr server again (the search core
        // is requested again after a delay, see LazySolrCoreTest).
        SolrServerException exception = assertThrows(SolrServerException.class, solrInstance::commit);
        assertEquals("Connection refused", exception.getMessage());
        assertInstanceOf(SolrException.class, exception.getCause());
        verify(this.solr).getCore(SolrClientInstance.CORE_NAME);

        // The failure is logged only once.
        assertEquals(1, this.logCapture.size());
    }

    @Test
    void embeddedCoreFailure()
    {
        // With the embedded Solr, a search core which cannot be loaded is a configuration error: fail loudly.
        ComponentLookupException exception = assertThrows(ComponentLookupException.class,
            () -> this.componentManager.getInstance(SolrInstance.class));
        assertEquals("SolrException: Failed to load the core", ExceptionUtils.getRootCauseMessage(exception));
        assertEquals(0, this.logCapture.size());
    }
}
