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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.XMLReader;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.source.InputSource;
import org.xwiki.wikistream.input.source.InputStreamInputSource;
import org.xwiki.wikistream.input.source.ReaderInputSource;
import org.xwiki.wikistream.internal.input.source.DefaultReaderInputSource;

public abstract class AbstractXMLInputWikiStream<P extends XMLInputProperties> implements InputWikiStream
{
    protected P parameters;

    public AbstractXMLInputWikiStream(P parameters)
    {
        this.parameters = parameters;
    }

    @Override
    public void read(Object listener) throws WikiStreamException
    {
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            SAXParser saxParser = parserFactory.newSAXParser();
            XMLReader xmlReader = saxParser.getXMLReader();

            xmlReader.setContentHandler(createContentHandler(listener));

            InputSource source = this.parameters.getSource();
            if (source instanceof ReaderInputSource) {
                xmlReader.parse(new org.xml.sax.InputSource(((DefaultReaderInputSource) source).getReader()));
            } else if (source instanceof InputStreamInputSource) {
                InputStreamInputSource inputStreamSource = (InputStreamInputSource) source;

                xmlReader.parse(new org.xml.sax.InputSource(inputStreamSource.getInputStream()));
            } else {
                throw new WikiStreamException("Unknown source type [" + source.getClass() + "]");
            }
        } catch (Exception e) {
            throw new WikiStreamException("Faild to parse XML source", e);
        }
    }

    protected abstract ContentHandler createContentHandler(Object listener);
}
