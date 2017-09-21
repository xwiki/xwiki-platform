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
package com.xpn.xwiki.internal.store.hibernate;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import org.xwiki.filter.input.DefaultReaderInputSource;
import org.xwiki.filter.output.DefaultWriterOutputTarget;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachmentContent;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.internal.file.TemporaryDeferredFileRepository;
import com.xpn.xwiki.internal.file.TemporaryDeferredFileRepository.TemporaryDeferredStringFile;
import com.xpn.xwiki.web.Utils;

/**
 * Hibernate based implementation of DeletedAttachmentContent.
 *
 * @version $Id$
 * @since 9.9RC1
 */
public class HibernateDeletedAttachmentContent implements DeletedAttachmentContent
{
    private final TemporaryDeferredStringFile content;

    /**
     * @param attachment the deleted attachment
     * @throws XWikiException when failing to serialize document
     */
    public HibernateDeletedAttachmentContent(XWikiAttachment attachment) throws XWikiException
    {
        this.content = createTemporaryDeferredFile();

        try {
            // Don't format the XML to reduce the size
            attachment.toXML(new DefaultWriterOutputTarget(this.content.getWriter(), true), true, true, false,
                StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error serializing document to xml", e, null);
        }
    }

    /**
     * @param xml the serialized document as XML
     * @throws IOException when failing to store xml
     */
    public HibernateDeletedAttachmentContent(String xml) throws IOException
    {
        this.content = createTemporaryDeferredFile();

        this.content.setString(xml);
    }

    private TemporaryDeferredStringFile createTemporaryDeferredFile()
    {
        return Utils.getComponent(TemporaryDeferredFileRepository.class)
            .createTemporaryDeferredStringFile("deleted-attachment-xml", StandardCharsets.UTF_8);
    }

    @Override
    public String getContentAsString() throws IOException
    {
        return this.content.getString();
    }

    @Override
    public XWikiAttachment getXWikiAttachment(XWikiAttachment attachment) throws XWikiException, IOException
    {
        XWikiAttachment result = attachment;
        if (result == null) {
            result = new XWikiAttachment();
        }

        try (Reader reader = this.content.getReader()) {
            result.fromXML(new DefaultReaderInputSource(reader));
        }

        return result;
    }
}
