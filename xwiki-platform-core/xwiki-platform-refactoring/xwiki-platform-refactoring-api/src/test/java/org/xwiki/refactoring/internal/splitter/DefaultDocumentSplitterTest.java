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
package org.xwiki.refactoring.internal.splitter;

import java.util.Arrays;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.refactoring.WikiDocument;
import org.xwiki.refactoring.splitter.criterion.SplittingCriterion;
import org.xwiki.refactoring.splitter.criterion.naming.NamingCriterion;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.Block.Axes;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test case for {@link DefaultDocumentSplitter}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
@ComponentTest
class DefaultDocumentSplitterTest
{
    @InjectMockComponents
    private DefaultDocumentSplitter splitter;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @Test
    void split() throws Exception
    {
        SplittingCriterion splittingCriterion = mock(SplittingCriterion.class);
        when(splittingCriterion.shouldSplit(any(Block.class), anyInt())).thenReturn(true, false, false, true);
        when(splittingCriterion.shouldIterate(any(SectionBlock.class), anyInt())).thenReturn(true, false, false, false);

        NamingCriterion namingCriterion = mock(NamingCriterion.class);
        DocumentReference firstChildReference = new DocumentReference("test", "Space", "Child1");
        DocumentReference secondChildReference = new DocumentReference("test", "Space", "Child2");
        when(namingCriterion.getDocumentReference(any(XDOM.class))).thenReturn(firstChildReference,
            secondChildReference);

        // = Chapter 1 =
        // Once upon a time..
        // == Section 1.1 ==
        // In a kingdom far away..

        HeaderBlock header = new HeaderBlock(Arrays.<Block>asList(new WordBlock("Section 1.1")), HeaderLevel.LEVEL2);
        ParagraphBlock paragraph = new ParagraphBlock(Arrays.<Block>asList(new WordBlock("In a kingdom far away..")));
        SectionBlock subSection = new SectionBlock(Arrays.<Block>asList(header, paragraph));

        header = new HeaderBlock(Arrays.<Block>asList(new WordBlock("Chapter 1")), HeaderLevel.LEVEL1);
        paragraph = new ParagraphBlock(Arrays.<Block>asList(new WordBlock("Once upon a time..")));
        SectionBlock section = new SectionBlock(Arrays.<Block>asList(header, paragraph, subSection));

        XDOM xdom = new XDOM(Arrays.<Block>asList(section));
        WikiDocument document = new WikiDocument(new DocumentReference("test", "Space", "Page"), xdom, null);

        List<WikiDocument> result = this.splitter.split(document, splittingCriterion, namingCriterion);

        assertEquals(3, result.size());
        assertSame(document, result.get(0));
        assertEquals(firstChildReference, result.get(1).getDocumentReference());
        assertSame(document, result.get(1).getParent());
        assertEquals(secondChildReference, result.get(2).getDocumentReference());
        assertSame(result.get(1), result.get(2).getParent());

        ClassBlockMatcher headerMatcher = new ClassBlockMatcher(HeaderBlock.class);
        ClassBlockMatcher linkMatcher = new ClassBlockMatcher(LinkBlock.class);
        assertTrue(document.getXdom().getBlocks(headerMatcher, Axes.DESCENDANT).isEmpty());
        assertEquals(1, document.getXdom().getBlocks(linkMatcher, Axes.DESCENDANT).size());
        assertEquals(1, result.get(1).getXdom().getBlocks(headerMatcher, Axes.DESCENDANT).size());
        assertEquals(1, result.get(1).getXdom().getBlocks(linkMatcher, Axes.DESCENDANT).size());
        assertEquals(1, result.get(2).getXdom().getBlocks(headerMatcher, Axes.DESCENDANT).size());
        assertTrue(result.get(2).getXdom().getBlocks(linkMatcher, Axes.DESCENDANT).isEmpty());
    }

    @Test
    void updateAnchors() throws Exception
    {
        SplittingCriterion splittingCriterion = mock(SplittingCriterion.class);
        when(splittingCriterion.shouldSplit(any(Block.class), anyInt())).thenReturn(false, false, true, true);
        when(splittingCriterion.shouldIterate(any(SectionBlock.class), anyInt())).thenReturn(false, false, false);

        NamingCriterion namingCriterion = mock(NamingCriterion.class);

        DocumentReference baseDocumentReference = new DocumentReference("test", "Space", "Page");
        DocumentReference firstChildReference = new DocumentReference("test", "Space", "Child1");
        DocumentReference secondChildReference = new DocumentReference("test", "Space", "Child2");
        when(this.compactWikiEntityReferenceSerializer.serialize(firstChildReference, baseDocumentReference))
            .thenReturn("Space.Child1");
        when(this.compactWikiEntityReferenceSerializer.serialize(firstChildReference, secondChildReference))
            .thenReturn("Space.Child1");

        when(namingCriterion.getDocumentReference(any(XDOM.class))).thenReturn(firstChildReference,
            secondChildReference);

        // [[link>>||anchor="chapter1"]]
        ResourceReference reference = new ResourceReference("", ResourceType.DOCUMENT);
        reference.setParameter("anchor", "chapter1");
        LinkBlock link = new LinkBlock(Arrays.<Block>asList(new WordBlock("link")), reference, false);
        ParagraphBlock firstParagraph = new ParagraphBlock(Arrays.<Block>asList(link));

        // [[link>>path:||anchor="chapter1"]]
        reference = new ResourceReference("", ResourceType.PATH);
        reference.setParameter("anchor", "chapter1");
        link = new LinkBlock(Arrays.<Block>asList(new WordBlock("link")), reference, false);
        ParagraphBlock secondParagraph = new ParagraphBlock(Arrays.<Block>asList(link));

        // = {{id name="chapter1"}}Chapter 1 =
        HeaderBlock header = new HeaderBlock(Arrays.<Block>asList(new IdBlock("chapter1"), new WordBlock("Chapter 1")),
            HeaderLevel.LEVEL1);
        SectionBlock section1 = new SectionBlock(Arrays.<Block>asList(header));

        // = Chapter 2 ==
        // [[link>>path:#chapter1]]
        header = new HeaderBlock(Arrays.<Block>asList(new WordBlock("Chapter 2")), HeaderLevel.LEVEL1);
        reference = new ResourceReference("#chapter1", ResourceType.PATH);
        link = new LinkBlock(Arrays.<Block>asList(new WordBlock("link")), reference, false);
        SectionBlock section2 =
            new SectionBlock(Arrays.<Block>asList(header, new ParagraphBlock(Arrays.<Block>asList(link))));

        XDOM xdom = new XDOM(Arrays.<Block>asList(firstParagraph, secondParagraph, section1, section2));
        WikiDocument document = new WikiDocument(baseDocumentReference, xdom, null);

        List<WikiDocument> result = this.splitter.split(document, splittingCriterion, namingCriterion);

        ClassBlockMatcher linkMatcher = new ClassBlockMatcher(LinkBlock.class);

        ResourceReference updatedReference =
            document.getXdom().<LinkBlock>getFirstBlock(linkMatcher, Axes.DESCENDANT).getReference();
        assertEquals("chapter1", updatedReference.getParameter("anchor"));
        assertEquals("Space.Child1", updatedReference.getReference());

        // Verify that the path reference was not updated.
        updatedReference = document.getXdom().<LinkBlock>getBlocks(linkMatcher, Axes.DESCENDANT).get(1).getReference();
        assertEquals("", updatedReference.getReference());
        assertEquals("chapter1", updatedReference.getParameter("anchor"));

        updatedReference =
            result.get(2).getXdom().<LinkBlock>getFirstBlock(linkMatcher, Axes.DESCENDANT).getReference();
        assertEquals(ResourceType.DOCUMENT, updatedReference.getType());
        assertEquals("chapter1", updatedReference.getParameter("anchor"));
        assertEquals("Space.Child1", updatedReference.getReference());
    }
}
