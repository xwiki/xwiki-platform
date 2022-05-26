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

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.export.pdf.PDFPrinter;

import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDF;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDFTransferMode;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.impl.ChromeServiceImpl;
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
    private static final String CHROME_IMAGE = "zenika/alpine-chrome:latest";

    private static final String CONTAINER_NAME = "headless-chrome-pdf-printer";

    private static final int REMOTE_DEBUGGING_PORT = 9222;

    @Inject
    private ContainerManager containerManager;

    private String containerId;

    private ChromeService chromeService;

    @Override
    public void initialize() throws InitializationException
    {
        initializeDockerContainer();
        this.chromeService = new ChromeServiceImpl(REMOTE_DEBUGGING_PORT);
    }

    private void initializeDockerContainer() throws InitializationException
    {
        try {
            this.containerId = this.containerManager.maybeReuseContainerByName(CONTAINER_NAME);
            if (this.containerId == null) {
                // The container doesn't exist so we have to create it.
                // But first we need to pull the image used to create the container, if we don't have it already.
                if (!this.containerManager.isLocalImagePresent(CHROME_IMAGE)) {
                    this.containerManager.pullImage(CHROME_IMAGE);
                }

                this.containerId = this.containerManager.createContainer(CHROME_IMAGE, CONTAINER_NAME,
                    REMOTE_DEBUGGING_PORT, Arrays.asList("--no-sandbox", "--remote-debugging-address=0.0.0.0",
                        "--remote-debugging-port=" + REMOTE_DEBUGGING_PORT));
                this.containerManager.startContainer(containerId);
            }
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize the Docker container for the PDF export.", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        try {
            this.containerManager.stopContainer(this.containerId);
        } catch (Exception e) {
            throw new ComponentLifecycleException("Failed to stop the Docker container used for PDF export.", e);
        }
    }

    @Override
    public InputStream print(URL input)
    {
        ChromeTab tab = chromeService.createTab();
        ChromeDevToolsService devToolsService = chromeService.createDevToolsService(tab);

        Page page = devToolsService.getPage();
        page.enable();
        page.navigate(input.toString());

        InputStream[] pdfStreams = new InputStream[] {null};

        page.onLoadEventFired(loadEventFired -> {
            Boolean landscape = false;
            Boolean displayHeaderFooter = false;
            Boolean printBackground = false;
            Double scale = 1d;
            // A4 paper format
            Double paperWidth = 8.27d;
            Double paperHeight = 11.7d;
            Double marginTop = 0d;
            Double marginBottom = 0d;
            Double marginLeft = 0d;
            Double marginRight = 0d;
            String pageRanges = "";
            Boolean ignoreInvalidPageRanges = false;
            String headerTemplate = "";
            String footerTemplate = "";
            Boolean preferCSSPageSize = false;
            PrintToPDFTransferMode mode = PrintToPDFTransferMode.RETURN_AS_STREAM;

            PrintToPDF printToPDF = devToolsService.getPage().printToPDF(landscape, displayHeaderFooter,
                printBackground, scale, paperWidth, paperHeight, marginTop, marginBottom, marginLeft, marginRight,
                pageRanges, ignoreInvalidPageRanges, headerTemplate, footerTemplate, preferCSSPageSize, mode);
            pdfStreams[0] = new PrintToPDFInputStream(devToolsService.getIO(), printToPDF.getStream());

            devToolsService.close();
        });

        devToolsService.waitUntilClosed();

        return pdfStreams[0];
    }
}
