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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.xwiki.component.annotation.Component;

/**
 * A serializer for saving the mappings between longs and strings.
 * This is needed because deleted attachments are retreived by database id style longs.
 *
 * @version $Id$
 * @since 3.0M3
 */
@Component("deleted-attachment-id-mappings/1.0")
public class DeletedAttachmentIdMappingSerializer
    extends AbstractXMLSerializer<Map<Long, String>, Map<Long, String>>
{
    /**
     * The root element for serialized element.
     */
    private static final String ROOT_ELEMENT_NAME = "deletedattachmentids";

    /**
     * Root node paramter which must be present in order to attempt parsing.
     */
    private static final String SERIALIZER_PARAM = "serializer";

    /**
     * Value of SERIALIZER_PARAM must be this in order to continue parsing.
     */
    private static final String THIS_SERIALIZER = "deleted-attachment-id-mappings/1.0";

    /**
     * Interpret a node by this name as an entry in the map.
     */
    private static final String ENTRY = "entry";

    /**
     * Interpret a node by this name as the long id number in base 10.
     */
    private static final String ID = "id";

    /**
     * Interpret this node as the path on the filesystem, relitive to the storage location.
     */
    private static final String PATH = "path";

    @Override
    public Map<Long, String> parse(final Element docel) throws IOException
    {
        if (!ROOT_ELEMENT_NAME.equals(docel.getName())) {
            throw new IOException("XML not recognizable as attachment metadata, "
                + "expecting <deletedattachmentids> tag");
        }
        if (docel.attribute(SERIALIZER_PARAM) == null
            || !THIS_SERIALIZER.equals(docel.attribute(SERIALIZER_PARAM).getValue()))
        {
            throw new IOException("Cannot parse this deleted attachment id mapping, "
                + "it was saved with a different serializer.");
        }
        final Map<Long, String> out = new HashMap<Long, String>();

        for (Element entry : ((List<Element>) docel.elements(ENTRY))) {
            out.put(Long.valueOf(entry.element(ID).getText()), entry.element(PATH).getText());
        }
        return out;
    }

    @Override
    public void serialize(final Map<Long, String> map, final XMLWriter writer)
        throws IOException
    {
        final Element docel = new DOMElement(ROOT_ELEMENT_NAME);
        docel.addAttribute(SERIALIZER_PARAM, THIS_SERIALIZER);
        writer.writeOpen(docel);

        for (Long id : map.keySet()) {
            final Element entry = new DOMElement(ENTRY);
            writer.writeOpen(entry);
            writer.write(new DOMElement(ID).addText(id.toString()));
            writer.write(new DOMElement(PATH).addText(map.get(id)));
            writer.writeClose(entry);
        }

        writer.writeClose(docel);
    }
}
