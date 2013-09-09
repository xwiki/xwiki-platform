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
package org.xwiki.wikistream.xml.internal.input;

import java.io.IOException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputSource;
import org.xwiki.wikistream.input.InputStreamInputSource;
import org.xwiki.wikistream.input.ReaderInputSource;
import org.xwiki.wikistream.xml.input.source.SourceInputSource;
import org.xwiki.xml.stax.StAXUtils;

/**
 * 
 * @version $Id$
 * @since 5.2M2
 */
public final class XMLInputWikiStreamUtils
{
    public static XMLStreamReader createXMLStreamWriter(XMLInputProperties properties) throws XMLStreamException,
        IOException, WikiStreamException
    {
        XMLStreamReader xmlStreamReader;

        InputSource source = properties.getSource();

        if (source instanceof ReaderInputSource) {
            xmlStreamReader =
                XMLInputFactory.newInstance().createXMLStreamReader(((ReaderInputSource) source).getReader());
        } else if (source instanceof InputStreamInputSource) {
            xmlStreamReader =
                XMLInputFactory.newInstance().createXMLStreamReader(((InputStreamInputSource) source).getInputStream());
        } else if (source instanceof SourceInputSource) {
            xmlStreamReader = StAXUtils.getXMLStreamReader(((SourceInputSource) source).getSource());
        } else {
            throw new WikiStreamException("Unknown source type [" + source.getClass() + "]");
        }

        return xmlStreamReader;
    }

    public static XMLEventReader createXMLEventReader(XMLInputProperties properties) throws XMLStreamException,
        IOException, WikiStreamException
    {
        XMLEventReader xmlEventReader;

        InputSource source = properties.getSource();

        if (source instanceof ReaderInputSource) {
            xmlEventReader =
                XMLInputFactory.newInstance().createXMLEventReader(((ReaderInputSource) source).getReader());
        } else if (source instanceof InputStreamInputSource) {
            xmlEventReader =
                XMLInputFactory.newInstance().createXMLEventReader(((InputStreamInputSource) source).getInputStream());
        } else if (source instanceof SourceInputSource) {
            xmlEventReader = StAXUtils.getXMLEventReader(((SourceInputSource) source).getSource());
        } else {
            throw new WikiStreamException("Unknown source type [" + source.getClass() + "]");
        }

        return xmlEventReader;
    }
}
