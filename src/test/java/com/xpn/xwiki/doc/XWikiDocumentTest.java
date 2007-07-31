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
package com.xpn.xwiki.doc;

import java.util.Date;

import org.jmock.cglib.MockObjectTestCase;
import org.jmock.Mock;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;

/**
 * Unit tests for {@link XWikiDocument}.
 *
 * @version $Id: $
 */
public class XWikiDocumentTest extends MockObjectTestCase
{
    private XWikiContext context;
    private XWikiDocument document;
    private Mock mockXWiki;
    private Mock mockXWikiRenderingEngine;
    private Mock mockXWikiVersioningStore;
    
    protected void setUp()
    {
        this.context = new XWikiContext();
        this.document = new XWikiDocument("Space", "Page");

        this.mockXWiki = mock(XWiki.class, new Class[] {XWikiConfig.class, XWikiContext.class},
            new Object[] {new XWikiConfig(), this.context});
        this.mockXWiki.stubs().method("Param").will(returnValue(null));

        this.mockXWikiRenderingEngine = mock(XWikiRenderingEngine.class);
        
        this.mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new Class[] {XWiki.class,
            XWikiContext.class}, new Object[] {this.mockXWiki.proxy(), this.context});
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(
            returnValue(null));

        this.mockXWiki.stubs().method("getRenderingEngine").will(returnValue(
            this.mockXWikiRenderingEngine.proxy()));
        this.mockXWiki.stubs().method("getVersioningStore").will(
            returnValue(this.mockXWikiVersioningStore.proxy()));
        
        this.context.setWiki((XWiki) this.mockXWiki.proxy());
    }

    public void testGetDisplayTitleWhenNoTitleAndNoContent()
    {
        this.document.setContent("Some content");

        assertEquals("Page", this.document.getDisplayTitle(this.context));
    }

    public void testGetDisplayWhenTitleExists()
    {
        this.document.setContent("Some content");
        this.document.setTitle("Title");
        this.mockXWikiRenderingEngine.expects(once()).method("interpretText")
            .with(eq("Title"), ANYTHING, ANYTHING).will(returnValue("Title"));

        assertEquals("Title", this.document.getDisplayTitle(this.context));
    }

    public void testGetDisplayWhenNoTitleButSectionExists()
    {
        this.document.setContent("Some content\n1 Title");
        this.mockXWikiRenderingEngine.expects(once()).method("interpretText")
            .with(eq("Title"), ANYTHING, ANYTHING).will(returnValue("Title"));

        assertEquals("Title", this.document.getDisplayTitle(this.context));
    }
    
    public void testAuthorAfterDocumentCopy() throws XWikiException
    {
        String author = "Albatross";
        this.document.setAuthor(author);
        XWikiDocument copy =
            this.document.copyDocument(this.document.getName() + " Copy", this.context);

        assertTrue(author.equals(copy.getAuthor()));
    }

    public void testCreatorAfterDocumentCopy() throws XWikiException
    {
        String creator = "Condor";
        this.document.setCreator(creator);
        XWikiDocument copy =
            this.document.copyDocument(this.document.getName() + " Copy", this.context);

        assertTrue(creator.equals(copy.getCreator()));
    }
    
    public void testCreationDateAfterDocumentCopy() throws XWikiException, InterruptedException
    {
        Date sourceCreationDate = this.document.getCreationDate();
        Thread.sleep(1000);
        XWikiDocument copy =
            this.document.copyDocument(this.document.getName() + " Copy", this.context);
        
        assertTrue(copy.getCreationDate().equals(sourceCreationDate));
    }
}
