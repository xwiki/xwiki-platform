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
package org.xwiki.wikistream.mediawiki.xml.internal.input;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xwiki.wikistream.mediawiki.xml.input.MediaWikiXMLInputParameters;

public class MediaWikiXMLContentHandlerParser extends AbstractContentHandlerParser
{
    private final Object listener;

    private final MediaWikiXMLInputParameters parameters;

    private StringBuffer value;

    public MediaWikiXMLContentHandlerParser(Object listener, MediaWikiXMLInputParameters parameters)
    {
        this.listener = listener;
        this.parameters = parameters;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if (this.level > 0) {
            if (getXmlTagParameters() != null && getXmlTagParameters().containsKey(qName)) {
                this.value = new StringBuffer();
            }
        }

        ++this.level;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        if (this.value != null) {
            this.value.append(ch, start, length);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        // Fill in.
        --this.level;
    }
}
