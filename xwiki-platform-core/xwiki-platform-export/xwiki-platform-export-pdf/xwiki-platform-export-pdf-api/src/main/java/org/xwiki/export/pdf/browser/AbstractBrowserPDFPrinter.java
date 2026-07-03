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
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.PDFPrinter;
import org.xwiki.export.pdf.internal.browser.CookieFilter;
import org.xwiki.export.pdf.internal.browser.CookieFilter.CookieFilterContext;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for {@link PDFPrinter} implementations that rely on a web browser to perform the PDF printing.
 * 
 * @version $Id$
 * @since 14.8
 */
public abstract class AbstractBrowserPDFPrinter implements PDFPrinter<URL>
{
    private static final String HTTP_HEADER_FORWARDED = "Forwarded";

    private static final String HTTP_HEADER_FORWARDED_FOR = "X-Forwarded-For";

    @Inject
    protected Logger logger;

    @Inject
    protected PDFExportConfiguration configuration;

    @Inject
    private List<CookieFilter> cookieFilters;

    @Override
    public InputStream print(URL printPreviewURL) throws IOException
    {
        return print(printPreviewURL, () -> false);
    }

    @Override
    public InputStream print(URL printPreviewURL, BooleanSupplier isCanceled) throws IOException
    {
        if (printPreviewURL == null) {
            throw new IOException("Print preview URL missing.");
        }
        this.logger.debug("Printing [{}]", printPreviewURL);

        // Each PDF export is performed in a separate, incognito, browser tab, where the user that triggered the PDF
        // export is authenticated, so their cookies need to be isolated because multiple exports can happen at the same
        // time.
        BrowserTab browserTab = getBrowserManager().createIncognitoTab();

        try {
            continueIfNotCanceled(isCanceled);

            // Find the actual print preview URL and the IP address of the client (browser) that will be used to load
            // this URL and print its content to PDF.
            CookieFilterContext cookieFilterContext = findCookieFilterContext(printPreviewURL, browserTab);

            continueIfNotCanceled(isCanceled);

            // Indicate that the browser used to generate the PDF acts as a proxy that forwards the PDF export request
            // to the XWiki backend and "modifies" the HTML response, replacing it with the PDF document, before sending
            // it back to the original client (users's browser) that triggered the PDF export.
            browserTab.setExtraHTTPHeaders(
                Map.of(HTTP_HEADER_FORWARDED, getForwardedHTTPHeader(cookieFilterContext.getClientIPAddress())));

            continueIfNotCanceled(isCanceled);

            // Authentication cookies are usually bound to the IP address of the client that made the authentication
            // request (the users's browser in our case), so we may have to re-encode the authentication cookies based
            // on the IP address of the browser used to generate the PDF.
            boolean isFilterRequired = this.cookieFilters.stream().anyMatch(CookieFilter::isFilterRequired);
            if (isFilterRequired) {
                // We need to re-fetch the (perceived) client IP address because it may have changed when we set the
                // Forwarded HTTP header above. In a strict environment, the number of proxies between the client and
                // the server is known and verified. Adding a new proxy (i.e. the browser used to generate the PDF) may
                // fail the proxy count validation. The result may be that the proxy is seen as the original client, so
                // its IP address is used to decode the authentication cookies.
                cookieFilterContext =
                    getCookieFilterContext(cookieFilterContext.getTargetURL(), browserTab).orElseThrow();
            }

            // Filter the cookies if needed. E.g. the authentication cookies may have to be decoded and re-encoded based
            // on the client IP address from the cookie filter context.
            Cookie[] cookies = getCookies(cookieFilterContext);

            continueIfNotCanceled(isCanceled);

            // Load the print preview URL and wait for the web page to be ready (wait for all images to be loaded, wait
            // for asynchronous HTTP requests made at page load time, wait for JavaScript code executed on page load).
            if (!browserTab.navigate(cookieFilterContext.getTargetURL(), cookies, true,
                this.configuration.getPageReadyTimeout(), isCanceled)) {
                throw new IOException("Failed to load the print preview URL: " + cookieFilterContext.getTargetURL());
            }

            continueIfNotCanceled(isCanceled);

            // Print the loaded web page to PDF, skipping the print preview modal dialog.
            return browserTab.printToPDF(browserTab::close);
        } catch (Exception e) {
            // Close the browser tab only if an exception is caught. Otherwise the tab will be closed after the PDF
            // input stream is read and closed.
            browserTab.close();

            if (e instanceof CancellationException) {
                return InputStream.nullInputStream();
            } else {
                // Propagate the caught exception.
                throw e;
            }
        }
    }

    private void continueIfNotCanceled(BooleanSupplier isCanceled)
    {
        if (isCanceled.getAsBoolean()) {
            throw new CancellationException();
        }
    }

    /**
     * @param cookieFilterContext the contextual information needed by the cookie filters
     * @return the cookies to pass to the web browser when printing the PDF
     */
    private Cookie[] getCookies(CookieFilterContext cookieFilterContext)
    {
        Cookie[] cookiesArray = getJakartaRequest().getCookies();
        List<Cookie> cookies = new LinkedList<>();
        if (cookiesArray != null) {
            Stream.of(cookiesArray).forEach(cookies::add);
        }
        this.cookieFilters.forEach(cookieFilter -> {
            try {
                if (cookieFilter.isFilterRequired()) {
                    cookieFilter.filter(cookies, cookieFilterContext);
                }
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
        return getClientIPAddress(targetURL, browserTab).map(ip -> new CookieFilterContext()
        {
            @Override
            public String getClientIPAddress()
            {
                return ip;
            }

            @Override
            public URL getTargetURL()
            {
                return targetURL;
            }
        });
    }

    private Optional<String> getClientIPAddress(URL targetURL, BrowserTab browserTab)
    {
        try {
            URL restURL = new URL(targetURL, getJakartaRequest().getContextPath() + "/rest/client?media=json");
            if (browserTab.navigate(restURL)) {
                ObjectMapper objectMapper = new ObjectMapper();
                String clientIPAddress = objectMapper.readTree(browserTab.getSource()).path("ip").asText();
                if (!StringUtils.isEmpty(clientIPAddress)) {
                    return Optional.of(InetAddress.getByName(clientIPAddress).getHostAddress());
                }
            }
        } catch (IOException e) {
            // Pass through.
        }

        return Optional.empty();
    }

    /**
     * Computes the values of the "Forwarded" HTTP header as if the request was forwarded by the specified proxy.
     *
     * @param proxyIPAddress the IP address of the proxy that forwards the request to XWiki
     * @return the values of the "Forwarded" HTTP header
     */
    private List<String> getForwardedHTTPHeader(String proxyIPAddress)
    {
        HttpServletRequest request = getJakartaRequest();

        List<String> forwarded = new LinkedList<>();
        Enumeration<String> forwardedValues = request.getHeaders(HTTP_HEADER_FORWARDED);
        if (forwardedValues != null) {
            forwardedValues.asIterator().forEachRemaining(forwarded::add);
        }

        String forwardedFor = request.getHeader(HTTP_HEADER_FORWARDED_FOR);
        if (StringUtils.isBlank(forwardedFor)) {
            forwardedFor = request.getRemoteAddr();
        }

        String host = request.getHeader("X-Forwarded-Host");
        if (StringUtils.isBlank(host)) {
            host = request.getHeader("Host");
        }

        String protocol = request.getHeader("X-Forwarded-Proto");
        if (StringUtils.isBlank(protocol)) {
            protocol = request.getScheme();
        }

        String lastForwarded =
            String.format("by=%s;for=%s;host=%s;proto=%s", proxyIPAddress, forwardedFor, host, protocol);
        forwarded.add(lastForwarded);

        return forwarded;
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
     * @deprecated since 17.4.0RC1
     */
    @Deprecated
    protected abstract javax.servlet.http.HttpServletRequest getRequest();

    protected HttpServletRequest getJakartaRequest()
    {
        return JakartaServletBridge.toJakarta(getRequest());
    }
}
