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
package org.xwiki.activeinstalls.internal.client;

import java.net.UnknownHostException;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.activeinstalls.ActiveInstallsConfiguration;
import org.xwiki.activeinstalls.internal.DefaultJestClientManager;
import org.xwiki.test.LogRule;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import io.searchbox.client.AbstractJestClient;

import static com.github.tomakehurst.wiremock.client.RequestPatternBuilder.allRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for {@link org.xwiki.activeinstalls.internal.client.DefaultPingSender}.
 *
 * @version $Id$
 * @since 6.2M1
 */
@ComponentList({
    DefaultPingSender.class,
    DefaultJestClientManager.class
})
public class DefaultPingSenderIntegrationTest
{
    // Capture the logs since we don't want anything printed in the console and our proxy isn't configured to return
    // a valid return value which makes Jest Client choke but we don't care since all we care is verifying that our
    // proxy is called, see below.
    @Rule
    public LogRule logRule = new LogRule() {{
        record(LogLevel.ERROR);
        recordLoggingForType(AbstractJestClient.class);
    }};

    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    @Rule
    public WireMockRule proxyWireMockRule = new WireMockRule(8888);

    @BeforeComponent
    public void registerMockComponents() throws Exception
    {
        ActiveInstallsConfiguration configuration =
            this.componentManager.registerMockComponent(ActiveInstallsConfiguration.class);
        when(configuration.getPingInstanceURL()).thenReturn("http://host");
    }

    @Test
    public void sendPingThroughProxy() throws Exception
    {
        PingSender pingSender = this.componentManager.getInstance(PingSender.class);

        // First call the Ping Sender but since we haven't set up any proxy our Mock HTTP Server is not going to be
        // called (since http://host will lead to nowhere...
        try {
            pingSender.sendPing();
            fail("Should have raised an exception here");
        } catch (UnknownHostException expected) {
            // Nothing to check, this proves the proxy isn't set up!
        }
        assertTrue("The HTTP server was not called by the ping sender", findAll(allRequests()).isEmpty());

        // Second, setup a proxy by using System Properties, then call again the ping sender and this time it should
        // succeed since http://host will go to the proxy which is pointing to our Mock HTTP Server!
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", "8888");
        try {
            pingSender.sendPing();
            fail("Should have raised an exception here");
        } catch (Exception expected) {
            // We expect a 404 since we haven't configured the proxy to return something specific, but we don't need to
            // do that since all we want is to ensure the proxy is called!
            assertEquals("404 Not Found", expected.getMessage());
        }
        assertFalse("The HTTP server was called by the ping sender", findAll(allRequests()).isEmpty());
    }
}
