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
import org.xwiki.model.reference.EntityReferenceSerializer;
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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

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

    @Before
    public void configure() throws Exception
    {
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(xwiki);

        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        this.explicitDocumentReferenceResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "explicit");
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
        when(xdom.getBlocks(any(ClassBlockMatcher.class), eq(Block.Axes.DESCENDANT))).thenReturn(
            Arrays.<Block>asList(linkBlock));

        DocumentReference originalLinkReference = new DocumentReference("C", oldReference.getLastSpaceReference());
        when(this.explicitDocumentReferenceResolver.resolve("C", oldReference)).thenReturn(originalLinkReference);
        when(this.explicitDocumentReferenceResolver.resolve("C", newReference)).thenReturn(
            new DocumentReference("C", newReference.getLastSpaceReference()));

        when(this.compactEntityReferenceSerializer.serialize(originalLinkReference, newReference)).thenReturn("X.C");

        this.mocker.getComponentUnderTest().updateRelativeLinks(oldReference, newReference);

        assertEquals("X.C", linkBlock.getReference().getReference());
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
        verify(this.xcontext.getWiki()).saveDocument(document, "Renamed back-links.", this.xcontext);
    }
}
