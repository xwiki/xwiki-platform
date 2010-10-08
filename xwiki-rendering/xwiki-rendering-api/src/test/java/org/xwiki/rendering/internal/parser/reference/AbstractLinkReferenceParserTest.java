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
import org.xwiki.rendering.listener.ResourceReference;
import org.xwiki.rendering.listener.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Common tests for Link implementations of {@link org.xwiki.rendering.parser.ResourceReferenceParser} for
 * XWiki Syntax 2.0 and 2.1.
 * 
 * @version $Id$
 * @since 2.5RC1
 */
public abstract class AbstractLinkReferenceParserTest extends AbstractComponentTestCase
{
    protected ResourceReferenceParser parser;

    @Test
    public void testParseLinksWhenInWikiModeCommon() throws Exception
    {
        ResourceReference reference = parser.parse("");
        Assert.assertEquals("", reference.getReference());
        Assert.assertFalse(reference.isTyped());
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = []", reference.toString());

        reference = parser.parse("Hello World");
        Assert.assertEquals("Hello World", reference.getReference());
        Assert.assertFalse(reference.isTyped());
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [Hello World]", reference.toString());

        reference = parser.parse("http://xwiki.org");
        Assert.assertEquals("http://xwiki.org", reference.getReference());
        Assert.assertFalse(reference.isTyped());
        Assert.assertEquals(ResourceType.URL, reference.getType());
        Assert.assertEquals("Typed = [false] Type = [url] Reference = [http://xwiki.org]", reference.toString());

        // Verify mailto: URI is recognized
        reference = parser.parse("mailto:john@smith.com?subject=test");
        Assert.assertEquals("john@smith.com?subject=test", reference.getReference());
        Assert.assertTrue(reference.isTyped());
        Assert.assertEquals(ResourceType.MAILTO, reference.getType());
        Assert.assertEquals("Typed = [true] Type = [mailto] Reference = [john@smith.com?subject=test]",
            reference.toString());

        // Verify image: URI is recognized
        reference = parser.parse("image:some:content");
        Assert.assertEquals("some:content", reference.getReference());
        Assert.assertTrue(reference.isTyped());
        Assert.assertEquals(ResourceType.IMAGE, reference.getType());
        Assert.assertEquals("Typed = [true] Type = [image] Reference = [some:content]", reference.toString());

        // Verify attach: URI is recognized
        reference = parser.parse("attach:some:content");
        Assert.assertEquals("some:content", reference.getReference());
        Assert.assertTrue(reference.isTyped());
        Assert.assertEquals(ResourceType.ATTACHMENT, reference.getType());
        Assert.assertEquals("Typed = [true] Type = [attach] Reference = [some:content]", reference.toString());

        // Verify that unknown URIs are ignored
        // Note: In this example we point to a document and we consider that myxwiki is the wiki name and
        // http://xwiki.org is the page name
        reference = parser.parse("mywiki:http://xwiki.org");
        Assert.assertEquals("mywiki:http://xwiki.org", reference.getReference());
        Assert.assertFalse(reference.isTyped());
        Assert.assertEquals(ResourceType.DOCUMENT, reference.getType());
        Assert.assertEquals("Typed = [false] Type = [doc] Reference = [mywiki:http://xwiki.org]", reference.toString());
    }
}
