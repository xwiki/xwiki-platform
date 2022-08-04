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
import java.util.Collections;
import java.util.List;

import javax.inject.Provider;
import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.github.kklisura.cdt.protocol.types.network.CookieParam;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChromePDFPrinter}.
 * 
 * @version $Id$
 */
@ComponentTest
class ChromePDFPrinterTest
{
    @InjectMockComponents
    private ChromePDFPrinter chromePDFPrinter;

    @MockComponent
    private PDFExportConfiguration configuration;

    @MockComponent
    private ChromeManager chromeManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    ChromeDevToolsService tabDevToolsService;

    @BeforeComponent
    void configure()
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(xcontext);

        XWikiRequest request = mock(XWikiRequest.class);
        when(xcontext.getRequest()).thenReturn(request);
        when(request.getContextPath()).thenReturn("/xwiki");

        when(this.configuration.getXWikiHost()).thenReturn("xwiki-host");
    }

    @Test
    void printWithoutPreviewURL()
    {
        try {
            this.chromePDFPrinter.print(null);
            fail();
        } catch (IOException e) {
            assertEquals("Print preview URL missing.", e.getMessage());
        }
    }

    @Test
    void print() throws Exception
    {
        URL printPreviewURL = new URL("http://external:9293/xwiki/bin/export/Some/Page?x=y#z");
        URL dockerPrintPreviewURL = new URL("http://xwiki-host:9293/xwiki/bin/export/Some/Page?x=y#z");

        Cookie[] cookies = new Cookie[] {};
        when(this.xcontextProvider.get().getRequest().getCookies()).thenReturn(cookies);

        CookieParam cookieParam = new CookieParam();
        List<CookieParam> cookieParams = Collections.singletonList(cookieParam);
        when(this.chromeManager.toCookieParams(cookies)).thenReturn(cookieParams);

        when(this.chromeManager.createIncognitoTab()).thenReturn(this.tabDevToolsService);
        when(this.chromeManager.navigate(this.tabDevToolsService, new URL("http://xwiki-host:9293/xwiki/rest"), false))
            .thenReturn(true);
        when(this.chromeManager.navigate(this.tabDevToolsService, dockerPrintPreviewURL, true)).thenReturn(true);

        InputStream pdfInputStream = mock(InputStream.class);
        when(this.chromeManager.printToPDF(same(this.tabDevToolsService), any(Runnable.class)))
            .then(new Answer<InputStream>()
            {
                @Override
                public InputStream answer(InvocationOnMock invocation) throws Throwable
                {
                    try {
                        return pdfInputStream;
                    } finally {
                        invocation.getArgument(1, Runnable.class).run();
                    }
                }
            });

        assertSame(pdfInputStream, this.chromePDFPrinter.print(printPreviewURL));

        verify(this.chromeManager).setCookies(this.tabDevToolsService, cookieParams);
        assertEquals(dockerPrintPreviewURL.toString(), cookieParam.getUrl());

        verify(this.chromeManager).setBaseURL(this.tabDevToolsService, printPreviewURL);
        verify(this.chromeManager).closeIncognitoTab(this.tabDevToolsService);
    }
}
