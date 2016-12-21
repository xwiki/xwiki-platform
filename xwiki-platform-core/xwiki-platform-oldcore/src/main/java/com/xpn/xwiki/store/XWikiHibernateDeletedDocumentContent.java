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
package com.xpn.xwiki.store;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.xwiki.filter.input.DefaultReaderInputSource;
import org.xwiki.filter.output.DefaultWriterOutputTarget;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocumentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.file.TemporaryDeferredFileRepository;
import com.xpn.xwiki.internal.file.TemporaryDeferredFileRepository.TemporaryDeferredStringFile;
import com.xpn.xwiki.web.Utils;

/**
 * The stored content of the deleted document.
 *
 * @version $Id$
 * @since 9.0RC1
 */
public class XWikiHibernateDeletedDocumentContent implements XWikiDeletedDocumentContent
{
    private final TemporaryDeferredStringFile content;

    /**
     * @param document the deleted document
     * @throws XWikiException when failing to serialize document
     */
    public XWikiHibernateDeletedDocumentContent(XWikiDocument document) throws XWikiException
    {
        this.content = createTemporaryDeferredFile();

        try {
            // Don't format the XML to reduce the size
            document.toXML(new DefaultWriterOutputTarget(this.content.getWriter(), true), true, false, true, true,
                false, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error serializing document to xml", e, null);
        }
    }

    /**
     * @param xml the serialized document as XML
     * @throws IOException when failing to store xml
     */
    public XWikiHibernateDeletedDocumentContent(String xml) throws IOException
    {
        this.content = createTemporaryDeferredFile();

        this.content.setString(xml);
    }

    private TemporaryDeferredStringFile createTemporaryDeferredFile()
    {
        return Utils.getComponent(TemporaryDeferredFileRepository.class)
            .createTemporaryDeferredStringFile("deleted-documents-xml", StandardCharsets.UTF_8);
    }

    @Override
    public String getContentAsString() throws IOException
    {
        return this.content.getString();
    }

    /**
     * @param document the document to write to or null to create a new one
     * @return restored document
     * @throws XWikiException if error in {@link XWikiDocument#fromXML(String)}
     * @throws IOException when failing to read the content
     */
    @Override
    public XWikiDocument getXWikiDocument(XWikiDocument document) throws XWikiException, IOException
    {
        XWikiDocument result = document;
        if (result == null) {
            result = new XWikiDocument();
        }

        try (Reader reader = this.content.getReader()) {
            result.fromXML(new DefaultReaderInputSource(reader, true), true);
        }

        return result;
    }
}
