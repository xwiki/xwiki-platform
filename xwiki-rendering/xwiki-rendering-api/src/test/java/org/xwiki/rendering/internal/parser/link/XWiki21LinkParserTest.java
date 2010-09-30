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
public class XWiki21LinkParserTest extends AbstractXWikiLinkParserTest
{
    @Override
    protected void registerComponents() throws Exception
    {
        // Create a Mock WikiModel implementation so that the link parser works in wiki mode
        registerMockComponent(WikiModel.class);

        this.parser = getComponentManager().lookup(LinkParser.class, "xwiki/2.1");
    }

    @Test
    public void testParseLinks() throws Exception
    {
        Link link = parser.parse("doc:wiki:space.page");
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("wiki:space.page", link.getReference());
        Assert.assertEquals("Typed = [true] Type = [doc] Reference = [wiki:space.page]", link.toString());
        Assert.assertTrue(link.isTyped());

        // Verify InterWiki links work
        link = parser.parse("interwiki:alias:content");
        Assert.assertEquals(LinkType.INTERWIKI, link.getType());
        Assert.assertEquals("content", link.getReference());
        Assert.assertTrue(link.isTyped());
        Assert.assertEquals("alias", ((InterWikiLink) link).getInterWikiAlias());
        Assert.assertEquals("Typed = [true] Type = [interwiki] Reference = [content] "
            + "Parameters = [[interWikiAlias] = [alias]]", link.toString());

        // Verify that an invalid InterWiki link is considered as Document link
        link = parser.parse("interwiki:invalid_since_doesnt_have_colon");
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("interwiki:invalid_since_doesnt_have_colon", link.getReference());
        Assert.assertFalse(link.isTyped());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [interwiki:invalid_since_doesnt_have_colon]",
            link.toString());

        // Verify typed URLs
        link = parser.parse("url:http://xwiki.org");
        Assert.assertEquals(LinkType.URL, link.getType());
        Assert.assertTrue(link.isTyped());
        Assert.assertEquals("http://xwiki.org", link.getReference());
        Assert.assertEquals("Typed = [true] Type = [url] Reference = [http://xwiki.org]", link.toString());

        // Verify query string and anchors have no meaning in link reference to documents.
        link = parser.parse("Hello World?no=queryString#notAnAnchor");
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("Hello World?no=queryString#notAnAnchor", link.getReference());
        Assert.assertFalse(link.isTyped());
        Assert.assertNull(((DocumentLink) link).getAnchor());
        Assert.assertNull(((DocumentLink) link).getQueryString());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [Hello World?no=queryString#notAnAnchor]",
                link.toString());

        // Verify that the interwiki separator from XWiki Syntax 2.0 has not meaning in link references to documents
        link = parser.parse("page@alias");
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertFalse(link.isTyped());
        Assert.assertEquals("page@alias", link.getReference());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [page@alias]", link.toString());

        // Verify path link types
        link = parser.parse("path:/some/path");
        Assert.assertEquals(LinkType.PATH, link.getType());
        Assert.assertTrue(link.isTyped());
        Assert.assertEquals("/some/path", link.getReference());
        Assert.assertEquals("Typed = [true] Type = [path] Reference = [/some/path]", link.toString());
    }

    @Test
    public void testParseLinksWithEscapes() throws Exception
    {
        // Veirfy that reference escapes are left as is by the link parser
        Link link = parser.parse("pa\\.ge");
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("pa\\.ge", link.getReference());
    }
}
