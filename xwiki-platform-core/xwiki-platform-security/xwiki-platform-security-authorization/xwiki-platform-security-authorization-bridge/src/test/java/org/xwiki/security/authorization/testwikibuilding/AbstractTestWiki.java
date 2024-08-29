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
package org.xwiki.security.authorization.testwikibuilding;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.xpn.xwiki.XWikiContext;

/**
 * This class is used for building a mocked test setup for testing the authorization component.
 *
 * Superclass for mocked test wikis built from xml sources.
 *
 * @since 4.2 
 * @version $Id$
 */
public abstract class AbstractTestWiki
{

    /** The subdirectory in the classpath were the test wiki definitions will be found. */
    private final static String TEST_WIKI_DEFINITIONS_DIRECTORY = "testwikis";

    /** The wiki description that is currently being parsed and built. */
    private HasWikiContents currentWiki;

    /** The space description that is currently being parsed and built. */
    private HasDocuments currentSpace;

    /**
     * The object (wiki, space or document) that have access control objects that is currently being parsed and built is
     * at the top of this stack.
     */
    private final Stack<HasAcl> currentRightsHolder = new Stack<HasAcl>();

    /**
     * The object (wiki or group) that have users that is currently being parsed and built is at the top of this stack.
     */
    private final Stack<HasUsers> currentUsersHolder = new Stack<HasUsers>();

    /**
     * Add a wiki definition.
     *
     * @param name The name of the wiki.
     * @param owner The owner of the wiki.
     * @param isMainWiki {@code true} if the wiki should be considered the main wiki.  It is an error unless exactly one
     * wiki in a test wiki setup is marked as the mainwiki.
     * @param isReadOnly Wether the wiki is in read only mode.
     */
    protected abstract HasWikiContents addWiki(String name, String owner, boolean isMainWiki, boolean isReadOnly,
        String alt);

    /**
     * @return The mocked context for this test wiki setup.
     */
    public abstract XWikiContext getXWikiContext();

    /**
     * Interface for xml sax parsing.
     */
    private interface ElementBuilder
    {

        /**
         * Indicate start of element.
         * @param attributes The xml attributes.
         */
        void startElement(Attributes attributes);

        /** Indicate end of element.  */
        void endElement();
    }

    /**
     * A SAX handler for building a test wiki.
     */
    private final class WikiBuilder extends DefaultHandler
    {

        /**
         * Abstract class for convenient inheritance.
         */
        private abstract class AbstractElementBuilder implements ElementBuilder
        {
            @Override
            public void startElement(Attributes attributes) {
            }

            @Override
            public void endElement() {
            }

        }

        /**
         * Abstract class for building a right declaration.
         */
        private abstract class AbstractRightElementBuilder extends AbstractElementBuilder
        {

            @Override
            public void startElement(Attributes attributes) {
                String name = attributes.getValue("name");
                String type = attributes.getValue("type");

                HasAcl rightsHolder = currentRightsHolder.peek();

                addRight(rightsHolder, name, type);
            }

            /**
             * Add a right to the rights holder.
             * @param rightsHolder The object (wiki, space or page) that can hold rights objects.
             * @param name The name (user- or group name) that should be assigned the right.
             * @param type The type of right (view, edit, etc.)
             */
            protected abstract void addRight(HasAcl rightsHolder, String name, String type);
        }

        /**
         * The map of all element builders.
         */
        private final Map<String, ElementBuilder> elementBuilders = new HashMap<String, ElementBuilder>();

        /**
         * Convenience class for declaring element builders.
         */
        private final class DeclareElementBuilders {

            /**
             * @param name The XML element name.
             * @param elementBuilder The builder instance.
             * @return this instance.
             */
            public DeclareElementBuilders declare(String name, ElementBuilder elementBuilder)
            {
                elementBuilders.put(name, elementBuilder);
                return this;
            }

        }

        {
            new DeclareElementBuilders()
                .declare(
                    "wikis",
                    new AbstractElementBuilder() {
                    })
                .declare(
                   "wiki",
                   new ElementBuilder() {

                       @Override
                       public void startElement(Attributes attributes) {
                           String name = attributes.getValue("name");
                           String owner = attributes.getValue("owner");
                           boolean isMainWiki = "true".equals(attributes.getValue("mainWiki"));
                           boolean isReadOnly = "true".equals(attributes.getValue("readOnly"));
                           String alt = attributes.getValue("alt");
                           HasWikiContents wiki = addWiki(name, owner, isMainWiki, isReadOnly, alt);

                           currentWiki = wiki;
                           currentRightsHolder.push(currentWiki);
                           currentUsersHolder.push(currentWiki);
                       }

                       @Override
                       public void endElement() {
                           currentWiki = null;
                           currentRightsHolder.pop();
                           currentUsersHolder.pop();
                       }
                   })
                .declare(
                   "user",
                   new AbstractElementBuilder() {

                       @Override
                       public void startElement(Attributes attributes) {
                           String name = attributes.getValue("name");

                           HasUsers usersHolder = currentUsersHolder.peek();
                           usersHolder.addUser(name);
                       }
                   })
                .declare(
                   "group",
                   new ElementBuilder() {

                       @Override
                       public void startElement(Attributes attributes) {
                           String name = attributes.getValue("name");

                           HasUsers group = currentWiki.addGroup(name);
                           currentUsersHolder.push(group);
                       }

                       @Override
                       public void endElement() {
                           currentUsersHolder.pop();
                       }
                   })
                .declare(
                   "space",
                   new ElementBuilder() {

                       @Override
                       public void startElement(Attributes attributes) {
                           String name = attributes.getValue("name");
                           String alt = attributes.getValue("alt");

                           currentSpace = currentWiki.addSpace(name, alt);
                           currentRightsHolder.push(currentSpace);
                       }

                       @Override
                       public void endElement() {
                           currentSpace = null;
                           currentRightsHolder.pop();
                       }
                   })
                .declare(
                   "document",
                   new ElementBuilder() {

                       @Override
                       public void startElement(Attributes attributes) {
                           String name = attributes.getValue("name");
                           String creator = attributes.getValue("creator");
                           String alt = attributes.getValue("alt");

                           if (creator == null) {
                               creator = "XWiki.Admin";
                           }

                           HasAcl document = currentSpace.addDocument(name, creator, alt);
                           currentRightsHolder.push(document);
                       }

                       @Override
                       public void endElement() {
                           currentRightsHolder.pop();
                       }
                   })
                .declare(
                   "allowUser",
                   new AbstractRightElementBuilder() {

                       @Override
                       public void addRight(HasAcl rightsHolder, String name, String type) {
                           rightsHolder.addAllowUser(name, type);
                       }
                   })
                .declare(
                   "denyUser",
                   new AbstractRightElementBuilder() {

                       @Override
                       public void addRight(HasAcl rightsHolder, String name, String type) {
                           rightsHolder.addDenyUser(name, type);
                       }
                   })
                .declare(
                   "allowGroup",
                   new AbstractRightElementBuilder() {

                       @Override
                       public void addRight(HasAcl rightsHolder, String name, String type) {
                           rightsHolder.addAllowGroup(name, type);
                       }
                   })
                .declare(
                   "denyGroup",
                   new AbstractRightElementBuilder() {

                       @Override
                       public void addRight(HasAcl rightsHolder, String name, String type) {
                           rightsHolder.addDenyGroup(name, type);
                       }
                   });

        }

        @Override
        public void startElement(String uri,
                                 String localName,
                                 String qName, 
                                 Attributes attributes) throws SAXException 
        {
            getElementBuilder(qName).startElement(attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName)
            throws SAXException {

            getElementBuilder(qName).endElement();
        }

        /**
         * @param qName The XML element name.
         * @return The element builder for the element name.
         * @throws SAXException if no element builder can be found for the element name.
         */
        private ElementBuilder getElementBuilder(String qName)
            throws SAXException
        {
            ElementBuilder elementBuilder = elementBuilders.get(qName);

            if (elementBuilder == null) {
                throw new SAXException(new Formatter().format("Invalid element name: '%s'", qName).toString());
            }

            return elementBuilder;
        }
    }

    /**
     * Load a test wiki from an xml-file located in the appropriate subdirectory in the classpath ({@link
     * TEST_WIKI_DEFINITIONS_DIRECTORY}).
     *
     * @param name The file name relative the test wiki subdirectory. 
     */
    protected void loadTestWiki(String name) throws Exception
    {
        URL testwikiUrl = ClassLoader.getSystemResource(TEST_WIKI_DEFINITIONS_DIRECTORY + File.separatorChar + name);

        if (testwikiUrl == null) {
            throw new FileNotFoundException(name);
        }

        URL schemaUrl = ClassLoader.getSystemResource("schemas" + File.separatorChar + "wikitest.xsd");

        Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaUrl);

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();

        parserFactory.setSchema(schema);

        SAXParser parser = parserFactory.newSAXParser();

        String filename = testwikiUrl.toURI().toString();

        parser.parse(filename, new WikiBuilder());
    }


}
