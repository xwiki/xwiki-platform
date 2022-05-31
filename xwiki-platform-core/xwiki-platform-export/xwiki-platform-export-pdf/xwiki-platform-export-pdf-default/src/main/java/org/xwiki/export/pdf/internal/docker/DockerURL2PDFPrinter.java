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
package org.xwiki.export.pdf.internal.docker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.PDFPrinter;
import org.xwiki.export.pdf.internal.chrome.ChromeManager;

import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.types.ChromeTab;

/**
 * Prints the content of a given URL using a headless Chrome web browser running inside a Docker container.
 * 
 * @version $Id$
 * @since 14.4.1
 * @since 14.5RC1
 */
@Component
@Singleton
@Named("docker")
public class DockerURL2PDFPrinter implements PDFPrinter<URL>, Initializable, Disposable
{
    @Inject
    private Logger logger;

    @Inject
    private PDFExportConfiguration configuration;

    @Inject
    private ChromeManager chromeManager;

    @Inject
    private ContainerManager containerManager;

    private String containerId;

    @Override
    public void initialize() throws InitializationException
    {
        initializeChromeDockerContainer(this.configuration.getChromeDockerImage(),
            this.configuration.getChromeDockerContainerName(), this.configuration.getChromeRemoteDebuggingPort());
        initializeChromeService(this.configuration.getChromeRemoteDebuggingPort());
    }

    private void initializeChromeDockerContainer(String imageName, String containerName, int remoteDebuggingPort)
        throws InitializationException
    {
        this.logger.debug("Initializing the Docker container running the headless Chrome web browser.");
        try {
            this.containerId = this.containerManager.maybeReuseContainerByName(containerName);
            if (this.containerId == null) {
                // The container doesn't exist so we have to create it.
                // But first we need to pull the image used to create the container, if we don't have it already.
                if (!this.containerManager.isLocalImagePresent(imageName)) {
                    this.containerManager.pullImage(imageName);
                }

                this.containerId = this.containerManager.createContainer(imageName, containerName, remoteDebuggingPort,
                    Arrays.asList("--no-sandbox", "--remote-debugging-address=0.0.0.0",
                        "--remote-debugging-port=" + remoteDebuggingPort));
                this.containerManager.startContainer(containerId);
            }
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize the Docker container for the PDF export.", e);
        }
    }

    private void initializeChromeService(int remoteDebuggingPort) throws InitializationException
    {
        try {
            this.chromeManager.connect(remoteDebuggingPort);
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize the Chrome remote debugging service.", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        try {
            this.containerManager.stopContainer(this.containerId);
        } catch (Exception e) {
            throw new ComponentLifecycleException(
                String.format("Failed to stop the Docker container [%s] used for PDF export.", this.containerId), e);
        }
    }

    @Override
    public InputStream print(URL input) throws IOException
    {
        this.logger.debug("Printing [{}]", input);

        // The headless Chrome web browser runs inside a Docker container where 'localhost' refers to the container
        // itself. We have to update the domain from the given URL to point to the host running both the XWiki instance
        // and the Docker container.
        URL printPreviewURL =
            new URL(input.toString().replace("://localhost", "://" + this.configuration.getChromeDockerHostName()));

        ChromeService chromeService = this.chromeManager.getChromeService();
        ChromeTab tab = chromeService.createTab();
        ChromeDevToolsService devToolsService = chromeService.createDevToolsService(tab);

        Page page = devToolsService.getPage();
        page.enable();
        page.navigate(printPreviewURL.toString());

        Runtime runtime = devToolsService.getRuntime();
        runtime.enable();
        this.chromeManager.waitForPageReady(runtime);

        return this.chromeManager.printToPDF(devToolsService, () -> {
            devToolsService.close();
            chromeService.closeTab(tab);
        });
    }
}
