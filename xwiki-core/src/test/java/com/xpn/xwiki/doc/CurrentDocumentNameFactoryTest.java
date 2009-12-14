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

import org.xwiki.model.DocumentName;
import org.xwiki.model.DocumentNameFactory;

import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for {@link CurrentDocumentNameFactory}.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
public class CurrentDocumentNameFactoryTest extends AbstractBridgedXWikiComponentTestCase
{
    private DocumentNameFactory factory;
    
    protected void setUp() throws Exception
    {
        super.setUp();
        this.factory = getComponentManager().lookup(DocumentNameFactory.class, "current");
    }

    public void testCreateDocumentNameWhenCurrentDocSet() throws Exception
    {
        getContext().setDatabase("testwiki");
        XWikiDocument document = new XWikiDocument("testspace", "testpage");
        getContext().setDoc(document);
        verify("testpage");
    }

    public void testCreateDocumentNameWhenNoCurrentDoc() throws Exception
    {
        verify("WebHome");
    }
    
    private void verify(String expectedDefaultPage)
    {
        // Note: we don't retest what's already tested in DefaultDocumentNameFactoryTest.
        
        DocumentName name = factory.createDocumentName("wiki:");
        assertEquals(expectedDefaultPage, name.getPage());

        name = factory.createDocumentName("wiki:space.");
        assertEquals(expectedDefaultPage, name.getPage());

        name = factory.createDocumentName("space.");
        assertEquals(expectedDefaultPage, name.getPage());

        name = factory.createDocumentName(".");
        assertEquals(expectedDefaultPage, name.getPage());

        name = factory.createDocumentName(":");
        assertEquals(expectedDefaultPage, name.getPage());

        name = factory.createDocumentName(null);
        assertEquals(expectedDefaultPage, name.getPage());
        
        name = factory.createDocumentName("");
        assertEquals(expectedDefaultPage, name.getPage());        
    }
}
