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
package org.xwiki.rendering.wikimacro.internal;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;

import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultWikiMacroFactory}.
 * 
 * @since 2.0M3
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
public class DefaultWikiMacroFactoryTest
{
    private XWikiDocument macroDefinitionDoc;

    @Mock
    private XWiki mockXWiki;

    @InjectMockComponents
    private DefaultWikiMacroFactory wikiMacroFactory;

    @MockComponent
    private BlockAsyncRendererExecutor rendererExecutor;

    @MockComponent
    private ContentParser contentParser;

    @BeforeEach
    protected void setUp() throws Exception
    {
        // Build the macro definition document.
        BaseObject obj = new BaseObject();
        obj.setClassName("XWiki.WikiMacroClass");
        obj.setStringValue("id", "testmacro");
        obj.setStringValue("name", "Test Macro");
        obj.setStringValue("description", "This is a macro used for testing purposes.");
        obj.setStringValue("defaultCategory", "Test");
        obj.setStringValue("visibility", "Current User");
        obj.setIntValue("supportsInlineMode", 1);
        obj.setStringValue("contentType", "No content");
        obj.setStringValue("code", "==Hi==");
        macroDefinitionDoc = new XWikiDocument(new DocumentReference("xwiki", "Macros", "Test"));
        macroDefinitionDoc.addObject("XWiki.WikiMacroClass", obj);

        // Setup the mock xwiki.
        when(this.mockXWiki.getDocument(any(DocumentReference.class), any())).thenReturn(macroDefinitionDoc);

        // Set this mock xwiki in context.
        Utils.getContext().setWiki(this.mockXWiki);
    }

    @Test
    public void createWikiMacro() throws Exception
    {
        // Build a wiki macro.
        WikiMacro macro = this.wikiMacroFactory.createWikiMacro(new DocumentReference("xwiki", "Macros", "Test"));
        assertNotNull(macro);

        // Check if the macro was built correctly.
        assertEquals("testmacro", macro.getId());
        assertEquals("Test Macro", macro.getDescriptor().getName());
        assertEquals("This is a macro used for testing purposes.", macro.getDescriptor().getDescription());
        assertEquals("Test", macro.getDescriptor().getDefaultCategory());
        assertEquals(WikiMacroVisibility.USER, ((WikiMacroDescriptor) macro.getDescriptor()).getVisibility());
        assertTrue(macro.supportsInlineMode());
        assertNull(macro.getDescriptor().getContentDescriptor());

        // Verify that the wiki macro descriptor has a macro id without a syntax since wiki macros are registered for
        // all syntaxes.
        assertNull(macro.getDescriptor().getId().getSyntax());
    }

    @Test
    public void createWikiMacroWithoutName() throws Exception
    {
        BaseObject obj = macroDefinitionDoc.getObject("XWiki.WikiMacroClass");
        obj.setStringValue("name", "");

        // Build a wiki macro.
        WikiMacro macro = this.wikiMacroFactory.createWikiMacro(new DocumentReference("xwiki", "Macros", "Test"));
        assertNotNull(macro);

        // Check if the macro was built correctly.
        assertEquals("testmacro", macro.getDescriptor().getName());
    }

    @Test
    public void createWikiMacroWithContent() throws Exception
    {
        BaseObject obj = macroDefinitionDoc.getObject("XWiki.WikiMacroClass");
        obj.setStringValue("contentType", "Optional");
        obj.setStringValue("contentDescription", "the content of the macro");

        // Build a wiki macro.
        WikiMacro macro = this.wikiMacroFactory.createWikiMacro(new DocumentReference("xwiki", "Macros", "Test"));
        assertNotNull(macro);

        ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();
        // Check if the macro was built correctly.
        assertNotNull(contentDescriptor);
        assertFalse(contentDescriptor.isMandatory());
        assertEquals("the content of the macro", contentDescriptor.getDescription());
        assertEquals(DefaultContentDescriptor.DEFAULT_CONTENT_TYPE, contentDescriptor.getType());
    }

    @Test
    public void createWikiMacroWithContentWikiType() throws Exception
    {
        BaseObject obj = macroDefinitionDoc.getObject("XWiki.WikiMacroClass");
        obj.setStringValue("contentType", "Mandatory");
        obj.setStringValue("contentDescription", "the content of the macro");
        obj.setStringValue("contentJavaType", "Wiki");

        // Build a wiki macro.
        WikiMacro macro = this.wikiMacroFactory.createWikiMacro(new DocumentReference("xwiki", "Macros", "Test"));
        assertNotNull(macro);

        ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();
        // Check if the macro was built correctly.
        assertNotNull(contentDescriptor);
        assertTrue(contentDescriptor.isMandatory());
        assertEquals("the content of the macro", contentDescriptor.getDescription());
        assertEquals(Block.LIST_BLOCK_TYPE, contentDescriptor.getType());
    }

    @Test
    public void createWikiMacroWithContentCustomType() throws Exception
    {
        BaseObject obj = macroDefinitionDoc.getObject("XWiki.WikiMacroClass");
        obj.setStringValue("contentType", "Mandatory");
        obj.setStringValue("contentDescription", "the content of the macro");
        obj.setStringValue("contentJavaType", "java.util.Map<java.lang.String, java.util.Set<java.lang.Integer>>");

        // Build a wiki macro.
        WikiMacro macro = this.wikiMacroFactory.createWikiMacro(new DocumentReference("xwiki", "Macros", "Test"));
        assertNotNull(macro);

        ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();
        // Check if the macro was built correctly.
        assertNotNull(contentDescriptor);
        assertTrue(contentDescriptor.isMandatory());
        assertEquals("the content of the macro", contentDescriptor.getDescription());
        Type expectedType = new DefaultParameterizedType(null, Map.class, String.class,
            new DefaultParameterizedType(null, Set.class, Integer.class));
        assertEquals(expectedType, contentDescriptor.getType());
    }
}
