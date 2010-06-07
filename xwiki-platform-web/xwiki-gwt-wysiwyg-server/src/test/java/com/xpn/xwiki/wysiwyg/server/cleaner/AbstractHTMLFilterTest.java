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
package com.xpn.xwiki.wysiwyg.server.cleaner;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xwiki.xml.html.filter.HTMLFilter;

/**
 * Base class for all test cases concerning {@link HTMLFilter} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractHTMLFilterTest extends TestCase
{
    /**
     * The text preceding the content of the document.
     */
    protected static final String PROLOGUE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    /**
     * The HTML filter being tested.
     */
    protected HTMLFilter filter;

    /**
     * Adds the XHTML envelope to the given XHTML fragment.
     * 
     * @param fragment the content to be placed inside the {@code body} tag
     * @return the given XHTML fragment wrapped in the XHTML envelope
     */
    protected String xhtmlFragment(String fragment)
    {
        return PROLOGUE + "<html><head><title>Test</title></head><body>" + fragment + "</body></html>\n";
    }

    /**
     * Parses the given XML and creates a {@link Document}.
     * 
     * @param xml the XML string to be parsed
     * @return the {@link Document} corresponding to the passed string
     * @throws Exception if parsing of the XML fails
     */
    protected Document parseXML(String xml) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    /**
     * Adds the XHTML envelope to the given XHTML fragment and creates a {@link Document} by parsing the result.
     * 
     * @param fragment the content to be placed inside the {@code body}
     * @return the {@link Document} corresponding to the given XHTML fragment
     * @throws Exception if parsing the XHTML fragment fails
     */
    protected Document parseXHTMLFragment(String fragment) throws Exception
    {
        return parseXML(xhtmlFragment(fragment));
    }
}
