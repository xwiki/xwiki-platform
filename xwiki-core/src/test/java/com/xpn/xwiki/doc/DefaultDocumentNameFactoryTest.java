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
package com.xpn.xwiki.doc;

import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameFactory;

import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for {@link DocumentNameFactory}.
 * 
 * @version $Id: $
 * @since 1.8.1
 */
public class DefaultDocumentNameFactoryTest extends AbstractBridgedXWikiComponentTestCase
{
    private DocumentNameFactory factory;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        this.factory = (DocumentNameFactory) getComponentManager().lookup(DocumentNameFactory.ROLE);
    }
    
    public void testCreateDocumentNameWhenCurrentDocSet() throws Exception
    {
        getContext().setDatabase("testwiki");
        XWikiDocument document = new XWikiDocument();
        document.setSpace("testspace");
        getContext().setDoc(document);
        verify("testwiki", "testspace");
    }

    public void testCreateDocumentNameWhenNoCurrentDoc() throws Exception
    {
        verify("xwiki", "XWiki");
    }
    
    private void verify(String expectedDefaultWiki, String expectedDefaultSpace)
    {
        DocumentName name = factory.createDocumentName("wiki:space.page");
        assertEquals("wiki", name.getWiki());
        assertEquals("space", name.getSpace());
        assertEquals("page", name.getPage());

        name = factory.createDocumentName("wiki1:wiki2:page");
        assertEquals("wiki1:wiki2", name.getWiki());
        assertEquals(expectedDefaultSpace, name.getSpace());
        assertEquals("page", name.getPage());

        name = factory.createDocumentName("wiki:");
        assertEquals("wiki", name.getWiki());
        assertEquals(expectedDefaultSpace, name.getSpace());
        assertEquals("WebHome", name.getPage());

        name = factory.createDocumentName("wiki1.wiki2:page");
        assertEquals("wiki1.wiki2", name.getWiki());
        assertEquals(expectedDefaultSpace, name.getSpace());
        assertEquals("page", name.getPage());

        name = factory.createDocumentName("wiki:page");
        assertEquals("wiki", name.getWiki());
        assertEquals(expectedDefaultSpace, name.getSpace());
        assertEquals("page", name.getPage());

        name = factory.createDocumentName("wiki:space.");
        assertEquals("wiki", name.getWiki());
        assertEquals("space", name.getSpace());
        assertEquals("WebHome", name.getPage());

        name = factory.createDocumentName("space.");
        assertEquals(expectedDefaultWiki, name.getWiki());
        assertEquals("space", name.getSpace());
        assertEquals("WebHome", name.getPage());

        name = factory.createDocumentName("page");
        assertEquals(expectedDefaultWiki, name.getWiki());
        assertEquals(expectedDefaultSpace, name.getSpace());
        assertEquals("page", name.getPage());

        name = factory.createDocumentName(".");
        assertEquals(expectedDefaultWiki, name.getWiki());
        assertEquals(expectedDefaultSpace, name.getSpace());
        assertEquals("WebHome", name.getPage());

        name = factory.createDocumentName(":");
        assertEquals(expectedDefaultWiki, name.getWiki());
        assertEquals(expectedDefaultSpace, name.getSpace());
        assertEquals("WebHome", name.getPage());

        name = factory.createDocumentName(null);
        assertEquals(expectedDefaultWiki, name.getWiki());
        assertEquals(expectedDefaultSpace, name.getSpace());
        assertEquals("WebHome", name.getPage());
        
        name = factory.createDocumentName("");
        assertEquals(expectedDefaultWiki, name.getWiki());
        assertEquals(expectedDefaultSpace, name.getSpace());
        assertEquals("WebHome", name.getPage());        

        name = factory.createDocumentName("wiki1.wiki2:wiki3:some.space.page");
        assertEquals("wiki1.wiki2:wiki3", name.getWiki());
        assertEquals("some.space", name.getSpace());
        assertEquals("page", name.getPage());

        name = factory.createDocumentName("some.space.page");
        assertEquals("some.space", name.getSpace());
        assertEquals("page", name.getPage());
    }
}
