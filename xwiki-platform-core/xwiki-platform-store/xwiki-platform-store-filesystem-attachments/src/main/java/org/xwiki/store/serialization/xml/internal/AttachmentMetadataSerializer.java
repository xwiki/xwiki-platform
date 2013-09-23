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
package org.xwiki.store.serialization.xml.internal;

import java.io.IOException;
import java.util.Date;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * A serializer for saving the metadata from an XWikiAttachment.
 *
 * @version $Id$
 * @since 3.0M2
 */
@Component("attachment-meta/1.0")
public class AttachmentMetadataSerializer extends AbstractXMLSerializer<XWikiAttachment, XWikiAttachment>
{
    /**
     * The root element for serialized element.
     */
    private static final String ROOT_ELEMENT_NAME = "attachment";

    /**
     * Root node paramter which must be present in order to attempt parsing.
     */
    private static final String SERIALIZER_PARAM = "serializer";

    /**
     * Value of SERIALIZER_PARAM must be this in order to continue parsing.
     */
    private static final String THIS_SERIALIZER = "attachment-meta/1.0";

    /**
     * Interpret a node by this name as the filename of the attachment.
     */
    private static final String FILENAME = "filename";

    /**
     * Interpret a node by this name as the size of the attachment in bytes.
     */
    private static final String FILESIZE = "filesize";

    /**
     * Interpret a node by this name as the name of the attachment author.
     */
    private static final String AUTHOR = "author";

    /**
     * Interpret a node by this name as a string representation of the RCS version of the attachment.
     */
    private static final String VERSION = "version";

    /**
     * Interpret a node by this name as a comment left by the author on this attachment.
     */
    private static final String COMMENT = "comment";

    /**
     * Interpret a node by this name as the date the attachment was saved.
     */
    private static final String DATE = "date";

    @Override
    public XWikiAttachment parse(final Element docel) throws IOException
    {
        if (!ROOT_ELEMENT_NAME.equals(docel.getName())) {
            throw new IOException("XML not recognizable as attachment metadata, expecting <attachment> tag");
        }
        if (docel.attribute(SERIALIZER_PARAM) == null
            || !THIS_SERIALIZER.equals(docel.attribute(SERIALIZER_PARAM).getValue()))
        {
            throw new IOException("Cannot parse this attachment metadata, it was saved with a different "
                + "serializer.");
        }
        final XWikiAttachment out = new XWikiAttachment();

        out.setFilename(docel.element(FILENAME).getText());
        out.setFilesize(Integer.parseInt(docel.element(FILESIZE).getText()));
        out.setAuthor(docel.element(AUTHOR).getText());
        out.setVersion(docel.element(VERSION).getText());
        out.setComment(docel.element(COMMENT).getText());

        final String sdate = docel.element(DATE).getText();
        final Date date = new Date(Long.parseLong(sdate));
        out.setDate(date);

        return out;
    }

    @Override
    public void serialize(final XWikiAttachment attach, final XMLWriter writer) throws IOException
    {
        final Element docel = new DOMElement(ROOT_ELEMENT_NAME);
        docel.addAttribute(SERIALIZER_PARAM, THIS_SERIALIZER);
        writer.writeOpen(docel);

        writer.write(new DOMElement(FILENAME).addText(attach.getFilename()));
        writer.write(new DOMElement(FILESIZE).addText(attach.getFilesize() + ""));
        writer.write(new DOMElement(AUTHOR).addText(attach.getAuthor()));
        writer.write(new DOMElement(VERSION).addText(attach.getVersion()));
        writer.write(new DOMElement(COMMENT).addText(attach.getComment()));
        writer.write(new DOMElement(DATE).addText(attach.getDate().getTime() + ""));

        writer.writeClose(docel);
    }
}
