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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.PDFPrinter;
import org.xwiki.export.pdf.internal.chrome.ChromeManager;

import com.github.kklisura.cdt.protocol.types.network.CookieParam;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.xpn.xwiki.XWikiContext;

/**
 * Prints the content of a given URL using a headless Chrome web browser running inside a Docker container.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5RC1
 */
@Component
@Singleton
@Named("docker")
public class DockerPDFPrinter implements PDFPrinter<URL>, Initializable, Disposable
{
    @Inject
    private Logger logger;

    @Inject
    private PDFExportConfiguration configuration;

    @Inject
    private ChromeManager chromeManager;

    @Inject
    private ContainerManager containerManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

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
                this.containerManager.startContainer(this.containerId);
            }
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize the Docker container for the PDF export.", e);
        }
    }

    private void initializeChromeService(int remoteDebuggingPort) throws InitializationException
    {
        try {
            String chromeContainerIpAddress = this.containerManager.inspectContainer(this.containerId)
                .getNetworkSettings().getNetworks().get("bridge").getIpAddress();
            this.chromeManager.connect(chromeContainerIpAddress, remoteDebuggingPort);
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
    public InputStream print(URL printPreviewURL) throws IOException
    {
        this.logger.debug("Printing [{}]", printPreviewURL);

        URL dockerPrintPreviewURL = getDockerPrintPreviewURL(printPreviewURL);
        ChromeDevToolsService devToolsService = this.chromeManager.createIncognitoTab();
        try {
            this.chromeManager.setCookies(devToolsService, getCookies(dockerPrintPreviewURL));
            this.chromeManager.navigate(devToolsService, dockerPrintPreviewURL);

            if (!printPreviewURL.toString().equals(dockerPrintPreviewURL.toString())) {
                // Make sure the relative URLs are resolved based on the original print preview URL otherwise the user
                // won't be able to open the links from the generated PDF because they use a host name defined only
                // inside the Docker container where the PDF was generated. See
                // PDFExportConfiguration#getChromeDockerHostName()
                this.chromeManager.setBaseURL(devToolsService, printPreviewURL);
            }

            return this.chromeManager.printToPDF(devToolsService, () -> {
                this.chromeManager.closeIncognitoTab(devToolsService);
            });
        } catch (Exception e) {
            // Close the developer tools associated with the opened incognito browser tab only if an exception is
            // caught. Otherwise the developer tools will be closed after the PDF input stream is read and closed.
            devToolsService.close();
            // Propagate the caught exception.
            throw e;
        }
    }

    /**
     * The given print preview URL was created based on the request made by the users's browser so it represents the way
     * the users's browser can access the print preview. The headless Chrome browser that we're using for PDF printing,
     * running inside a dedicated Docker container, is not necessarily able to access the print preview in the same way,
     * because:
     * <ul>
     * <li>The user's browser may be behind some proxy or inside a different Docker container (with different settings)
     * like when running the functional tests, so the print preview URL suffers transformations before reaching XWiki,
     * transformations that don't happen for the headless Chrome browser.</li>
     * <li>For safety reasons the Docker containing running the headless Chrome browser uses its own separate network
     * interface, which means for it 'localhost' doesn't point to the host running XWiki, but the Docker container
     * itself. See {@link PDFExportConfiguration#getChromeDockerHostName()}.</li>
     * </ul>
     * 
     * @param printPreviewURL the print preview URL used by the user's browser
     * @return the print preview URL to be used by the headless Chrome browser running inside a Docker container
     * @throws IOException if building the print preview URL fails
     */
    private URL getDockerPrintPreviewURL(URL printPreviewURL) throws IOException
    {
        if (printPreviewURL == null) {
            throw new IOException("Print preview URL missing.");
        }

        XWikiContext xcontext = this.xcontextProvider.get();
        // We expect the print preview URL to match the current request URL (i.e. same host) and to target the current
        // wiki, in which case the following line should produce a relative URL. Moreover, by recreating the print
        // preview URL we make sure it points to XWiki and not to some external site we don't control.
        String relativePrintPreviewURL =
            xcontext.getDoc().getURL("export", printPreviewURL.getQuery(), printPreviewURL.getRef(), xcontext);
        // When called from a daemon thread (like the one running the PDF export job) the following line should produce
        // the URL to access the current wiki independent of the current request URL. We call it "local" because we
        // expect it to be the URL that can be used to access the wiki from the same host running it.
        URL baseLocalWikiURL = xcontext.getWiki().getServerURL(xcontext.getWikiId(), xcontext);
        URL dockerPrintPreviewURL = new URL(baseLocalWikiURL, relativePrintPreviewURL);
        // The headless Chrome web browser runs inside a Docker container where 'localhost' refers to the container
        // itself. We have to update the domain from the print preview URL to point to the host running both the XWiki
        // instance and the Docker container.
        List<String> standardLocalHosts = Arrays.asList("localhost", "127.0.0.1");
        if (standardLocalHosts.contains(dockerPrintPreviewURL.getHost())) {
            try {
                dockerPrintPreviewURL = new URIBuilder(dockerPrintPreviewURL.toURI())
                    .setHost(this.configuration.getChromeDockerHostName()).build().toURL();
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }

        return dockerPrintPreviewURL;
    }

    /**
     * If set on the browser, the returned cookies will be applied whenever the given URL is requested.
     * 
     * @param targetURL the URL that the cookies will be bound to
     * @return the cookies from the current request, bound to the given URL
     */
    private List<CookieParam> getCookies(URL targetURL)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        List<CookieParam> cookies = this.chromeManager.toCookieParams((Cookie[]) xcontext.getRequest().getCookies());
        // Cookies must have either the URL or the domain set otherwise the browser rejects them.
        cookies.forEach(cookie -> cookie.setUrl(targetURL.toString()));
        return cookies;
    }
}
