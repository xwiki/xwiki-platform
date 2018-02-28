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
package org.xwiki.rendering.internal.macro.context;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.properties.BeanDescriptor;
import org.xwiki.properties.BeanManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.context.ContextMacroParameters;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ContextMacro}.
 *
 * @version $Id$
 * @since 8.3RC1
 */
public class ContextMacroTest
{
    @Rule
    public MockitoComponentMockingRule<ContextMacro> mocker = new MockitoComponentMockingRule(ContextMacro.class);

    @Before
    public void setUp() throws Exception
    {
        // Macro Descriptor set up
        BeanManager beanManager = this.mocker.getInstance(BeanManager.class);
        BeanDescriptor descriptor = mock(BeanDescriptor.class);
        when(descriptor.getProperties()).thenReturn(Collections.emptyList());
        when(beanManager.getBeanDescriptor(any())).thenReturn(descriptor);
    }

    @Test
    public void executeWhenNoDocumentSpecified() throws Exception
    {
        ContextMacroParameters parameters = new ContextMacroParameters();

        try {
            this.mocker.getComponentUnderTest().execute(parameters, "", new MacroTransformationContext());
            fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("You must specify a 'document' parameter pointing to the document to set in the "
                + "context as the current document.", expected.getMessage());
        }
    }

    @Test
    public void executeWithReferencedDocumentHavingProgrammingRightsButNotTheCallingDocument() throws Exception
    {
        MacroTransformationContext macroContext = new MacroTransformationContext();
        MacroBlock macroBlock = new MacroBlock("context", Collections.emptyMap(), false);
        macroContext.setCurrentMacroBlock(macroBlock);

        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "macro");
        when(resolver.resolve("wiki:space.page", macroBlock)).thenReturn(
            new DocumentReference("wiki", "space", "page"));

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(dab.hasProgrammingRights()).thenReturn(false).thenReturn(true);

        ContextMacroParameters parameters = new ContextMacroParameters();
        parameters.setDocument("wiki:space.page");

        try {
            this.mocker.getComponentUnderTest().execute(parameters, "", macroContext);
            fail("Should have thrown an exception");
        } catch (MacroExecutionException expected) {
            assertEquals("Current document must have programming rights since the context document provided ["
                + "wiki:space.page] has programming rights.", expected.getMessage());
        }
    }

    @Test
    public void executeWithReferencedDocumentHavingProgrammingRightsAndCallingDocumentToo() throws Exception
    {
        MacroBlock macroBlock = new MacroBlock("context", Collections.<String, String>emptyMap(), false);
        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setSyntax(Syntax.XWIKI_2_0);
        macroContext.setCurrentMacroBlock(macroBlock);

        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "macro");
        DocumentReference referencedDocumentReference = new DocumentReference("wiki", "space", "page");
        when(resolver.resolve("wiki:space.page", macroBlock)).thenReturn(referencedDocumentReference);

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(dab.hasProgrammingRights()).thenReturn(true).thenReturn(true);
        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(dab.getTranslatedDocumentInstance(referencedDocumentReference)).thenReturn(dmb);

        MacroContentParser parser = this.mocker.getInstance(MacroContentParser.class);
        when(parser.parse(eq(""), same(macroContext), eq(false), any(MetaData.class), eq(false))).thenReturn(
            new XDOM(Collections.emptyList()));

        ContextMacroParameters parameters = new ContextMacroParameters();
        parameters.setDocument("wiki:space.page");

        this.mocker.getComponentUnderTest().execute(parameters, "", macroContext);
    }

    @Test
    public void executeOk() throws Exception
    {
        MacroBlock macroBlock = new MacroBlock("context", Collections.<String, String>emptyMap(), false);
        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setSyntax(Syntax.XWIKI_2_0);
        macroContext.setCurrentMacroBlock(macroBlock);

        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "macro");
        DocumentReference referencedDocumentReference = new DocumentReference("wiki", "space", "page");
        when(resolver.resolve("wiki:space.page", macroBlock)).thenReturn(referencedDocumentReference);

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(dab.getTranslatedDocumentInstance(referencedDocumentReference)).thenReturn(dmb);

        MacroContentParser parser = this.mocker.getInstance(MacroContentParser.class);
        XDOM xdom = new XDOM(Arrays.asList((Block) new ParagraphBlock(Arrays.asList((Block) new LinkBlock(
            Collections.emptyList(), new ResourceReference("", ResourceType.DOCUMENT), false)))));
        when(parser.parse(eq(""), same(macroContext), eq(false), any(MetaData.class), eq(false))).thenReturn(xdom);

        ContextMacroParameters parameters = new ContextMacroParameters();
        parameters.setDocument("wiki:space.page");

        // Note: we're not testing the returned value here since this is done in integation tests.
        this.mocker.getComponentUnderTest().execute(parameters, "", macroContext);
    }

    @Test
    public void executeWithRelativeDocumentReferenceParameter() throws Exception
    {
        MacroBlock macroBlock = new MacroBlock("context", Collections.<String, String>emptyMap(), false);

        MacroTransformationContext macroContext = new MacroTransformationContext();
        macroContext.setSyntax(Syntax.XWIKI_2_0);
        macroContext.setCurrentMacroBlock(macroBlock);

        DocumentReferenceResolver<String> resolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "macro");
        DocumentReference referencedDocumentReference = new DocumentReference("basewiki", "basespace", "page");
        when(resolver.resolve("page", macroBlock)).thenReturn(referencedDocumentReference);

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(dab.getTranslatedDocumentInstance(referencedDocumentReference)).thenReturn(dmb);

        MacroContentParser parser = this.mocker.getInstance(MacroContentParser.class);
        when(parser.parse(eq(""), same(macroContext), eq(false), any(MetaData.class), eq(false))).thenReturn(
            new XDOM(Collections.emptyList()));

        ContextMacroParameters parameters = new ContextMacroParameters();
        parameters.setDocument("page");

        this.mocker.getComponentUnderTest().execute(parameters, "", macroContext);
    }
}
