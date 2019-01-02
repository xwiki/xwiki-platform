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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.InterWikiResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

/**
 * Integration tests for {@link DefaultLinkReferenceParser} in the context of XWiki.
 *
 * @version $Id$
 * @since 2.6M1
 */
@AllComponents
public class DefaultLinkReferenceParserTest
{
    @Rule
    public final MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private ResourceReferenceParser parser;

    @Before
    public void setUp() throws Exception
    {
        this.parser = this.componentManager.getInstance(ResourceReferenceParser.class, "link");
    }

    @Test
    public void parseWhenInWikiMode() throws Exception
    {
        // Create a Mock WikiModel implementation so that the link parser works in wiki mode
        WikiModel mockWikiModel = this.componentManager.registerMockComponent(WikiModel.class);

        ResourceReference reference = this.parser.parse("");
        assertEquals("", reference.getReference());
        assertFalse(reference.isTyped());
        assertEquals(ResourceType.DOCUMENT, reference.getType());
        assertEquals("Typed = [false] Type = [doc] Reference = []", reference.toString());

        when(mockWikiModel.isDocumentAvailable(new DocumentResourceReference("existingpage"))).thenReturn(true);
        reference = this.parser.parse("existingpage");
        assertEquals("existingpage", reference.getReference());
        assertFalse(reference.isTyped());
        assertEquals(ResourceType.DOCUMENT, reference.getType());
        assertEquals("Typed = [false] Type = [doc] Reference = [existingpage]", reference.toString());

        reference = this.parser.parse("unexistingpage");
        assertEquals("unexistingpage", reference.getReference());
        assertFalse(reference.isTyped());
        assertEquals(ResourceType.DOCUMENT, reference.getType());
        assertEquals("Typed = [false] Type = [doc] Reference = [unexistingpage]", reference.toString());

        reference = this.parser.parse("space.unexistingpage");
        assertEquals("space.unexistingpage", reference.getReference());
        assertFalse(reference.isTyped());
        assertEquals(ResourceType.DOCUMENT, reference.getType());
        assertEquals("Typed = [false] Type = [doc] Reference = [space.unexistingpage]", reference.toString());

        reference = this.parser.parse("http://xwiki.org");
        assertEquals("http://xwiki.org", reference.getReference());
        assertFalse(reference.isTyped());
        assertEquals(ResourceType.URL, reference.getType());
        assertEquals("Typed = [false] Type = [url] Reference = [http://xwiki.org]", reference.toString());

        // Verify mailto: URI is recognized
        reference = this.parser.parse("mailto:john@smith.com?subject=test");
        assertEquals("john@smith.com?subject=test", reference.getReference());
        assertTrue(reference.isTyped());
        assertEquals(ResourceType.MAILTO, reference.getType());
        assertEquals("Typed = [true] Type = [mailto] Reference = [john@smith.com?subject=test]",
            reference.toString());

        // Verify attach: URI is recognized
        reference = this.parser.parse("attach:some:content");
        assertEquals("some:content", reference.getReference());
        assertTrue(reference.isTyped());
        assertEquals(ResourceType.ATTACHMENT, reference.getType());
        assertEquals("Typed = [true] Type = [attach] Reference = [some:content]", reference.toString());

        // Verify that unknown URIs are ignored
        // Note: In this example we point to a document and we consider that myxwiki is the wiki name and
        // http://xwiki.org is the page name
        reference = this.parser.parse("mywiki:http://xwiki.org");
        assertEquals("mywiki:http://xwiki.org", reference.getReference());
        assertFalse(reference.isTyped());
        assertEquals(ResourceType.DOCUMENT, reference.getType());
        assertEquals("Typed = [false] Type = [doc] Reference = [mywiki:http://xwiki.org]",
            reference.toString());

        // Verify doc links work
        reference = this.parser.parse("doc:wiki:space.page");
        assertEquals(ResourceType.DOCUMENT, reference.getType());
        assertEquals("wiki:space.page", reference.getReference());
        assertEquals("Typed = [true] Type = [doc] Reference = [wiki:space.page]", reference.toString());
        assertTrue(reference.isTyped());

        // Verify space links work
        reference = this.parser.parse("space:wiki:space");
        assertEquals(ResourceType.SPACE, reference.getType());
        assertEquals("wiki:space", reference.getReference());
        assertEquals("Typed = [true] Type = [space] Reference = [wiki:space]", reference.toString());
        assertTrue(reference.isTyped());

        // Verify page links work
        reference = this.parser.parse("page:wiki:page");
        assertEquals(ResourceType.PAGE, reference.getType());
        assertEquals("wiki:page", reference.getReference());
        assertEquals("Typed = [true] Type = [page] Reference = [wiki:page]", reference.toString());
        assertTrue(reference.isTyped());

        // Verify InterWiki links work
        reference = this.parser.parse("interwiki:alias:content");
        assertEquals(ResourceType.INTERWIKI, reference.getType());
        assertEquals("content", reference.getReference());
        assertTrue(reference.isTyped());
        assertEquals("alias", ((InterWikiResourceReference) reference).getInterWikiAlias());
        assertEquals("Typed = [true] Type = [interwiki] Reference = [content] "
            + "Parameters = [[interWikiAlias] = [alias]]", reference.toString());

        // Verify that an invalid InterWiki link is considered as Document link
        reference = this.parser.parse("interwiki:invalid_since_doesnt_have_colon");
        assertEquals(ResourceType.DOCUMENT, reference.getType());
        assertEquals("interwiki:invalid_since_doesnt_have_colon", reference.getReference());
        assertFalse(reference.isTyped());
        assertEquals("Typed = [false] Type = [doc] Reference = [interwiki:invalid_since_doesnt_have_colon]",
            reference.toString());

        // Verify typed URLs
        reference = this.parser.parse("url:http://xwiki.org");
        assertEquals(ResourceType.URL, reference.getType());
        assertTrue(reference.isTyped());
        assertEquals("http://xwiki.org", reference.getReference());
        assertEquals("Typed = [true] Type = [url] Reference = [http://xwiki.org]", reference.toString());

        // Verify query string and anchors have no meaning in link reference to documents.
        reference = this.parser.parse("Hello World?no=queryString#notAnAnchor");
        assertEquals(ResourceType.DOCUMENT, reference.getType());
        assertEquals("Hello World?no=queryString#notAnAnchor", reference.getReference());
        assertFalse(reference.isTyped());
        assertNull(((DocumentResourceReference) reference).getAnchor());
        assertNull(((DocumentResourceReference) reference).getQueryString());
        assertEquals("Typed = [false] Type = [doc] Reference = [Hello World?no=queryString#notAnAnchor]",
            reference.toString());

        // Verify that the interwiki separator from XWiki Syntax 2.0 has not meaning in link references to documents
        reference = this.parser.parse("page@alias");
        assertEquals(ResourceType.DOCUMENT, reference.getType());
        assertFalse(reference.isTyped());
        assertEquals("page@alias", reference.getReference());
        assertEquals("Typed = [false] Type = [doc] Reference = [page@alias]", reference.toString());

        // Verify path link types
        reference = this.parser.parse("path:/some/path");
        assertEquals(ResourceType.PATH, reference.getType());
        assertTrue(reference.isTyped());
        assertEquals("/some/path", reference.getReference());
        assertEquals("Typed = [true] Type = [path] Reference = [/some/path]", reference.toString());

        // Verify UNC link types
        reference = this.parser.parse("unc:\\\\myserver\\myshare\\mydoc.txt");
        assertEquals(ResourceType.UNC, reference.getType());
        assertTrue(reference.isTyped());
        assertEquals("\\\\myserver\\myshare\\mydoc.txt", reference.getReference());
        assertEquals("Typed = [true] Type = [unc] Reference = [\\\\myserver\\myshare\\mydoc.txt]",
            reference.toString());

        // Verify that reference escapes are left as is by the link parser
        reference = this.parser.parse("pa\\.ge");
        assertEquals(ResourceType.DOCUMENT, reference.getType());
        assertEquals("pa\\.ge", reference.getReference());
    }
}
