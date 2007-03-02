/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.content;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;
import org.radeox.filter.interwiki.InterWiki;

/**
 * Unit tests for {@link com.xpn.xwiki.content.Link}.
 *
 * @version $Id: $
 */
public class LinkTest extends MockObjectTestCase
{
    private Link link;
    private XWikiContext context;
    private Mock mockDocument;
    private Mock mockXWiki;

    protected void setUp()
    {
        this.link = new Link();

        this.context = new XWikiContext();

        this.mockDocument = mock(XWikiDocument.class);

        this.mockXWiki = mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class},
            new Object[] {new XWikiConfig(), this.context});

        this.context.setDoc((XWikiDocument) this.mockDocument.proxy());
        this.context.setWiki((XWiki) this.mockXWiki.proxy());
    }

    public void testGetNormalizedNameWhenEmptyLink() throws Exception
    {
        this.mockDocument.expects(once()).method("getSpace").will(returnValue("Main"));
        this.mockXWiki.expects(once()).method("exists").will(returnValue(true));
        
        assertEquals("Main.WebHome", this.link.getNormalizedName(this.context));
    }

    public void testGetNormalizedNameWhenSpaceInPageName() throws Exception
    {
        this.link.setPage("Main+Page");
        this.mockDocument.expects(once()).method("getSpace").will(returnValue("Main"));
        this.mockXWiki.expects(once()).method("exists").will(returnValue(true));

        assertEquals("Main.Main Page", this.link.getNormalizedName(this.context));
    }

    public void testGetNormalizedNameWithAccentedCharactersAndWithExistingAccentedPage()
        throws Exception
    {
        this.link.setSpace("Space");
        this.link.setPage("àéè");
        this.mockXWiki.expects(once()).method("exists").will(returnValue(true));

        assertEquals("Space.àéè", this.link.getNormalizedName(this.context));
    }

    public void testGetNormalizedNameWithAccentedCharactersAndWithNonExistingAccentedPage()
        throws Exception
    {
        this.link.setSpace("Space");
        this.link.setPage("hello àéè");
        this.mockXWiki.expects(once()).method("exists").will(returnValue(false));

        assertEquals("Space.helloaee", this.link.getNormalizedName(this.context));
    }

    public void testGetNormalizedNameWithVirtualWikiAlias()
        throws Exception
    {
        this.link.setVirtualWikiAlias("wiki");
        this.link.setSpace("Space");
        this.link.setPage("Page");
        this.mockXWiki.expects(once()).method("exists").will(returnValue(false));

        assertEquals("Space.Page", this.link.getNormalizedName(this.context));
    }

    public void testGetNormalizedNameWithNonExistentInterWikiAlias()
    {
        Mock mockInterWiki = mock(InterWiki.class);
        mockInterWiki.expects(once()).method("contains").will(returnValue(false));
        this.link.setInterWikiManager((InterWiki) mockInterWiki.proxy());

        this.link.setInterWikiAlias("wikipedia");
        this.link.setPage("Page");

        try {
            this.link.getNormalizedName(this.context);
            fail("Should have thrown an exception here");
        } catch (XWikiException expected) {
            assertEquals("Error number 2 in 17: Failed to find URL for inter wiki link "
                + "[wikipedia]. Make sure you have configured InterWiki correctly in your wiki.",
                expected.getMessage());
        }
    }

    public void testGetNormalizedNameWithExistingInterWikiAlias() throws XWikiException
    {
        Mock mockInterWiki = mock(InterWiki.class);
        mockInterWiki.expects(once()).method("contains").will(returnValue(true));
        mockInterWiki.expects(once()).method("getWikiUrl").will(
            returnValue("http://wikipipedia/Page"));
        this.link.setInterWikiManager((InterWiki) mockInterWiki.proxy());

        this.link.setInterWikiAlias("wikipedia");
        this.link.setPage("Page");

        assertEquals("http://wikipipedia/Page", this.link.getNormalizedName(this.context));
    }
}
