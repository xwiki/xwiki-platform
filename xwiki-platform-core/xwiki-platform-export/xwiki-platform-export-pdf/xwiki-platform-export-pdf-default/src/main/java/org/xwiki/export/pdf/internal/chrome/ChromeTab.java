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
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.Cookie;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.export.pdf.browser.BrowserTab;

import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.types.network.CookieParam;
import com.github.kklisura.cdt.protocol.types.page.Frame;
import com.github.kklisura.cdt.protocol.types.page.Navigate;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDF;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDFTransferMode;
import com.github.kklisura.cdt.protocol.types.target.TargetInfo;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.config.ChromeDevToolsServiceConfiguration;

/**
 * Represents a Chrome web browser tab.
 * 
 * @version $Id$
 * @since 14.8
 */
public class ChromeTab implements BrowserTab
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ChromeTab.class);

    private final ChromeDevToolsService tabDevToolsService;

    private final ChromeDevToolsService browserDevToolsService;

    private final PageReadyPromise pageReadyPromise;

    ChromeTab(ChromeDevToolsService tabDevToolsService, ChromeDevToolsService browserDevToolsService,
        ChromeDevToolsServiceConfiguration configuration)
    {
        this.tabDevToolsService = tabDevToolsService;
        this.browserDevToolsService = browserDevToolsService;
        this.pageReadyPromise =
            new PageReadyPromise(tabDevToolsService, configuration.getReadTimeout());
    }

    @Override
    public void close()
    {
        TargetInfo tabInfo = this.tabDevToolsService.getTarget().getTargetInfo();
        LOGGER.debug("Closing incognito tab [{}].", tabInfo.getTargetId());
        String browserContextId = tabInfo.getBrowserContextId();
        this.tabDevToolsService.close();
        LOGGER.debug("Disposing browser context [{}].", browserContextId);
        this.browserDevToolsService.getTarget().disposeBrowserContext(browserContextId);
    }

    @Override
    public boolean navigate(URL url, Cookie[] cookies, boolean wait, int timeout) throws IOException
    {
        return navigate(url, cookies, wait, timeout, () -> false);
    }

    @Override
    public boolean navigate(URL url, Cookie[] cookies, boolean wait, int timeout, BooleanSupplier isCanceled)
        throws IOException
    {
        LOGGER.debug("Navigating to [{}].", url);

        if (cookies != null) {
            setCookies(cookies, url);

            continueIfNotCanceled(isCanceled);
        }

        Page page = this.tabDevToolsService.getPage();
        page.enable();
        Navigate navigate = page.navigate(url.toString());
        boolean success = navigate.getErrorText() == null;

        if (success && wait && !isCanceled.getAsBoolean()) {
            this.pageReadyPromise.wait(timeout, isCanceled);
        }

        return success;
    }

    private void continueIfNotCanceled(BooleanSupplier isCanceled)
    {
        if (isCanceled.getAsBoolean()) {
            throw new CancellationException();
        }
    }

    @Override
    public String getSource()
    {
        Page page = this.tabDevToolsService.getPage();
        Frame frame = page.getFrameTree().getFrame();
        return page.getResourceContent(frame.getId(), frame.getUrl()).getContent();
    }

    @Override
    public InputStream printToPDF(Runnable cleanup)
    {
        LOGGER.debug("Printing web page to PDF.");
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
        String headerTemplate = "";
        String footerTemplate = "";
        Boolean preferCSSPageSize = false;
        PrintToPDFTransferMode transferMode = PrintToPDFTransferMode.RETURN_AS_STREAM;
        Boolean generateTaggedPDF = true;
        Boolean generateDocumentOutline = true;

        // See https://chromedevtools.github.io/devtools-protocol/tot/Page/#method-printToPDF
        PrintToPDF printToPDF =
            this.tabDevToolsService.getPage().printToPDF(landscape, displayHeaderFooter, printBackground, scale,
                paperWidth, paperHeight, marginTop, marginBottom, marginLeft, marginRight, pageRanges, headerTemplate,
                footerTemplate, preferCSSPageSize, transferMode, generateTaggedPDF, generateDocumentOutline);
        return new PrintToPDFInputStream(this.tabDevToolsService.getIO(), printToPDF.getStream(), cleanup);
    }

    /**
     * Converts servlet cookies to browser cookies.
     *
     * @param cookies the servlet cookies to convert to browser cookies
     * @return the browser cookies
     */
    private List<CookieParam> toCookieParams(Cookie[] cookies, URL targetURL)
    {
        if (cookies == null) {
            return List.of();
        } else {
            return Stream.of(cookies).filter(Objects::nonNull)
                .map(servletCookie -> toCookieParam(servletCookie, targetURL)).toList();
        }
    }

    /**
     * Converts a servlet cookie to a browser cookie.
     *
     * @param servletCookie the servlet cookie to convert to a browser cookie
     * @param targetURL the URL the cookie is applied to
     * @return the browser cookie
     */
    private CookieParam toCookieParam(Cookie servletCookie, URL targetURL)
    {
        CookieParam browserCookie = new CookieParam();
        browserCookie.setName(servletCookie.getName());
        browserCookie.setValue(servletCookie.getValue());
        browserCookie.setDomain(targetURL.getHost());
        // Preserve the original path. Note that the target web page behind the target URL may load additional resources
        // from different paths, that also need the cookie (e.g. for authentication), which is why we can't use the path
        // from the target URL.
        browserCookie.setPath(servletCookie.getPath());
        browserCookie.setSecure(servletCookie.getSecure());
        browserCookie.setHttpOnly(servletCookie.isHttpOnly());
        browserCookie.setUrl(targetURL.toString());
        return browserCookie;
    }

    /**
     * Sets the specified cookies.
     * 
     * @param servletCookies the cookies to set
     * @param targetURL the URL to apply the cookies to
     */
    private void setCookies(Cookie[] servletCookies, URL targetURL)
    {
        List<CookieParam> browserCookies = toCookieParams(servletCookies, targetURL);
        LOGGER.debug("Setting cookies [{}].", browserCookies.stream()
            .map(cookie -> String.format("%s: %s", cookie.getName(), cookie.getValue())).toList());
        Network network = this.tabDevToolsService.getNetwork();
        network.enable();
        network.clearBrowserCookies();
        network.setCookies(browserCookies);
    }

    @Override
    public void setExtraHTTPHeaders(Map<String, List<String>> headers)
    {
        LOGGER.debug("Setting extra HTTP headers [{}].", headers);
        Network network = this.tabDevToolsService.getNetwork();
        network.enable();
        // The documentation of setExtraHTTPHeaders is not clear about the type of value we can pass for a header (key).
        // We tried passing a List<String> and a String[] but in both cases we got an exception: "Invalid header value,
        // string expected". So we concatenate the values of a header with a comma.
        Map<String, Object> extraHeaders = headers.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> StringUtils.join(entry.getValue(), ",")));
        network.setExtraHTTPHeaders(extraHeaders);
    }
}
