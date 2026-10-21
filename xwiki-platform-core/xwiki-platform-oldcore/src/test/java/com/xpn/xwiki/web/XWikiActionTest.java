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
package com.xpn.xwiki.web;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;

import jakarta.inject.Named;

import org.apache.velocity.VelocityContext;
import org.eclipse.jetty.io.EofException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.resource.NotFoundResourceHandlerException;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiAction}.
 *
 * @version $Id$
 */
@OldcoreTest
class XWikiActionTest
{
    /**
     * An action failing with the configured exception.
     */
    public static class FailingAction extends XWikiAction
    {
        private XWikiException exception;

        @Override
        public boolean action(XWikiContext context) throws XWikiException
        {
            throw this.exception;
        }
    }

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private FailingAction action;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG, XWikiAction.class.getName());

    @MockComponent
    private ResourceTypeResolver<ExtendedURL> resourceTypeResolver;

    @MockComponent
    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    @MockComponent
    private ResourceReferenceManager resourceReferenceManager;

    @MockComponent
    @Named("bin")
    private ResourceReferenceHandler<ResourceType> entityResourceReferenceHandler;

    @MockComponent
    private AsyncContext asyncContext;

    @MockComponent
    private JobProgressManager progress;

    @MockComponent
    private VelocityManager velocityManager;

    @Mock
    private XWikiRequest request;

    @Mock
    private XWikiResponse response;

    @Mock
    private XWikiEngineContext engineContext;

    @Mock
    private ServletOutputStream out;

    @BeforeEach
    void beforeEach() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        context.setRequest(this.request);
        context.setResponse(this.response);
        context.setEngineContext(this.engineContext);
        context.setAction("download");
        context.setURL(new URL("http://localhost:8080/xwiki/bin/download/Space/Page/missing.png"));
        context.setDoc(new XWikiDocument(new DocumentReference("xwiki", "Space", "Page")));

        when(this.velocityManager.getVelocityContext()).thenReturn(new VelocityContext());
        when(this.engineContext.getAttribute("xwiki")).thenReturn(this.oldcore.getSpyXWiki());
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(this.request.getHttpServletRequest()).thenReturn(httpServletRequest);
        when(this.response.getOutputStream()).thenReturn(this.out);

        XWikiURLFactoryService urlFactoryService = mock(XWikiURLFactoryService.class);
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(urlFactoryService.createURLFactory(any(Integer.class), any())).thenReturn(urlFactory);
        doReturn(urlFactoryService).when(this.oldcore.getSpyXWiki()).getURLFactoryService();
        doReturn(true).when(this.oldcore.getSpyXWiki()).prepareDocuments(any(), any(), any());
        XWikiPluginManager pluginManager = new XWikiPluginManager();
        pluginManager.initInterface();
        doReturn(pluginManager).when(this.oldcore.getSpyXWiki()).getPluginManager();
        doReturn("The attachment doesn't exist.").when(this.oldcore.getSpyXWiki()).evaluateTemplate(anyString(),
            any());

        EntityResourceReference entityResourceReference = mock(EntityResourceReference.class);
        when(entityResourceReference.getAction()).thenReturn(new EntityResourceAction("download"));
        when(this.resourceReferenceManager.getResourceReference()).thenReturn(entityResourceReference);
        doThrow(new NotFoundResourceHandlerException(entityResourceReference)).when(
            this.entityResourceReferenceHandler).handle(eq(entityResourceReference), any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Broken pipe", "Connection reset by peer" })
    void executeWhenTheClientClosesTheConnectionWhileSendingTheErrorPage(String rootCause) throws Exception
    {
        // Like the download of a missing attachment.
        this.action.exception = new XWikiException(XWikiException.MODULE_XWIKI_APP,
            XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND, "Attachment not found");
        doThrow(new IOException(rootCause)).when(this.out).write(any(byte[].class));

        this.action.execute(this.oldcore.getXWikiContext());

        // The client closed the connection, it's not an error of the server.
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.DEBUG, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Connection aborted", this.logCapture.getMessage(0));
        assertEquals(rootCause, this.logCapture.getLogEvent(0).getThrowableProxy().getCause().getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "Broken pipe", "Connection reset by peer" })
    void executeWhenTheActionFailsBecauseTheClientClosedTheConnection(String rootCause) throws Exception
    {
        this.action.exception = new XWikiException(XWikiException.MODULE_XWIKI_APP,
            XWikiException.ERROR_XWIKI_UNKNOWN, "Failed to send the attachment", new IOException(rootCause));

        this.action.execute(this.oldcore.getXWikiContext());

        // Nothing is logged and no error page is sent since the client closed the connection.
        assertEquals(0, this.logCapture.size());
        verify(this.out, never()).write(any(byte[].class));
    }

    @Test
    void executeWhenTheActionFailsBecauseTheClientClosedTheConnectionOnJetty() throws Exception
    {
        // The message of the root cause depends on the operating system, so it's not used to detect the client abort.
        this.action.exception = new XWikiException(XWikiException.MODULE_XWIKI_APP,
            XWikiException.ERROR_XWIKI_UNKNOWN, "Failed to send the attachment",
            new EofException(new IOException("An established connection was aborted by the software in your host")));

        this.action.execute(this.oldcore.getXWikiContext());

        // Nothing is logged and no error page is sent since the client closed the connection.
        assertEquals(0, this.logCapture.size());
        verify(this.out, never()).write(any(byte[].class));
    }

    @Test
    void executeWhenTheActionFails() throws Exception
    {
        this.action.exception = new XWikiException(XWikiException.MODULE_XWIKI_APP,
            XWikiException.ERROR_XWIKI_UNKNOWN, "Failed to store the attachment",
            new IOException("No space left on device"));

        this.action.execute(this.oldcore.getXWikiContext());

        // A failure of the server is still logged, and the error page is sent.
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Uncaught exception", this.logCapture.getMessage(0));
        verify(this.out).write(any(byte[].class));
    }
}
