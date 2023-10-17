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

import java.util.List;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.transformation.Transformation;
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
import static org.junit.jupiter.api.Assertions.fail;
import static org.xwiki.rendering.wikimacro.internal.LegacyWikiMacroConstants.MACRO_DEFAULT_CATEGORY_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_CODE_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_CONTENT_TYPE_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_DEFAULT_CATEGORIES_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_DESCRIPTION_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_ID_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_INLINE_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_NAME_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_PRIORITY_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.MACRO_VISIBILITY_PROPERTY;
import static org.xwiki.rendering.wikimacro.internal.WikiMacroConstants.WIKI_MACRO_CLASS_REFERENCE;

/**
 * Test of {@link DefaultWikiMacroFactoryAspect}.
 *
 * @version $Id$
 * @since 14.6RC1
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList(DefaultWikiMacro.class)
class DefaultWikiMacroFactoryAspectTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Macros", "Test");

    @InjectMockComponents
    private DefaultWikiMacroFactory wikiMacroFactory;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private BlockAsyncRendererExecutor rendererExecutor;

    @MockComponent
    private ContentParser contentParser;

    @MockComponent
    @Named("macro")
    protected Transformation macroTransformation;

    private BaseObject macroObject;

    private XWikiDocument macroDefinitionDoc;

    private void attachMacroObject() throws XWikiException
    {
     
        this.macroDefinitionDoc =
            this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.oldcore.getXWikiContext());
        this.macroDefinitionDoc.addXObject(this.macroObject);
    }

    @Test
    void createWikiMacro() throws Exception
    {
        initMacroObject();
        attachMacroObject();
        saveDocument();
        WikiMacro wikiMacro = this.wikiMacroFactory.createWikiMacro(DOCUMENT_REFERENCE);
        assertEquals(Set.of("CatA"), wikiMacro.getDescriptor().getDefaultCategories());
    }

    @Test
    void createWikiMacroWithDefaultCategories() throws Exception
    {
        initMacroObject();
        this.macroObject.setStringListValue(MACRO_DEFAULT_CATEGORIES_PROPERTY, List.of("Cat1", "Cat2"));
        attachMacroObject();
        saveDocument();
        WikiMacro wikiMacro = this.wikiMacroFactory.createWikiMacro(DOCUMENT_REFERENCE);
        assertEquals(Set.of("Cat1", "Cat2"), wikiMacro.getDescriptor().getDefaultCategories());
    }

    private void initMacroObject()
    {
        this.macroObject = new BaseObject();
        this.macroObject.setXClassReference(WIKI_MACRO_CLASS_REFERENCE);
        this.macroObject.setStringValue(MACRO_ID_PROPERTY, "testmacro");
        this.macroObject.setStringValue(MACRO_NAME_PROPERTY, "Test Macro");
        this.macroObject.setStringValue(MACRO_DESCRIPTION_PROPERTY, "This is a macro used for testing purposes.");
        this.macroObject.setStringValue(MACRO_DEFAULT_CATEGORY_PROPERTY, "CatA");
        this.macroObject.setStringValue(MACRO_VISIBILITY_PROPERTY, "Current User");
        this.macroObject.setIntValue(MACRO_INLINE_PROPERTY, 1);
        this.macroObject.setStringValue(MACRO_CONTENT_TYPE_PROPERTY, "No content");
        this.macroObject.setStringValue(MACRO_CODE_PROPERTY, "==Hi==");
        this.macroObject.setIntValue(MACRO_PRIORITY_PROPERTY, 42);
    }

    private void saveDocument()
    {
        try {
            this.oldcore.getSpyXWiki().saveDocument(this.macroDefinitionDoc, this.oldcore.getXWikiContext());
        } catch (XWikiException e) {
            fail(e);
        }
    }
}
