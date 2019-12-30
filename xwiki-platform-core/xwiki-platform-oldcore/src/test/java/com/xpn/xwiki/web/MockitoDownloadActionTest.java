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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DownloadAction} using Mockito. This class is supposed to replace the
 * {@link DownloadActionTest} test, after all the tests have been moved in this one.
 *
 * @version $Id$
 * @since 7.2M2
 */
public class MockitoDownloadActionTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private class StubServletOutputStream extends ServletOutputStream
    {
        public ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public void write(int i) throws IOException
        {
            baos.write(i);
        }

        @Override
        public boolean isReady()
        {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener)
        {
            // Nor needed
        }
    }

    @Test
    public void renderWhenAttachmentIsInANestedSpace() throws Exception
    {
        DownloadAction action = new DownloadAction();
        XWikiContext xcontext = this.oldcore.getXWikiContext();

        // Set the Request URL
        XWikiServletRequestStub request = new XWikiServletRequestStub();
        request.setRequestURI("http://localhost:8080/xwiki/bin/download/space1/space2/page/file.ext");
        xcontext.setRequest(request);

        // Setup the returned attachment
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(attachment.getContentLongSize(xcontext)).thenReturn(100L);
        Date now = new Date();
        when(attachment.getDate()).thenReturn(now);
        when(attachment.getFilename()).thenReturn("file.ext");
        when(attachment.getContentInputStream(xcontext)).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(attachment.getMimeType(xcontext)).thenReturn("mimetype");

        // Set the current doc
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getAttachment("file.ext")).thenReturn(attachment);
        xcontext.setDoc(document);

        // Set the Plugin Manager
        XWikiPluginManager pluginManager = mock(XWikiPluginManager.class);
        when(pluginManager.downloadAttachment(attachment, xcontext)).thenReturn(attachment);
        doReturn(pluginManager).when(this.oldcore.getSpyXWiki()).getPluginManager();

        // Set the Response
        XWikiResponse response = mock(XWikiResponse.class);
        StubServletOutputStream ssos = new StubServletOutputStream();
        when(response.getOutputStream()).thenReturn(ssos);
        xcontext.setResponse(response);

        // Set the Resource Reference Manager used to parse the URL and extract the attachment name
        ResourceReferenceManager rrm = this.oldcore.getMocker().registerMockComponent(ResourceReferenceManager.class);
        when(rrm.getResourceReference()).thenReturn(new EntityResourceReference(new AttachmentReference("file.ext",
            new DocumentReference("wiki", Arrays.asList("space1", "space2"), "page")), EntityResourceAction.VIEW));

        // Note: we don't give PR and the attachment is not an authorized mime type.

        assertNull(action.render(xcontext));

        // This is the test, we verify what is set in the response
        verify(response).setContentType("mimetype");
        verify(response).setHeader("Accept-Ranges", "bytes");
        verify(response).addHeader("Content-disposition", "attachment; filename*=utf-8''file.ext");
        verify(response).setDateHeader("Last-Modified", now.getTime());
        verify(response).setContentLengthLong(100);
        assertEquals("test", ssos.baos.toString());
    }

    @Test
    public void renderWhenZipExplorerPluginURL() throws Exception
    {
        DownloadAction action = new DownloadAction();
        XWikiContext xcontext = this.oldcore.getXWikiContext();

        // Set the Request URL
        XWikiServletRequestStub request = new XWikiServletRequestStub();
        request.setRequestURI("http://localhost:8080/xwiki/bin/download/space/page/file.ext/some/path");
        xcontext.setRequest(request);

        // Set the current doc and current wiki
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getAttachment("path")).thenReturn(null);
        xcontext.setDoc(document);
        xcontext.setWikiId("wiki");
        xcontext.setAction("download");

        // Set the Response
        XWikiResponse response = mock(XWikiResponse.class);
        StubServletOutputStream ssos = new StubServletOutputStream();
        when(response.getOutputStream()).thenReturn(ssos);
        xcontext.setResponse(response);

        // Set the Resource Reference Manager used to parse the URL and extract the attachment name
        ResourceReferenceManager rrm = this.oldcore.getMocker().registerMockComponent(ResourceReferenceManager.class);
        when(rrm.getResourceReference()).thenReturn(new EntityResourceReference(new AttachmentReference("path",
            new DocumentReference("wiki", Arrays.asList("space", "page", "file.ext"), "some")),
            EntityResourceAction.VIEW));

        // Setup the returned attachment
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(attachment.getContentLongSize(xcontext)).thenReturn(100L);
        Date now = new Date();
        when(attachment.getDate()).thenReturn(now);
        when(attachment.getFilename()).thenReturn("file.ext");
        when(attachment.getContentInputStream(xcontext)).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(attachment.getMimeType(xcontext)).thenReturn("mimetype");
        when(attachment.clone()).thenReturn(attachment);

        // Configure an existing doc in the store
        XWiki xwiki = this.oldcore.getSpyXWiki();
        XWikiDocument backwardCompatDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        backwardCompatDocument.addAttachment(attachment);
        xwiki.saveDocument(backwardCompatDocument, xcontext);

        // Make sure the user has permission to access the doc
        doReturn(true).when(xwiki).checkAccess(eq("download"), any(XWikiDocument.class), any(XWikiContext.class));

        // Setup ExecutionContextManager & VelocityManager using in the context backup
        ExecutionContextManager ecm = this.oldcore.getMocker().registerMockComponent(ExecutionContextManager.class);
        ExecutionContext ec = this.oldcore.getExecutionContext();
        when(ecm.clone(ec)).thenReturn(ec);
        VelocityManager vm = this.oldcore.getMocker().registerMockComponent(VelocityManager.class);

        // Set the Plugin Manager
        XWikiPluginManager pluginManager = mock(XWikiPluginManager.class);
        when(pluginManager.downloadAttachment(attachment, xcontext)).thenReturn(attachment);
        doReturn(pluginManager).when(this.oldcore.getSpyXWiki()).getPluginManager();

        assertNull(action.render(xcontext));

        // This is the test, we verify what is set in the response
        verify(response).setContentType("mimetype");
        verify(response).setHeader("Accept-Ranges", "bytes");
        verify(response).addHeader("Content-disposition", "attachment; filename*=utf-8''file.ext");
        verify(response).setDateHeader("Last-Modified", now.getTime());
        verify(response).setContentLengthLong(100);
        assertEquals("test", ssos.baos.toString());
    }
}
