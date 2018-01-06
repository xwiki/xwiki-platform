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
package com.xpn.xwiki.pdf.impl;

import java.io.ByteArrayInputStream;
import java.util.Collections;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PDFURIResolver}.
 *
 * @version $Id$
 * @since 5.0RC1
 */
public class PDFURIResolverTest
{
    @Test
    public void resolveWhenNoMap() throws Exception
    {
        XWikiContext context = mock(XWikiContext.class);
        PDFURIResolver resolver = new PDFURIResolver(context);
        Source source = resolver.resolve("href", "base");
        Assert.assertNull(source);
    }

    @Test
    public void resolveWhenMapItemExists() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        AttachmentReference attachmentReference = new AttachmentReference("fileName", documentReference);
        com.xpn.xwiki.XWiki xwiki = mock(com.xpn.xwiki.XWiki.class);
        XWikiDocument document = mock(XWikiDocument.class);
        XWikiAttachment attachment = mock(XWikiAttachment.class);

        XWikiContext context = mock(XWikiContext.class);
        // Add an encoded space character to test that the URL is not decoded. The input URL (to be resolved) is
        // expected to be encoded because the URL factory creates encoded URLs.
        String url = "encoded+url";
        when(context.get("pdfExportImageURLMap")).thenReturn(Collections.singletonMap(url, attachmentReference));
        when(context.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(attachmentReference.extractReference(EntityType.DOCUMENT), context)).thenReturn(
            document);
        when(document.getAttachment("fileName")).thenReturn(attachment);
        when(attachment.getContentInputStream(context)).thenReturn(new ByteArrayInputStream("content".getBytes()));

        PDFURIResolver resolver = new PDFURIResolver(context);
        Source source = resolver.resolve(url, "base");
        Assert.assertEquals(StreamSource.class, source.getClass());
        Assert.assertEquals("content", IOUtils.readLines(((StreamSource) source).getInputStream()).get(0));
    }
}
