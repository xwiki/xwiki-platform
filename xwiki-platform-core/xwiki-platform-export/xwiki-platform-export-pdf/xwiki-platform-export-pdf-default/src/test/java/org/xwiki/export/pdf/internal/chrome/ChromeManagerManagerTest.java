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
package org.xwiki.export.pdf.internal.chrome;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.browser.BrowserManager;
import org.xwiki.export.pdf.internal.docker.ContainerManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.github.dockerjava.api.model.HostConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChromeManagerManager}.
 * 
 * @version $Id$
 */
@ComponentTest
class ChromeManagerManagerTest
{
    @InjectMockComponents
    private ChromeManagerManager chromeManagerManager;

    @MockComponent
    private PDFExportConfiguration configuration;

    @MockComponent
    @Named("chrome")
    private BrowserManager chromeManager;

    @MockComponent
    private ContainerManager containerManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    HostConfig hostConfig;

    private String containerId = "8f55a905efec";

    private String containerIpAddress = "172.17.0.2";

    @BeforeEach
    void configure() throws Exception
    {
        when(this.configuration.getChromeDockerContainerName()).thenReturn("test-pdf-printer");
        when(this.configuration.getChromeDockerImage()).thenReturn("test/chrome:latest");
        when(this.configuration.getChromeRemoteDebuggingPort()).thenReturn(1234);
        when(this.configuration.getXWikiURI()).thenReturn(new URI("//xwiki-host"));

        mockNetwork("bridge");

        List<String> envVars = List.of("CHROME_DEBUG_PORT=" + (this.configuration.getChromeRemoteDebuggingPort() + 1));

        when(this.containerManager.createContainer(this.configuration.getChromeDockerImage(),
            this.configuration.getChromeDockerContainerName(), getChromeParams(), envVars, this.hostConfig))
                .thenReturn(this.containerId);
        when(
            this.containerManager.execInContainer(this.containerId, "bash", "-c",
                "timeout 30 bash -c 'until curl -s http://127.0.0.1:"
                    + this.configuration.getChromeRemoteDebuggingPort() + "/json/version; do sleep 1; done'"))
                        .thenReturn("{\"Browser\":\"142\"}");
    }

    private List<String> getChromeParams()
    {
        return List.of("--remote-allow-origins=http://localhost:" + this.configuration.getChromeRemoteDebuggingPort(),
            "--disable-dev-shm-usage");
    }

    private void mockNetwork(String networkIdOrName)
    {
        when(this.configuration.getDockerNetwork()).thenReturn(networkIdOrName);
        when(this.containerManager.getIpAddress(this.containerId, networkIdOrName)).thenReturn(this.containerIpAddress);

        this.hostConfig = mock(HostConfig.class);
        when(this.containerManager.getHostConfig(networkIdOrName, this.configuration.getChromeRemoteDebuggingPort()))
            .thenReturn(this.hostConfig);
        when(this.hostConfig.withSecurityOpts(any(List.class))).thenReturn(this.hostConfig);
        when(this.hostConfig.withExtraHosts("xwiki-host:host-gateway")).thenReturn(this.hostConfig);
    }

    @Test
    void getAndDispose() throws Exception
    {
        when(this.containerManager.maybeReuseContainerByName(this.configuration.getChromeDockerContainerName()))
            .thenReturn(null);
        when(this.containerManager.isLocalImagePresent(this.configuration.getChromeDockerImage())).thenReturn(false);

        assertEquals(this.chromeManager, this.chromeManagerManager.get());

        verify(this.containerManager).pullImage(this.configuration.getChromeDockerImage());
        verify(this.containerManager).startContainer(this.containerId);
        // Verify that the proxy for Chrome remote debugging is set up.
        verify(this.containerManager).execInContainer(this.containerId, true, "bash", "-c",
            "apt update && apt install -y socat");
        int localDebuggingPort = this.configuration.getChromeRemoteDebuggingPort() + 1;
        verify(this.containerManager).execInContainer(this.containerId, "bash", "-c",
            "timeout 30 bash -c 'until curl -s http://127.0.0.1:" + localDebuggingPort
                + "/json/version; do sleep 1; done'");
        verify(this.containerManager).execInContainer(this.containerId, "bash", "-c",
            "nohup socat TCP-LISTEN:" + this.configuration.getChromeRemoteDebuggingPort()
                + ",fork,reuseaddr TCP:127.0.0.1:" + localDebuggingPort + " > /dev/null 2>&1 &");
        verify(this.chromeManager).connect("localhost", this.configuration.getChromeRemoteDebuggingPort());

        this.chromeManagerManager.dispose();

        verify(this.chromeManager, times(2)).close();
        verify(this.containerManager).stopContainer(this.containerId);
    }

    @Test
    void getWithExistingContainer() throws Exception
    {
        mockNetwork("test-network");
        when(this.containerManager.maybeReuseContainerByName(this.configuration.getChromeDockerContainerName()))
            .thenReturn(this.containerId);

        assertEquals(this.chromeManager, this.chromeManagerManager.get());

        verify(this.containerManager, never()).pullImage(any(String.class));
        verify(this.containerManager, never()).startContainer(any(String.class));
        verify(this.hostConfig, never()).withExtraHosts(any(String.class));
        verify(this.chromeManager).connect(this.containerIpAddress, this.configuration.getChromeRemoteDebuggingPort());

        this.chromeManagerManager.dispose();
        verify(this.containerManager, never()).stopContainer(this.containerId);
    }

    @Test
    void getWithRemoteChrome() throws Exception
    {
        when(this.configuration.getChromeHost()).thenReturn("remote-chrome");

        assertEquals(this.chromeManager, this.chromeManagerManager.get());

        verify(this.containerManager, never()).maybeReuseContainerByName(any(String.class));
        verify(this.containerManager, never()).startContainer(any(String.class));

        verify(this.chromeManager).connect("remote-chrome", this.configuration.getChromeRemoteDebuggingPort());

        this.chromeManagerManager.dispose();
        verify(this.containerManager, never()).stopContainer(any(String.class));
    }

    @Test
    void getWithConfigurationChange() throws Exception
    {
        when(this.configuration.getChromeHost()).thenReturn("remote-chrome");
        // For the purpose of this test we assume that Chrome remains connected after we establish the connection.
        when(this.chromeManager.isConnected()).thenReturn(true);

        assertEquals(this.chromeManager, this.chromeManagerManager.get());
        assertEquals(this.chromeManager, this.chromeManagerManager.get());

        // Change the configuration and get the instance again.
        when(this.configuration.getChromeHost()).thenReturn("another-chrome");

        assertEquals(this.chromeManager, this.chromeManagerManager.get());

        verify(this.chromeManager).connect("remote-chrome", this.configuration.getChromeRemoteDebuggingPort());
        verify(this.chromeManager).connect("another-chrome", this.configuration.getChromeRemoteDebuggingPort());
        verify(this.chromeManager, times(2)).close();
    }

    @Test
    void getAfterRemoteChromeDisconnects() throws Exception
    {
        when(this.configuration.getChromeHost()).thenReturn("remote-chrome");

        assertEquals(this.chromeManager, this.chromeManagerManager.get());
        // Assume that we manage to connect with Chrome.
        when(this.chromeManager.isConnected()).thenReturn(true);

        // This shouldn't trigger a reconnect because the configuration didn't change and Chrome is still connected.
        assertEquals(this.chromeManager, this.chromeManagerManager.get());

        // Simulate that Chrome disconnected.
        when(this.chromeManager.isConnected()).thenReturn(false);

        // Both should trigger a reconnect because Chrome is not connected anymore.
        assertEquals(this.chromeManager, this.chromeManagerManager.get());
        assertEquals(this.chromeManager, this.chromeManagerManager.get());

        verify(this.chromeManager, times(3)).connect("remote-chrome",
            this.configuration.getChromeRemoteDebuggingPort());
        verify(this.chromeManager, times(3)).close();
    }

    @Test
    void getWithSandboxFails() throws Exception
    {
        when(this.containerManager.maybeReuseContainerByName(this.configuration.getChromeDockerContainerName()))
            .thenReturn(null);
        int port = this.configuration.getChromeRemoteDebuggingPort();
        RuntimeException exception = new RuntimeException("Test exception");
        doThrow(exception).when(this.chromeManager).connect("localhost", port);

        List<String> envVars = List.of("CHROME_DEBUG_PORT=" + (port + 1));
        List<String> parameters = new ArrayList<>();
        parameters.addAll(getChromeParams());
        parameters.add("--no-sandbox");
        when(this.containerManager.createContainer(this.configuration.getChromeDockerImage(),
            this.configuration.getChromeDockerContainerName(), parameters, envVars, this.hostConfig))
                .thenReturn(this.containerId);

        try {
            this.chromeManagerManager.get();
            fail("Getting the Chrome manager should have failed.");
        } catch (Exception e) {
            assertEquals(exception, e.getCause());
        }
        assertEquals("Starting Chrome headless with sandbox mode disabled.", this.logCapture.getMessage(0));

        verify(this.containerManager, times(2)).startContainer(this.containerId);
    }
}
