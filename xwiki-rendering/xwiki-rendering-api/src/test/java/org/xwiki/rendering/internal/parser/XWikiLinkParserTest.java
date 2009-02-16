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
package org.xwiki.rendering.internal.parser;

import junit.framework.TestCase;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.parser.LinkParser;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class XWikiLinkParserTest extends TestCase
{
    public void testParseLinks()
    {
        LinkParser parser = new XWikiLinkParser();

        Link link = parser.parse("");
        assertEquals("", link.getReference());
        assertEquals("Reference = []", link.toString());

        link = parser.parse("Hello World");
        assertEquals("Hello World", link.getReference());
        assertEquals(LinkType.DOCUMENT, link.getType());
        assertEquals("Reference = [Hello World]", link.toString());

        link = parser.parse("HelloWorld#anchor?param1=1&param2=2@wikipedia");
        assertEquals("HelloWorld", link.getReference());
        assertEquals("anchor", link.getAnchor());
        assertEquals("param1=1&param2=2", link.getQueryString());
        assertEquals("wikipedia", link.getInterWikiAlias());
        assertEquals("Reference = [HelloWorld] QueryString = [param1=1&param2=2] "
            + "Anchor = [anchor] InterWikiAlias = [wikipedia]", link.toString());

        link = parser.parse("Hello World?xredirect=../whatever");
        assertEquals("Hello World", link.getReference());
        assertEquals("xredirect=../whatever", link.getQueryString());
        assertEquals("Reference = [Hello World] QueryString = [xredirect=../whatever]", link.toString());

        link = parser.parse("HelloWorld?xredirect=http://xwiki.org");
        assertEquals("HelloWorld", link.getReference());
        assertEquals("xredirect=http://xwiki.org", link.getQueryString());
        assertEquals("Reference = [HelloWorld] QueryString = [xredirect=http://xwiki.org]", link.toString());

        link = parser.parse("http://xwiki.org");
        assertEquals("http://xwiki.org", link.getReference());
        assertEquals(LinkType.URI, link.getType());
        assertEquals("Reference = [http://xwiki.org]", link.toString());

        link = parser.parse("#anchor");
        assertEquals("anchor", link.getAnchor());
        assertEquals("Reference = [] Anchor = [anchor]", link.toString());

        link = parser.parse("Hello#anchor");
        assertEquals("Hello", link.getReference());
        assertEquals("anchor", link.getAnchor());
        assertEquals("Reference = [Hello] Anchor = [anchor]", link.toString());

        // Verify mailto: URI is recognized
        link = parser.parse("mailto:john@smith.com");
        assertEquals("mailto:john@smith.com", link.getReference());
        assertEquals(LinkType.URI, link.getType());
        assertEquals("Reference = [mailto:john@smith.com]", link.toString());
        
        // Verify image: URI is recognized
        link = parser.parse("image:some:content");
        assertEquals("image:some:content", link.getReference());
        assertEquals(LinkType.URI, link.getType());
        assertEquals("Reference = [image:some:content]", link.toString());

        // Verify attach: URI is recognized
        link = parser.parse("attach:some:content");
        assertEquals("attach:some:content", link.getReference());
        assertEquals(LinkType.URI, link.getType());
        assertEquals("Reference = [attach:some:content]", link.toString());
        
        // Verify that unknown URIs are ignored
        // Note: We consider that myxwiki is the wiki name and http://xwiki.org is the page name
        link = parser.parse("mywiki:http://xwiki.org");
        assertEquals("mywiki:http://xwiki.org", link.getReference());
        assertEquals(LinkType.DOCUMENT, link.getType());
        assertEquals("Reference = [mywiki:http://xwiki.org]", link.toString());
        
}
}
