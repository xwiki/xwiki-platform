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

import com.xpn.xwiki.doc.XWikiAttachment;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.xwiki.component.annotation.Component;
import org.xwiki.store.legacy.doc.internal.DeletedFilesystemAttachment;
import org.xwiki.store.legacy.doc.internal.MutableDeletedFilesystemAttachment;
import org.xwiki.store.serialization.xml.XMLSerializer;

/**
 * A serializer for saving the metadata from a deleted XWikiAttachment.
 *
 * @version $Id$
 * @since 3.0M3
 */
@Component
@Named("deleted-attachment-meta/1.0")
@Singleton
public class DeletedAttachmentMetadataSerializer
    extends AbstractXMLSerializer<DeletedFilesystemAttachment, MutableDeletedFilesystemAttachment>
{
    /**
     * The root element for serialized element.
     */
    private static final String ROOT_ELEMENT_NAME = "deletedattachment";

    /**
     * Root node paramter which must be present in order to attempt parsing.
     */
    private static final String SERIALIZER_PARAM = "serializer";

    /**
     * Value of SERIALIZER_PARAM must be this in order to continue parsing.
     */
    private static final String THIS_SERIALIZER = "deletedattachment-meta/1.0";

    /**
     * Interpret a node by this name as the document full name of the deleter's user document.
     */
    private static final String DELETER = "deleter";

    /**
     * Interpret this node as the date of deletion in seconds from the epoch.
     */
    private static final String DATE_DELETED = "datedeleted";

    /**
     * Interpret this node as the an attachment to be parsed by the attachment-meta serializer.
     */
    private static final String ATTACHMENT = "attachment";

    /**
     * Needed to serialize/parse the deleted attachment metadata.
     */
    @Inject
    @Named("attachment-meta/1.0")
    private XMLSerializer<XWikiAttachment, XWikiAttachment> attachSerializer;

    @Override
    public MutableDeletedFilesystemAttachment parse(final Element docel) throws IOException
    {
        if (!ROOT_ELEMENT_NAME.equals(docel.getName())) {
            throw new IOException("XML not recognizable as deleted attachment metadata, "
                + "expecting <deletedattachment> tag");
        }
        if (docel.attribute(SERIALIZER_PARAM) == null
            || !THIS_SERIALIZER.equals(docel.attribute(SERIALIZER_PARAM).getValue()))
        {
            throw new IOException("Cannot parse this deleted attachment metadata, "
                + "it was saved with a different serializer.");
        }
        final MutableDeletedFilesystemAttachment out = new MutableDeletedFilesystemAttachment();

        out.setDeleter(docel.element(DELETER).getText());
        out.setDate(new Date(Long.parseLong(docel.element(DATE_DELETED).getText())));
        out.setAttachment(this.attachSerializer.parse(docel.element(ATTACHMENT)), null);

        return out;
    }

    @Override
    public void serialize(final DeletedFilesystemAttachment delAttach, final XMLWriter writer)
        throws IOException
    {
        final Element docel = new DOMElement(ROOT_ELEMENT_NAME);
        docel.addAttribute(SERIALIZER_PARAM, THIS_SERIALIZER);
        writer.writeOpen(docel);

        writer.write(new DOMElement(DELETER).addText(delAttach.getDeleter()));
        writer.write(new DOMElement(DATE_DELETED).addText(delAttach.getDate().getTime() + ""));
        this.attachSerializer.serialize(delAttach.getAttachment(), writer);

        writer.writeClose(docel);
    }
}
