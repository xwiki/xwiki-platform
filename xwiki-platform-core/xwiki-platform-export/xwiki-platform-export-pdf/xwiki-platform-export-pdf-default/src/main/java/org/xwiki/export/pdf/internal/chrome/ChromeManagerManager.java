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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.io.IOUtils;
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

    private static String chromeSecureComputingProfile;

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
        // Try running Chrome headless in sandbox mode in a Docker container. This is not supported on all environments
        // (e.g. when XWiki runs in a Virtual Machine) which is why we retry below without the sandbox mode.
        connect(true);
    }

    private void connect(boolean inSandboxMode)
    {
        String chromeHost = this.configuration.getChromeHost();
        boolean shouldUseDocker = StringUtils.isBlank(chromeHost);
        if (shouldUseDocker) {
            chromeHost = prepareChromeDockerContainer(this.configuration.getChromeDockerImage(),
                this.configuration.getChromeDockerContainerName(), this.configuration.getDockerNetwork(),
                this.configuration.getChromeRemoteDebuggingPort(), inSandboxMode);
        }
        try {
            connectChromeService(chromeHost, this.configuration.getChromeRemoteDebuggingPort());
        } catch (Exception e) {
            if (inSandboxMode && shouldUseDocker) {
                // Try again to run Chrome headless in a Docker container but this time without sandbox mode.
                connect(false);
            } else {
                throw e;
            }
        }
    }

    private String prepareChromeDockerContainer(String imageName, String containerName, String network,
        int remoteDebuggingPort, boolean inSandboxMode)
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
                // Set the secure computing profile in order to be able to run Chrome in sandbox mode.
                // See https://hub.docker.com/r/femtopixel/google-chrome-headless#usage--improved
                hostConfig =
                    hostConfig.withSecurityOpts(Collections.singletonList("seccomp=" + getSecureComputingProfile()));

                // FIXME: We allow connections from any origins when not using the bridge network (i.e. when XWiki is
                // also running inside a Docker container) because we don't know the IP address of the Chrome Docker
                // container before it is created.
                String chromeRemoteAllowOrigins = "*";
                if (BRIDGE_NETWORK.equals(network)) {
                    // The extra host is needed in order for the created container to be able to access the XWiki
                    // instance running on the same machine as the Docker daemon.
                    hostConfig =
                        hostConfig.withExtraHosts(this.configuration.getXWikiURI().getHost() + ":host-gateway");
                    // Allow only localhost connections to the Chrome remote debugging service when using the bridge
                    // network (the Chrome Docker container exposes the remote debugging port on the Docker host).
                    chromeRemoteAllowOrigins = "http://localhost:" + remoteDebuggingPort;
                }

                // Chrome remote debugging accepts connections only on localhost, which means we have to use a proxy to
                // forward incoming requests to Chrome. For this we need two ports:
                // * one exposed by the container (i.e. the port used to make the remote debugging requests)
                // * one used by Chrome inside the Docker container to receive localhost requests
                // For simplicity we use consecutive port numbers.
                int localDebuggingPort = remoteDebuggingPort + 1;
                List<String> envVars = List.of("CHROME_DEBUG_PORT=" + localDebuggingPort);
                List<String> parameters = new ArrayList<>(List.of(
                    // It's mandatory to specify who can connect to the Chrome remote debugging service (using the
                    // WebSocket API).
                    "--remote-allow-origins=" + chromeRemoteAllowOrigins,
                    // This seems to fix the net::ERR_INSUFFICIENT_RESOURCES error.
                    "--disable-dev-shm-usage"));

                if (inSandboxMode) {
                    this.logger.debug("Starting Chrome headless in sandbox mode.");
                } else {
                    // Disable the sandbox mode.
                    this.logger.warn("Starting Chrome headless with sandbox mode disabled.");
                    parameters.add("--no-sandbox");
                }

                this.containerId =
                    containerManager.createContainer(imageName, containerName, parameters, envVars, hostConfig);
                this.isContainerCreator = true;
                containerManager.startContainer(this.containerId);

                // Setup the proxy that must run in front on headless Chrome.
                setupChromeProxy(containerManager, remoteDebuggingPort, localDebuggingPort);
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

    /**
     * Chrome accepts "remote" debugging connections only on localhost. But Chrome is running inside a Docker container
     * that is not using the host network most of the time. In order to overcome this limitation we need to set up a
     * proxy that runs inside the Chrome Docker container, forwarding external requests to Chrome. The proxy must
     * properly handle the HTTP-to-WebSocket upgrade requests used by the Chrome DevTools Protocol and ensure requests
     * appear to come from localhost to avoid Chrome's security restrictions.
     *
     * @param containerManager the container manager used to set up the proxy
     * @param remoteDebuggingPort the port exposed externally by the container
     * @param localDebuggingPort the port Chrome is listening on inside the container
     */
    private void setupChromeProxy(ContainerManager containerManager, int remoteDebuggingPort, int localDebuggingPort)
    {
        this.logger.debug("Setting up the proxy for Chrome remote debugging: {}:{} -> 127.0.0.1:{}",
            remoteDebuggingPort, remoteDebuggingPort, localDebuggingPort);

        // Install socat in the container (needs to be done as root).
        this.logger.debug("Installing socat in the Chrome container...");
        containerManager.execInContainer(this.containerId, true, bashCommand("apt update && apt install -y socat"));

        // Wait for Chrome to listen on the local port.
        String waitForChromeReady =
            "timeout 30 bash -c 'until curl -s http://127.0.0.1:%s/json/version; do sleep 1; done'";
        this.logger.debug("Waiting for Chrome to start on port {}...", localDebuggingPort);
        containerManager.execInContainer(this.containerId,
            bashCommand(waitForChromeReady.formatted(localDebuggingPort)));

        // Start the socat proxy and leave it runnning in the background.
        this.logger.debug("Starting socat proxy: 0.0.0.0:{} -> 127.0.0.1:{}", remoteDebuggingPort, localDebuggingPort);
        String startSocat = "nohup socat TCP-LISTEN:%s,fork,reuseaddr TCP:127.0.0.1:%s > /dev/null 2>&1 &";
        containerManager.execInContainer(this.containerId,
            bashCommand(startSocat.formatted(remoteDebuggingPort, localDebuggingPort)));

        // Wait for socat proxy to be ready by checking if it can forward requests to Chrome.
        this.logger.debug("Waiting for socat proxy to be ready on port {}...", remoteDebuggingPort);
        String response = containerManager.execInContainer(this.containerId,
            bashCommand(waitForChromeReady.formatted(remoteDebuggingPort)));

        // Verify the proxy is working by checking the JSON response.
        if (response != null && response.contains("Browser")) {
            this.logger.debug("Finished setting up the proxy for Chrome remote debugging.");
        } else {
            throw new RuntimeException("Failed to set up the proxy for Chrome remote debugging. Result: " + response);
        }
    }

    private String[] bashCommand(String command)
    {
        return new String[] {"bash", "-c", command};
    }

    /**
     * @return the secure computing profile file for running the Chrome Docker container (in sandbox mode)
     * @throws IOException if the secure computing profile can't be read
     * @see <a href="https://docs.docker.com/engine/security/seccomp/">Seccomp security profiles for Docker</a>
     */
    private static String getSecureComputingProfile() throws IOException
    {
        if (chromeSecureComputingProfile == null) {
            chromeSecureComputingProfile = IOUtils
                .toString(ChromeManagerManager.class.getResourceAsStream("/chrome.json"), StandardCharsets.UTF_8);
        }
        return chromeSecureComputingProfile;
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
