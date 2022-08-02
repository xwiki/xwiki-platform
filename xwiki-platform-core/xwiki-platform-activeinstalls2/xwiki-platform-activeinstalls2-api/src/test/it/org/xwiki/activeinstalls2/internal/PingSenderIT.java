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
package org.xwiki.activeinstalls2.internal;

import java.sql.DatabaseMetaData;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.xwiki.activeinstalls2.ActiveInstallsConfiguration;
import org.xwiki.activeinstalls2.DataManager;
import org.xwiki.activeinstalls2.internal.data.ExtensionPing;
import org.xwiki.activeinstalls2.internal.data.Ping;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.internal.core.DefaultCoreExtensionRepository;
import org.xwiki.extension.repository.internal.installed.DefaultInstalledExtensionRepository;
import org.xwiki.instance.InstanceId;
import org.xwiki.instance.InstanceIdManager;
import org.xwiki.instance.internal.DefaultInstanceIdManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.DefaultQueryManager;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiHibernateStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests to verify that the ping sender correctly sends all the ping data to an Elasticsearch instance
 * (the tests used the official Elasticsearch docker image to ensure this).
 *
 * @version $Id$
 * @since 14.4RC1
 */
@ExtendWith(XWikiElasticSearchExtension.class)
@ComponentTest
@AllComponents(excludes = {
    // Excluded since we want to mock them as our goal is not to test them (we only want to test ES interactions)
    DefaultActiveInstallsConfiguration.class,
    DefaultInstanceIdManager.class,
    DefaultInstalledExtensionRepository.class,
    DefaultCoreExtensionRepository.class,
    DefaultQueryManager.class
})
class PingSenderIT
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private ActiveInstallsConfiguration configuration;

    @MockComponent
    private InstanceIdManager instanceIdManager;

    @MockComponent
    private InstalledExtensionRepository installedExtensionRepository;

    @MockComponent
    private CoreExtensionRepository coreExtensionRepository;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private QueryManager queryManager;

    @Inject
    private PingSender pingSender;

    @Inject
    private PingSetup pingSetup;

    @Inject
    private DataManager dataManager;

    @Inject
    private ElasticsearchClientManager clientManager;

    @Inject
    private Environment environment;

    @Inject
    private Execution execution;

    @InjectElasticSearchContainer
    private ElasticsearchContainer container;

    @BeforeComponent
    void beforeComponentInitialize()
    {
        // We need to configure ActiveInstallsConfiguration before DefaultElasticsearchClientManager is looked up,
        // since it's using that component in its Initializable#initialize() method.
        when(this.configuration.getPingInstanceURL()).thenReturn(
            String.format("http://%s", this.container.getHttpHostAddress()));
    }

    @Test
    void sendPingData() throws Exception
    {
        // Configure the Ping Sender to perform the ES setup since the ES instance is virgin at this stage.
        this.pingSetup.setup();

        // Distribution Ping Data Provider setup
        String activeInstanceId = UUID.randomUUID().toString();
        when(this.instanceIdManager.getInstanceId()).thenReturn(new InstanceId(activeInstanceId));

        // Servlet Container Ping Data Provider setup
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getServerInfo()).thenReturn(
            "servletcontainername/servletcontainerversion (servletcontainer text)");
        ((ServletEnvironment) this.environment).setServletContext(servletContext);

        // Distribution Ping Data Provider setup
        CoreExtension distributionExtension = mock(CoreExtension.class);
        ExtensionId extensionId = new ExtensionId("distributionId", "distributionVersion");
        when(distributionExtension.getId()).thenReturn(extensionId);
        Collection<ExtensionId> features = List.of(new ExtensionId("featureId", "featureVersion"));
        when(distributionExtension.getExtensionFeatures()).thenReturn(features);
        when(this.coreExtensionRepository.getEnvironmentExtension()).thenReturn(distributionExtension);

        // Database Ping Data Provider setup
        ExecutionContext ec = new ExecutionContext();
        this.execution.pushContext(ec);
        XWikiContext xcontext = mock(XWikiContext.class);
        XWiki xwiki = mock(XWiki.class);
        XWikiHibernateStore storeInterface = mock(XWikiHibernateStore.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getDatabaseProductName()).thenReturn("databaseProductName");
        when(metaData.getDatabaseProductVersion()).thenReturn("databaseProductVersion");
        when(storeInterface.getDatabaseMetaData()).thenReturn(metaData);
        when(xwiki.getStore()).thenReturn(storeInterface);
        when(xcontext.getWiki()).thenReturn(xwiki);
        ec.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xcontext);

        // Extension Ping Data Provider setup
        InstalledExtension extension1 = mock(InstalledExtension.class);
        when(extension1.getId()).thenReturn(new ExtensionId("extensionId1", "extensionVersion1"));
        when(extension1.getExtensionFeatures()).thenReturn(List.of(new ExtensionId("featureId1", "featureVersion1")));
        InstalledExtension extension2 = mock(InstalledExtension.class);
        when(extension2.getId()).thenReturn(new ExtensionId("extensionId2", "extensionVersion2"));
        when(extension2.getExtensionFeatures()).thenReturn(List.of(new ExtensionId("featureId2", "featureVersion2")));
        Collection<InstalledExtension> installedExtensions = List.of(extension1, extension2);
        when(this.installedExtensionRepository.getInstalledExtensions()).thenReturn(installedExtensions);

        // Users Ping Data Provider setup (and Wikis Ping Data Provider)
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(List.of("wiki1", "xwiki", "wiki2"));
        when(this.wikiDescriptorManager.isMainWiki("xwiki")).thenReturn(true);
        Query usersQuery = mock(Query.class);
        when(this.queryManager.createQuery(startsWith("SELECT COUNT(DISTINCT doc.fullName) FROM Document doc"),
            eq(Query.XWQL))).thenReturn(usersQuery);
        when(usersQuery.setWiki(any())).thenReturn(usersQuery);
        when(usersQuery.execute())
            // For wiki1
            .thenReturn(List.of(10L))
            // For xwiki (main wiki)
            .thenReturn(List.of(100L))
            // For wiki2
            .thenReturn(List.of(1000L));

        // Documents Ping Data Provider setup
        Query documentsQuery = mock(Query.class);
        when(this.queryManager.createQuery(eq(""), eq(Query.XWQL))).thenReturn(documentsQuery);
        when(documentsQuery.setWiki(any())).thenReturn(documentsQuery);
        when(documentsQuery.addFilter(any())).thenReturn(documentsQuery);
        when(documentsQuery.execute())
            // For wiki1
            .thenReturn(List.of(1000L))
            // For xwiki (main wiki)
            .thenReturn(List.of(10000L))
            // For wiki2
            .thenReturn(List.of(100000L));

        // Send a ping
        this.pingSender.sendPing();

        // Make sure the index is refreshed so that the data is available to search (we use search in the
        // DatePingDataProvider, see
        // https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-refresh.html
        this.clientManager.getClient().indices().refresh();

        // Send another ping. This verifies that we can send several pings (like verify we handle correctly not
        // creating the index the second time).
        this.pingSender.sendPing();

        // Make sure the index is refreshed so that the data is available to search below, see
        // https://www.elastic.co/guide/en/elasticsearch/reference/current/indices-refresh.html
        this.clientManager.getClient().indices().refresh();

        // Verify that the ping is stored inside ES by doing a search and asserting its content.

        // Verify that a count query works with an empty json
        assertEquals(2, this.dataManager.countInstalls(null));

        // Verify that a count query works with an non-empty json
        String jsonString = "{ \"term\" : { \"distribution.instanceId\" : \"" + activeInstanceId + "\" } }";
        assertEquals(2, this.dataManager.countInstalls(jsonString));

        // Verify that a search query works with an empty json
        List<Ping> pings = this.dataManager.searchInstalls(null);
        assertEquals(2, pings.size());

        // Verify that a search query works with an non-empty json
        pings = this.dataManager.searchInstalls(jsonString);
        assertEquals(2, pings.size());

        Ping ping = pings.get(0);

        // OS Ping Data Provider tests
        assertEquals(System.getProperty("os.name"), ping.getOS().getName());
        assertEquals(System.getProperty("os.arch"), ping.getOS().getArch());
        assertEquals(System.getProperty("os.version"), ping.getOS().getVersion());

        // Servlet Ping Data Provider tests
        assertEquals("servletcontainername", ping.getServletContainer().getName());
        assertEquals("servletcontainerversion", ping.getServletContainer().getVersion());

        // Memory Ping Data Provider tests
        assertNotNull(ping.getMemory().getMax());
        assertNotNull(ping.getMemory().getFree());
        assertNotNull(ping.getMemory().getTotal());
        assertNotNull(ping.getMemory().getUsed());

        // Java Ping Data Provider tests
        assertEquals(System.getProperty("java.vendor"), ping.getJava().getVendor());
        assertEquals(System.getProperty("java.specification.version"), ping.getJava().getSpecificationVersion());
        assertEquals(System.getProperty("java.version"), ping.getJava().getVersion());

        // First Date Data Provider tests
        assertNotNull(ping.getDate().getCurrent());
        assertNotNull(ping.getDate().getFirst());
        assertEquals(ping.getDate().getCurrent(), ping.getDate().getFirst());
        assertEquals(0, ping.getDate().getSince());

        // Distribution Ping Data Provider tests
        assertEquals(activeInstanceId, ping.getDistribution().getInstanceId());
        assertEquals("distributionId", ping.getDistribution().getExtension().getId());
        assertEquals("distributionVersion", ping.getDistribution().getExtension().getVersion());
        assertEquals(1, ping.getDistribution().getExtension().getFeatures().size());
        assertEquals("featureId/featureVersion", ping.getDistribution().getExtension().getFeatures().iterator().next());

        // Database Ping Data Provider Test
        assertEquals("databaseProductName", ping.getDatabase().getName());
        assertEquals("databaseProductVersion", ping.getDatabase().getVersion());

        // Extension Ping Data Provider Test
        assertEquals(2, ping.getExtensions().size());
        Iterator<ExtensionPing> extensionPingIterator = ping.getExtensions().iterator();
        ExtensionPing extensionPing1 = extensionPingIterator.next();
        assertEquals("extensionId1", extensionPing1.getId());
        assertEquals("extensionVersion1", extensionPing1.getVersion());
        assertEquals(1, extensionPing1.getFeatures().size());
        assertEquals("featureId1/featureVersion1", extensionPing1.getFeatures().iterator().next());
        ExtensionPing extensionPing2 = extensionPingIterator.next();
        assertEquals("extensionId2", extensionPing2.getId());
        assertEquals("extensionVersion2", extensionPing2.getVersion());
        assertEquals(1, extensionPing2.getFeatures().size());
        assertEquals("featureId2/featureVersion2", extensionPing2.getFeatures().iterator().next());

        // Users Ping Data Provider Test
        assertEquals(1110, ping.getUsers().getTotal());
        assertEquals(100, ping.getUsers().getMain());
        assertEquals(2, ping.getUsers().getWikis().size());
        assertEquals(10, ping.getUsers().getWikis().get(0));
        assertEquals(1000, ping.getUsers().getWikis().get(1));

        // Wikis Ping Data Provider Test
        assertEquals(3, ping.getWikis().getTotal());

        // Documents Ping Data Provider Test
        assertEquals(111000, ping.getDocuments().getTotal());
        assertEquals(10000, ping.getDocuments().getMain());
        assertEquals(2, ping.getDocuments().getWikis().size());
        assertEquals(1000, ping.getDocuments().getWikis().get(0));
        assertEquals(100000, ping.getDocuments().getWikis().get(1));
    }
}
