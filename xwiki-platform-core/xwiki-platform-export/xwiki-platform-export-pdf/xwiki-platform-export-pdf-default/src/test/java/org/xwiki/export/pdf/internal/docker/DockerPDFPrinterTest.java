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
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.internal.chrome.ChromeManager;
import org.xwiki.export.pdf.internal.job.PDFExportContextStore;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.github.kklisura.cdt.protocol.types.network.CookieParam;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.xpn.xwiki.internal.context.XWikiContextContextStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DockerPDFPrinter}.
 * 
 * @version $Id$
 */
@ComponentTest
class DockerPDFPrinterTest
{
    @InjectMockComponents
    private DockerPDFPrinter dockerPDFPrinter;

    @MockComponent
    private PDFExportConfiguration configuration;

    @MockComponent
    private ChromeManager chromeManager;

    @MockComponent
    private ContainerManager containerManager;

    @Mock
    ChromeDevToolsService tabDevToolsService;

    private PDFExportJobRequest request = new PDFExportJobRequest();

    private String containerId = "8f55a905efec";

    @BeforeComponent
    void configure()
    {
        this.request.setContext(new HashMap<>());

        when(this.configuration.getChromeDockerContainerName()).thenReturn("test-pdf-printer");
        when(this.configuration.getChromeDockerImage()).thenReturn("test/chrome:latest");
        when(this.configuration.getChromeDockerHostName()).thenReturn("docker");
        when(this.configuration.getChromeRemoteDebuggingPort()).thenReturn(1234);
    }

    @Test
    void printWithoutPreviewURL()
    {
        try {
            this.dockerPDFPrinter.print(this.request);
            fail();
        } catch (IOException e) {
            assertEquals("Print preview URL missing.", e.getMessage());
        }
    }

    @Test
    void printWithUnsupportedProtocol() throws Exception
    {
        this.request.getContext().put(XWikiContextContextStore.PROP_REQUEST_URL, new URL("file://some/file.txt"));

        try {
            this.dockerPDFPrinter.print(this.request);
            fail();
        } catch (IOException e) {
            assertEquals("Unsupported protocol [file].", e.getMessage());
        }
    }

    @Test
    void print() throws Exception
    {
        URL printPreviewURL = new URL("http://localhost:8080/xwiki/bin/export/Some/Page");
        URL dockerPrintPreviewURL = new URL("http://docker:8080/xwiki/bin/export/Some/Page");

        this.request.getContext().put(XWikiContextContextStore.PROP_REQUEST_URL, printPreviewURL);
        Cookie[] cookies = new Cookie[] {};
        this.request.getContext().put(PDFExportContextStore.ENTRY_COOKIES, cookies);

        CookieParam cookieParam = new CookieParam();
        List<CookieParam> cookieParams = Collections.singletonList(cookieParam);
        when(this.chromeManager.toCookieParams(cookies)).thenReturn(cookieParams);

        when(this.chromeManager.createIncognitoTab()).thenReturn(this.tabDevToolsService);
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

        assertSame(pdfInputStream, this.dockerPDFPrinter.print(this.request));

        verify(this.chromeManager).setCookies(this.tabDevToolsService, cookieParams);
        assertEquals(dockerPrintPreviewURL.toString(), cookieParam.getUrl());

        verify(this.chromeManager).navigate(this.tabDevToolsService, dockerPrintPreviewURL);
        verify(this.chromeManager).setBaseURL(this.tabDevToolsService, printPreviewURL);
        verify(this.chromeManager).closeIncognitoTab(this.tabDevToolsService);
    }

    @BeforeComponent("initializeAndDispose")
    void beforeInitializeAndDispose()
    {
        when(this.containerManager.maybeReuseContainerByName(this.configuration.getChromeDockerContainerName()))
            .thenReturn(null);
        when(this.containerManager.isLocalImagePresent(this.configuration.getChromeDockerImage())).thenReturn(false);
        when(this.containerManager.createContainer(this.configuration.getChromeDockerImage(),
            this.configuration.getChromeDockerContainerName(), this.configuration.getChromeRemoteDebuggingPort(),
            Arrays.asList("--no-sandbox", "--remote-debugging-address=0.0.0.0",
                "--remote-debugging-port=" + this.configuration.getChromeRemoteDebuggingPort())))
                    .thenReturn(this.containerId);
    }

    @Test
    void initializeAndDispose() throws Exception
    {
        verify(this.containerManager).pullImage(this.configuration.getChromeDockerImage());
        verify(this.containerManager).startContainer(this.containerId);

        this.dockerPDFPrinter.dispose();
        verify(this.containerManager).stopContainer(this.containerId);
    }

    @BeforeComponent("initializeWithExistingContainer")
    void beforeInitializeWithExistingContainer()
    {
        when(this.containerManager.maybeReuseContainerByName(this.configuration.getChromeDockerContainerName()))
            .thenReturn(this.containerId);
    }

    @Test
    void initializeWithExistingContainer() throws Exception
    {
        verify(this.containerManager, never()).startContainer(any(String.class));
    }
}
