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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.internal.browser.CookieFilter;
import org.xwiki.export.pdf.internal.browser.CookieFilter.CookieFilterContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;

/**
 * Unit tests for {@link AbstractBrowserPDFPrinter}.
 * 
 * @version $Id$
 */
@ComponentTest
class BrowserPDFPrinterTest
{
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AbstractBrowserPDFPrinter printer;

    @MockComponent
    private Logger logger;

    @MockComponent
    private PDFExportConfiguration configuration;

    @Mock
    private CookieFilter cookieFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private BrowserManager browserManager;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private BrowserTab browserTab;

    @Captor
    ArgumentCaptor<CookieFilterContext> cookieFilterContextCaptor;

    @BeforeEach
    void configure() throws Exception
    {
        ReflectionUtils.setFieldValue(this.printer, "logger", this.logger);
        ReflectionUtils.setFieldValue(this.printer, "configuration", this.configuration);
        ReflectionUtils.setFieldValue(this.printer, "cookieFilters", List.of(this.cookieFilter));

        when(this.cookieFilter.isFilterRequired()).thenReturn(true);

        when(this.printer.getBrowserManager()).thenReturn(this.browserManager);
        when(this.printer.getRequest()).thenReturn(this.request);

        when(this.request.getContextPath()).thenReturn("/xwiki");
        when(this.configuration.getXWikiURI()).thenReturn(new URI("//xwiki-host"));
        when(this.configuration.isXWikiURISpecified()).thenReturn(true);
        when(this.configuration.getPageReadyTimeout()).thenReturn(30);
    }

    @Test
    void printWithoutPreviewURL()
    {
        try {
            this.printer.print(null);
            fail();
        } catch (IOException e) {
            assertEquals("Print preview URL missing.", e.getMessage());
        }
    }

    @Test
    void print() throws Exception
    {
        URL printPreviewURL = new URL("http://external:9293/xwiki/bin/export/Some/Page?x=y#z");
        URL browserPrintPreviewURL = new URL("http://xwiki-host:9293/xwiki/bin/export/Some/Page?x=y#z");

        Cookie[] cookies = new Cookie[] {mock(Cookie.class)};
        when(this.request.getCookies()).thenReturn(cookies);

        when(this.browserManager.createIncognitoTab()).thenReturn(this.browserTab);
        when(this.browserTab.navigate(new URL("http://xwiki-host:9293/xwiki/rest/client?media=json"))).thenReturn(true);
        when(this.browserTab.getSource()).thenReturn("{\"ip\":\"172.12.0.3\"}");
        when(this.browserTab.navigate(browserPrintPreviewURL, cookies, true, 30)).thenReturn(true);

        InputStream pdfInputStream = mock(InputStream.class);
        when(this.browserTab.printToPDF(any(Runnable.class))).then(new Answer<InputStream>()
        {
            @Override
            public InputStream answer(InvocationOnMock invocation) throws Throwable
            {
                try {
                    return pdfInputStream;
                } finally {
                    invocation.getArgument(0, Runnable.class).run();
                }
            }
        });

        assertSame(pdfInputStream, this.printer.print(printPreviewURL));

        verify(this.cookieFilter).filter(eq(Arrays.asList(cookies)), this.cookieFilterContextCaptor.capture());
        assertEquals("172.12.0.3", this.cookieFilterContextCaptor.getValue().getBrowserIPAddress());
        assertEquals(browserPrintPreviewURL, this.cookieFilterContextCaptor.getValue().getTargetURL());

        // Only the configured XWiki URI should be used to get the browser IP address.
        verify(this.browserTab, never()).navigate(new URL("http://external:9293/xwiki/rest/client?media=json"));
        verify(this.browserTab).close();
    }

    @Test
    void printWhenXWikiURINotSpecified() throws Exception
    {
        when(this.configuration.isXWikiURISpecified()).thenReturn(false);
        when(this.browserManager.createIncognitoTab()).thenReturn(this.browserTab);

        try {
            this.printer.print(new URL("http://external:9293/xwiki/bin/export/Some/Page?x=y#z"));
            fail();
        } catch (IOException e) {
            assertEquals("Couldn't find an alternative print preview URL "
                + "that the web browser used for PDF printing can access.", e.getMessage());
        }

        verify(this.browserTab).navigate(new URL("http://external:9293/xwiki/rest/client?media=json"));
        verify(this.browserTab).navigate(new URL("http://xwiki-host:9293/xwiki/rest/client?media=json"));
    }

    @Test
    void printWithoutCookieFiltering() throws Exception
    {
        when(this.cookieFilter.isFilterRequired()).thenReturn(false);
        Cookie[] cookies = new Cookie[] {mock(Cookie.class)};
        when(this.request.getCookies()).thenReturn(cookies);

        when(this.browserManager.createIncognitoTab()).thenReturn(this.browserTab);
        when(this.browserTab.navigate(new URL("http://xwiki-host:9293/xwiki/bin/export/Some/Page?x=y#z"), cookies, true,
            30)).thenReturn(true);

        this.printer.print(new URL("http://external:9293/xwiki/bin/export/Some/Page?x=y#z"));

        verify(this.browserTab, never()).navigate(new URL("http://external:9293/xwiki/rest/client?media=json"));
        verify(this.browserTab, never()).navigate(new URL("http://xwiki-host:9293/xwiki/rest/client?media=json"));
        verify(this.cookieFilter, never()).filter(any(), any());
    }

    @Test
    void printWithXWikiSchemeAndPortSpecified() throws Exception
    {
        when(this.configuration.getXWikiURI()).thenReturn(new URI("ftp://xwiki-host:8080"));

        Cookie[] cookies = new Cookie[] {mock(Cookie.class)};
        when(this.request.getCookies()).thenReturn(cookies);

        when(this.browserManager.createIncognitoTab()).thenReturn(this.browserTab);

        try {
            this.printer.print(new URL("https://external:9293/xwiki/bin/export/Some/Page?x=y#z"));
        } catch (IOException e) {
            assertEquals("Couldn't find an alternative print preview URL " +
                "that the web browser used for PDF printing can access.", e.getMessage());
        }

        verify(this.browserTab).navigate(new URL("ftp://xwiki-host:8080/xwiki/rest/client?media=json"));
    }

    @Test
    void isAvailable()
    {
        assertFalse(this.printer.isAvailable());

        when(this.browserManager.isConnected()).thenReturn(true);
        assertTrue(this.printer.isAvailable());

        RuntimeException exception = new RuntimeException("Connection failed!");
        when(this.browserManager.isConnected()).thenThrow(exception);
        assertFalse(this.printer.isAvailable());

        verify(this.logger).warn("Failed to connect to the web browser used for server-side PDF printing.", exception);
    }

    @Test
    void navigate() throws Exception
    {
        URL url = new URL("http://xwiki.org");
        when(this.browserTab.navigate(url, null, false, 60)).thenReturn(true);
        assertTrue(this.browserTab.navigate(url));
    }
}
