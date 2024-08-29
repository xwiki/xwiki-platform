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
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.xwiki.component.annotation.Component;
import org.xwiki.store.serialization.xml.XMLSerializer;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * A serializer for saving the metadata from a list of XWikiAttachments.
 * the format is:
 * &lt;attachment-list serializer="attachment-archive-meta/1.0"&gt;
 *   &lt;attachment&gt;
 *     (XML formatted meta data of first version)
 *   &lt;/attachment&gt;
 *   &lt;attachment&gt;
 *     (XML formatted meta data of second version)
 *   &lt;/attachment&gt;
 * &lt;/attachment-list&gt;
 *
 * @version $Id$
 * @since 3.0M2
 */
@Component
@Named("attachment-list-meta/1.0")
@Singleton
public class AttachmentListMetadataSerializer
    extends AbstractXMLSerializer<List<XWikiAttachment>, List<XWikiAttachment>>
{
    /**
     * The root element for serialized element.
     */
    private static final String ROOT_ELEMENT_NAME = "attachment-list";

    /**
     * Root node paramter which must be present in order to attempt parsing.
     */
    private static final String SERIALIZER_PARAM = "serializer";

    /**
     * Value of SERIALIZER_PARAM must be this in order to continue parsing.
     */
    private static final String THIS_SERIALIZER = "attachment-list-meta/1.0";

    /**
     * Needed to serialize/parse the individual attachments.
     */
    @Inject
    @Named("attachment-meta/1.0")
    private XMLSerializer<XWikiAttachment, XWikiAttachment> attachSerializer;

    /**
     * Default constructor. For component manager.
     */
    public AttachmentListMetadataSerializer()
    {
        // Do nothing.
    }

    /**
     * Testing Constructor.
     * Dependencied specified.
     *
     * @param attachSerializer the serializer used to serialize/parse the individual attachments.
     */
    public AttachmentListMetadataSerializer(
        final XMLSerializer<XWikiAttachment, XWikiAttachment> attachSerializer)
    {
        this.attachSerializer = attachSerializer;
    }

    @Override
    public List<XWikiAttachment> parse(final Element docel) throws IOException
    {
        if (!ROOT_ELEMENT_NAME.equals(docel.getName())) {
            throw new IOException("XML not recognizable as archive metadata, expecting <archive> tag");
        }
        if (!THIS_SERIALIZER.equals(docel.attribute(SERIALIZER_PARAM).getValue())) {
            throw new IOException("Cannot parse this attachment archive metadata, it was saved with a "
                + "different serializer.");
        }
        final List<XWikiAttachment> attachments = new ArrayList<XWikiAttachment>(docel.elements().size());
        for (Element attach : docel.elements()) {
            attachments.add(this.attachSerializer.parse(attach));
        }
        return attachments;
    }

    @Override
    public void serialize(final List<XWikiAttachment> attachments,
        final XMLWriter writer)
        throws IOException
    {
        final Element docel = new DOMElement(ROOT_ELEMENT_NAME);
        docel.addAttribute(SERIALIZER_PARAM, THIS_SERIALIZER);
        writer.writeOpen(docel);

        for (XWikiAttachment attachment : attachments) {
            this.attachSerializer.serialize(attachment, writer);
        }

        writer.writeClose(docel);
    }
}
