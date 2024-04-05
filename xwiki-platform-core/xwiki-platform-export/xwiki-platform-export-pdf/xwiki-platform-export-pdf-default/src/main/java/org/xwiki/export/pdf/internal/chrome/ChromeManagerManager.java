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
import java.util.Arrays;
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
                // See https://github.com/Zenika/alpine-chrome/tree/master#-the-best-with-seccomp
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

                // The default flags set by the zenika/alpine-chrome image are causing lots of
                // net::ERR_INSUFFICIENT_RESOURCES errors that break the PDF export. See
                // https://github.com/Zenika/alpine-chrome/issues/222 . We prefer to set the flags ourselves through the
                // parameters (see below).
                List<String> envVars = Arrays.asList("CHROMIUM_FLAGS=\"\"");

                List<String> parameters = new ArrayList<>(Arrays.asList(
                    // We don't know the IP address of the docker container at this point.
                    "--remote-debugging-address=0.0.0.0",
                    // Use the configured remote debugging port.
                    "--remote-debugging-port=" + remoteDebuggingPort,
                    // Older versions of Chrome (e.g. 102) worked fine without this but it seems newer versions require
                    // us to specify explicitly how the remote debugging service can be accessed.
                    "--remote-allow-origins=" + chromeRemoteAllowOrigins,
                    // This seems to fix the net::ERR_INSUFFICIENT_RESOURCES error. Strangely, it doesn't have the same
                    // effect if I put it in the CHROMIUM_FLAGS (environment variable).
                    "--disable-dev-shm-usage",
                    // This is only useful if you want to inspect the headless Chrome using chrome://inspect/#devices
                    // (without this the headless Chrome is detected but I can't open a new tab).
                    "about:blank"));

                if (inSandboxMode) {
                    this.logger.debug("Starting Chrome headless in sandbox mode.");
                } else {
                    // Disable the sandbox mode.
                    this.logger.warn("Starting Chrome headless with sandbox mode disabled.");
                    parameters.add(0, "--no-sandbox");
                }

                this.containerId =
                    containerManager.createContainer(imageName, containerName, parameters, envVars, hostConfig);
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
