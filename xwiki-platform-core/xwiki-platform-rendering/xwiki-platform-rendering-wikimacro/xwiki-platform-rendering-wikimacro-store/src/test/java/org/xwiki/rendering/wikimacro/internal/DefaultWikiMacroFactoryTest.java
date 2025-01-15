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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultWikiMacroFactory}.
 * 
 * @since 2.0M3
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList(DefaultWikiMacro.class)
class DefaultWikiMacroFactoryTest
{
    private final static DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Macros", "Test");

    private BaseObject macroObject;

    private XWikiDocument macroDefinitionDoc;

    @InjectMockComponents
    private DefaultWikiMacroFactory wikiMacroFactory;

    @MockComponent
    private BlockAsyncRendererExecutor rendererExecutor;

    @MockComponent
    private ContentParser contentParser;

    @MockComponent
    @Named("macro")
    protected Transformation macroTransformation;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @BeforeEach
    protected void setUp() throws Exception
    {
        // Build the macro definition document.
        this.macroObject = new BaseObject();
        this.macroObject.setXClassReference(WikiMacroConstants.WIKI_MACRO_CLASS_REFERENCE);
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_ID_PROPERTY, "testmacro");
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_NAME_PROPERTY, "Test Macro");
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_DESCRIPTION_PROPERTY,
            "This is a macro used for testing purposes.");
        this.macroObject.setStringListValue(WikiMacroConstants.MACRO_DEFAULT_CATEGORIES_PROPERTY, List.of("Test"));
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_VISIBILITY_PROPERTY, "Current User");
        this.macroObject.setIntValue(WikiMacroConstants.MACRO_INLINE_PROPERTY, 1);
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_CONTENT_TYPE_PROPERTY, "No content");
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_CODE_PROPERTY, "==Hi==");
        this.macroObject.setIntValue(WikiMacroConstants.MACRO_PRIORITY_PROPERTY, 42);
        this.macroDefinitionDoc =
            this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.oldcore.getXWikiContext());
        this.macroDefinitionDoc.addXObject(this.macroObject);
        saveDocument();
    }

    private void saveDocument()
    {
        try {
            this.oldcore.getSpyXWiki().saveDocument(this.macroDefinitionDoc, this.oldcore.getXWikiContext());
        } catch (XWikiException e) {
            fail(e);
        }
    }

    @Test
    void createWikiMacro() throws Exception
    {
        // Build a wiki macro.
        WikiMacro macro = this.wikiMacroFactory.createWikiMacro(DOCUMENT_REFERENCE);
        assertNotNull(macro);

        // Check if the macro was built correctly.
        assertEquals("testmacro", macro.getId());
        assertEquals("testmacro", macro.getDescriptor().getId().getId());
        assertEquals("Test Macro", macro.getDescriptor().getName());
        assertEquals(42, macro.getPriority());
        assertEquals("This is a macro used for testing purposes.", macro.getDescriptor().getDescription());
        assertEquals(Set.of("Test"), macro.getDescriptor().getDefaultCategories());
        assertEquals(WikiMacroVisibility.USER, ((WikiMacroDescriptor) macro.getDescriptor()).getVisibility());
        assertTrue(macro.supportsInlineMode());
        assertNull(macro.getDescriptor().getContentDescriptor());
        assertTrue(macro.getDescriptor().getParameterDescriptorMap().isEmpty());

        // Verify that the wiki macro descriptor has a macro id without a syntax since wiki macros are registered for
        // all syntaxes.
        assertNull(macro.getDescriptor().getId().getSyntax());
    }

    @Test
    void isAllowed() throws Exception
    {
        assertTrue(this.wikiMacroFactory.isAllowed(DOCUMENT_REFERENCE, WikiMacroVisibility.USER));
        assertFalse(this.wikiMacroFactory.isAllowed(DOCUMENT_REFERENCE, WikiMacroVisibility.WIKI));
        assertFalse(this.wikiMacroFactory.isAllowed(DOCUMENT_REFERENCE, WikiMacroVisibility.GLOBAL));

        when(this.oldcore.getMockDocumentAuthorizationManager()
            .hasAccess(Right.ADMIN, EntityType.WIKI, null, DOCUMENT_REFERENCE)).thenReturn(true);

        assertTrue(this.wikiMacroFactory.isAllowed(DOCUMENT_REFERENCE, WikiMacroVisibility.USER));
        assertTrue(this.wikiMacroFactory.isAllowed(DOCUMENT_REFERENCE, WikiMacroVisibility.WIKI));
        assertFalse(this.wikiMacroFactory.isAllowed(DOCUMENT_REFERENCE, WikiMacroVisibility.GLOBAL));

        when(this.oldcore.getMockDocumentAuthorizationManager()
            .hasAccess(Right.PROGRAM, null, null, DOCUMENT_REFERENCE)).thenReturn(true);

        assertTrue(this.wikiMacroFactory.isAllowed(DOCUMENT_REFERENCE, WikiMacroVisibility.USER));
        assertTrue(this.wikiMacroFactory.isAllowed(DOCUMENT_REFERENCE, WikiMacroVisibility.WIKI));
        assertTrue(this.wikiMacroFactory.isAllowed(DOCUMENT_REFERENCE, WikiMacroVisibility.GLOBAL));
    }

    @Test
    void createWikiMacroWithoutName() throws Exception
    {
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_NAME_PROPERTY, "");
        saveDocument();

        // Build a wiki macro.
        WikiMacro macro = this.wikiMacroFactory.createWikiMacro(DOCUMENT_REFERENCE);
        assertNotNull(macro);

        // Check if the macro was built correctly.
        assertEquals("testmacro", macro.getDescriptor().getName());
    }

    @Test
    void createWikiMacroWithContent() throws Exception
    {
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_CONTENT_TYPE_PROPERTY, "Optional");
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_CONTENT_DESCRIPTION_PROPERTY,
            "the content of the macro");
        saveDocument();

        // Build a wiki macro.
        WikiMacro macro = this.wikiMacroFactory.createWikiMacro(DOCUMENT_REFERENCE);
        assertNotNull(macro);

        ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();
        // Check if the macro was built correctly.
        assertNotNull(contentDescriptor);
        assertFalse(contentDescriptor.isMandatory());
        assertEquals("the content of the macro", contentDescriptor.getDescription());
        assertEquals(DefaultContentDescriptor.DEFAULT_CONTENT_TYPE, contentDescriptor.getType());
    }

    @Test
    void createWikiMacroWithEmptyCode() throws Exception
    {
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_CODE_PROPERTY, "");
        saveDocument();

        // Build a wiki macro.
        assertThrows(WikiMacroException.class, () -> this.wikiMacroFactory.createWikiMacro(DOCUMENT_REFERENCE));
    }

    @Test
    void createWikiMacroWithContentWikiType() throws Exception
    {
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_CONTENT_TYPE_PROPERTY, "Mandatory");
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_CONTENT_DESCRIPTION_PROPERTY,
            "the content of the macro");
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_CONTENT_JAVA_TYPE_PROPERTY, "Wiki");
        saveDocument();

        // Build a wiki macro.
        WikiMacro macro = this.wikiMacroFactory.createWikiMacro(DOCUMENT_REFERENCE);
        assertNotNull(macro);

        ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();
        // Check if the macro was built correctly.
        assertNotNull(contentDescriptor);
        assertTrue(contentDescriptor.isMandatory());
        assertEquals("the content of the macro", contentDescriptor.getDescription());
        assertEquals(Block.LIST_BLOCK_TYPE, contentDescriptor.getType());
    }

    @Test
    void createWikiMacroWithContentCustomType() throws Exception
    {
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_CONTENT_TYPE_PROPERTY, "Mandatory");
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_CONTENT_DESCRIPTION_PROPERTY,
            "the content of the macro");
        this.macroObject.setStringValue(WikiMacroConstants.MACRO_CONTENT_JAVA_TYPE_PROPERTY,
            "java.util.Map<java.lang.String, java.util.Set<java.lang.Integer>>");
        saveDocument();

        // Build a wiki macro.
        WikiMacro macro = this.wikiMacroFactory.createWikiMacro(DOCUMENT_REFERENCE);
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

    @Test
    void containsWikiMacro()
    {
        assertTrue(this.wikiMacroFactory.containsWikiMacro(DOCUMENT_REFERENCE));

        this.macroDefinitionDoc.removeXObject(this.macroObject);
        saveDocument();

        assertFalse(this.wikiMacroFactory.containsWikiMacro(DOCUMENT_REFERENCE));
    }
}
