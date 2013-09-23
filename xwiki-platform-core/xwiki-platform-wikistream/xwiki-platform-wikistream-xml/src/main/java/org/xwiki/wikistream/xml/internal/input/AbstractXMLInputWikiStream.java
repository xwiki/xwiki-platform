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

import javanet.staxutils.XMLStreamUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Result;

import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputSource;
import org.xwiki.wikistream.input.InputStreamInputSource;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.ReaderInputSource;

/**
 * 
 * @param <P>
 * @version $Id$
 * @since 5.2M2
 */
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
            XMLEventReader xmlEventReader;

            InputSource source = this.parameters.getSource();
            if (source instanceof ReaderInputSource) {
                xmlEventReader = XMLInputFactory.newInstance().createXMLEventReader(((ReaderInputSource) source).getReader());
            } else if (source instanceof InputStreamInputSource) {
                xmlEventReader = XMLInputFactory.newInstance().createXMLEventReader(((InputStreamInputSource) source).getInputStream());
            } else {
                throw new WikiStreamException("Unknown source type [" + source.getClass() + "]");
            }

            XMLStreamUtils.copy(xmlEventReader, createParser(listener, this.parameters));
        } catch (Exception e) {
            throw new WikiStreamException("Faild to parse XML source", e);
        }
    }

    protected abstract Result createParser(Object listener, P parameters);
}
