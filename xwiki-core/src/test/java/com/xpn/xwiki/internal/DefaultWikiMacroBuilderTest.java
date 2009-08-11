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

package com.xpn.xwiki.internal;

import org.jmock.Mock;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroBuilder;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit test for {@link DefaultWikiMacroBuilder}.
 * 
 * @since 2.0M3
 * @version $Id$
 */
public class DefaultWikiMacroBuilderTest extends AbstractBridgedXWikiComponentTestCase
{
    private XWikiDocument macroDefinitionDoc;
    
    private Mock mockXWiki;
    
    private WikiMacroBuilder wikiMacroBuilder;
        
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.wikiMacroBuilder = getComponentManager().lookup(WikiMacroBuilder.class);
        
        // Build the macro definition document.
        BaseObject obj = new BaseObject();
        obj.setClassName("XWiki.WikiMacroClass");
        obj.setStringValue("id", "testmacro");
        obj.setStringValue("name", "Test Macro");
        obj.setStringValue("description", "This is a macro used for testing purposes.");
        obj.setStringValue("defaultCategory", "Test");
        obj.setIntValue("supportsInlineMode", 1);
        obj.setStringValue("contentType", "No content");
        obj.setStringValue("code", "==Hi==");
        macroDefinitionDoc = new XWikiDocument("xwiki", "Macros", "Test");
        macroDefinitionDoc.addObject("XWiki.WikiMacroClass", obj);
        
        // Setup the mock xwiki.
        this.mockXWiki = mock(XWiki.class);
        this.mockXWiki.stubs().method("getDocument").will(returnValue(macroDefinitionDoc));
        
        // Set this mock xwiki in context.
        getContext().setWiki((XWiki) mockXWiki.proxy());
    }
    
    public void testWikiMacroBuilding() throws Exception
    {
        // Build a wiki macro.
        WikiMacro macro = this.wikiMacroBuilder.buildMacro("xwiki:Macros.Test");
        assertNotNull(macro);
        
        // Check if the macro was built correctly.
        assertEquals("xwiki:Macros.Test", macro.getDocumentName());
        assertEquals("testmacro", macro.getId());
        assertEquals("Test Macro", macro.getDescriptor().getName());
        assertEquals("This is a macro used for testing purposes.", macro.getDescriptor().getDescription());
        assertEquals("Test", macro.getDescriptor().getDefaultCategory());
        assertTrue(macro.supportsInlineMode());
        assertNull(macro.getDescriptor().getContentDescriptor());        
    }
}
