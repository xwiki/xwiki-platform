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

import java.util.Arrays;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.browser.BrowserManager;
import org.xwiki.export.pdf.internal.docker.ContainerManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.github.dockerjava.api.model.HostConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChromeManagerProvider}.
 * 
 * @version $Id$
 */
@ComponentTest
class ChromeManagerProviderTest
{
    @InjectMockComponents
    private ChromeManagerProvider chromeManagerProvider;

    @MockComponent
    private PDFExportConfiguration configuration;

    @MockComponent
    @Named("chrome")
    private BrowserManager chromeManager;

    @MockComponent
    private ContainerManager containerManager;

    HostConfig hostConfig;

    private String containerId = "8f55a905efec";

    private String containerIpAddress = "172.17.0.2";

    @BeforeComponent
    void configure()
    {
        when(this.configuration.getChromeDockerContainerName()).thenReturn("test-pdf-printer");
        when(this.configuration.getChromeDockerImage()).thenReturn("test/chrome:latest");
        when(this.configuration.getChromeRemoteDebuggingPort()).thenReturn(1234);
        when(this.configuration.getXWikiHost()).thenReturn("xwiki-host");

        mockNetwork("bridge");

        when(this.containerManager.createContainer(this.configuration.getChromeDockerImage(),
            this.configuration.getChromeDockerContainerName(),
            Arrays.asList("--no-sandbox", "--remote-debugging-address=0.0.0.0",
                "--remote-debugging-port=" + this.configuration.getChromeRemoteDebuggingPort()),
            this.hostConfig)).thenReturn(this.containerId);
    }

    private void mockNetwork(String networkIdOrName)
    {
        when(this.configuration.getDockerNetwork()).thenReturn(networkIdOrName);
        when(this.containerManager.getIpAddress(this.containerId, networkIdOrName)).thenReturn(this.containerIpAddress);

        this.hostConfig = mock(HostConfig.class);
        when(this.containerManager.getHostConfig(networkIdOrName, this.configuration.getChromeRemoteDebuggingPort()))
            .thenReturn(this.hostConfig);
        when(this.hostConfig.withExtraHosts(this.configuration.getXWikiHost() + ":host-gateway"))
            .thenReturn(this.hostConfig);
    }

    @BeforeComponent("initializeAndDispose")
    void beforeInitializeAndDispose()
    {
        when(this.containerManager.maybeReuseContainerByName(this.configuration.getChromeDockerContainerName()))
            .thenReturn(null);
        when(this.containerManager.isLocalImagePresent(this.configuration.getChromeDockerImage())).thenReturn(false);
    }

    @Test
    void initializeAndDispose() throws Exception
    {
        verify(this.containerManager).pullImage(this.configuration.getChromeDockerImage());
        verify(this.containerManager).startContainer(this.containerId);
        verify(this.chromeManager).connect("localhost", this.configuration.getChromeRemoteDebuggingPort());

        this.chromeManagerProvider.dispose();
        verify(this.containerManager).stopContainer(this.containerId);
    }

    @BeforeComponent("initializeWithExistingContainer")
    void beforeInitializeWithExistingContainer()
    {
        mockNetwork("test-network");
        when(this.containerManager.maybeReuseContainerByName(this.configuration.getChromeDockerContainerName()))
            .thenReturn(this.containerId);
    }

    @Test
    void initializeWithExistingContainer() throws Exception
    {
        verify(this.containerManager, never()).pullImage(any(String.class));
        verify(this.containerManager, never()).startContainer(any(String.class));
        verify(this.hostConfig, never()).withExtraHosts(any(String.class));
        verify(this.chromeManager).connect(this.containerIpAddress, this.configuration.getChromeRemoteDebuggingPort());

        this.chromeManagerProvider.dispose();
        verify(this.containerManager, never()).stopContainer(this.containerId);
    }

    @BeforeComponent("initializeWithRemoteChrome")
    void beforeInitializeWithRemoteChrome()
    {
        when(this.configuration.getChromeHost()).thenReturn("remote-chrome");
    }

    @Test
    void initializeWithRemoteChrome() throws Exception
    {
        verify(this.containerManager, never()).maybeReuseContainerByName(any(String.class));
        verify(this.containerManager, never()).startContainer(any(String.class));

        verify(this.chromeManager).connect("remote-chrome", this.configuration.getChromeRemoteDebuggingPort());

        this.chromeManagerProvider.dispose();
        verify(this.containerManager, never()).stopContainer(any(String.class));
    }
}
