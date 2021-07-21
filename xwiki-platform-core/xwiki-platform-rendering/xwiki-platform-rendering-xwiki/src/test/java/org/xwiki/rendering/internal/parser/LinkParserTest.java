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
package org.xwiki.rendering.internal.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.MacroRefactoring;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LinkParser}.
 *
 * @version $Id$
 * @since 13.7RC1
 */
@ComponentTest
class LinkParserTest
{
    @InjectMockComponents
    private LinkParser linkParser;

    @MockComponent
    private Provider<MacroRefactoring> macroRefactoringProvider;

    @MockComponent
    private EntityReferenceResolver<ResourceReference> entityReferenceResolver;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentDocumentReferenceResolver;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @Test
    void extractReferences(MockitoComponentManager componentManager) throws Exception
    {
        XDOM xdom = mock(XDOM.class);

        LinkBlock linkBlock1 = mock(LinkBlock.class);
        LinkBlock linkBlock2 = mock(LinkBlock.class);
        LinkBlock linkBlock3 = mock(LinkBlock.class);

        ImageBlock imageBlock = mock(ImageBlock.class);

        MacroBlock macroBlock1 = mock(MacroBlock.class);
        MacroBlock macroBlock2 = mock(MacroBlock.class);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).then(invocationOnMock -> {
            BlockMatcher blockMatcher = invocationOnMock.getArgument(0);
            assertTrue(blockMatcher instanceof ClassBlockMatcher);
            ClassBlockMatcher classBlockMatcher = (ClassBlockMatcher) blockMatcher;
            if (classBlockMatcher.match(mock(LinkBlock.class))) {
                return Arrays.asList(linkBlock1, linkBlock2, linkBlock3);
            } else if (classBlockMatcher.match(mock(ImageBlock.class))) {
                return Collections.singletonList(imageBlock);
            } else if (classBlockMatcher.match(mock(MacroBlock.class))) {
                return Arrays.asList(macroBlock1, macroBlock2);
            } else {
                fail("Only Link, Image and macro should be filtered from the xdom.");
            }
            return null;
        });

        ResourceReference ref1 = mock(ResourceReference.class);
        ResourceReference ref2 = mock(ResourceReference.class);
        ResourceReference ref3 = mock(ResourceReference.class);
        ResourceReference ref4 = mock(ResourceReference.class);
        ResourceReference ref5 = mock(ResourceReference.class);
        ResourceReference ref6 = mock(ResourceReference.class);
        ResourceReference ref7 = mock(ResourceReference.class);
        ResourceReference ref8 = mock(ResourceReference.class);

        when(linkBlock1.getReference()).thenReturn(ref1);
        when(linkBlock2.getReference()).thenReturn(ref2);
        when(linkBlock3.getReference()).thenReturn(ref3);

        when(imageBlock.getReference()).thenReturn(ref4);

        when(macroBlock1.getId()).thenReturn("mymacro");
        MacroRefactoring defaultMacroRefactoring = mock(MacroRefactoring.class);
        when(this.macroRefactoringProvider.get()).thenReturn(defaultMacroRefactoring);

        MacroRefactoring myMacroRefactoring = componentManager.registerMockComponent(MacroRefactoring.class, "mymacro");
        when(myMacroRefactoring.extractReferences(macroBlock1))
            .thenReturn(new HashSet<>(Arrays.asList(ref5, ref6, ref7)));
        when(defaultMacroRefactoring.extractReferences(macroBlock2)).thenReturn(Collections.singleton(ref8));

        assertEquals(new HashSet<>(Arrays.asList(ref1, ref2, ref3, ref4, ref5, ref6, ref7, ref8)),
            this.linkParser.extractReferences(xdom));
    }

    @Test
    void getUniqueLinkedEntityReference()
    {
        XDOM xdom = mock(XDOM.class);
        EntityType entityType = EntityType.DOCUMENT;
        DocumentReference currentReference = mock(DocumentReference.class);

        LinkBlock linkBlock1 = mock(LinkBlock.class);
        LinkBlock linkBlock2 = mock(LinkBlock.class);
        LinkBlock linkBlock3 = mock(LinkBlock.class);
        LinkBlock linkBlock4 = mock(LinkBlock.class);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).then(invocationOnMock -> {
            BlockMatcher blockMatcher = invocationOnMock.getArgument(0);
            assertTrue(blockMatcher instanceof ClassBlockMatcher);
            ClassBlockMatcher classBlockMatcher = (ClassBlockMatcher) blockMatcher;
            if (classBlockMatcher.match(mock(LinkBlock.class))) {
                return Arrays.asList(linkBlock1, linkBlock2, linkBlock3, linkBlock4);
            }
            return Collections.emptyList();
        });
        ResourceReference ref1 = mock(ResourceReference.class);
        ResourceReference ref2 = mock(ResourceReference.class);
        ResourceReference ref3 = mock(ResourceReference.class);
        ResourceReference ref4 = mock(ResourceReference.class);

        when(linkBlock1.getReference()).thenReturn(ref1);
        when(linkBlock2.getReference()).thenReturn(ref2);
        when(linkBlock3.getReference()).thenReturn(ref3);
        when(linkBlock4.getReference()).thenReturn(ref4);

        when(ref1.getType()).thenReturn(ResourceType.URL);
        when(ref2.getType()).thenReturn(ResourceType.ATTACHMENT);
        when(ref3.getType()).thenReturn(ResourceType.SPACE);
        when(ref4.getType()).thenReturn(ResourceType.DOCUMENT);

        when(ref1.getReference()).thenReturn("myref1");
        when(ref2.getReference()).thenReturn("myref2");
        when(ref3.getReference()).thenReturn("myref3");

        EntityReference entityReference2 = mock(EntityReference.class);
        EntityReference entityReference3 = mock(EntityReference.class);
        when(this.entityReferenceResolver.resolve(ref2, entityType)).thenReturn(entityReference2);
        when(this.entityReferenceResolver.resolve(ref3, entityType)).thenReturn(entityReference3);

        DocumentReference otherReference = mock(DocumentReference.class);
        when(this.currentDocumentReferenceResolver.resolve(entityReference2, EntityType.DOCUMENT))
            .thenReturn(otherReference);
        when(this.currentDocumentReferenceResolver.resolve(entityReference3, EntityType.DOCUMENT))
            .thenReturn(currentReference);

        assertEquals(Collections.singleton(entityReference2),
            this.linkParser.getUniqueLinkedEntityReferences(xdom, entityType, currentReference));
        verify(this.currentDocumentReferenceResolver).resolve(entityReference3, EntityType.DOCUMENT);
    }
}
