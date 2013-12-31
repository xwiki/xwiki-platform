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
package org.xwiki.xar.internal;

import java.io.InputStream;
import java.util.Locale;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.LocaleUtils;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.xar.internal.model.XarDocumentModel;
import org.xwiki.xml.stax.StAXUtils;

/**
 * @version $Id$
 * @since 5.4M1
 */
public final class XarUtils
{
    private XarUtils()
    {
        // Utility class
    }

    /**
     * Extract {@link LocalDocumentReference} from a XAR document XML stream.
     * 
     * @param documentStream the stream to parse
     * @return the reference extracted from the stream
     * @throws XarException when failing to parse the document stream
     * @since 5.4M1
     */
    public static LocalDocumentReference getReference(InputStream documentStream) throws XarException
    {
        XMLStreamReader xmlReader;
        try {
            xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(documentStream);
        } catch (XMLStreamException e) {
            throw new XarException("Failed to create a XML read", e);
        }

        String space = null;
        String page = null;
        Locale locale = null;

        try {
            // <xwikidoc>

            xmlReader.nextTag();

            xmlReader.require(XMLStreamReader.START_ELEMENT, null, XarDocumentModel.ELEMENT_DOCUMENT);

            for (xmlReader.nextTag(); xmlReader.isStartElement(); xmlReader.nextTag()) {
                String elementName = xmlReader.getLocalName();

                if (XarDocumentModel.ELEMENT_NAME.equals(elementName)) {
                    page = xmlReader.getElementText();

                    if (space != null && locale != null) {
                        break;
                    }
                } else if (XarDocumentModel.ELEMENT_SPACE.equals(elementName)) {
                    space = xmlReader.getElementText();

                    if (page != null && locale != null) {
                        break;
                    }
                } else if (XarDocumentModel.ELEMENT_LOCALE.equals(elementName)) {
                    String value = xmlReader.getElementText();
                    if (value.length() == 0) {
                        locale = Locale.ROOT;
                    } else {
                        locale = LocaleUtils.toLocale(value);
                    }

                    if (space != null && page != null) {
                        break;
                    }
                } else {
                    StAXUtils.skipElement(xmlReader);
                }
            }
        } catch (XMLStreamException e) {
            throw new XarException("Failed to parse document", e);
        } finally {
            try {
                xmlReader.close();
            } catch (XMLStreamException e) {
                throw new XarException("Failed to close XML reader", e);
            }
        }

        if (space == null) {
            throw new XarException("Missing space element");
        }
        if (page == null) {
            throw new XarException("Missing page element");
        }
        if (locale == null) {
            throw new XarException("Missing locale element");
        }

        return new LocalDocumentReference(space, page, locale);
    }
}
