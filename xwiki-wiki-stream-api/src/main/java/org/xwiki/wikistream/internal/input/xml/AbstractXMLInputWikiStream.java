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
package org.xwiki.wikistream.internal.input.xml;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.wikistream.input.ContentHandlerParser;
import org.xwiki.wikistream.internal.input.AbstractInputWikiStream;
import org.xwiki.wikistream.listener.Listener;

/**
 * @version $Id$
 */
public abstract class AbstractXMLInputWikiStream<P> extends AbstractInputWikiStream<P>
{
    /**
     * Used to lookup parser instances.
     */
    @Inject
    protected ComponentManager componentManager;

    /**
     * Reuse the same factory.
     */
    private SAXParserFactory parserFactory;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();
        this.parserFactory = SAXParserFactory.newInstance();
    }

    public AbstractXMLInputWikiStream(String name, String description, Class< ? > parameterBeanClass)
    {
        super(name, description, parameterBeanClass);
        // TODO Fix the constructor
    }

    public ContentHandlerParser createParser(Listener listener)
    {
        return this.createParser(listener, null);
    }

    public ContentHandlerParser createParser(Listener listener, Map<String, String> xmlTagParameters)
    {
        ContentHandlerParser parser = null;
        try {
            parser =
                this.componentManager.lookup(ContentHandlerParser.class, getType().toIdString() + "/contenthandler");
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to create [" + getType().toString() + "] ContentHandler parser", e);
        }

        parser.setListener(listener);
        parser.setXmlTagParameters(null);
        return parser;
    }

    /**
     * @param source the content to parse
     * @param listener receive event for each element
     * @throws ParserConfigurationException error when rendering
     * @throws SAXException error when rendering
     * @throws IOException error when rendering
     */
    public void parseXML(Reader source, Listener listener) throws ParserConfigurationException, SAXException,
        IOException
    {
        this.parseXML(source, listener, null);
    }

    /**
     * @param source the content to parse
     * @param listener receive event for each element
     * @throws ParserConfigurationException error when rendering
     * @throws SAXException error when rendering
     * @throws IOException error when rendering
     */
    public void parseXML(Reader source, Listener listener, Map<String, String> xmlTagParameters)
        throws ParserConfigurationException, SAXException, IOException
    {
        SAXParser saxParser = this.parserFactory.newSAXParser();
        XMLReader xmlReader = saxParser.getXMLReader();

        ContentHandlerParser parser = createParser(listener, xmlTagParameters);
        xmlReader.setContentHandler(parser);

        xmlReader.parse(new InputSource(source));
    }
}
