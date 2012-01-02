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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Validate {@link DownloadAction}.
 * 
 * @version $Id$
 */
public class DownloadActionTest extends AbstractBridgedComponentTestCase
{
    /** Mocked global XWiki object. */
    private XWiki xwiki;

    /** Mocked context document. */
    private XWikiDocument document;

    /** Mocked client request. */
    private XWikiRequest request;

    /** Mocked client response. */
    private XWikiResponse response;

    /** Mocked engine context. */
    private XWikiEngineContext ec;

    /** A mocked output stream where the output file data is being written. */
    private ServletOutputStream out;

    /** The action being tested. */
    private DownloadAction action = new DownloadAction();

    /** The content of the file being downloaded in most of the tests. */
    private byte[] fileContent = "abcdefghijklmn".getBytes(XWiki.DEFAULT_ENCODING);

    /** The name of the attachment being downloaded in most of the tests. */
    private static final String DEFAULT_FILE_NAME = "file.txt";

    /** The URI requested in most of the tests. */
    private static final String DEFAULT_URI = "/xwiki/bin/download/space/page/file.txt";

    /**
     * Default constructor.
     * 
     * @throws UnsupportedEncodingException if UTF-8 is not available, so never
     */
    public DownloadActionTest() throws UnsupportedEncodingException
    {
        // Empty, needed for declaring the exception thrown while initializing fileContent
    }

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.xwiki = getMockery().mock(XWiki.class);
        getContext().setWiki(this.xwiki);
        this.request = getMockery().mock(XWikiRequest.class);
        getContext().setRequest(this.request);
        this.response = getMockery().mock(XWikiResponse.class);
        getContext().setResponse(this.response);
        this.ec = getMockery().mock(XWikiEngineContext.class);
        getContext().setEngineContext(this.ec);
        this.out = getMockery().mock(ServletOutputStream.class);

        final XWikiPluginManager pluginManager = new XWikiPluginManager();
        pluginManager.initInterface();

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        getContext().setDoc(this.document);
        getMockery().checking(new Expectations()
        {
            {
                allowing(DownloadActionTest.this.xwiki).getPluginManager();
                will(returnValue(pluginManager));
                allowing(DownloadActionTest.this.ec).getMimeType(with("file.txt"));
                will(returnValue("text/plain"));
                allowing(DownloadActionTest.this.response).setCharacterEncoding(with(""));
                allowing(DownloadActionTest.this.response).getOutputStream();
                will(returnValue(DownloadActionTest.this.out));
            }
        });
    }

    @Test
    public void testNormalDownload() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, null, -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length);
        setOutputExpectations(0, this.fileContent.length);
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testIfModifiedSinceBefore() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, null, d.getTime() - 1000l);
        setResponseExpectations(d.getTime(), this.fileContent.length);
        setOutputExpectations(0, this.fileContent.length);
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testIfModifiedSinceSame() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, null, d.getTime());
        getMockery().checking(new Expectations()
        {
            {
                allowing(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_NOT_MODIFIED));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testIfModifiedSinceAfter() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, null, d.getTime() + 1000l);
        getMockery().checking(new Expectations()
        {
            {
                allowing(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_NOT_MODIFIED));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test(expected = XWikiException.class)
    public void testDownloadMissingFile() throws XWikiException, IOException
    {
        setRequestExpectations("/xwiki/bin/download/space/page/nofile.txt", null, null, null, -1l);
        this.action.render(getContext());
    }

    @Test
    public void testDownloadById() throws XWikiException, IOException
    {
        final Date d = new Date();
        XWikiAttachment att;
        for (byte i = 0; i < 10; ++i) {
            att = new XWikiAttachment(this.document, "file." + i + ".txt");
            byte[] content = new byte[1];
            content[0] = (byte) ('0' + i);
            att.setContent(new ByteArrayInputStream(content));
            att.setDate(d);
            this.document.getAttachmentList().add(att);
        }

        setRequestExpectations("/xwiki/bin/download/space/page/file.2.txt", "5", null, null, -1l);
        setResponseExpectations(d.getTime(), 1, "text/plain", "inline; filename*=utf-8''file.5.txt");
        getMockery().checking(new Expectations()
        {
            {
                allowing(DownloadActionTest.this.ec).getMimeType(with("file.5.txt"));
                will(returnValue("text/plain"));

                allowing(DownloadActionTest.this.out).write(with(new BaseMatcher<byte[]>()
                {
                    @Override
                    public boolean matches(Object other)
                    {
                        if (!(other instanceof byte[])) {
                            return false;
                        }
                        byte[] otherByteArray = (byte[]) other;
                        return (otherByteArray[0] == '5');
                    }

                    @Override
                    public void describeTo(Description desc)
                    {
                        desc.appendValue(new byte[] {'5'});
                    }
                }), with(0), with(1));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test(expected = XWikiException.class)
    public void testDownloadByWrongId() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, "42", null, null, -1l);
        this.action.render(getContext());
    }

    @Test
    public void testDownloadByInvalidId() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, "two", null, null, -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length);
        setOutputExpectations(0, this.fileContent.length);
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test(expected = XWikiException.class)
    public void testDownloadWithIncompletePath() throws XWikiException, IOException
    {
        setRequestExpectations("/xwiki/bin/download/", null, null, null, -1l);
        this.action.render(getContext());
    }

    @Test
    public void testDifferentMimeType() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, "file.png");
        setRequestExpectations("/xwiki/bin/download/space/page/file.png", null, null, null, -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length, "image/png", "inline; filename*=utf-8''file.png");
        setOutputExpectations(0, this.fileContent.length);
        getMockery().checking(new Expectations()
        {
            {
                allowing(DownloadActionTest.this.ec).getMimeType(with("file.png"));
                will(returnValue("image/png"));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testForceDownload() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, "1", null, -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length,
            "text/plain", "attachment; filename*=utf-8''file.txt");
        setOutputExpectations(0, this.fileContent.length);
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testRealDate() throws XWikiException, IOException
    {
        final Date d = new Date(411757300000l);
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, null, -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length);
        setOutputExpectations(0, this.fileContent.length);
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testNameWithSpacesEncodedWithPlus() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, "file name.txt");
        setRequestExpectations("/xwiki/bin/download/space/page/file+name.txt", null, null, null, -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length, "text/plain",
            "inline; filename*=utf-8''file%20name.txt");
        setOutputExpectations(0, this.fileContent.length);
        getMockery().checking(new Expectations()
        {
            {
                allowing(DownloadActionTest.this.ec).getMimeType(with("file name.txt"));
                will(returnValue("text/plain"));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testNameWithSpacesEncodedWithPercent() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, "file name.txt");
        setRequestExpectations("/xwiki/bin/download/space/page/file%20name.txt", null, "1", null, -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length, "text/plain",
            "attachment; filename*=utf-8''file%20name.txt");
        setOutputExpectations(0, this.fileContent.length);
        getMockery().checking(new Expectations()
        {
            {
                allowing(DownloadActionTest.this.ec).getMimeType(with("file name.txt"));
                will(returnValue("text/plain"));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testNameWithNonAsciiChars() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, "file\u021B.txt");
        setRequestExpectations("/xwiki/bin/download/space/page/file%C8%9B.txt", null, "1", null, -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length, "text/plain",
            "attachment; filename*=utf-8''file%C8%9B.txt");
        setOutputExpectations(0, this.fileContent.length);
        getMockery().checking(new Expectations()
        {
            {
                allowing(DownloadActionTest.this.ec).getMimeType(with("file\u021B.txt"));
                will(returnValue("text/plain"));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testValidStartRange() throws XWikiException, IOException
    {
        // This test expects bytes 0, 1, 2 and 3 from the file.
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=0-3", -1l);
        setResponseExpectations(d.getTime(), 4);
        setOutputExpectations(0, 4);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_PARTIAL_CONTENT));
                one(DownloadActionTest.this.response).setHeader(with("Content-Range"),
                    with("bytes 0-3/" + DownloadActionTest.this.fileContent.length));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testValidMiddleRange() throws XWikiException, IOException
    {
        // This test expects bytes 3, 4 and 5 from the file.
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=3-5", -1l);
        setResponseExpectations(d.getTime(), 3);
        setOutputExpectations(3, 6);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_PARTIAL_CONTENT));
                one(DownloadActionTest.this.response).setHeader(with("Content-Range"),
                    with("bytes 3-5/" + DownloadActionTest.this.fileContent.length));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testValidEndRange() throws XWikiException, IOException
    {
        // This test expects bytes 9, 10, 11, 12 and 13 from the file.
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=9-13", -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length - 9);
        setOutputExpectations(9, this.fileContent.length);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_PARTIAL_CONTENT));
                one(DownloadActionTest.this.response).setHeader(with("Content-Range"),
                    with("bytes 9-13/" + DownloadActionTest.this.fileContent.length));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testOneByteRange() throws XWikiException, IOException
    {
        // This test expects the last four bytes (10, 11, 12 and 13) from the file
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=0-0", -1l);
        setResponseExpectations(d.getTime(), 1);
        setOutputExpectations(0, 1);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_PARTIAL_CONTENT));
                one(DownloadActionTest.this.response).setHeader(with("Content-Range"),
                    with("bytes 0-0/" + DownloadActionTest.this.fileContent.length));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testRestRange() throws XWikiException, IOException
    {
        // This test expects bytes from 11 to the end of the file (11, 12 and 13)
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=11-", -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length - 11);
        setOutputExpectations(11, this.fileContent.length);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_PARTIAL_CONTENT));
                one(DownloadActionTest.this.response).setHeader(with("Content-Range"),
                    with("bytes 11-13/" + DownloadActionTest.this.fileContent.length));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testFullRestRange() throws XWikiException, IOException
    {
        // This test expects the whole file
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=0-", -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length);
        setOutputExpectations(0, this.fileContent.length);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_PARTIAL_CONTENT));
                one(DownloadActionTest.this.response).setHeader(with("Content-Range"),
                    with("bytes 0-13/" + DownloadActionTest.this.fileContent.length));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testTailRange() throws XWikiException, IOException
    {
        // This test expects the last four bytes (10, 11, 12 and 13) from the file
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=-4", -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length - 10);
        setOutputExpectations(10, this.fileContent.length);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_PARTIAL_CONTENT));
                one(DownloadActionTest.this.response).setHeader(with("Content-Range"),
                    with("bytes 10-13/" + DownloadActionTest.this.fileContent.length));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testFullTailRange() throws XWikiException, IOException
    {
        // This test expects the whole file
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=-14", -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length);
        setOutputExpectations(0, this.fileContent.length);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_PARTIAL_CONTENT));
                one(DownloadActionTest.this.response).setHeader(with("Content-Range"),
                    with("bytes 0-13/" + DownloadActionTest.this.fileContent.length));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testOverflowingTailRange() throws XWikiException, IOException
    {
        // This test expects the whole file, although the client requested more
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=-40", -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length);
        setOutputExpectations(0, this.fileContent.length);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_PARTIAL_CONTENT));
                one(DownloadActionTest.this.response).setHeader(with("Content-Range"),
                    with("bytes 0-13/" + DownloadActionTest.this.fileContent.length));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testValidOverflowingRange() throws XWikiException, IOException
    {
        // This test expects bytes 9, 10, 11, 12 and 13 from the file, although 14 and 15 are requested as well.
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=9-15", -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length - 9);
        setOutputExpectations(9, this.fileContent.length);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(with(HttpServletResponse.SC_PARTIAL_CONTENT));
                one(DownloadActionTest.this.response).setHeader(with("Content-Range"),
                    with("bytes 9-13/" + DownloadActionTest.this.fileContent.length));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testInvalidSwappedRange() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=9-5", -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length);
        setOutputExpectations(0, this.fileContent.length);
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testInvalidRange() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=all", -1l);
        setResponseExpectations(d.getTime(), this.fileContent.length);
        setOutputExpectations(0, this.fileContent.length);
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testOutsideRange() throws XWikiException, IOException
    {
        // This test expects a 416 response
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=129-145", -1l);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(
                    with(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testOutsideRestRange() throws XWikiException, IOException
    {
        // This test expects a 416 response
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=129-", -1L);
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setStatus(
                    with(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE));
            }
        });
        Assert.assertNull(this.action.render(getContext()));
    }

    @Test
    public void testEmptyRange() throws XWikiException, IOException
    {
        final Date d = new Date();
        createAttachment(d, DEFAULT_FILE_NAME);
        setRequestExpectations(DEFAULT_URI, null, null, "bytes=-", -1L);
        setResponseExpectations(d.getTime(), this.fileContent.length);
        setOutputExpectations(0, this.fileContent.length);
        Assert.assertNull(this.action.render(getContext()));
    }

    private void createAttachment(Date d, String name) throws IOException
    {
        XWikiAttachment filetxt = new XWikiAttachment(this.document, name);
        filetxt.setContent(new ByteArrayInputStream(this.fileContent));
        filetxt.setDate(d);
        this.document.getAttachmentList().add(filetxt);
    }

    private void setRequestExpectations(final String uri, final String id, final String forceDownload,
        final String range,
        final long modifiedSince)
    {
        getMockery().checking(new Expectations()
        {
            {
                allowing(DownloadActionTest.this.request).getRequestURI();
                will(returnValue(uri));
                allowing(DownloadActionTest.this.request).getParameter(with("id"));
                will(returnValue(id));
                allowing(DownloadActionTest.this.request).getDateHeader(with("If-Modified-Since"));
                will(returnValue(modifiedSince));
                allowing(DownloadActionTest.this.request).getParameter(with("force-download"));
                will(returnValue(forceDownload));
                allowing(DownloadActionTest.this.request).getHeader(with("Range"));
                will(returnValue(range));
            }
        });
    }

    private void setResponseExpectations(final long modified, final int length)
    {
        setResponseExpectations(modified, length, "text/plain", "inline; filename*=utf-8''file.txt");
    }

    private void setResponseExpectations(final long modified, final int length, final String mime,
        final String disposition)
    {
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.response).setContentType(with(mime));
                one(DownloadActionTest.this.response).setHeader(with("Accept-Ranges"), with("bytes"));
                one(DownloadActionTest.this.response).addHeader(with("Content-disposition"), with(disposition));
                one(DownloadActionTest.this.response).setDateHeader(with("Last-Modified"), with(modified));
                one(DownloadActionTest.this.response).setContentLength(with(length));
            }
        });
    }

    private void setOutputExpectations(final int start, final int end) throws IOException
    {
        getMockery().checking(new Expectations()
        {
            {
                one(DownloadActionTest.this.out).write(with(new BaseMatcher<byte[]>()
                {
                    @Override
                    public boolean matches(Object other)
                    {
                        if (!(other instanceof byte[])) {
                            return false;
                        }
                        byte[] otherByteArray = (byte[]) other;
                        for (int i = start; i < end; ++i) {
                            if (otherByteArray[i - start] != DownloadActionTest.this.fileContent[i]) {
                                return false;
                            }
                        }
                        return true;
                    }

                    @Override
                    public void describeTo(Description desc)
                    {
                        desc.appendValue(ArrayUtils.subarray(DownloadActionTest.this.fileContent, start, end));
                    }
                }), with(0),
                    with(end - start));

            }
        });
    }
}
