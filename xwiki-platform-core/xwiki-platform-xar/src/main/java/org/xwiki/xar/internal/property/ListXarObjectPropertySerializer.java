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
package org.xwiki.xar.internal.property;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.xwiki.component.annotation.Component;
import org.xwiki.xar.internal.XarObjectPropertySerializer;

/**
 * {@link List} based implementation of {@link XarObjectPropertySerializer}.
 * 
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = {"StaticList", "DBList", "DBTreeList", "Page" })
@Singleton
public class ListXarObjectPropertySerializer implements XarObjectPropertySerializer
{
    /**
     * The local name of the <code>value</code> XML element.
     */
    public static final String ELEMENT_VALUE = "value";

    @Override
    public Object read(XMLStreamReader reader) throws XMLStreamException
    {
        StringBuffer content = new StringBuffer();

        for (int eventType = reader.next(); eventType != XMLStreamConstants.END_ELEMENT; eventType = reader.next()) {
            if (eventType == XMLStreamConstants.CHARACTERS || eventType == XMLStreamConstants.CDATA
                || eventType == XMLStreamConstants.SPACE || eventType == XMLStreamConstants.ENTITY_REFERENCE) {
                content.append(reader.getText());
            } else if (eventType == XMLStreamConstants.PROCESSING_INSTRUCTION
                || eventType == XMLStreamConstants.COMMENT) {
                // skipping
            } else if (eventType == XMLStreamConstants.END_DOCUMENT) {
                throw new XMLStreamException("unexpected end of document when reading element text content");
            } else if (eventType == XMLStreamConstants.START_ELEMENT) {
                return readList(reader);
            } else {
                throw new XMLStreamException("Unexpected event type " + eventType, reader.getLocation());
            }
        }

        return content.toString();
    }

    private List<String> readList(XMLStreamReader reader) throws XMLStreamException
    {
        List<String> list = new ArrayList<String>();

        for (; reader.getEventType() == XMLStreamConstants.START_ELEMENT; reader.nextTag()) {
            reader.require(XMLStreamConstants.START_ELEMENT, null, ELEMENT_VALUE);

            list.add(reader.getElementText());
        }

        return list;
    }

    @Override
    public void write(XMLStreamWriter writer, Object value) throws XMLStreamException
    {
        if (value instanceof List) {
            List<String> list = (List<String>) value;
            for (String element : list) {
                if (value != null) {
                    writer.writeStartElement(ELEMENT_VALUE);
                    writer.writeCharacters(element);
                    writer.writeEndElement();
                }
            }
        } else if (value != null) {
            writer.writeCharacters(value.toString());
        }
    }
}
