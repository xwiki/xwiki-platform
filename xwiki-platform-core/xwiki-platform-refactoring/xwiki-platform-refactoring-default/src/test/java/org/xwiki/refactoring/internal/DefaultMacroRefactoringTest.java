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
package org.xwiki.refactoring.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.ReferenceRenamer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.LinkParser;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.MacroRefactoringException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultMacroRefactoring}.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@ComponentTest
class DefaultMacroRefactoringTest
{
    @InjectMockComponents
    private DefaultMacroRefactoring macroRefactoring;

    @MockComponent
    private MacroManager macroManager;

    @MockComponent
    private MacroContentParser macroContentParser;

    @MockComponent
    private Provider<ReferenceRenamer> referenceRenamerProvider;

    @MockComponent
    private RenderingContext renderingContext;

    @MockComponent
    private Provider<LinkParser> linkParserProvider;

    private ReferenceRenamer referenceRenamer;
    private MacroBlock macroBlock;
    private Syntax syntax;
    private ContentDescriptor contentDescriptor;
    private String macroId;
    private DocumentReference currentDocumentReference;
    private DocumentReference sourceReference;
    private DocumentReference targetReference;
    private BlockRenderer blockRenderer;
    private LinkParser linkParser;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @BeforeEach
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        this.referenceRenamer = mock(ReferenceRenamer.class);
        when(this.referenceRenamerProvider.get()).thenReturn(this.referenceRenamer);

        this.linkParser = mock(LinkParser.class);
        when(this.linkParserProvider.get()).thenReturn(this.linkParser);

        this.macroBlock = mock(MacroBlock.class);
        this.macroId = "macroId";
        when(this.macroBlock.getId()).thenReturn(this.macroId);
        this.syntax = mock(Syntax.class);
        Macro macro = mock(Macro.class);
        when(this.macroManager.getMacro(new MacroId(this.macroId, this.syntax))).thenReturn(macro);
        MacroDescriptor macroDescriptor = mock(MacroDescriptor.class);
        when(macro.getDescriptor()).thenReturn(macroDescriptor);
        this.contentDescriptor = mock(ContentDescriptor.class);
        when(macroDescriptor.getContentDescriptor()).thenReturn(this.contentDescriptor);
        this.currentDocumentReference = mock(DocumentReference.class);
        this.sourceReference = mock(DocumentReference.class);
        this.targetReference = mock(DocumentReference.class);

        String syntaxId = "syntaxId";
        when(this.syntax.toIdString()).thenReturn(syntaxId);
        this.blockRenderer = componentManager.registerMockComponent(BlockRenderer.class, syntaxId);
        when(this.renderingContext.getDefaultSyntax()).thenReturn(syntax);
    }

    @Test
    void replaceReferenceUnparseableMacro() throws MacroRefactoringException
    {
        when(this.contentDescriptor.getType()).thenReturn(String.class);
        when(this.macroContentParser.getCurrentSyntax(any())).thenAnswer(invocationOnMock -> {
            MacroTransformationContext transformationContext = invocationOnMock.getArgument(0);
            assertEquals(transformationContext.getId(), "refactoring_" + macroId);
            assertEquals(macroBlock, transformationContext.getCurrentMacroBlock());
            assertEquals(this.syntax, transformationContext.getSyntax());
            assertFalse(transformationContext.isInline());
            return this.syntax;
        });
        assertEquals(Optional.empty(),
            this.macroRefactoring.replaceReference(this.macroBlock, this.currentDocumentReference,
                this.sourceReference, this.targetReference, true, Map.of()));
        assertEquals(Optional.empty(),
            this.macroRefactoring.replaceReference(this.macroBlock, this.currentDocumentReference,
                this.sourceReference, this.targetReference, false, Map.of()));
        verify(this.referenceRenamer, never()).renameReferences(any(), any(), any(DocumentReference.class), any(),
            anyBoolean(), any());
    }

    @Test
    void replaceReferenceNotUpdated() throws Exception
    {
        when(this.contentDescriptor.getType()).thenReturn(Block.LIST_BLOCK_TYPE);
        when(this.macroBlock.isInline()).thenReturn(false);
        String macroContent = "some macro content";
        when(this.macroBlock.getContent()).thenReturn(macroContent);

        XDOM xdom = mock(XDOM.class);
        when(this.macroContentParser.getCurrentSyntax(any())).thenAnswer(invocationOnMock -> {
            MacroTransformationContext transformationContext = invocationOnMock.getArgument(0);
            assertEquals(transformationContext.getId(), "refactoring_" + macroId);
            assertEquals(macroBlock, transformationContext.getCurrentMacroBlock());
            assertEquals(this.syntax, transformationContext.getSyntax());
            assertFalse(transformationContext.isInline());
            return this.syntax;
        });
        when(this.macroContentParser.parse(eq(macroContent), any(), eq(true), eq(false)))
            .thenAnswer(invocationOnMock -> {
            MacroTransformationContext transformationContext = invocationOnMock.getArgument(1);
            assertEquals(transformationContext.getId(), "refactoring_" + macroId);
            assertEquals(macroBlock, transformationContext.getCurrentMacroBlock());
            assertEquals(this.syntax, transformationContext.getSyntax());
            assertFalse(transformationContext.isInline());
            return xdom;
        });
        when(this.referenceRenamer.renameReferences(xdom, this.currentDocumentReference, this.sourceReference,
            this.targetReference, true, Map.of())).thenReturn(false);
        assertEquals(Optional.empty(),
            this.macroRefactoring.replaceReference(this.macroBlock, this.currentDocumentReference, this.sourceReference,
                this.targetReference, true, Map.of()));
        verify(this.blockRenderer, never()).render(any(Block.class), any());
    }

    @Test
    void replaceReferenceUpdated() throws Exception
    {
        when(this.contentDescriptor.getType()).thenReturn(Block.LIST_BLOCK_TYPE);
        when(this.macroBlock.isInline()).thenReturn(false);
        String macroContent = "some macro content";
        when(this.macroBlock.getContent()).thenReturn(macroContent);
        XDOM xdom = mock(XDOM.class);
        when(this.macroContentParser.parse(eq(macroContent), any(), eq(true), eq(false)))
            .thenAnswer(invocationOnMock -> {
            MacroTransformationContext transformationContext = invocationOnMock.getArgument(1);
            assertEquals(transformationContext.getId(), "refactoring_" + macroId);
            assertEquals(macroBlock, transformationContext.getCurrentMacroBlock());
            assertEquals(this.syntax, transformationContext.getSyntax());
            assertFalse(transformationContext.isInline());
            return xdom;
        });
        when(this.referenceRenamer.renameReferences(xdom, this.currentDocumentReference, this.sourceReference,
            this.targetReference, true, Map.of())).thenReturn(true);
        String expectedContent = "the expected content";
        doAnswer(invocationOnMock -> {
            WikiPrinter printer = invocationOnMock.getArgument(1);
            printer.print(expectedContent);
            return null;
        }).when(this.blockRenderer).render(eq(xdom), any());
        when(this.macroContentParser.getCurrentSyntax(any())).thenAnswer(invocationOnMock -> {
            MacroTransformationContext transformationContext = invocationOnMock.getArgument(0);
            assertEquals(transformationContext.getId(), "refactoring_" + macroId);
            assertEquals(macroBlock, transformationContext.getCurrentMacroBlock());
            assertEquals(this.syntax, transformationContext.getSyntax());
            assertFalse(transformationContext.isInline());
            return this.syntax;
        });

        Map<String, String> parameters = Collections.singletonMap("some",  "thing");
        when(macroBlock.getParameters()).thenReturn(parameters);
        MacroBlock expectedBlock = new MacroBlock(this.macroId, parameters, expectedContent, false);
        assertEquals(Optional.of(expectedBlock),
            this.macroRefactoring.replaceReference(this.macroBlock, this.currentDocumentReference, this.sourceReference,
                this.targetReference, true, Map.of()));
    }

    @Test
    void extractReferences() throws MacroExecutionException, MacroRefactoringException
    {
        when(this.contentDescriptor.getType()).thenReturn(Block.LIST_BLOCK_TYPE);
        when(this.macroBlock.isInline()).thenReturn(false);
        String macroContent = "some macro content";
        when(this.macroBlock.getContent()).thenReturn(macroContent);
        XDOM xdom = mock(XDOM.class);
        when(this.macroContentParser.getCurrentSyntax(any())).thenAnswer(invocationOnMock -> {
            MacroTransformationContext transformationContext = invocationOnMock.getArgument(0);
            assertEquals(transformationContext.getId(), "refactoring_" + macroId);
            assertEquals(macroBlock, transformationContext.getCurrentMacroBlock());
            assertEquals(this.syntax, transformationContext.getSyntax());
            assertFalse(transformationContext.isInline());
            return this.syntax;
        });
        when(this.macroContentParser.parse(eq(macroContent), any(), eq(true), eq(false)))
            .thenAnswer(invocationOnMock -> {
            MacroTransformationContext transformationContext = invocationOnMock.getArgument(1);
            assertEquals(transformationContext.getId(), "refactoring_" + macroId);
            assertEquals(macroBlock, transformationContext.getCurrentMacroBlock());
            assertEquals(this.syntax, transformationContext.getSyntax());
            assertFalse(transformationContext.isInline());
            return xdom;
        });

        ResourceReference resourceReference = mock(ResourceReference.class);
        when(this.linkParser.extractReferences(xdom)).thenReturn(Collections.singleton(resourceReference));
        assertEquals(Collections.singleton(resourceReference),
            this.macroRefactoring.extractReferences(this.macroBlock));
    }
}
