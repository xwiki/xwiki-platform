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

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLinkRefactoring}.
 *
 * @version $Id$
 */
public class DefaultLinkRefactoringTest
{
    @Rule
    public MockitoComponentMockingRule<LinkRefactoring> mocker = new MockitoComponentMockingRule<LinkRefactoring>(
        DefaultLinkRefactoring.class);

    private XWikiContext xcontext = mock(XWikiContext.class);

    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    private SpaceReferenceResolver<String> spaceReferenceResolver;

    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    @Before
    public void configure() throws Exception
    {
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(xwiki);

        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        this.explicitDocumentReferenceResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "explicit");
        this.spaceReferenceResolver = this.mocker.getInstance(SpaceReferenceResolver.TYPE_STRING);
        this.defaultReferenceDocumentReferenceResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE);
        this.compactEntityReferenceSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "compact");

        Provider<ComponentManager> contextComponentManagerProvider =
            this.mocker.registerMockComponent(
                new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(contextComponentManagerProvider.get()).thenReturn(this.mocker);
    }

    @Test
    public void updateRelativeLinks() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("wiki", "A", "B");
        DocumentReference newReference = new DocumentReference("wiki", "X", "Y");

        XWikiDocument newDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(newReference, this.xcontext)).thenReturn(newDocument);
        when(newDocument.getDocumentReference()).thenReturn(newReference);
        when(newDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        this.mocker.registerMockComponent(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString());

        XDOM xdom = mock(XDOM.class);
        when(newDocument.getXDOM()).thenReturn(xdom);

        LinkBlock linkBlock =
            new LinkBlock(Collections.<Block>emptyList(), new ResourceReference("C", ResourceType.DOCUMENT), false);
        LinkBlock spaceLinkBlock =
            new LinkBlock(Collections.<Block>emptyList(), new ResourceReference("Z", ResourceType.SPACE), false);
        when(xdom.getBlocks(any(ClassBlockMatcher.class), eq(Block.Axes.DESCENDANT))).thenReturn(
            Arrays.<Block>asList(linkBlock, spaceLinkBlock));

        DocumentReference originalLinkReference = new DocumentReference("C", oldReference.getLastSpaceReference());
        when(this.explicitDocumentReferenceResolver.resolve("C", oldReference)).thenReturn(originalLinkReference);
        when(this.explicitDocumentReferenceResolver.resolve("C", newReference)).thenReturn(
            new DocumentReference("C", newReference.getLastSpaceReference()));

        SpaceReference originalSpaceReference = new SpaceReference("wiki", "Z");
        when(this.spaceReferenceResolver.resolve("Z", oldReference)).thenReturn(originalSpaceReference);
        when(this.spaceReferenceResolver.resolve("Z", newReference)).thenReturn(originalSpaceReference);

        when(this.compactEntityReferenceSerializer.serialize(originalLinkReference, newReference)).thenReturn("X.C");

        this.mocker.getComponentUnderTest().updateRelativeLinks(oldReference, newReference);

        // Document link block is updated.
        assertEquals("X.C", linkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, linkBlock.getReference().getType());
        // Space link block stays the same, since they were on the same wiki.
        assertEquals("Z", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.SPACE, spaceLinkBlock.getReference().getType());
        verify(xcontext.getWiki()).saveDocument(newDocument, "Updated the relative links.", true, this.xcontext);
    }

    @Test
    public void updateRelativeLinksAcrossWikis() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("wiki1", "A", "B");
        DocumentReference newReference = new DocumentReference("wiki2", "X", "Y");

        XWikiDocument newDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(newReference, this.xcontext)).thenReturn(newDocument);
        when(newDocument.getDocumentReference()).thenReturn(newReference);
        when(newDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        this.mocker.registerMockComponent(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString());

        XDOM xdom = mock(XDOM.class);
        when(newDocument.getXDOM()).thenReturn(xdom);

        LinkBlock linkBlock =
            new LinkBlock(Collections.<Block>emptyList(), new ResourceReference("C", ResourceType.DOCUMENT), false);
        LinkBlock spaceLinkBlock =
            new LinkBlock(Collections.<Block>emptyList(), new ResourceReference("Z", ResourceType.SPACE), false);
        when(xdom.getBlocks(any(ClassBlockMatcher.class), eq(Block.Axes.DESCENDANT))).thenReturn(
            Arrays.<Block>asList(linkBlock, spaceLinkBlock));

        DocumentReference originalLinkReference = new DocumentReference("C", oldReference.getLastSpaceReference());
        when(this.explicitDocumentReferenceResolver.resolve("C", oldReference)).thenReturn(originalLinkReference);
        when(this.explicitDocumentReferenceResolver.resolve("C", newReference)).thenReturn(
            new DocumentReference("C", newReference.getLastSpaceReference()));

        SpaceReference originalSpaceReference = new SpaceReference("wiki1", "Z");
        when(this.spaceReferenceResolver.resolve("Z", oldReference)).thenReturn(originalSpaceReference);
        SpaceReference newSpaceReference = new SpaceReference("wiki2", "Z");
        when(this.spaceReferenceResolver.resolve("Z", newReference)).thenReturn(newSpaceReference);

        when(this.compactEntityReferenceSerializer.serialize(originalLinkReference, newReference)).thenReturn(
            "wiki1:A.C");
        when(this.compactEntityReferenceSerializer.serialize(originalSpaceReference, newReference)).thenReturn(
            "wiki1:Z");

        this.mocker.getComponentUnderTest().updateRelativeLinks(oldReference, newReference);

        // Document link block is updated.
        assertEquals("wiki1:A.C", linkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, linkBlock.getReference().getType());
        // Space link is also updated, since they were refering entities on a diferent wiki.
        assertEquals("wiki1:Z", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.SPACE, spaceLinkBlock.getReference().getType());
        verify(xcontext.getWiki()).saveDocument(newDocument, "Updated the relative links.", true, this.xcontext);
    }

    @Test
    public void renameLinks() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        this.mocker.registerMockComponent(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString());

        // From a terminal document to another terminal document.
        DocumentReference oldLinkTarget = new DocumentReference("wiki", "A", "B");
        DocumentReference newLinkTarget = new DocumentReference("wiki", "X", "Y");

        XDOM xdom = mock(XDOM.class);
        when(document.getXDOM()).thenReturn(xdom);

        LinkBlock linkBlock =
            new LinkBlock(Collections.<Block>emptyList(), new ResourceReference("A.B", ResourceType.DOCUMENT), false);
        when(xdom.getBlocks(any(ClassBlockMatcher.class), eq(Block.Axes.DESCENDANT))).thenReturn(
            Arrays.<Block>asList(linkBlock));

        when(this.explicitDocumentReferenceResolver.resolve("A.B", documentReference)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");

        this.mocker.getComponentUnderTest().renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.Y", linkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, linkBlock.getReference().getType());
        verify(this.xcontext.getWiki()).saveDocument(document, "Renamed back-links.", this.xcontext);
    }

    @Test
    public void renameNonTerminalDocumentLinks() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        this.mocker.registerMockComponent(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString());

        // From a non-terminal document to another non-terminal document.
        DocumentReference oldLinkTarget = new DocumentReference("wiki", "A", "WebHome");
        DocumentReference newLinkTarget = new DocumentReference("wiki", "X", "WebHome");

        XDOM xdom = mock(XDOM.class);
        when(document.getXDOM()).thenReturn(xdom);

        LinkBlock documentLinkBlock =
            new LinkBlock(Collections.<Block>emptyList(), new ResourceReference("A.WebHome", ResourceType.DOCUMENT),
                false);
        LinkBlock spaceLinkBlock =
            new LinkBlock(Collections.<Block>emptyList(), new ResourceReference("A", ResourceType.SPACE), false);
        when(xdom.getBlocks(any(ClassBlockMatcher.class), eq(Block.Axes.DESCENDANT))).thenReturn(
            Arrays.<Block>asList(documentLinkBlock, spaceLinkBlock));

        // Doc link
        when(this.explicitDocumentReferenceResolver.resolve("A.WebHome", documentReference)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.WebHome");
        // Space link
        SpaceReference spaceReference = oldLinkTarget.getLastSpaceReference();
        when(this.spaceReferenceResolver.resolve("A", documentReference)).thenReturn(spaceReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(spaceReference)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(spaceReference, documentReference)).thenReturn("X");

        this.mocker.getComponentUnderTest().renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.WebHome", documentLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, documentLinkBlock.getReference().getType());
        assertEquals("X", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.SPACE, spaceLinkBlock.getReference().getType());
        verify(this.xcontext.getWiki()).saveDocument(document, "Renamed back-links.", this.xcontext);
    }

    @Test
    public void renameNonTerminalToTerminalDocumentLinks() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        this.mocker.registerMockComponent(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString());

        // From a non-terminal document to a terminal document.
        DocumentReference oldLinkTarget = new DocumentReference("wiki", "A", "WebHome");
        DocumentReference newLinkTarget = new DocumentReference("wiki", "X", "Y");

        XDOM xdom = mock(XDOM.class);
        when(document.getXDOM()).thenReturn(xdom);

        LinkBlock documentLinkBlock =
            new LinkBlock(Collections.<Block>emptyList(), new ResourceReference("A.WebHome", ResourceType.DOCUMENT),
                false);
        LinkBlock spaceLinkBlock =
            new LinkBlock(Collections.<Block>emptyList(), new ResourceReference("A", ResourceType.SPACE), false);
        when(xdom.getBlocks(any(ClassBlockMatcher.class), eq(Block.Axes.DESCENDANT))).thenReturn(
            Arrays.<Block>asList(documentLinkBlock, spaceLinkBlock));

        // Doc link
        when(this.explicitDocumentReferenceResolver.resolve("A.WebHome", documentReference)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");
        // Space link
        SpaceReference spaceReference = oldLinkTarget.getLastSpaceReference();
        when(this.spaceReferenceResolver.resolve("A", documentReference)).thenReturn(spaceReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(spaceReference)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(spaceReference, documentReference)).thenReturn("X");

        this.mocker.getComponentUnderTest().renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        // Note that both resulting renamed back-links are of type document. (i.e. the space link was converted to a doc
        // link)
        assertEquals("X.Y", documentLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, documentLinkBlock.getReference().getType());
        assertEquals("X.Y", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, spaceLinkBlock.getReference().getType());
        verify(this.xcontext.getWiki()).saveDocument(document, "Renamed back-links.", this.xcontext);
    }
}
