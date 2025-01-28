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
import java.util.List;

import jakarta.servlet.http.Cookie;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.test.junit5.mockito.ComponentTest;

import com.github.kklisura.cdt.protocol.commands.IO;
import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.commands.Target;
import com.github.kklisura.cdt.protocol.types.io.Read;
import com.github.kklisura.cdt.protocol.types.network.CookieParam;
import com.github.kklisura.cdt.protocol.types.page.Frame;
import com.github.kklisura.cdt.protocol.types.page.FrameTree;
import com.github.kklisura.cdt.protocol.types.page.Navigate;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDF;
import com.github.kklisura.cdt.protocol.types.page.PrintToPDFTransferMode;
import com.github.kklisura.cdt.protocol.types.page.ResourceContent;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.protocol.types.runtime.ExceptionDetails;
import com.github.kklisura.cdt.protocol.types.runtime.RemoteObject;
import com.github.kklisura.cdt.protocol.types.target.TargetInfo;
import com.github.kklisura.cdt.services.ChromeDevToolsService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChromeTab}.
 * 
 * @version $Id$
 */
@ComponentTest
class ChromeTabTest
{
    @Mock(name = "tabDevToolsService")
    private ChromeDevToolsService tabDevToolsService;

    @Mock(name = "browserDevToolsService")
    private ChromeDevToolsService browserDevToolsService;

    @Mock
    private PDFExportConfiguration configuration;

    private ChromeTab chromeTab;

    @Captor
    private ArgumentCaptor<List<CookieParam>> cookiesCaptor;

    @Mock
    private Runtime runtime;

    @Mock
    private Page page;

    @BeforeEach
    void configure()
    {
        when(this.tabDevToolsService.getRuntime()).thenReturn(this.runtime);
        when(this.tabDevToolsService.getPage()).thenReturn(this.page);
        when(this.configuration.getChromeRemoteDebuggingTimeout()).thenReturn(10);

        this.chromeTab = new ChromeTab(this.tabDevToolsService, this.browserDevToolsService, this.configuration);
    }

    @Test
    void close()
    {
        Target tabTarget = mock(Target.class, "tabTarget");
        when(this.tabDevToolsService.getTarget()).thenReturn(tabTarget);

        TargetInfo tabInfo = mock(TargetInfo.class, "tabInfo");
        when(tabTarget.getTargetInfo()).thenReturn(tabInfo);
        when(tabInfo.getTargetId()).thenReturn("tabId");
        when(tabInfo.getBrowserContextId()).thenReturn("browserContextId");

        Target browserTarget = mock(Target.class, "browserTarget");
        when(this.browserDevToolsService.getTarget()).thenReturn(browserTarget);

        this.chromeTab.close();

        verify(this.tabDevToolsService).close();
        verify(browserTarget).disposeBrowserContext("browserContextId");
    }

    @Test
    void navigateWithoutWait() throws Exception
    {
        URL url = new URL("http://www.xwiki.com/xwiki/bin/view/Support/");
        Cookie cookie = mock(Cookie.class);
        when(cookie.getName()).thenReturn("color");
        when(cookie.getValue()).thenReturn("blue");
        when(cookie.getDomain()).thenReturn("www.xwiki.org");
        when(cookie.getPath()).thenReturn("/xwiki/bin/view/Main/");
        when(cookie.getSecure()).thenReturn(true);
        when(cookie.isHttpOnly()).thenReturn(true);

        Network network = mock(Network.class);
        when(this.tabDevToolsService.getNetwork()).thenReturn(network);

        Navigate navigate = mock(Navigate.class);
        when(this.page.navigate(url.toString())).thenReturn(navigate);

        assertTrue(this.chromeTab.navigate(url, new Cookie[] {cookie, null}, false));

        verify(network).enable();
        verify(network).clearBrowserCookies();

        verify(network).setCookies(this.cookiesCaptor.capture());
        assertEquals(1, this.cookiesCaptor.getValue().size());
        CookieParam browserCookie = this.cookiesCaptor.getValue().get(0);
        assertEquals("color", browserCookie.getName());
        assertEquals("blue", browserCookie.getValue());
        assertEquals("www.xwiki.com", browserCookie.getDomain());
        // The original path is preserved.
        assertEquals("/xwiki/bin/view/Main/", browserCookie.getPath());
        assertTrue(browserCookie.getSecure());
        assertTrue(browserCookie.getHttpOnly());
        assertEquals("http://www.xwiki.com/xwiki/bin/view/Support/", browserCookie.getUrl());

        verify(this.page).enable();
    }

    @Test
    void navigateWithError() throws Exception
    {
        URL url = new URL("http://www.xwiki.org");

        Navigate navigate = mock(Navigate.class);
        when(this.page.navigate(url.toString())).thenReturn(navigate);
        when(navigate.getErrorText()).thenReturn("Failed to navigate!");

        assertFalse(this.chromeTab.navigate(url, (Cookie[]) null, false));
    }

    @Test
    void navigateWithWait() throws Exception
    {
        URL url = new URL("http://www.xwiki.org");

        Navigate navigate = mock(Navigate.class);
        when(this.page.navigate(url.toString())).thenReturn(navigate);

        Evaluate evaluate = mock(Evaluate.class);
        String pageReadyPromise = ChromeTab.PAGE_READY_PROMISE.replace("__pageReadyTimeout__", "25000");
        when(this.runtime.evaluate(/* expression */ pageReadyPromise, /* objectGroup */ null,
            /* includeCommandLineAPI */ false, /* silent */ false, /* contextId */ null, /* returnByValue */ true,
            /* generatePreview */ false, /* userGesture */ false, /* awaitPromise */ true,
            /* throwOnSideEffect */ false, /* timeout */ 10000.0,
            /* disableBreaks */ true, /* replMode */ false, /* allowUnsafeEvalBlockedByCSP */ false,
            /* uniqueContextId */ null)).thenReturn(evaluate);

        RemoteObject result = mock(RemoteObject.class);
        when(evaluate.getResult()).thenReturn(result);
        when(result.getValue()).thenReturn("Page ready.");

        assertTrue(this.chromeTab.navigate(url, (Cookie[]) null, true, 25));

        verify(this.runtime).enable();
    }

    @Test
    void navigateWithWaitAndException() throws Exception
    {
        URL url = new URL("http://www.xwiki.org");

        Navigate navigate = mock(Navigate.class);
        when(this.page.navigate(url.toString())).thenReturn(navigate);

        Evaluate evaluate = mock(Evaluate.class);
        String pageReadyPromise = ChromeTab.PAGE_READY_PROMISE.replace("__pageReadyTimeout__", "60000");
        when(this.runtime.evaluate(/* expression */ pageReadyPromise, /* objectGroup */ null,
            /* includeCommandLineAPI */ false, /* silent */ false, /* contextId */ null, /* returnByValue */ true,
            /* generatePreview */ false, /* userGesture */ false, /* awaitPromise */ true,
            /* throwOnSideEffect */ false, /* timeout */ 10000.0,
            /* disableBreaks */ true, /* replMode */ false, /* allowUnsafeEvalBlockedByCSP */ false,
            /* uniqueContextId */ null)).thenReturn(evaluate);

        ExceptionDetails exceptionDetails = mock(ExceptionDetails.class);
        when(evaluate.getExceptionDetails()).thenReturn(exceptionDetails);

        RemoteObject exception = mock(RemoteObject.class);
        when(exceptionDetails.getException()).thenReturn(exception);
        when(exception.getValue()).thenReturn("'xwiki-page-ready' module not found");

        try {
            this.chromeTab.navigate(url, (Cookie[]) null, true);
            fail("Navigation should have thrown an exception.");
        } catch (IOException e) {
            assertEquals("Failed to wait for page to be ready. Root cause: 'xwiki-page-ready' module not found",
                e.getMessage());
        }
    }

    @Test
    void getSource()
    {
        FrameTree frameTree = mock(FrameTree.class);
        when(this.page.getFrameTree()).thenReturn(frameTree);

        Frame frame = mock(Frame.class);
        when(frameTree.getFrame()).thenReturn(frame);
        when(frame.getId()).thenReturn("frameId");
        when(frame.getUrl()).thenReturn("frameURL");

        ResourceContent resourceContent = mock(ResourceContent.class);
        when(this.page.getResourceContent("frameId", "frameURL")).thenReturn(resourceContent);
        when(resourceContent.getContent()).thenReturn("source");

        assertEquals("source", this.chromeTab.getSource());
    }

    @Test
    void printToPDF() throws Exception
    {
        PrintToPDF printToPDF = mock(PrintToPDF.class);
        when(this.tabDevToolsService.getPage().printToPDF(false, false, false, 1d, 8.27d, 11.7d, 0d, 0d, 0d, 0d, "",
            false, "", "", false, PrintToPDFTransferMode.RETURN_AS_STREAM)).thenReturn(printToPDF);
        when(printToPDF.getStream()).thenReturn("pdf-stream");

        IO io = mock(IO.class);
        when(this.tabDevToolsService.getIO()).thenReturn(io);

        Read read = mock(Read.class);
        when(io.read("pdf-stream", null, 1 << 20)).thenReturn(read);
        when(read.getEof()).thenReturn(true);
        when(read.getData()).thenReturn("pdf-content");

        Runnable cleanup = mock(Runnable.class);
        try (InputStream inputStream = this.chromeTab.printToPDF(cleanup)) {
            assertEquals("pdf-content", IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        }

        verify(io).close("pdf-stream");
        verify(cleanup).run();
    }
}
