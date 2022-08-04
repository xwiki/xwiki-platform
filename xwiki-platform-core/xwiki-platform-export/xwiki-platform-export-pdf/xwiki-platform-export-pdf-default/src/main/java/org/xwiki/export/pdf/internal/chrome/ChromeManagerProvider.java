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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.internal.docker.ContainerManager;

import com.github.dockerjava.api.model.HostConfig;

/**
 * Default provider of {@link ChromeManager}.
 * 
 * @version $Id$
 * @since 14.7RC1
 */
@Component
@Singleton
public class ChromeManagerProvider implements Provider<ChromeManager>, Initializable, Disposable
{
    @Inject
    private Logger logger;

    @Inject
    private PDFExportConfiguration configuration;

    @Inject
    private ChromeManager chromeManager;

    /**
     * We use a provider (i.e. lazy initialization) because we don't always need this component (e.g. when the Chrome
     * web browser is remote and thus not managed by XWiki).
     */
    @Inject
    private Provider<ContainerManager> containerManagerProvider;

    private String containerId;

    @Override
    public void initialize() throws InitializationException
    {
        String chromeHost = this.configuration.getChromeHost();
        if (StringUtils.isBlank(chromeHost)) {
            chromeHost = initializeChromeDockerContainer(this.configuration.getChromeDockerImage(),
                this.configuration.getChromeDockerContainerName(), this.configuration.getDockerNetwork(),
                this.configuration.getChromeRemoteDebuggingPort());
        }
        initializeChromeService(chromeHost, this.configuration.getChromeRemoteDebuggingPort());
    }

    private String initializeChromeDockerContainer(String imageName, String containerName, String network,
        int remoteDebuggingPort) throws InitializationException
    {
        this.logger.debug("Initializing the Docker container running the headless Chrome web browser.");
        ContainerManager containerManager = this.containerManagerProvider.get();
        try {
            this.containerId = containerManager.maybeReuseContainerByName(containerName,
                this.configuration.isChromeDockerContainerReusable());
            if (this.containerId == null) {
                // The container doesn't exist so we have to create it.
                // But first we need to pull the image used to create the container, if we don't have it already.
                if (!containerManager.isLocalImagePresent(imageName)) {
                    containerManager.pullImage(imageName);
                }

                HostConfig hostConfig = containerManager.getHostConfig(network, remoteDebuggingPort);
                if ("bridge".equals(network)) {
                    // The extra host is needed in order for the created container to be able to access the XWiki
                    // instance running on the same machine as the Docker daemon.
                    hostConfig = hostConfig.withExtraHosts(this.configuration.getXWikiHost() + ":host-gateway");
                }

                this.containerId = containerManager.createContainer(imageName, containerName,
                    Arrays.asList("--no-sandbox", "--remote-debugging-address=0.0.0.0",
                        "--remote-debugging-port=" + remoteDebuggingPort),
                    hostConfig);
                containerManager.startContainer(this.containerId);
            }
            return containerManager.getIpAddress(this.containerId, network);
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize the Docker container for the PDF export.", e);
        }
    }

    private void initializeChromeService(String host, int remoteDebuggingPort) throws InitializationException
    {
        try {
            this.chromeManager.connect(host, remoteDebuggingPort);
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize the Chrome remote debugging service.", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.containerId != null) {
            try {
                this.containerManagerProvider.get().stopContainer(this.containerId);
            } catch (Exception e) {
                throw new ComponentLifecycleException(
                    String.format("Failed to stop the Docker container [%s] used for PDF export.", this.containerId),
                    e);
            }
        }
    }

    @Override
    public ChromeManager get()
    {
        return this.chromeManager;
    }
}
