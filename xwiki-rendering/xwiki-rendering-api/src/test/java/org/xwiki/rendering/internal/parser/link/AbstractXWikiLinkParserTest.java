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
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.test.AbstractComponentTestCase;

public abstract class AbstractXWikiLinkParserTest extends AbstractComponentTestCase
{
    protected LinkParser parser;

    /**
     * Tests common to XWiki Syntax 2.0 and 2.1
     */
    @Test
    public void testParseLinksWhenInWikiModeCommon() throws Exception
    {
        Link link = parser.parse("");
        Assert.assertEquals("", link.getReference());
        Assert.assertFalse(link.isTyped());
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = []", link.toString());

        link = parser.parse("Hello World");
        Assert.assertEquals("Hello World", link.getReference());
        Assert.assertFalse(link.isTyped());
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [Hello World]", link.toString());

        link = parser.parse("http://xwiki.org");
        Assert.assertEquals("http://xwiki.org", link.getReference());
        Assert.assertFalse(link.isTyped());
        Assert.assertEquals(LinkType.URL, link.getType());
        Assert.assertEquals("Typed = [false] Type = [url] Reference = [http://xwiki.org]", link.toString());

        // Verify mailto: URI is recognized
        link = parser.parse("mailto:john@smith.com?subject=test");
        Assert.assertEquals("john@smith.com?subject=test", link.getReference());
        Assert.assertTrue(link.isTyped());
        Assert.assertEquals(LinkType.MAILTO, link.getType());
        Assert.assertEquals("Typed = [true] Type = [mailto] Reference = [john@smith.com?subject=test]",
            link.toString());

        // Verify image: URI is recognized
        link = parser.parse("image:some:content");
        Assert.assertEquals("some:content", link.getReference());
        Assert.assertTrue(link.isTyped());
        Assert.assertEquals(LinkType.IMAGE, link.getType());
        Assert.assertEquals("Typed = [true] Type = [image] Reference = [some:content]", link.toString());

        // Verify attach: URI is recognized
        link = parser.parse("attach:some:content");
        Assert.assertEquals("some:content", link.getReference());
        Assert.assertTrue(link.isTyped());
        Assert.assertEquals(LinkType.ATTACHMENT, link.getType());
        Assert.assertEquals("Typed = [true] Type = [attach] Reference = [some:content]", link.toString());

        // Verify that unknown URIs are ignored
        // Note: In this example we point to a document and we consider that myxwiki is the wiki name and
        // http://xwiki.org is the page name
        link = parser.parse("mywiki:http://xwiki.org");
        Assert.assertEquals("mywiki:http://xwiki.org", link.getReference());
        Assert.assertFalse(link.isTyped());
        Assert.assertEquals(LinkType.DOCUMENT, link.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [mywiki:http://xwiki.org]", link.toString());
    }
}
