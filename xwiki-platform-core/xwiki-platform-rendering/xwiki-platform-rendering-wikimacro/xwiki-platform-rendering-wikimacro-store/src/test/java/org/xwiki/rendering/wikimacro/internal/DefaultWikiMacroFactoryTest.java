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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.PropertyGroupDescriptor;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
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
    private MacroContentParser macroContentParser;

    @MockComponent
    @Named("macro")
    protected Transformation macroTransformation;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @BeforeEach
    void setUp() throws Exception
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

        // add parameters

        // first param is mandatory and deprecated, of type unknown with default value 42
        BaseObject param1 = new BaseObject();
        param1.setXClassReference(WikiMacroConstants.WIKI_MACRO_PARAMETER_CLASS_REFERENCE);
        param1.setStringValue(WikiMacroConstants.PARAMETER_NAME_PROPERTY, "param1");
        param1.setStringValue(WikiMacroConstants.PARAMETER_DESCRIPTION_PROPERTY, "First parameter description");
        param1.setIntValue(WikiMacroConstants.PARAMETER_MANDATORY_PROPERTY, 1);
        param1.setIntValue(WikiMacroConstants.PARAMETER_DEPRECATED_PROPERTY, 1);
        param1.setStringValue(WikiMacroConstants.PARAMETER_DEFAULT_VALUE_PROPERTY, "42");
        this.macroDefinitionDoc.addXObject(param1);

        // second param is optional, advanced, it belongs to group foo and expose mandatory feature bar and is of type
        // wiki
        BaseObject param2 = new BaseObject();
        param2.setXClassReference(WikiMacroConstants.WIKI_MACRO_PARAMETER_CLASS_REFERENCE);
        param2.setStringValue(WikiMacroConstants.PARAMETER_NAME_PROPERTY, "param2");
        param2.setStringValue(WikiMacroConstants.PARAMETER_DESCRIPTION_PROPERTY, "Second parameter description");
        param2.setIntValue(WikiMacroConstants.PARAMETER_MANDATORY_PROPERTY, 0);
        param2.setIntValue(WikiMacroConstants.PARAMETER_ADVANCED_PROPERTY, 1);
        param2.setStringValue(WikiMacroConstants.PARAMETER_TYPE_PROPERTY, WikiMacroConstants.PARAMETER_TYPE_WIKI);
        param2.setStringListValue(WikiMacroConstants.PARAMETER_GROUP_PROPERTY, List.of("foo"));
        param2.setStringValue(WikiMacroConstants.PARAMETER_FEATURE_PROPERTY, "bar");
        param2.setIntValue(WikiMacroConstants.PARAMETER_FEATURE_MANDATORY_PROPERTY, 1);
        this.macroDefinitionDoc.addXObject(param2);

        // third param is optional, hidden, it belongs to group foo
        BaseObject param3 = new BaseObject();
        param3.setXClassReference(WikiMacroConstants.WIKI_MACRO_PARAMETER_CLASS_REFERENCE);
        param3.setStringValue(WikiMacroConstants.PARAMETER_NAME_PROPERTY, "param3");
        param3.setStringValue(WikiMacroConstants.PARAMETER_DESCRIPTION_PROPERTY, "Third parameter description");
        param3.setIntValue(WikiMacroConstants.PARAMETER_HIDDEN_PROPERTY, 1);
        param3.setStringListValue(WikiMacroConstants.PARAMETER_GROUP_PROPERTY, List.of("foo"));
        this.macroDefinitionDoc.addXObject(param3);

        // fourth param is mandatory, deprecated, and advanced, belongs to groups foo and buz
        BaseObject param4 = new BaseObject();
        param4.setXClassReference(WikiMacroConstants.WIKI_MACRO_PARAMETER_CLASS_REFERENCE);
        param4.setStringValue(WikiMacroConstants.PARAMETER_NAME_PROPERTY, "param4");
        param4.setStringValue(WikiMacroConstants.PARAMETER_DESCRIPTION_PROPERTY, "Fourth parameter description");
        param4.setIntValue(WikiMacroConstants.PARAMETER_MANDATORY_PROPERTY, 1);
        param4.setIntValue(WikiMacroConstants.PARAMETER_ADVANCED_PROPERTY, 1);
        param4.setIntValue(WikiMacroConstants.PARAMETER_DEPRECATED_PROPERTY, 1);
        param4.setStringListValue(WikiMacroConstants.PARAMETER_GROUP_PROPERTY, List.of("foo", "buz"));
        this.macroDefinitionDoc.addXObject(param4);

        // Fifth parameter is optional, of type java.util.List, as default value [1,2], bound to feature foo
        BaseObject param5 = new BaseObject();
        param5.setXClassReference(WikiMacroConstants.WIKI_MACRO_PARAMETER_CLASS_REFERENCE);
        param5.setStringValue(WikiMacroConstants.PARAMETER_NAME_PROPERTY, "param5");
        param5.setStringValue(WikiMacroConstants.PARAMETER_TYPE_PROPERTY, "java.util.List");
        param5.setStringValue(WikiMacroConstants.PARAMETER_DEFAULT_VALUE_PROPERTY, "1,2");
        param5.setStringValue(WikiMacroConstants.PARAMETER_FEATURE_PROPERTY, "foo");
        this.macroDefinitionDoc.addXObject(param5);

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

        WikiMacroParameterDescriptor descriptor1 =
            new WikiMacroParameterDescriptor("param1", "First parameter description", true, "42", String.class,
                Map.of(WikiMacroParameterDescriptor.DEPRECATED_PARAMETER_NAME, true));

        PropertyGroupDescriptor fooGroupDescriptor = new PropertyGroupDescriptor(List.of("foo"));
        fooGroupDescriptor.setFeature("bar");
        fooGroupDescriptor.setFeatureMandatory(true);
        WikiMacroParameterDescriptor descriptor2 =
            new WikiMacroParameterDescriptor("param2", "Second parameter description", false, null,
                Block.LIST_BLOCK_TYPE, Map.of(
                    WikiMacroParameterDescriptor.ADVANCED_PARAMETER_NAME, true,
                    WikiMacroParameterDescriptor.GROUP_PARAMETER_NAME, fooGroupDescriptor));

        WikiMacroParameterDescriptor descriptor3 =
            new WikiMacroParameterDescriptor("param3", "Third parameter description", false, null, String.class, Map.of(
                WikiMacroParameterDescriptor.HIDDEN_PARAMETER_NAME, true,
                WikiMacroParameterDescriptor.GROUP_PARAMETER_NAME, fooGroupDescriptor));

        PropertyGroupDescriptor fooBuzGroupDescriptor = new PropertyGroupDescriptor(List.of("foo", "buz"));
        WikiMacroParameterDescriptor descriptor4 =
            new WikiMacroParameterDescriptor("param4", "Fourth parameter description", true, null, String.class, Map.of(
                WikiMacroParameterDescriptor.ADVANCED_PARAMETER_NAME, true,
                WikiMacroParameterDescriptor.DEPRECATED_PARAMETER_NAME, true,
                WikiMacroParameterDescriptor.GROUP_PARAMETER_NAME, fooBuzGroupDescriptor));

        PropertyGroupDescriptor featureFooGroupDescriptor = new PropertyGroupDescriptor(null);
        featureFooGroupDescriptor.setFeature("foo");
        WikiMacroParameterDescriptor descriptor5 =
            new WikiMacroParameterDescriptor("param5", "", false, "1,2", List.class, Map.of(
                WikiMacroParameterDescriptor.GROUP_PARAMETER_NAME, featureFooGroupDescriptor));

        Map<String, WikiMacroParameterDescriptor> descriptorMap = new LinkedHashMap<>();
        descriptorMap.put("param1", descriptor1);
        descriptorMap.put("param2", descriptor2);
        descriptorMap.put("param3", descriptor3);
        descriptorMap.put("param4", descriptor4);
        descriptorMap.put("param5", descriptor5);

        assertEquals(descriptorMap, macro.getDescriptor().getParameterDescriptorMap());

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

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void createWikiMacroWithIsolatedExecution(boolean isolated) throws Exception
    {
        this.macroObject.setIntValue(WikiMacroConstants.MACRO_EXECUTION_ISOLATED_PROPERTY, isolated ? 1 : 0);
        saveDocument();

        WikiMacro macro = this.wikiMacroFactory.createWikiMacro(DOCUMENT_REFERENCE);
        assertNotNull(macro);

        assertEquals(isolated, macro.isExecutionIsolated(new WikiMacroParameters(), "test"));
    }
}
