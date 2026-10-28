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

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.inject.Named;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.common.util.NamedList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrCoreInitializer;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate the way {@link RemoteSolr} resolves and migrates the Solr cores.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList(RemoteSolr.class)
class RemoteSolrCoresTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private SolrConfiguration solrConfiguration;

    @MockComponent
    private SolrSchemaUtils solrSchemaUtils;

    @MockComponent
    @Named("search")
    private SolrCoreInitializer searchInitializer;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private HttpSolrClient rootClient;

    private RemoteSolr solr;

    @BeforeAll
    static void beforeAll()
    {
        System.setProperty("xwiki.solr.remote.requestVersion", "false");
    }

    @BeforeEach
    void beforeEach() throws Exception
    {
        // Use the default configuration
        when(this.solrConfiguration.getInstanceConfiguration(any(), any(), any()))
            .then(invocation -> invocation.getArgument(2));

        this.solr = this.componentManager.getInstance(Solr.class, RemoteSolr.TYPE);

        this.rootClient = mock(HttpSolrClient.class);
        when(this.rootClient.getBaseURL()).thenReturn(RemoteSolr.DEFAULT_BASE_URL);
        ReflectionUtils.setFieldValue(this.solr, "rootClient", this.rootClient);
    }

    private void setServer(int solrMajorVersion, String... cores) throws Exception
    {
        setServer(solrMajorVersion, Map.of(), cores);
    }

    private void setServer(int solrMajorVersion, Map<String, String> initFailures, String... cores) throws Exception
    {
        ReflectionUtils.setFieldValue(this.solr, "solrMajorVersion", solrMajorVersion);

        NamedList<Object> status = new NamedList<>();
        for (String core : cores) {
            status.add(core, new NamedList<>());
        }
        // Like the real Solr servers, return the failures as a map
        Map<String, String> failures = new LinkedHashMap<>(initFailures);
        when(this.rootClient.request(any(SolrRequest.class), nullable(String.class)))
            .thenReturn(new NamedList<>(Map.of("status", status, "initFailures", failures)));
    }

    private RemoteSolr spySolr(int coreMajorVersion, int serverMajorVersion)
    {
        RemoteSolr spySolr = spy(this.solr);
        doReturn(coreMajorVersion).when(spySolr).getCoreMajorVersion();
        ReflectionUtils.setFieldValue(spySolr, "solrMajorVersion", serverMajorVersion);

        return spySolr;
    }

    @Test
    void toSolrCoreName()
    {
        // The name only depends on the requested version, not on the version of the server
        ReflectionUtils.setFieldValue(this.solr, "solrMajorVersion", 10);

        assertEquals("xwiki", this.solr.toSolrCoreName("search", 8));
        assertEquals("xwiki_events", this.solr.toSolrCoreName("events", 8));
        assertEquals("xwiki_search_9", this.solr.toSolrCoreName("search", 9));
        assertEquals("xwiki_search_10", this.solr.toSolrCoreName("search", 10));
    }

    @Test
    void getCoreDesignedForCurrentVersionOnNewerServer() throws Exception
    {
        int coreMajorVersion = this.solr.getCoreMajorVersion();
        setServer(coreMajorVersion + 1, "xwiki_search_" + coreMajorVersion);

        XWikiSolrCore core = this.solr.getCore("search");

        assertEquals("xwiki_search_" + coreMajorVersion, core.getSolrName());
        assertEquals(coreMajorVersion, core.getSolrMajorVersion());
        verify(this.searchInitializer).initialize(core);
        verify(this.searchInitializer, never()).migrate(any(), any());
    }

    @Test
    void getCoreDesignedForNewerVersionThanCurrent() throws Exception
    {
        int coreMajorVersion = this.solr.getCoreMajorVersion();
        setServer(coreMajorVersion + 1, "xwiki_search_" + (coreMajorVersion + 1));

        assertNull(this.solr.getCore("search"));
    }

    @Test
    void getCoreDesignedForPreviousVersion() throws Exception
    {
        RemoteSolr spySolr = spy(this.solr);
        doReturn(10).when(spySolr).getCoreMajorVersion();
        setServer(10, "xwiki_search_9");

        XWikiSolrCore core = spySolr.getCore("search");

        // The core designed for the previous version is used as is until the one for the current version is created
        assertEquals("xwiki_search_9", core.getSolrName());
        assertEquals(9, core.getSolrMajorVersion());
        verify(this.searchInitializer).initialize(core);
        verify(this.searchInitializer, never()).migrate(any(), any());
    }

    @Test
    void getCoreMigratedFromPreviousVersion() throws Exception
    {
        RemoteSolr spySolr = spy(this.solr);
        doReturn(10).when(spySolr).getCoreMajorVersion();
        setServer(10, "xwiki_search_9", "xwiki_search_10");

        XWikiSolrCore core = spySolr.getCore("search");

        assertEquals("xwiki_search_10", core.getSolrName());
        assertEquals(10, core.getSolrMajorVersion());

        // The data is migrated from the core designed for the previous version
        ArgumentCaptor<XWikiSolrCore> sourceCaptor = ArgumentCaptor.forClass(XWikiSolrCore.class);
        verify(this.searchInitializer).migrate(sourceCaptor.capture(), any());
        assertEquals("xwiki_search_9", sourceCaptor.getValue().getSolrName());
        assertEquals(9, sourceCaptor.getValue().getSolrMajorVersion());
        verify(this.solrSchemaUtils).setFieldType(any(XWikiSolrCore.class),
            any(FieldTypeDefinition.class), anyBoolean());
    }

    @Test
    void getCoreMigratedFromCoreDesignedForOneVersionOlderThanServer() throws Exception
    {
        setServer(9, "xwiki_search_9", "xwiki");
        RemoteSolr spySolr = spySolr(9, 9);

        XWikiSolrCore core = spySolr.getCore("search");

        // Solr 9 can read the data of a core created with Solr 8
        assertEquals("xwiki_search_9", core.getSolrName());
        ArgumentCaptor<XWikiSolrCore> sourceCaptor = ArgumentCaptor.forClass(XWikiSolrCore.class);
        verify(this.searchInitializer).migrate(sourceCaptor.capture(), any());
        assertEquals("xwiki", sourceCaptor.getValue().getSolrName());
        verify(this.solrSchemaUtils).setFieldType(any(XWikiSolrCore.class),
            any(FieldTypeDefinition.class), anyBoolean());
    }

    @Test
    void getCoreWithPreviousCoreTooOldForServer() throws Exception
    {
        // Solr 10 fails to load a core created with Solr 8, which is then only listed among the failures
        setServer(10, Map.of("xwiki", "org.apache.solr.common.SolrException: Error opening new searcher"),
            "xwiki_search_9");
        RemoteSolr spySolr = spySolr(9, 10);

        XWikiSolrCore core = spySolr.getCore("search");

        // The core is usable, but neither migrated nor marked as migrated, so that the migration is attempted again
        assertEquals("xwiki_search_9", core.getSolrName());
        verify(this.searchInitializer).initialize(core);
        verify(this.searchInitializer, never()).migrate(any(), any());
        verify(this.solrSchemaUtils, never()).setFieldType(any(XWikiSolrCore.class),
            any(FieldTypeDefinition.class), anyBoolean());

        assertEquals("The data of the Solr core [xwiki] (designed for Solr 8) cannot be migrated to the Solr core"
            + " [xwiki_search_9]: it failed to be loaded by the Solr server"
            + " (org.apache.solr.common.SolrException: Error opening new searcher). This is most probably because"
            + " Solr 10 can only read the data of cores created with Solr 9 or more recent. To keep the data, migrate"
            + " it first with a Solr 9 server: start XWiki with a Solr 9 server containing both cores, then move the"
            + " migrated core to the current Solr server. In the meantime, the Solr core [xwiki_search_9] is used"
            + " without the previous data, and the migration will be attempted again at the next startup.",
            this.logCapture.getMessage(0));
    }

    @Test
    void getCoreWithPreviousCoreFailingToLoad() throws Exception
    {
        // A failure which is not related to the version of the core
        setServer(9, Map.of("xwiki", "org.apache.solr.common.SolrException: Could not load conf"), "xwiki_search_9");
        RemoteSolr spySolr = spySolr(9, 9);

        XWikiSolrCore core = spySolr.getCore("search");

        assertEquals("xwiki_search_9", core.getSolrName());
        verify(this.searchInitializer, never()).migrate(any(), any());
        verify(this.solrSchemaUtils, never()).setFieldType(any(XWikiSolrCore.class),
            any(FieldTypeDefinition.class), anyBoolean());

        assertEquals("The data of the Solr core [xwiki] (designed for Solr 8) cannot be migrated to the Solr core"
            + " [xwiki_search_9]: it failed to be loaded by the Solr server"
            + " (org.apache.solr.common.SolrException: Could not load conf). In the meantime, the Solr core"
            + " [xwiki_search_9] is used without the previous data, and the migration will be attempted again at the"
            + " next startup.", this.logCapture.getMessage(0));
    }

    @Test
    void getCoreWithPreviousCacheCoreFailingToLoad() throws Exception
    {
        setServer(10, Map.of("xwiki", "org.apache.solr.common.SolrException: Error opening new searcher"),
            "xwiki_search_9");
        RemoteSolr spySolr = spySolr(9, 10);
        // The data of a cache core is filled again when lost
        when(this.searchInitializer.isCache()).thenReturn(true);

        XWikiSolrCore core = spySolr.getCore("search");

        // No error, and the core is marked as migrated
        assertEquals("xwiki_search_9", core.getSolrName());
        verify(this.searchInitializer, never()).migrate(any(), any());
        verify(this.solrSchemaUtils).setFieldType(any(XWikiSolrCore.class),
            any(FieldTypeDefinition.class), anyBoolean());
    }
}
