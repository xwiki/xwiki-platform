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
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.PDFPrinter;

import com.github.kklisura.cdt.protocol.types.network.CookieParam;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.xpn.xwiki.XWikiContext;

/**
 * Prints the content of a given URL using a (headless) Chrome web browser (that may be running inside a Docker
 * container).
 * 
 * @version $Id$
 * @since 14.7RC1
 */
@Component
@Singleton
@Named("chrome")
public class ChromePDFPrinter implements PDFPrinter<URL>
{
    @Inject
    private Logger logger;

    @Inject
    private PDFExportConfiguration configuration;

    @Inject
    private Provider<ChromeManager> chromeManagerProvider;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public InputStream print(URL printPreviewURL) throws IOException
    {
        if (printPreviewURL == null) {
            throw new IOException("Print preview URL missing.");
        }
        this.logger.debug("Printing [{}]", printPreviewURL);

        ChromeManager chromeManager = this.chromeManagerProvider.get();
        ChromeDevToolsService devToolsService = chromeManager.createIncognitoTab();
        URL dockerPrintPreviewURL = getDockerPrintPreviewURL(printPreviewURL, devToolsService);
        try {
            chromeManager.setCookies(devToolsService, getCookies(dockerPrintPreviewURL));
            if (!chromeManager.navigate(devToolsService, dockerPrintPreviewURL, true)) {
                throw new IOException("Failed to load the print preview URL: " + dockerPrintPreviewURL);
            }

            if (!printPreviewURL.toString().equals(dockerPrintPreviewURL.toString())) {
                // Make sure the relative URLs are resolved based on the original print preview URL otherwise the user
                // won't be able to open the links from the generated PDF because they use a host name defined only
                // inside the Docker container where the PDF was generated. See PDFExportConfiguration#getXWikiHost()
                chromeManager.setBaseURL(devToolsService, printPreviewURL);
            }

            return chromeManager.printToPDF(devToolsService, () -> {
                chromeManager.closeIncognitoTab(devToolsService);
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
     * itself. See {@link PDFExportConfiguration#getXWikiHost()}.</li>
     * </ul>
     * 
     * @param printPreviewURL the print preview URL used by the user's browser
     * @param devToolsService the developer tools service that should be able to access the print preview URL
     * @return the print preview URL to be used by the headless Chrome browser running inside a Docker container
     * @throws IOException if building the print preview URL fails
     */
    private URL getDockerPrintPreviewURL(URL printPreviewURL, ChromeDevToolsService devToolsService) throws IOException
    {
        return getDockerPrintPreviewURLs(printPreviewURL).stream()
            .filter(url -> this.isURLAccessibleFromChromeContainer(url, devToolsService)).findFirst()
            .orElseThrow(() -> new IOException("Couldn't find an alternative print preview URL that the headless "
                + "Chrome web browser can access from within its Docker container."));
    }

    private List<URL> getDockerPrintPreviewURLs(URL printPreviewURL) throws IOException
    {
        List<URL> dockerPrintPreviewURLs = new ArrayList<>();

        // 1. Try first with the same URL as the user (this may work in a domain-based multi-wiki setup).
        dockerPrintPreviewURLs.add(printPreviewURL);

        // 2. Try with the configured host.
        try {
            dockerPrintPreviewURLs.add(
                new URIBuilder(printPreviewURL.toURI()).setHost(this.configuration.getXWikiHost()).build().toURL());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        return dockerPrintPreviewURLs;
    }

    private boolean isURLAccessibleFromChromeContainer(URL printPreviewURL, ChromeDevToolsService devToolsService)
    {
        try {
            URL restURL = new URL(printPreviewURL, this.xcontextProvider.get().getRequest().getContextPath() + "/rest");
            return this.chromeManagerProvider.get().navigate(devToolsService, restURL, false);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * If set on the browser, the returned cookies will be applied whenever the given URL is requested.
     * 
     * @param targetURL the URL that the cookies will be bound to
     * @return the cookies from the current request, bound to the given URL
     */
    private List<CookieParam> getCookies(URL targetURL)
    {
        List<CookieParam> cookies = this.chromeManagerProvider.get()
            .toCookieParams((Cookie[]) this.xcontextProvider.get().getRequest().getCookies());
        // Cookies must have either the URL or the domain set otherwise the browser rejects them.
        cookies.forEach(cookie -> cookie.setUrl(targetURL.toString()));
        return cookies;
    }
}
