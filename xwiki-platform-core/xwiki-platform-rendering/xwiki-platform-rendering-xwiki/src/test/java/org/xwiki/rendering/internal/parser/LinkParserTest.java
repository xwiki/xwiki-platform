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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
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
    private static final DocumentReference CURRENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    @InjectMockComponents
    private LinkParser linkParser;

    @MockComponent
    private Provider<MacroRefactoring> macroRefactoringProvider;

    @MockComponent
    private EntityReferenceResolver<ResourceReference> entityReferenceResolver;

    @Mock
    private XDOM xdom;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @Test
    void extractReferences(MockitoComponentManager componentManager) throws Exception
    {
        LinkBlock linkBlock1 = mock(LinkBlock.class);
        LinkBlock linkBlock2 = mock(LinkBlock.class);
        LinkBlock linkBlock3 = mock(LinkBlock.class);

        ImageBlock imageBlock = mock(ImageBlock.class);

        MacroBlock macroBlock1 = mock(MacroBlock.class);
        MacroBlock macroBlock2 = mock(MacroBlock.class);

        when(this.xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .then(new AnswerGetBlocks(
                List.of(linkBlock1, linkBlock2, linkBlock3),
                List.of(imageBlock),
                List.of(macroBlock1, macroBlock2)
            ));

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
            this.linkParser.extractReferences(this.xdom));
    }

    @Test
    void getUniqueLinkedEntityReference()
    {
        ResourceReference ref1 = new ResourceReference("myref1", ResourceType.URL);
        ResourceReference ref2 = new ResourceReference("myref2", ResourceType.ATTACHMENT);
        ResourceReference ref3 = new ResourceReference("myref3", ResourceType.SPACE);
        ResourceReference ref4 = new ResourceReference("", ResourceType.DOCUMENT);

        LinkBlock linkBlock1 = new LinkBlock(List.of(), ref1, false);
        LinkBlock linkBlock2 = new LinkBlock(List.of(), ref2, false);
        LinkBlock linkBlock3 = new LinkBlock(List.of(), ref3, false);
        LinkBlock linkBlock4 = new LinkBlock(List.of(), ref4, false);

        when(this.xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).thenAnswer(new AnswerGetBlocks(
            List.of(linkBlock1, linkBlock2, linkBlock3, linkBlock4),
            List.of(),
            List.of()
        ));

        EntityReference entityReference2 = new EntityReference("myref2", EntityType.DOCUMENT);
        EntityReference entityReference3 = new EntityReference("myref3", EntityType.SPACE);
        when(this.entityReferenceResolver.resolve(ref2, EntityType.DOCUMENT)).thenReturn(entityReference2);
        when(this.entityReferenceResolver.resolve(ref3, EntityType.DOCUMENT)).thenReturn(entityReference3);

        assertEquals(Set.of(entityReference2), this.linkParser.getUniqueLinkedEntityReferences(this.xdom,
            Map.of(EntityType.DOCUMENT, Set.of(ResourceType.DOCUMENT, ResourceType.ATTACHMENT)),
            CURRENT_REFERENCE));
        verify(this.entityReferenceResolver).resolve(ref2, EntityType.DOCUMENT);
    }

    @Test
    void getUniqueLinkedEntityReferenceAttachment()
    {
        AttachmentReference entityReference1 =
            new AttachmentReference("file.txt", new DocumentReference("wiki", "space", "page"));
        DocumentReference entityReference2 = new DocumentReference("wiki", "space2", "page2");
        AttachmentReference entityReference3 =
            new AttachmentReference("file.txt", new DocumentReference("wiki", "space2", "page"));

        ResourceReference ref1 = new ResourceReference("ref1", ResourceType.ATTACHMENT);
        ResourceReference ref2 = new ResourceReference("ref2", ResourceType.DOCUMENT);
        ResourceReference ref3 = new ResourceReference("ref3", ResourceType.ATTACHMENT);

        List<LinkBlock> linkBlocks = List.of(
            new LinkBlock(List.of(), ref1, false),
            new LinkBlock(List.of(), ref2, false),
            new LinkBlock(List.of(), ref3, false)
        );
        when(this.xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenAnswer(new AnswerGetBlocks(linkBlocks, List.of(), List.of()));

        when(this.entityReferenceResolver.resolve(ref1, EntityType.ATTACHMENT)).thenReturn(entityReference1);
        when(this.entityReferenceResolver.resolve(ref2, EntityType.DOCUMENT)).thenReturn(entityReference2);
        when(this.entityReferenceResolver.resolve(ref3, EntityType.ATTACHMENT)).thenReturn(entityReference3);

        Set<EntityReference> actual = this.linkParser.getUniqueLinkedEntityReferences(this.xdom, Map.of(
            EntityType.DOCUMENT, Set.of(ResourceType.DOCUMENT),
            EntityType.ATTACHMENT, Set.of(ResourceType.ATTACHMENT)
        ), CURRENT_REFERENCE);
        assertEquals(Set.of(entityReference3, entityReference2), actual);
    }

    private static final class AnswerGetBlocks implements Answer<Object>
    {
        private final List<LinkBlock> answerLinkBlock;

        private final List<ImageBlock> answerImageBlock;

        private final List<MacroBlock> answerMacroBlock;

        private AnswerGetBlocks(List<LinkBlock> answerLinkBlock, List<ImageBlock> answerImageBlock,
            List<MacroBlock> answerMacroBlock)
        {
            this.answerLinkBlock = answerLinkBlock;
            this.answerImageBlock = answerImageBlock;
            this.answerMacroBlock = answerMacroBlock;
        }

        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable
        {
            BlockMatcher blockMatcher = invocationOnMock.getArgument(0);
            assertTrue(blockMatcher instanceof ClassBlockMatcher);
            ClassBlockMatcher classBlockMatcher = (ClassBlockMatcher) blockMatcher;
            if (classBlockMatcher.match(mock(LinkBlock.class))) {
                return this.answerLinkBlock;
            } else if (classBlockMatcher.match(mock(ImageBlock.class))) {
                return this.answerImageBlock;
            } else if (classBlockMatcher.match(mock(MacroBlock.class))) {
                return this.answerMacroBlock;
            } else {
                fail("Only Link, Image and macro should be filtered from the xdom.");
            }
            return null;
        }
    }
}
