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
package org.xwiki.wikistream.xar.internal.input;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xwiki.filter.FilterEventParameters;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.xar.internal.XARAttachmentModel;
import org.xwiki.wikistream.xar.internal.XARClassModel;
import org.xwiki.wikistream.xar.internal.XARDocumentModel;
import org.xwiki.wikistream.xar.internal.XARFilter;
import org.xwiki.wikistream.xar.internal.XARObjectModel;
import org.xwiki.wikistream.xml.internal.input.XMLInputWikiStreamUtils;

/**
 * @version $Id$
 * @since 5.2RC1
 */
public class DocumentLocaleReader
{
    private String currentSpace;

    private String currentDocument;

    private FilterEventParameters currentDocumentParameters;

    public void read(Object filter, XARFilter proxyFilter, XARInputProperties properties) throws XMLStreamException,
        IOException, WikiStreamException
    {
        XMLStreamReader xmlReader = XMLInputWikiStreamUtils.createXMLStreamReader(properties);

        // <xwikidoc>

        xmlReader.nextTag();

        xmlReader.require(XMLStreamReader.START_ELEMENT, null, XARDocumentModel.ELEMENT_DOCUMENT);

        readDocument(xmlReader, filter, proxyFilter, properties);
    }

    private void readDocument(XMLStreamReader xmlReader, Object filter, XARFilter proxyFilter,
        XARInputProperties properties) throws XMLStreamException
    {
        for (xmlReader.nextTag(); !xmlReader.getLocalName().equals(XARDocumentModel.ELEMENT_DOCUMENT); xmlReader
            .nextTag()) {
            String elementName = xmlReader.getLocalName();

            if (elementName.equals(XARAttachmentModel.ELEMENT_ATTACHMENT)) {
                readAtachment(xmlReader, filter, proxyFilter, properties);
            } else if (elementName.equals(XARObjectModel.ELEMENT_OBJECT)) {
                readObject(xmlReader, filter, proxyFilter, properties);
            } else if (elementName.equals(XARClassModel.ELEMENT_CLASS)) {
                readClass(xmlReader, filter, proxyFilter, properties);
            } else {
                
            }
        }
    }

    private void readObject(XMLStreamReader xmlReader, Object filter, XARFilter proxyFilter,
        XARInputProperties properties)
    {

    }

    private void readClass(XMLStreamReader xmlReader, Object filter, XARFilter proxyFilter,
        XARInputProperties properties)
    {

    }

    private void readAtachment(XMLStreamReader xmlReader, Object filter, XARFilter proxyFilter,
        XARInputProperties properties)
    {

    }
}
