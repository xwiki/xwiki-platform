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
package org.xwiki.export.pdf.browser;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.PDFPrinter;
import org.xwiki.export.pdf.internal.browser.CookieFilter;
import org.xwiki.export.pdf.internal.browser.CookieFilter.CookieFilterContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for {@link PDFPrinter} implementations that rely on a web browser to perform the PDF printing.
 * 
 * @version $Id$
 * @since 14.8
 */
public abstract class AbstractBrowserPDFPrinter implements PDFPrinter<URL>
{
    @Inject
    protected Logger logger;

    @Inject
    protected PDFExportConfiguration configuration;

    @Inject
    private List<CookieFilter> cookieFilters;

    @Override
    public InputStream print(URL printPreviewURL) throws IOException
    {
        if (printPreviewURL == null) {
            throw new IOException("Print preview URL missing.");
        }
        this.logger.debug("Printing [{}]", printPreviewURL);

        BrowserTab browserTab = getBrowserManager().createIncognitoTab();
        CookieFilterContext cookieFilterContext = findCookieFilterContext(printPreviewURL, browserTab);
        Cookie[] cookies = getCookies(cookieFilterContext);
        try {
            if (!browserTab.navigate(cookieFilterContext.getTargetURL(), cookies, true,
                this.configuration.getPageReadyTimeout())) {
                throw new IOException("Failed to load the print preview URL: " + cookieFilterContext.getTargetURL());
            }

            return browserTab.printToPDF(browserTab::close);
        } catch (Exception e) {
            // Close the browser tab only if an exception is caught. Otherwise the tab will be closed after the PDF
            // input stream is read and closed.
            browserTab.close();
            // Propagate the caught exception.
            throw e;
        }
    }

    /**
     * @param cookieFilterContext the contextual information needed by the cookie filters
     * @return the cookies to pass to the web browser when printing the PDF
     */
    private Cookie[] getCookies(CookieFilterContext cookieFilterContext)
    {
        Cookie[] cookiesArray = getRequest().getCookies();
        List<Cookie> cookies = new LinkedList<>();
        if (cookiesArray != null) {
            Stream.of(cookiesArray).forEach(cookies::add);
        }
        this.cookieFilters.forEach(cookieFilter -> {
            try {
                cookieFilter.filter(cookies, cookieFilterContext);
            } catch (Exception e) {
                this.logger.warn("Failed to apply cookie filter [{}]. Root cause is: [{}].", cookieFilter,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        });
        return cookies.isEmpty() ? null : cookies.toArray(new Cookie[cookies.size()]);
    }

    /**
     * The given print preview URL was created based on the request made by the users's browser so it represents the way
     * the users's browser can access the print preview. The browser that we're using for PDF printing, that may be
     * running inside a dedicated Docker container, is not necessarily able to access the print preview in the same way,
     * because:
     * <ul>
     * <li>The user's browser may be behind some proxy or inside a different Docker container (with different settings)
     * like when running the functional tests, so the print preview URL suffers transformations before reaching XWiki,
     * transformations that don't happen for the web browser we're using for PDF printing.</li>
     * <li>For safety reasons the Docker container running the web browser uses its own separate network interface,
     * which means for it 'localhost' doesn't point to the host running XWiki, but the Docker container itself. See
     * {@link PDFExportConfiguration#getXWikiURI()}.</li>
     * </ul>
     * 
     * @param printPreviewURL the print preview URL used by the user's browser
     * @param browserTab browser tab that should be able to access the print preview URL
     * @return the cookie filter context, including the print preview URL to be used by the browser performing the PDF
     *         printing
     * @throws IOException if finding the cookie filter context fails, or we can't find any
     */
    private CookieFilterContext findCookieFilterContext(URL printPreviewURL, BrowserTab browserTab) throws IOException
    {
        return getBrowserPrintPreviewURLs(printPreviewURL).stream()
            .map(url -> this.getCookieFilterContext(url, browserTab)).flatMap(Optional::stream).findFirst()
            .orElseThrow(() -> new IOException("Couldn't find an alternative print preview URL that the web browser "
                + "used for PDF printing can access."));
    }

    private List<URL> getBrowserPrintPreviewURLs(URL printPreviewURL) throws IOException
    {
        try {
            if (this.configuration.isXWikiURISpecified()) {
                // Try only with the configured XWiki URI.
                return List.of(getBrowserPrintPreviewURL(printPreviewURL));
            } else {
                // Try first with the same URL as the user (this may work in a domain-based multi-wiki setup). If it
                // fails, try with the default XWiki URI.
                return List.of(printPreviewURL, getBrowserPrintPreviewURL(printPreviewURL));
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    private URL getBrowserPrintPreviewURL(URL printPreviewURL) throws URISyntaxException, MalformedURLException
    {
        URI xwikiURI = this.configuration.getXWikiURI();
        URIBuilder uriBuilder = new URIBuilder(printPreviewURL.toURI());
        if (xwikiURI.getScheme() != null) {
            uriBuilder.setScheme(xwikiURI.getScheme());
        }
        if (xwikiURI.getHost() != null) {
            uriBuilder.setHost(xwikiURI.getHost());
        }
        if (xwikiURI.getPort() != -1) {
            uriBuilder.setPort(xwikiURI.getPort());
        }
        return uriBuilder.build().toURL();
    }

    private Optional<CookieFilterContext> getCookieFilterContext(URL targetURL, BrowserTab browserTab)
    {
        return getBrowserIPAddress(targetURL, browserTab).map(browserIPAddress -> new CookieFilterContext()
        {
            @Override
            public String getBrowserIPAddress()
            {
                return browserIPAddress;
            }

            @Override
            public URL getTargetURL()
            {
                return targetURL;
            }
        });
    }

    private Optional<String> getBrowserIPAddress(URL targetURL, BrowserTab browserTab)
    {
        try {
            URL restURL = new URL(targetURL, getRequest().getContextPath() + "/rest/client?media=json");
            if (browserTab.navigate(restURL)) {
                ObjectMapper objectMapper = new ObjectMapper();
                String browserIPAddress = objectMapper.readTree(browserTab.getSource()).path("ip").asText();
                if (!StringUtils.isEmpty(browserIPAddress)) {
                    return Optional.of(InetAddress.getByName(browserIPAddress).getHostAddress());
                }
            }
        } catch (IOException e) {
            // Pass through.
        }

        return Optional.empty();
    }

    @Override
    public boolean isAvailable()
    {
        try {
            return getBrowserManager().isConnected();
        } catch (Exception e) {
            this.logger.warn("Failed to connect to the web browser used for server-side PDF printing.", e);
            return false;
        }
    }

    /**
     * @return the browser manager used to interact with the browser used for PDF printing
     */
    protected abstract BrowserManager getBrowserManager();

    /**
     * @return the current HTTP servlet request, used to take the cookies from
     */
    protected abstract HttpServletRequest getRequest();
}
