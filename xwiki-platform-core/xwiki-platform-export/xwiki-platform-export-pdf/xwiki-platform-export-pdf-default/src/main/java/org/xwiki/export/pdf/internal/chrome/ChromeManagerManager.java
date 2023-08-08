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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Disposable;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.browser.BrowserManager;
import org.xwiki.export.pdf.internal.docker.ContainerManager;

import com.github.dockerjava.api.model.HostConfig;

/**
 * Wraps a {@link ChromeManager} instance and takes care of reconnecting it when the configuration changes.
 * 
 * @version $Id$
 * @since 14.10.7
 * @since 15.2RC1
 */
@Component(roles = ChromeManagerManager.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ChromeManagerManager implements Disposable
{
    private static final String BRIDGE_NETWORK = "bridge";

    @Inject
    private Logger logger;

    @Inject
    private PDFExportConfiguration configuration;

    @Inject
    @Named("chrome")
    private BrowserManager chromeManager;

    /**
     * We use a provider (i.e. lazy initialization) because we don't always need this component (e.g. when the Chrome
     * web browser is remote and thus not managed by XWiki).
     */
    @Inject
    private Provider<ContainerManager> containerManagerProvider;

    private String containerId;

    /**
     * Flag indicating if the Docker container used for PDF export was created by this component instance or not. This
     * is needed in order to know whether to stop the Docker container or not when the component is disposed. We
     * shouldn't stop a Docker container that we didn't create (i.e. that is being reused).
     */
    private boolean isContainerCreator;

    /**
     * We store the previous configuration values in order to detect when the configuration changes.
     */
    private Map<String, Object> previousConfig;

    @Override
    public void dispose()
    {
        disconnect();
    }

    /**
     * @return the {@link ChromeManager} instance
     */
    public BrowserManager get()
    {
        // We need to reconnect if the configuration changed (e.g. if the Chrome remote debugging port changed) or if
        // the connection with the Chrome web browser was closed (e.g. if Chrome crashed or was restarted).
        if (configurationChanged() || !this.chromeManager.isConnected()) {
            disconnect();
            connect();
        }

        return this.chromeManager;
    }

    private synchronized boolean configurationChanged()
    {
        Map<String, Object> nextConfig = copyConfiguration();
        boolean changed = !Objects.equals(this.previousConfig, nextConfig);
        this.previousConfig = nextConfig;
        return changed;
    }

    /**
     * @return a copy of the configuration properties that influence the connection with the Chrome web browser
     */
    private Map<String, Object> copyConfiguration()
    {
        Map<String, Object> config = new HashMap<>();
        config.put("chromeHost", this.configuration.getChromeHost());
        config.put("chromeRemoteDebuggingPort", this.configuration.getChromeRemoteDebuggingPort());
        config.put("chromeDockerImage", this.configuration.getChromeDockerImage());
        config.put("chromeDockerContainerName", this.configuration.getChromeDockerContainerName());
        config.put("dockerNetwork", this.configuration.getDockerNetwork());
        return config;
    }

    private void disconnect()
    {
        this.chromeManager.close();

        if (this.containerId != null && this.isContainerCreator) {
            this.containerManagerProvider.get().stopContainer(this.containerId);
            this.containerId = null;
            this.isContainerCreator = false;
        }
    }

    private void connect()
    {
        String chromeHost = this.configuration.getChromeHost();
        if (StringUtils.isBlank(chromeHost)) {
            chromeHost = prepareChromeDockerContainer(this.configuration.getChromeDockerImage(),
                this.configuration.getChromeDockerContainerName(), this.configuration.getDockerNetwork(),
                this.configuration.getChromeRemoteDebuggingPort());
        }
        connectChromeService(chromeHost, this.configuration.getChromeRemoteDebuggingPort());
    }

    private String prepareChromeDockerContainer(String imageName, String containerName, String network,
        int remoteDebuggingPort)
    {
        this.logger.debug("Preparing the Docker container running the headless Chrome web browser.");
        ContainerManager containerManager = this.containerManagerProvider.get();
        try {
            this.containerId = containerManager.maybeReuseContainerByName(containerName);
            if (this.containerId == null) {
                // The container doesn't exist so we have to create it.
                // But first we need to pull the image used to create the container, if we don't have it already.
                if (!containerManager.isLocalImagePresent(imageName)) {
                    containerManager.pullImage(imageName);
                }

                HostConfig hostConfig = containerManager.getHostConfig(network, remoteDebuggingPort);
                if (BRIDGE_NETWORK.equals(network)) {
                    // The extra host is needed in order for the created container to be able to access the XWiki
                    // instance running on the same machine as the Docker daemon.
                    hostConfig =
                        hostConfig.withExtraHosts(this.configuration.getXWikiURI().getHost() + ":host-gateway");
                }

                this.containerId = containerManager.createContainer(imageName, containerName,
                    Arrays.asList("--no-sandbox", "--remote-debugging-address=0.0.0.0",
                        "--remote-debugging-port=" + remoteDebuggingPort),
                    hostConfig);
                this.isContainerCreator = true;
                containerManager.startContainer(this.containerId);
            }

            // By default we assume XWiki is not running inside a Docker container (i.e. it runs on the same host as the
            // Docker daemon) so it can access the Chrome browser (that runs inside a Docker container) using localhost
            // because the Chrome container is supposed to export the remote debugging port on the Docker daemon host.
            // Note that on Linux we could have also used the Chrome container's IP address but unfortunately that
            // doesn't work on MacOS and Windows. The recommendation is to either use localhost plus port forwarding
            // when you are on the Docker daemon host, or use the container IP/alias when you are inside a container.
            String chromeHost = "localhost";
            if (!BRIDGE_NETWORK.equals(network)) {
                // Using a dedicated (user-defined) Docker network (instead of the default bridge) normally signals the
                // fact that XWiki is also running inside a Docker container and so both the XWiki container and the
                // Chrome container are in the specified network. In this case the Chrome browser must be accessed using
                // the IP address of its Docker container.
                chromeHost = containerManager.getIpAddress(this.containerId, network);
            }
            return chromeHost;
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare the Docker container for the PDF export.", e);
        }
    }

    private void connectChromeService(String host, int remoteDebuggingPort)
    {
        try {
            this.chromeManager.connect(host, remoteDebuggingPort);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to the Chrome remote debugging service.", e);
        }
    }
}
