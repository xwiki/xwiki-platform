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
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DownloadAction}.
 * 
 * @version $Id$
 */
@OldcoreTest
class DownloadActionTest
{
    /** The name of the attachment being downloaded in most of the tests. */
    private static final String DEFAULT_FILE_NAME = "file.txt";

    /** The URI requested in most of the tests. */
    private static final String DEFAULT_URI = "/xwiki/bin/download/space/page/file.txt";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    /** Mocked context document. */
    private XWikiDocument document;

    /** Mocked client request. */
    @Mock
    private XWikiRequest request;

    /** Mocked client response. */
    @Mock
    private XWikiResponse response;

    /** Mocked engine context. */
    @Mock
    private XWikiEngineContext engineContext;

    /** A mocked output stream where the output file data is being written. */
    @Mock
    private ServletOutputStream out;

    /** The action being tested. */
    @InjectMockComponents
    private DownloadAction action;

    /** The content of the file being downloaded in most of the tests. */
    private byte[] fileContent = "abcdefghijklmn".getBytes(XWiki.DEFAULT_ENCODING);

    @MockComponent
    private ResourceReferenceManager resourceReferenceManager;

    private DocumentReference documentReference;

    /**
     * Default constructor.
     *
     * @throws UnsupportedEncodingException if UTF-8 is not available, so never
     */
    public DownloadActionTest() throws UnsupportedEncodingException
    {
        // Empty, needed for declaring the exception thrown while initializing fileContent
    }

    @BeforeEach
    public void before() throws Exception
    {
        this.oldcore.getXWikiContext().setRequest(this.request);
        this.oldcore.getXWikiContext().setResponse(this.response);
        this.oldcore.getXWikiContext().setEngineContext(this.engineContext);

        XWikiPluginManager pluginManager = new XWikiPluginManager();
        pluginManager.initInterface();

        this.documentReference = new DocumentReference("wiki", "space", "page");
        this.document = new XWikiDocument(this.documentReference);
        this.oldcore.getXWikiContext().setDoc(this.document);

        doReturn(pluginManager).when(this.oldcore.getSpyXWiki()).getPluginManager();
        when(this.engineContext.getMimeType("file.txt")).thenReturn("text/plain");
        when(this.response.getOutputStream()).thenReturn(this.out);
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("programming"), any(), any(),
            any(XWikiContext.class))).thenReturn(false);
    }

    // Helpers for the tests
    private void createAttachment(Date d, String name) throws IOException
    {
        XWikiAttachment filetxt = new XWikiAttachment(this.document, name);
        filetxt.setContent(new ByteArrayInputStream(this.fileContent));
        filetxt.setDate(d);
        this.document.getAttachmentList().add(filetxt);
    }

    private void setRequestExpectations(String uri, String id, String forceDownload, String range, long modifiedSince,
        String attachmentName)
    {
        ResourceReference rr = new EntityResourceReference(
            new AttachmentReference(attachmentName, this.documentReference), EntityResourceAction.VIEW);

        when(this.request.getRequestURI()).thenReturn(uri);
        when(this.request.getParameter("id")).thenReturn(id);
        when(this.request.getDateHeader("If-Modified-Since")).thenReturn(modifiedSince);
        when(this.request.getParameter("force-download")).thenReturn(forceDownload);
        when(this.request.getHeader("Range")).thenReturn(range);
        when(this.resourceReferenceManager.getResourceReference()).thenReturn(rr);
    }

    private void verifyResponseExpectations(long modified, long length)
    {
        verifyResponseExpectations(modified, length, "text/plain", "inline; filename*=utf-8''file.txt");
    }

    private void verifyResponseExpectations(long modified, long length, String mime, String disposition)
    {
        verify(this.response).setContentType(mime);
        verify(this.response).setHeader("Accept-Ranges", "bytes");
        verify(this.response).addHeader("Content-disposition", disposition);
        verify(this.response).setDateHeader("Last-Modified", modified);
        if (length > -1) {
            verify(this.response).setContentLengthLong(length);
        } else {
            verify(this.response, times(0)).setContentLengthLong(length);
        }
    }

    private void verifyOutputExpectations(final int start, final int end) throws IOException
    {
        verify(this.out).write(argThat(new ArgumentMatcher<byte[]>()
        {
            @Override
            public boolean matches(byte[] argument)
            {
                for (int i = start; i < end; ++i) {
                    if (argument[i - start] != DownloadActionTest.this.fileContent[i]) {
                        return false;
                    }
                }
                return true;
            }
        }), eq(0), eq(end - start));
    }

    @Test
    void downloadNormal() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, null, -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length);
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadLongLength() throws XWikiException
    {
        XWikiAttachment filetxt = new XWikiAttachment(this.document, DEFAULT_FILE_NAME);
        XWikiAttachmentContent content = mock(XWikiAttachmentContent.class);
        when(content.getAttachment()).thenReturn(filetxt);
        when(content.getContentInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));
        when(content.getLongSize()).thenReturn(Long.MAX_VALUE);
        filetxt.setAttachment_content(content);
        filetxt.setLongSize(Long.MAX_VALUE);
        this.document.getAttachmentList().add(filetxt);

        setRequestExpectations(DEFAULT_URI, null, null, null, -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        // Make sure we set the right content length
        verify(this.response, times(1)).setContentLengthLong(Long.MAX_VALUE);
    }

    @Test
    void downloadWhenIfModifiedSinceBefore() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, null, d.getTime() - 1000l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length);
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenIfModifiedSinceSame() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, null, d.getTime(), DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));
    }

    @Test
    void downloadWhenIfModifiedSinceAfter() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, null, d.getTime() + 1000l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));
    }

    @Test
    void downloadWhenMissingFile() throws XWikiException
    {
        setRequestExpectations("/xwiki/bin/download/space/page/nofile.txt", null, null, null, -1l, DEFAULT_FILE_NAME);
        XWikiException xWikiException =
            assertThrows(XWikiException.class, () -> this.action.render(this.oldcore.getXWikiContext()));
        assertEquals("Error number 11003 in 11: Attachment [file.txt] not found", xWikiException.getMessage());
    }

    @Test
    void downloadWhenURLNotPointingToAttachment() throws XWikiException
    {
        ResourceReference rr = new EntityResourceReference(this.documentReference, EntityResourceAction.VIEW);
        when(this.resourceReferenceManager.getResourceReference()).thenReturn(rr);

        when(this.request.getRequestURI()).thenReturn("/xwiki/bin/download/space/page");

        XWikiException xWikiException =
            assertThrows(XWikiException.class, () -> this.action.render(this.oldcore.getXWikiContext()));
        assertEquals("Error number 11003 in 11: Attachment not found", xWikiException.getMessage());
    }

    @Test
    void downloadById() throws XWikiException, IOException
    {
        Date d = new Date();
        XWikiAttachment att;
        for (byte i = 0; i < 10; ++i) {
            att = new XWikiAttachment(this.document, "file." + i + ".txt");
            byte[] content = new byte[1];
            content[0] = (byte) ('0' + i);
            att.setContent(new ByteArrayInputStream(content));
            att.setDate(d);
            this.document.getAttachmentList().add(att);
        }

        when(this.engineContext.getMimeType("file.5.txt")).thenReturn("text/plain");

        setRequestExpectations("/xwiki/bin/download/space/page/file.2.txt", "5", null, null, -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), 1, "text/plain", "inline; filename*=utf-8''file.5.txt");
        verify(this.out).write(argThat(new ArgumentMatcher<byte[]>()
        {
            @Override
            public boolean matches(byte[] argument)
            {
                return argument[0] == '5';
            }
        }), eq(0), eq(1));
        verify(this.out).write(argThat(new ArgumentMatcher<byte[]>()
        {
            @Override
            public boolean matches(byte[] argument)
            {
                return argument[0] == '5';
            }
        }), eq(0), eq(1));
    }

    @Test
    void testDownloadByWrongId() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, "42", null, null, -1l, DEFAULT_FILE_NAME);

        XWikiException xWikiException =
            assertThrows(XWikiException.class, () -> this.action.render(this.oldcore.getXWikiContext()));
        assertEquals("Error number 11003 in 11: Attachment [file.txt] not found", xWikiException.getMessage());
    }

    @Test
    void downloadWhenInvalidId() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, "two", null, null, -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length);
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenIncompletePath() throws XWikiException
    {
        setRequestExpectations("/xwiki/bin/download/", null, null, null, -1l, DEFAULT_FILE_NAME);

        XWikiException xWikiException =
            assertThrows(XWikiException.class, () -> this.action.render(this.oldcore.getXWikiContext()));
        assertEquals("Error number 11003 in 11: Attachment [file.txt] not found", xWikiException.getMessage());
    }

    @Test
    void downloadWhenDifferentMimeType() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, "file.png");
        when(this.engineContext.getMimeType("file.png")).thenReturn("image/png");
        setRequestExpectations("/xwiki/bin/download/space/page/file.png", null, null, null, -1l, "file.png");

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length, "image/png",
            "inline; filename*=utf-8''file.png");
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenForce() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, "1", null, -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length, "text/plain",
            "attachment; filename*=utf-8''file.txt");
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenMimeTypeBlacklisted() throws Exception
    {
        Date d = new Date();
        createAttachment(d, "file.png");
        when(this.engineContext.getMimeType("file.png")).thenReturn("image/png");
        setRequestExpectations("/xwiki/bin/download/space/page/file.png", null, null, null, -1l, "file.png");
        this.oldcore.getConfigurationSource().setProperty(DownloadAction.BLACKLIST_PROPERTY,
            Arrays.asList("application/x-bzip", "image/png"));

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length, "image/png",
            "attachment; filename*=utf-8''file.png");
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenMimeTypeBlacklistedAndAttachmentAddedByPRUser() throws Exception
    {
        Date d = new Date();
        createAttachment(d, "file.png");
        when(this.engineContext.getMimeType("file.png")).thenReturn("image/png");
        setRequestExpectations("/xwiki/bin/download/space/page/file.png", null, null, null, -1l, "file.png");
        this.oldcore.getConfigurationSource().setProperty(DownloadAction.BLACKLIST_PROPERTY,
            Arrays.asList("application/x-bzip", "image/png"));
        // Consider PR rights
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("programming"), any(), any(),
            any(XWikiContext.class))).thenReturn(true);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length, "image/png",
            "inline; filename*=utf-8''file.png");
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenMimeTypeForcedDownloadAndAttachmentAddedByPRUser() throws Exception
    {
        Date d = new Date();
        createAttachment(d, "file.png");
        when(this.engineContext.getMimeType("file.png")).thenReturn("image/png");
        setRequestExpectations("/xwiki/bin/download/space/page/file.png", null, null, null, -1l, "file.png");
        this.oldcore.getConfigurationSource().setProperty(DownloadAction.FORCE_DOWNLOAD_PROPERTY,
            Arrays.asList("application/x-bzip", "image/png"));
        // Consider PR rights
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("programming"), any(), any(),
            any(XWikiContext.class))).thenReturn(true);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length, "image/png",
            "attachment; filename*=utf-8''file.png");
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenMimeTypeNotWhitelisted() throws Exception
    {
        Date d = new Date();
        createAttachment(d, "file.png");
        when(this.engineContext.getMimeType("file.png")).thenReturn("image/png");
        setRequestExpectations("/xwiki/bin/download/space/page/file.png", null, null, null, -1l, "file.png");
        this.oldcore.getConfigurationSource().setProperty(DownloadAction.WHITELIST_PROPERTY,
            Collections.singletonList("application/x-bzip"));

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length, "image/png",
            "attachment; filename*=utf-8''file.png");
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenMimeTypeNotWhitelistedButAddedByPRUser() throws Exception
    {
        Date d = new Date();
        createAttachment(d, "file.png");
        when(this.engineContext.getMimeType("file.png")).thenReturn("image/png");
        setRequestExpectations("/xwiki/bin/download/space/page/file.png", null, null, null, -1l, "file.png");
        this.oldcore.getConfigurationSource().setProperty(DownloadAction.WHITELIST_PROPERTY,
            Collections.singletonList("application/x-bzip"));
        // Consider PR rights
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("programming"), any(), any(),
            any(XWikiContext.class))).thenReturn(true);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length, "image/png",
            "inline; filename*=utf-8''file.png");
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWithRealDate() throws XWikiException, IOException
    {
        Date d = new Date(411757300000l);
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, null, -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length);
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenNameWithSpacesEncodedWithPlus() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, "file name.txt");
        when(this.engineContext.getMimeType("file name.txt")).thenReturn("text/plain");
        setRequestExpectations("/xwiki/bin/download/space/page/file+name.txt", null, null, null, -1l, "file name.txt");

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length, "text/plain",
            "inline; filename*=utf-8''file%20name.txt");
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenNameWithSpacesEncodedWithPercent() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, "file name.txt");
        when(this.engineContext.getMimeType("file name.txt")).thenReturn("text/plain");
        setRequestExpectations("/xwiki/bin/download/space/page/file%20name.txt", null, "1", null, -1l, "file name.txt");

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length, "text/plain",
            "attachment; filename*=utf-8''file%20name.txt");
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenNameWithNonAsciiChars() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, "file\u021B.txt");

        when(this.engineContext.getMimeType("file\u021B.txt")).thenReturn("text/plain");
        setRequestExpectations("/xwiki/bin/download/space/page/file%C8%9B.txt", null, "1", null, -1l, "file\u021B.txt");

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length, "text/plain",
            "attachment; filename*=utf-8''file%C8%9B.txt");
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenValidStartRange() throws XWikiException, IOException
    {
        // This test expects bytes 0, 1, 2 and 3 from the file.
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=0-3", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        verify(this.response).setHeader("Content-Range", "bytes 0-3/" + DownloadActionTest.this.fileContent.length);
        verifyResponseExpectations(d.getTime(), 4);
        verifyOutputExpectations(0, 4);
    }

    @Test
    void downloadWhenValidMiddleRange() throws XWikiException, IOException
    {
        // This test expects bytes 3, 4 and 5 from the file.
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=3-5", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        verify(this.response).setHeader("Content-Range", "bytes 3-5/" + DownloadActionTest.this.fileContent.length);
        verifyResponseExpectations(d.getTime(), 3);
        verifyOutputExpectations(3, 6);
    }

    @Test
    void downloadWhenValidEndRange() throws XWikiException, IOException
    {
        // This test expects bytes 9, 10, 11, 12 and 13 from the file.
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=9-13", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        verify(this.response).setHeader("Content-Range", "bytes 9-13/" + DownloadActionTest.this.fileContent.length);
        verifyResponseExpectations(d.getTime(), this.fileContent.length - 9);
        verifyOutputExpectations(9, this.fileContent.length);
    }

    @Test
    void downloadWhenOneByteRange() throws XWikiException, IOException
    {
        // This test expects the last four bytes (10, 11, 12 and 13) from the file
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=0-0", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        verify(this.response).setHeader("Content-Range", "bytes 0-0/" + DownloadActionTest.this.fileContent.length);
        verifyResponseExpectations(d.getTime(), 1);
        verifyOutputExpectations(0, 1);
    }

    @Test
    void downloadWhenRestRange() throws XWikiException, IOException
    {
        // This test expects bytes from 11 to the end of the file (11, 12 and 13)
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=11-", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        verify(this.response).setHeader("Content-Range", "bytes 11-13/" + DownloadActionTest.this.fileContent.length);
        verifyResponseExpectations(d.getTime(), this.fileContent.length - 11);
        verifyOutputExpectations(11, this.fileContent.length);
    }

    @Test
    void downloadWhenFullRestRange() throws XWikiException, IOException
    {
        // This test expects the whole file
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=0-", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        verify(this.response).setHeader("Content-Range", "bytes 0-13/" + DownloadActionTest.this.fileContent.length);
        verifyResponseExpectations(d.getTime(), this.fileContent.length);
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenTailRange() throws XWikiException, IOException
    {
        // This test expects the last four bytes (10, 11, 12 and 13) from the file
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=-4", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        verify(this.response).setHeader("Content-Range", "bytes 10-13/" + DownloadActionTest.this.fileContent.length);
        verifyResponseExpectations(d.getTime(), this.fileContent.length - 10);
        verifyOutputExpectations(10, this.fileContent.length);
    }

    @Test
    void downloadWhenFullTailRange() throws XWikiException, IOException
    {
        // This test expects the whole file
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=-14", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        verify(this.response).setHeader("Content-Range", "bytes 0-13/" + DownloadActionTest.this.fileContent.length);
        verifyResponseExpectations(d.getTime(), this.fileContent.length);
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenOverflowingTailRange() throws XWikiException, IOException
    {
        // This test expects the whole file, although the client requested more
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=-40", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        verify(this.response).setHeader("Content-Range", "bytes 0-13/" + DownloadActionTest.this.fileContent.length);
        verifyResponseExpectations(d.getTime(), this.fileContent.length);
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenValidOverflowingRange() throws XWikiException, IOException
    {
        // This test expects bytes 9, 10, 11, 12 and 13 from the file, although 14 and 15 are requested as well.
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=9-15", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        verify(this.response).setHeader("Content-Range", "bytes 9-13/" + DownloadActionTest.this.fileContent.length);
        verifyResponseExpectations(d.getTime(), this.fileContent.length - 9);
        verifyOutputExpectations(9, this.fileContent.length);
    }

    @Test
    void downloadWhenInvalidSwappedRange() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=9-5", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length);
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenInvalidRange() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=all", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyResponseExpectations(d.getTime(), this.fileContent.length);
        verifyOutputExpectations(0, this.fileContent.length);
    }

    @Test
    void downloadWhenOutsideRange() throws XWikiException, IOException
    {
        // This test expects a 416 response
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=129-145", -1l, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    @Test
    void downloadWhenOutsideRestRange() throws XWikiException, IOException
    {
        // This test expects a 416 response
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=129-", -1L, DEFAULT_FILE_NAME);

        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verify(this.response).setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
    }

    @Test
    void downloadWhenEmptyRange() throws XWikiException, IOException
    {
        Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=-", -1L, DEFAULT_FILE_NAME);
        assertNull(this.action.render(this.oldcore.getXWikiContext()));

        verifyOutputExpectations(0, this.fileContent.length);
        verifyResponseExpectations(d.getTime(), this.fileContent.length);
    }

    private static final class StubServletOutputStream extends ServletOutputStream
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
    void renderWhenAttachmentIsInANestedSpace() throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
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
        StubServletOutputStream ssos = new StubServletOutputStream();
        when(response.getOutputStream()).thenReturn(ssos);

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
    void renderWhenZipExplorerPluginURL() throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();

        when(request.getRequestURI())
            .thenReturn("http://localhost:8080/xwiki/bin/download/space/page/file.ext/some/path");

        // Set the current doc and current wiki
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getAttachment("path")).thenReturn(null);
        xcontext.setDoc(document);
        xcontext.setWikiId("wiki");
        xcontext.setAction("download");

        // Set the Response
        StubServletOutputStream ssos = new StubServletOutputStream();
        when(response.getOutputStream()).thenReturn(ssos);

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
