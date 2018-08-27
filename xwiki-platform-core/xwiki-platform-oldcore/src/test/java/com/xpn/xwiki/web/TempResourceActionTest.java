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

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.tika.internal.TikaUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.file.TemporaryFile;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.includeservletasstring.BufferOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TempResourceAction}.
 * 
 * @version $Id$
 */
@OldcoreTest
public class TempResourceActionTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    /**
     * The action being tested.
     */
    private TempResourceAction action;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        this.oldcore.registerMockEnvironment();
        this.action = new TempResourceAction();
    }

    /**
     * Creates an empty file at the specified path.
     * 
     * @param path the file path
     * @throws IOException if creating the empty file fails
     */
    private void createEmptyFile(String path) throws IOException
    {
        File emptyFile = new File(this.oldcore.getTemporaryDirectory(), path);
        emptyFile.getParentFile().mkdirs();
        emptyFile.createNewFile();
        emptyFile.deleteOnExit();
    }

    /**
     * Creates a file at the specified path, with the specified content.
     *
     * @param path the file path
     * @throws IOException if creating the empty file fails
     */
    private void createFile(String path, String content) throws IOException
    {
        File file = new File(this.oldcore.getTemporaryDirectory(), path);
        file.getParentFile().mkdirs();
        file.deleteOnExit();
        FileUtils.write(file, content);
    }

    /**
     * {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} should return {@code null} if the given URI
     * doesn't match the known pattern.
     */
    @Test
    public void testGetTemporaryFileForBadURI() throws Exception
    {
        createEmptyFile("temp/secret.txt");
        assertNull(action.getTemporaryFile("/xwiki/bin/temp/secret.txt", oldcore.getXWikiContext()));
    }

    /**
     * {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} should prevent access to files outside the
     * temporary directory by ignoring relative URIs (i.e. which use ".." to move to the parent folder).
     */
    @Test
    public void testGetTemporaryFileForRelativeURI() throws Exception
    {
        createEmptyFile("temp/secret.txt");
        assertNull(action.getTemporaryFile("/xwiki/bin/temp/../../module/secret.txt", oldcore.getXWikiContext()));
    }

    /**
     * Tests {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} when the file is missing.
     */
    @Test
    public void testGetTemporaryFileMissing() throws Exception
    {
        assertFalse(new File(this.oldcore.getTemporaryDirectory(), "temp/module/xwiki/Space/Page/file.txt").exists());
        assertNull(action.getTemporaryFile("/xwiki/bin/temp/Space/Page/module/file.txt", oldcore.getXWikiContext()));
    }

    /**
     * Tests {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} when the file is present.
     */
    @Test
    public void testGetTemporaryFile() throws Exception
    {
        oldcore.getXWikiContext().setWikiId("wiki");
        createEmptyFile("temp/module/wiki/Space/Page/file.txt");
        assertNotNull(action.getTemporaryFile("/xwiki/bin/temp/Space/Page/module/file.txt", oldcore.getXWikiContext()));
    }

    /**
     * Tests {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} when the URL is over encoded.
     */
    @Test
    public void testGetTemporaryFileForOverEncodedURL() throws Exception
    {
        createEmptyFile("temp/officeviewer/xwiki/Sp*ace/Pa-ge/presentation.odp/presentation-slide0.jpg");
        assertNotNull(action.getTemporaryFile(
            "/xwiki/bin/temp/Sp%2Aace/Pa%2Dge/officeviewer/presentation.odp/presentation-slide0.jpg",
            oldcore.getXWikiContext()));
    }

    /**
     * Tests {@link TempResourceAction#getTemporaryFile(String, XWikiContext)} when the URL is partially decoded. This
     * can happen for instance when XWiki is behind Apache's {@code mode_proxy} with {@code nocanon} option disabled.
     */
    @Test
    public void testGetTemporaryFileForPartiallyDecodedURL() throws Exception
    {
        createEmptyFile("temp/officeviewer/xwiki/Space/Page/"
            + "attach%3Axwiki%3ASpace.Page%40pres%2Fentation.odp/13/presentation-slide0.jpg");
        assertNotNull(action.getTemporaryFile("/xwiki/bin/temp/Space/Page/officeviewer/"
            + "attach:xwiki:Space.Page@pres%2Fentation.odp/13/presentation-slide0.jpg", oldcore.getXWikiContext()));
    }

    @Test
    public void renderNormalBehavior() throws Exception
    {
        XWikiRequest request = mock(XWikiRequest.class);
        XWikiResponse response = mock(XWikiResponse.class);
        BufferOutputStream out = new BufferOutputStream();
        oldcore.getXWikiContext().setRequest(request);
        oldcore.getXWikiContext().setResponse(response);
        when(request.getRequestURI()).thenReturn("/xwiki/bin/temp/Space/Page/module/file.txt");
        when(response.getOutputStream()).thenReturn(out);
        oldcore.getXWikiContext().setWikiId("wiki");
        createFile("temp/module/wiki/Space/Page/file.txt", "Hello World!");
        action.render(oldcore.getXWikiContext());
        assertArrayEquals("Hello World!".getBytes(), out.getContentsAsByteArray());
        verify(response, never()).addHeader("Content-disposition", "attachment; filename*=utf-8''file.txt");
    }

    @Test
    public void renderWithForceDownload() throws Exception
    {
        XWikiRequest request = mock(XWikiRequest.class);
        XWikiResponse response = mock(XWikiResponse.class);
        oldcore.getXWikiContext().setRequest(request);
        oldcore.getXWikiContext().setResponse(response);
        when(request.getRequestURI()).thenReturn("/xwiki/bin/temp/Space/Page/module/file.txt");
        when(request.getParameter("force-download")).thenReturn("1");
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
        oldcore.getXWikiContext().setWikiId("wiki");
        createEmptyFile("temp/module/wiki/Space/Page/file.txt");
        action.render(oldcore.getXWikiContext());
        verify(response).addHeader("Content-disposition", "attachment; filename*=utf-8''file.txt");
    }

    public void renderWithInvalidPathThrowsException() throws XWikiException
    {
        XWikiRequest request = mock(XWikiRequest.class);
        XWikiResponse response = mock(XWikiResponse.class);
        oldcore.getXWikiContext().setRequest(request);
        oldcore.getXWikiContext().setResponse(response);
        when(request.getRequestURI()).thenReturn("/xwiki/bin/temp/Space/Page/module/nosuchfile.txt");
        oldcore.getXWikiContext().setWikiId("wiki");
        assertThrows(XWikiException.class, () -> action.render(oldcore.getXWikiContext()));
    }
}
