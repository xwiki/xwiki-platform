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
package org.xwiki.rendering.internal.parser.link;

import org.junit.*;
import org.xwiki.rendering.listener.DocumentLink;
import org.xwiki.rendering.listener.InterWikiLink;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.parser.link.XWiki20LinkParser}.
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class XWiki20LinkParserTest extends AbstractXWikiLinkParserTest
{
    @Override
    protected void registerComponents() throws Exception
    {
        // Create a Mock WikiModel implementation so that the link parser works in wiki mode
        registerMockComponent(WikiModel.class);
        
        this.parser = getComponentManager().lookup(LinkParser.class, "xwiki/2.0");
    }

    @Test
    public void testParseLinksWhenInWikiMode() throws Exception
    {
        // Test Query Strings in links to document
        Link link = parser.parse("Hello World?xredirect=../whatever");
        Assert.assertEquals("Hello World", link.getReference());
        Assert.assertEquals("xredirect=../whatever", ((DocumentLink) link).getQueryString());
        Assert.assertFalse(link.isTyped());
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [Hello World] "
            + "Parameters = [[queryString] = [xredirect=../whatever]]", link.toString());

        link = parser.parse("HelloWorld?xredirect=http://xwiki.org");
        Assert.assertEquals("HelloWorld", link.getReference());
        Assert.assertEquals("xredirect=http://xwiki.org", ((DocumentLink) link).getQueryString());
        Assert.assertFalse(link.isTyped());
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [HelloWorld] "
            + "Parameters = [[queryString] = [xredirect=http://xwiki.org]]", link.toString());

        // Test Anchors in links to documents
        link = parser.parse("#anchor");
        Assert.assertEquals("anchor", ((DocumentLink) link).getAnchor());
        Assert.assertFalse(link.isTyped());
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [] Parameters = [[anchor] = [anchor]]",
            link.toString());

        link = parser.parse("Hello#anchor");
        Assert.assertEquals("Hello", link.getReference());
        Assert.assertEquals("anchor", ((DocumentLink) link).getAnchor());
        Assert.assertFalse(link.isTyped());
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [Hello] Parameters = [[anchor] = [anchor]]",
            link.toString());

        // Test InterWiki links
        link = parser.parse("HelloWorld#anchor?param1=1&param2=2@wikipedia");
        Assert.assertEquals("HelloWorld#anchor?param1=1&param2=2", link.getReference());
        Assert.assertEquals("wikipedia", ((InterWikiLink) link).getInterWikiAlias());
        Assert.assertTrue(link.isTyped());
        Assert.assertEquals(LinkType.INTERWIKI, link.getType());
        Assert.assertEquals("Typed = [true] Type = [interwiki] Reference = [HelloWorld#anchor?param1=1&param2=2] "
            + "Parameters = [[interWikiAlias] = [wikipedia]]", link.toString());

        // Verify in XWiki Syntax 2.0 the "doc" prefix is not meaningful
        link = parser.parse("doc:whatever");
        Assert.assertEquals("doc:whatever", link.getReference());
        Assert.assertFalse(link.isTyped());
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [doc:whatever]", link.toString());
    }

    @Test
    public void testParseLinksWithEscapes() throws Exception
    {
        Link link = parser.parse("\\.\\#notanchor");
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("\\.#notanchor", link.getReference());
        Assert.assertNull(((DocumentLink) link).getAnchor());

        link = parser.parse("page\\?notquerystring");
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("page?notquerystring", link.getReference());
        Assert.assertNull(((DocumentLink) link).getQueryString());

        // Verify that \ can be escaped and that escaped chars in query string, and anchors are escaped
        link = parser.parse("page\\\\#anchor\\\\?querystring\\\\");
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("page\\\\", link.getReference());
        Assert.assertEquals("anchor\\", ((DocumentLink) link).getAnchor());
        Assert.assertEquals("querystring\\", ((DocumentLink) link).getQueryString());

        link = parser.parse("pa\\.ge\\?query\\#anchor\\@notinterwiki");
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("pa\\.ge?query#anchor@notinterwiki", link.getReference());

    	// Verify that \ can be escaped and that escaped chars in query string, anchors and InterWiki aliases are
    	// escaped.
        link = parser.parse("page\\\\#anchor\\\\?querystring\\\\@alias\\\\");
        Assert.assertEquals(LinkType.INTERWIKI, link.getType());
        Assert.assertEquals("page\\#anchor\\?querystring\\", link.getReference());
        Assert.assertEquals("alias\\", ((InterWikiLink) link).getInterWikiAlias());

        link = parser.parse("something\\\\@inter\\@wikilink");
        Assert.assertEquals(LinkType.INTERWIKI, link.getType());
        Assert.assertEquals("something\\", link.getReference());
        Assert.assertEquals("inter@wikilink", ((InterWikiLink) link).getInterWikiAlias());
    }
}
