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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.Cookie;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.browser.BrowserTab;

import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.types.network.CookieParam;
import com.github.kklisura.cdt.protocol.types.page.Frame;
import com.github.kklisura.cdt.protocol.types.page.Navigate;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDF;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDFTransferMode;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;
import com.github.kklisura.cdt.protocol.types.target.TargetInfo;
import com.github.kklisura.cdt.services.ChromeDevToolsService;

/**
 * Represents a Chrome web browser tab.
 * 
 * @version $Id$
 * @since 14.8
 */
public class ChromeTab implements BrowserTab
{
    /**
     * The JavaScript code used to wait for the page to be fully ready before printing it to PDF.
     */
    static final String PAGE_READY_PROMISE;

    private static final Logger LOGGER = LoggerFactory.getLogger(ChromeTab.class);

    static {
        // Read the JavaScript code for page ready promise once and cache it.
        String filePath = "/pageReadyPromise.js";
        try {
            PAGE_READY_PROMISE =
                IOUtils.toString(ChromeTab.class.getResourceAsStream(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to read the [%s] file.", filePath), e);
        }
    }

    private final ChromeDevToolsService tabDevToolsService;

    private final ChromeDevToolsService browserDevToolsService;

    private final PDFExportConfiguration configuration;

    ChromeTab(ChromeDevToolsService tabDevToolsService, ChromeDevToolsService browserDevToolsService,
        PDFExportConfiguration configuration)
    {
        this.tabDevToolsService = tabDevToolsService;
        this.browserDevToolsService = browserDevToolsService;
        this.configuration = configuration;
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
        LOGGER.debug("Navigating to [{}].", url);

        if (cookies != null) {
            setCookies(cookies, url);
        }

        Page page = this.tabDevToolsService.getPage();
        page.enable();
        Navigate navigate = page.navigate(url.toString());
        boolean success = navigate.getErrorText() == null;

        if (success && wait) {
            Runtime runtime = this.tabDevToolsService.getRuntime();
            runtime.enable();
            waitForPageReady(runtime, timeout);
        }

        return success;
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
        Boolean ignoreInvalidPageRanges = false;
        String headerTemplate = "";
        String footerTemplate = "";
        Boolean preferCSSPageSize = false;
        PrintToPDFTransferMode mode = PrintToPDFTransferMode.RETURN_AS_STREAM;

        // See https://chromedevtools.github.io/devtools-protocol/tot/Page/#method-printToPDF
        PrintToPDF printToPDF = this.tabDevToolsService.getPage().printToPDF(landscape, displayHeaderFooter,
            printBackground, scale, paperWidth, paperHeight, marginTop, marginBottom, marginLeft, marginRight,
            pageRanges, ignoreInvalidPageRanges, headerTemplate, footerTemplate, preferCSSPageSize, mode);
        return new PrintToPDFInputStream(this.tabDevToolsService.getIO(), printToPDF.getStream(), cleanup);
    }

    /**
     * Wait for a page to be ready.
     * 
     * @param runtime the page runtime
     * @param timeout the number of seconds to wait for the web page to be ready before timing out
     */
    private void waitForPageReady(Runtime runtime, int timeout) throws IOException
    {
        LOGGER.debug("Waiting for page to be ready.");
        String pageReadyPromise = PAGE_READY_PROMISE.replace("__pageReadyTimeout__", String.valueOf(timeout * 1000));
        Evaluate evaluate = runtime.evaluate(/* expression */ pageReadyPromise, /* objectGroup */ null,
            /* includeCommandLineAPI */ false, /* silent */ false, /* contextId */ null, /* returnByValue */ true,
            /* generatePreview */ false, /* userGesture */ false, /* awaitPromise */ true,
            /* throwOnSideEffect */ false, /* timeout */ this.configuration.getChromeRemoteDebuggingTimeout() * 1000.0,
            /* disableBreaks */ true, /* replMode */ false, /* allowUnsafeEvalBlockedByCSP */ false,
            /* uniqueContextId */ null);
        checkEvaluation(evaluate, "Page ready.", "Failed to wait for page to be ready.",
            "Timeout waiting for page to be ready.");
    }

    private void checkEvaluation(Evaluate evaluate, Object expectedValue, String evaluationException,
        String unexpectedValueException) throws IOException
    {
        String messageTemplae = "%s Root cause: %s";
        if (evaluate.getExceptionDetails() != null) {
            RemoteObject exception = evaluate.getExceptionDetails().getException();
            Object cause = exception.getDescription();
            if (cause == null) {
                // When the exception was thrown as a string or when a promise was rejected.
                cause = exception.getValue();
            }
            throw new IOException(String.format(messageTemplae, evaluationException, cause));
        } else {
            RemoteObject result = evaluate.getResult();
            if (!Objects.equals(expectedValue, result.getValue())) {
                throw new IOException(String.format(messageTemplae, unexpectedValueException, result.getValue()));
            }
        }
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
            return Collections.emptyList();
        } else {
            return Stream.of(cookies).filter(Objects::nonNull)
                .map(servletCookie -> toCookieParam(servletCookie, targetURL)).collect(Collectors.toList());
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
            .map(cookie -> String.format("%s: %s", cookie.getName(), cookie.getValue())).collect(Collectors.toList()));
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
