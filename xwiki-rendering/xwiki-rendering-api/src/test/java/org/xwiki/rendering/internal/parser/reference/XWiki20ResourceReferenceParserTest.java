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
package org.xwiki.rendering.internal.parser.reference;

import org.junit.*;
import org.xwiki.rendering.listener.DocumentResourceReference;
import org.xwiki.rendering.listener.InterWikiResourceReference;
import org.xwiki.rendering.listener.ResourceReference;
import org.xwiki.rendering.listener.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.wiki.WikiModel;

/**
 * Unit tests for {@link XWiki20ResourceReferenceParser}.
 * 
 * @version $Id$
 * @since 2.5M2
 */
public class XWiki20ResourceReferenceParserTest extends AbstractResourceReferenceParserTest
{
    @Override
    protected void registerComponents() throws Exception
    {
        // Create a Mock WikiModel implementation so that the link parser works in wiki mode
        registerMockComponent(WikiModel.class);
        
        this.parser = getComponentManager().lookup(ResourceReferenceParser.class, "xwiki/2.0");
    }

    @Test
    public void testParseLinksWhenInWikiMode() throws Exception
    {
        // Test Query Strings in links to document
        ResourceReference reference = parser.parse("Hello World?xredirect=../whatever");
        Assert.assertEquals("Hello World", reference.getReference());
        Assert.assertEquals("xredirect=../whatever", ((DocumentResourceReference) reference).getQueryString());
        Assert.assertFalse(reference.isTyped());
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [Hello World] "
            + "Parameters = [[queryString] = [xredirect=../whatever]]", reference.toString());

        reference = parser.parse("HelloWorld?xredirect=http://xwiki.org");
        Assert.assertEquals("HelloWorld", reference.getReference());
        Assert.assertEquals("xredirect=http://xwiki.org", ((DocumentResourceReference) reference).getQueryString());
        Assert.assertFalse(reference.isTyped());
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [HelloWorld] "
            + "Parameters = [[queryString] = [xredirect=http://xwiki.org]]", reference.toString());

        // Test Anchors in links to documents
        reference = parser.parse("#anchor");
        Assert.assertEquals("anchor", ((DocumentResourceReference) reference).getAnchor());
        Assert.assertFalse(reference.isTyped());
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [] Parameters = [[anchor] = [anchor]]",
            reference.toString());

        reference = parser.parse("Hello#anchor");
        Assert.assertEquals("Hello", reference.getReference());
        Assert.assertEquals("anchor", ((DocumentResourceReference) reference).getAnchor());
        Assert.assertFalse(reference.isTyped());
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [Hello] Parameters = [[anchor] = [anchor]]",
            reference.toString());

        // Test InterWiki links
        reference = parser.parse("HelloWorld#anchor?param1=1&param2=2@wikipedia");
        Assert.assertEquals("HelloWorld#anchor?param1=1&param2=2", reference.getReference());
        Assert.assertEquals("wikipedia", ((InterWikiResourceReference) reference).getInterWikiAlias());
        Assert.assertTrue(reference.isTyped());
        Assert.assertEquals(ResourceType.INTERWIKI, reference.getType());
        Assert.assertEquals("Typed = [true] Type = [interwiki] Reference = [HelloWorld#anchor?param1=1&param2=2] "
            + "Parameters = [[interWikiAlias] = [wikipedia]]", reference.toString());

        // Verify in XWiki Syntax 2.0 the "doc" prefix is not meaningful
        reference = parser.parse("doc:whatever");
        Assert.assertEquals("doc:whatever", reference.getReference());
        Assert.assertFalse(reference.isTyped());
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [doc:whatever]", reference.toString());
    }

    @Test
    public void testParseLinksWithEscapes() throws Exception
    {
        ResourceReference reference = parser.parse("\\.\\#notanchor");
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("\\.#notanchor", reference.getReference());
        Assert.assertNull(((DocumentResourceReference) reference).getAnchor());

        reference = parser.parse("page\\?notquerystring");
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("page?notquerystring", reference.getReference());
        Assert.assertNull(((DocumentResourceReference) reference).getQueryString());

        // Verify that \ can be escaped and that escaped chars in query string, and anchors are escaped
        reference = parser.parse("page\\\\#anchor\\\\?querystring\\\\");
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("page\\\\", reference.getReference());
        Assert.assertEquals("anchor\\", ((DocumentResourceReference) reference).getAnchor());
        Assert.assertEquals("querystring\\", ((DocumentResourceReference) reference).getQueryString());

        reference = parser.parse("pa\\.ge\\?query\\#anchor\\@notinterwiki");
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("pa\\.ge?query#anchor@notinterwiki", reference.getReference());

    	// Verify that \ can be escaped and that escaped chars in query string, anchors and InterWiki aliases are
    	// escaped.
        reference = parser.parse("page\\\\#anchor\\\\?querystring\\\\@alias\\\\");
        Assert.assertEquals(ResourceType.INTERWIKI, reference.getType());
        Assert.assertEquals("page\\#anchor\\?querystring\\", reference.getReference());
        Assert.assertEquals("alias\\", ((InterWikiResourceReference) reference).getInterWikiAlias());

        reference = parser.parse("something\\\\@inter\\@wikilink");
        Assert.assertEquals(ResourceType.INTERWIKI, reference.getType());
        Assert.assertEquals("something\\", reference.getReference());
        Assert.assertEquals("inter@wikilink", ((InterWikiResourceReference) reference).getInterWikiAlias());
    }
}
