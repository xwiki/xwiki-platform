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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.internal.api.SolrConfiguration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Validate {@link RemoteSolr}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({RemoteSolr.class, SolrSchemaUtils.class})
class RemoteSolrTest
{
    private static final String SYSTEM_INFO_PATH = "/solr/admin/info/system";

    private static final String ACCESS_FAILED = "Failed to access the Solr server at [";

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @MockComponent
    SolrConfiguration solrConfiguration;

    @InjectComponentManager
    MockitoComponentManager componentManager;

    @Test
    void init() throws ComponentLookupException
    {
        RemoteSolr solr = this.componentManager.getInstance(Solr.class, RemoteSolr.TYPE);

        assertEquals(RemoteSolr.DEFAULT_BASE_URL, solr.getRootClient().getBaseURL());
        // No Solr server in the unit tests: the initialization does not fail
        assertTrue(this.logCapture.getMessage(0).startsWith(ACCESS_FAILED + RemoteSolr.DEFAULT_BASE_URL + "]"));
    }

    @Test
    void initWithBaseURL() throws ComponentLookupException
    {
        when(this.solrConfiguration.getInstanceConfiguration(RemoteSolr.TYPE, "baseURL", null))
            .thenReturn("http://baseurl/");

        RemoteSolr solr = this.componentManager.getInstance(Solr.class, RemoteSolr.TYPE);

        assertEquals("http://baseurl", solr.getRootClient().getBaseURL());
        assertTrue(this.logCapture.getMessage(0).startsWith(ACCESS_FAILED));
    }

    @Test
    void initWithURL() throws ComponentLookupException
    {
        when(this.solrConfiguration.getInstanceConfiguration(RemoteSolr.TYPE, "url", null))
            .thenReturn("http://host/xwiki");
        when(this.solrConfiguration.getInstanceConfiguration(RemoteSolr.TYPE, "corePrefix",
            RemoteSolr.DEFAULT_CORE_PREFIX)).thenReturn(RemoteSolr.DEFAULT_CORE_PREFIX);

        RemoteSolr solr = this.componentManager.getInstance(Solr.class, RemoteSolr.TYPE);

        assertEquals("http://host", solr.getRootClient().getBaseURL());
        assertEquals(
            "[solr.remote.url] property in xwiki.properties file is deprecated, use [solr.remote.baseURL] instead",
            this.logCapture.getMessage(0));
        assertTrue(this.logCapture.getMessage(1).startsWith(ACCESS_FAILED));
        // The name of the search core configured with the deprecated property does not depend on the server version
        assertEquals("xwiki", solr.cores.get(SolrClientInstance.CORE_NAME).getSolrName());
    }

    @Test
    void initWithURLandBaseURL() throws ComponentLookupException
    {
        when(this.solrConfiguration.getInstanceConfiguration(RemoteSolr.TYPE, "url", null))
            .thenReturn("http://host/xwiki");
        when(this.solrConfiguration.getInstanceConfiguration(RemoteSolr.TYPE, "baseURL", null))
            .thenReturn("http://baseurl/");

        RemoteSolr solr = this.componentManager.getInstance(Solr.class, RemoteSolr.TYPE);

        assertEquals("http://baseurl", solr.getRootClient().getBaseURL());
        assertTrue(this.logCapture.getMessage(0).startsWith(ACCESS_FAILED));
    }

    @Test
    void serverInformationRequestedAgainAfterAFailure() throws Exception
    {
        AtomicInteger systemInfoRequests = new AtomicInteger();

        // A Solr server which is not ready to answer the first two requests
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(SYSTEM_INFO_PATH, exchange -> {
            if (systemInfoRequests.incrementAndGet() <= 2) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
            } else {
                NamedList<Object> lucene = new SimpleOrderedMap<>();
                lucene.add("solr-impl-version", "9.8.0");
                NamedList<Object> systemInfo = new SimpleOrderedMap<>();
                systemInfo.add("lucene", lucene);
                sendJavaBin(exchange, systemInfo);
            }
        });
        // No core
        server.createContext("/solr/admin/cores", exchange -> {
            NamedList<Object> response = new SimpleOrderedMap<>();
            response.add("status", new SimpleOrderedMap<>());
            sendJavaBin(exchange, response);
        });
        server.start();

        try {
            when(this.solrConfiguration.getInstanceConfiguration(RemoteSolr.TYPE, "baseURL", null))
                .thenReturn("http://" + server.getAddress().getHostString() + ':' + server.getAddress().getPort()
                    + "/solr");

            // The Solr server cannot answer while the component is initialized, which does not fail
            RemoteSolr solr = this.componentManager.getInstance(Solr.class, RemoteSolr.TYPE);
            assertEquals(1, systemInfoRequests.get());
            assertEquals(-1, solr.getSolrMajorVersion());
            assertTrue(this.logCapture.getMessage(0).startsWith(ACCESS_FAILED));

            // The Solr server still cannot answer
            SolrException exception = assertThrows(SolrException.class, () -> solr.getCore("events"));
            assertTrue(exception.getMessage().startsWith("Failed to access the Solr server information at ["));
            assertEquals(2, systemInfoRequests.get());
            assertEquals(-1, solr.getSolrMajorVersion());

            // The Solr server is asked again
            assertNull(solr.getCore("events"));
            assertEquals(3, systemInfoRequests.get());
            assertEquals(9, solr.getSolrMajorVersion());

            // The server information is not requested again once known
            assertNull(solr.getCore("events"));
            assertEquals(3, systemInfoRequests.get());
        } finally {
            server.stop(0);
        }
    }

    private static void sendJavaBin(HttpExchange exchange, NamedList<Object> response) throws IOException
    {
        exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream stream = exchange.getResponseBody(); JavaBinCodec codec = new JavaBinCodec()) {
            codec.marshal(response, stream);
        }
    }
}
