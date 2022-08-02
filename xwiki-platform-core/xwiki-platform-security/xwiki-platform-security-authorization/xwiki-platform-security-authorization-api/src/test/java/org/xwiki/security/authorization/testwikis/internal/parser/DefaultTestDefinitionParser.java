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

package org.xwiki.security.authorization.testwikis.internal.parser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.testwikis.TestDefinition;
import org.xwiki.security.authorization.testwikis.TestDefinitionParser;

/**
 * Parser for parsing Tests Wikis XML definition.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class DefaultTestDefinitionParser extends DefaultHandler implements ElementParser, TestDefinitionParser
{
    /**
     * Stack of entity factories.
     * The top level represent the active factories. The key is the element name supported by the factory.
     * The same factory may be registered for different element names. Only one factory can be registered
     * per element, the last arrived win.
     */
    private Stack<Map<String, EntityFactory>> elementHandlers;

    /** The locator to retrieve the current parsing location when logging errors. */
    private Locator locator;

    /** During a given parsing, the factory handling the root level. */
    private TestWikisFactory handler;

    /** During a given parsing, the entity resolver that could be used by factories. */
    private EntityReferenceResolver<String> resolver;

    /** During a given parsing, the entity serializer that could be used by factories. */
    private EntityReferenceSerializer<String> serializer;

    /** Default constructor. */
    public DefaultTestDefinitionParser()
    {
    }

    @Override
    public TestDefinition parse(String filename,
        EntityReferenceResolver<String> resolver,
        EntityReferenceSerializer<String> serializer)
        throws IOException, URISyntaxException, ParserConfigurationException, SAXException
    {
        URL url = ClassLoader.getSystemResource(filename);

        if (url == null) {
            throw new FileNotFoundException(filename);
        }

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser = parserFactory.newSAXParser();

        this.resolver = resolver;
        this.serializer = serializer;
        elementHandlers = new Stack<Map<String, EntityFactory>>();
        elementHandlers.push(new HashMap<String, EntityFactory>());
        handler = new TestWikisFactory();
        register(handler);

        parser.parse(url.toURI().toString(), this);

        if (elementHandlers.size() > 1) {
            throw new SAXException("Handlers stack as been corrupted while parsing.");
        }

        elementHandlers = null;
        locator = null;
        this.resolver = null;
        this.serializer = null;
        TestDefinition wikis = getWikis();
        handler = null;
        return wikis;
    }

    @Override
    public TestDefinition getWikis()
    {
        if (handler == null) {
            throw new IllegalStateException("Wikis are only reachable during parsing");
        }
        return handler.getWikis();
    }

    @Override
    public EntityReferenceResolver<String> getResolver()
    {
        return resolver;
    }

    @Override
    public EntityReferenceSerializer<String> getSerializer()
    {
        return serializer;
    }

    @Override
    public void register(EntityFactory handler) {
        if (handler == null) {
            return;
        }
        Map<String, EntityFactory> handlers = elementHandlers.peek();
        for (String tagName : handler.getTagNames()) {
            handlers.put(tagName, handler);
        }
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException
    {
        Map<String, EntityFactory> handlers = elementHandlers.peek();
        elementHandlers.push(new HashMap<String, EntityFactory>());
        EntityFactory factory = handlers.get(name);
        if (factory == null) {
            throw new SAXException(getLocatedMessage("No factory for element %s", name));
        }
        factory.newElement(this, name, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException
    {
        elementHandlers.pop();
    }

    @Override
    public void setDocumentLocator(Locator locator)
    {
        super.setDocumentLocator(locator);

        this.locator = locator;
    }

    /**
     * @return the current parsing location in string format " (at line %d, column %d)".
     */
    private String getCurrentLocation()
    {
        if (locator != null) {
            return String.format(" (at line %d, column %d)", locator.getLineNumber(), locator.getColumnNumber());
        } else {
            return " (Unknown location)";
        }
    }

    @Override
    public String getLocatedMessage(String format, Object... objects)
    {
        return String.format(format, objects) + getCurrentLocation();
    }

    @Override
    public void warning(SAXParseException e) throws SAXException
    {
        throw new SAXException(e.getLocalizedMessage(), e);
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException
    {
        throw new SAXException(e.getLocalizedMessage(), e);
    }

    @Override
    public void error(SAXParseException e) throws SAXException
    {
        throw new SAXException(e.getLocalizedMessage(), e);
    }
}
