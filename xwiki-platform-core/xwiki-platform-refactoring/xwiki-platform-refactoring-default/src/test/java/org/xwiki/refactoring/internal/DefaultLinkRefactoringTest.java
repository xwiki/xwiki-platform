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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.internal.resolver.DefaultResourceReferenceEntityReferenceResolver;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.render.DefaultLinkedResourceHelper;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLinkRefactoring}.
 *
 * @version $Id$
 */
@ComponentList(DefaultLinkedResourceHelper.class)
public class DefaultLinkRefactoringTest
{
    @Rule
    public MockitoComponentMockingRule<LinkRefactoring> mocker = new MockitoComponentMockingRule<LinkRefactoring>(
        DefaultLinkRefactoring.class);

    private XWikiContext xcontext = mock(XWikiContext.class);

    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    private EntityReferenceResolver<ResourceReference> resourceReferenceResolver;

    @Before
    public void configure() throws Exception
    {
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(xwiki);

        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        this.resourceReferenceResolver =
            this.mocker.getInstance(DefaultResourceReferenceEntityReferenceResolver.TYPE_RESOURCEREFERENCE);
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

        ResourceReference docLinkReference = new ResourceReference("C", ResourceType.DOCUMENT);
        LinkBlock docLinkBlock = new LinkBlock(Collections.<Block>emptyList(), docLinkReference, false);

        ResourceReference spaceLinkReference = new ResourceReference("Z", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(Collections.<Block>emptyList(), spaceLinkReference, false);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(Arrays.<Block>asList(docLinkBlock, spaceLinkBlock));

        DocumentReference originalDocLinkReference = new DocumentReference("C", oldReference.getLastSpaceReference());
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, oldReference)).thenReturn(
            originalDocLinkReference);
        DocumentReference newDocLinkReference = new DocumentReference("C", newReference.getLastSpaceReference());
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, newReference)).thenReturn(
            newDocLinkReference);

        SpaceReference originalSpaceReference = new SpaceReference("wiki", "Z");
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, oldReference)).thenReturn(
            originalSpaceReference);
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, newReference)).thenReturn(
            originalSpaceReference);

        when(this.compactEntityReferenceSerializer.serialize(originalDocLinkReference, newReference)).thenReturn("A.C");

        this.mocker.getComponentUnderTest().updateRelativeLinks(oldReference, newReference);

        // Document link block is updated.
        assertEquals("A.C", docLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, docLinkBlock.getReference().getType());
        // Space link block stays the same, since they were on the same wiki.
        assertEquals("Z", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.SPACE, spaceLinkBlock.getReference().getType());
        verifyDocumentSave(newDocument, "Updated the relative links.", true);
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

        ResourceReference docLinkReference = new ResourceReference("C", ResourceType.DOCUMENT);
        LinkBlock docLinkBlock = new LinkBlock(Collections.<Block>emptyList(), docLinkReference, false);

        ResourceReference spaceLinkReference = new ResourceReference("Z", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(Collections.<Block>emptyList(), spaceLinkReference, false);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(Arrays.<Block>asList(docLinkBlock, spaceLinkBlock));

        DocumentReference originalDocLinkReference = new DocumentReference("C", oldReference.getLastSpaceReference());
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, oldReference)).thenReturn(
            originalDocLinkReference);
        DocumentReference newDocLinkReference = new DocumentReference("C", newReference.getLastSpaceReference());
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, newReference)).thenReturn(
            newDocLinkReference);

        SpaceReference originalSpaceReference = new SpaceReference("wiki1", "Z");
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, oldReference)).thenReturn(
            originalSpaceReference);
        SpaceReference newSpaceReference = new SpaceReference("wiki2", "Z");
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, newReference)).thenReturn(
            newSpaceReference);

        when(this.compactEntityReferenceSerializer.serialize(originalDocLinkReference, newReference)).thenReturn(
            "wiki1:A.C");
        when(this.compactEntityReferenceSerializer.serialize(originalSpaceReference, newReference)).thenReturn(
            "wiki1:Z");

        this.mocker.getComponentUnderTest().updateRelativeLinks(oldReference, newReference);

        // Document link block is updated.
        assertEquals("wiki1:A.C", docLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, docLinkBlock.getReference().getType());
        // Space link is also updated, since they were referring entities on a different wiki.
        assertEquals("wiki1:Z", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.SPACE, spaceLinkBlock.getReference().getType());
        verifyDocumentSave(newDocument, "Updated the relative links.", true);
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

        ResourceReference linkReference = new ResourceReference("A.B", ResourceType.DOCUMENT);
        LinkBlock linkBlock = new LinkBlock(Collections.<Block>emptyList(), linkReference, false);
        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).thenReturn(Arrays.<Block>asList(linkBlock));

        when(this.resourceReferenceResolver.resolve(linkReference, null, documentReference)).thenReturn(oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);

        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");

        this.mocker.getComponentUnderTest().renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.Y", linkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, linkBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false);
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

        ResourceReference docLinkReference = new ResourceReference("A.WebHome", ResourceType.DOCUMENT);
        LinkBlock documentLinkBlock = new LinkBlock(Collections.<Block>emptyList(), docLinkReference, false);

        ResourceReference spaceLinkReference = new ResourceReference("A", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(Collections.<Block>emptyList(), spaceLinkReference, false);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(Arrays.<Block>asList(documentLinkBlock, spaceLinkBlock));

        // Doc link
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, documentReference)).thenReturn(
            oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.WebHome");

        // Space link
        SpaceReference spaceReference = oldLinkTarget.getLastSpaceReference();
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, documentReference)).thenReturn(
            spaceReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(spaceReference)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget.getLastSpaceReference(), documentReference))
            .thenReturn("X");

        this.mocker.getComponentUnderTest().renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.WebHome", documentLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, documentLinkBlock.getReference().getType());
        assertEquals("X", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.SPACE, spaceLinkBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false);
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

        ResourceReference docLinkReference = new ResourceReference("A.WebHome", ResourceType.DOCUMENT);
        LinkBlock documentLinkBlock = new LinkBlock(Collections.<Block>emptyList(), docLinkReference, false);

        ResourceReference spaceLinkReference = new ResourceReference("A", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(Collections.<Block>emptyList(), spaceLinkReference, false);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(Arrays.<Block>asList(documentLinkBlock, spaceLinkBlock));

        // Doc link
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, documentReference)).thenReturn(
            oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");

        // Space link
        SpaceReference spaceReference = oldLinkTarget.getLastSpaceReference();
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, documentReference)).thenReturn(
            spaceReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(spaceReference)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget.getLastSpaceReference(), documentReference))
            .thenReturn("X");

        this.mocker.getComponentUnderTest().renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        // Note that both resulting renamed back-links are of type document. (i.e. the space link was converted to a doc
        // link)
        assertEquals("X.Y", documentLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, documentLinkBlock.getReference().getType());
        assertEquals("X.Y", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, spaceLinkBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false);
    }

    @Test
    public void renameLinksFromMacros() throws Exception
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

        Map<String, String> includeParameters = new HashMap<String, String>();
        includeParameters.put("reference", "A.B");
        MacroBlock includeMacroBlock1 = new MacroBlock("include", includeParameters, false);

        Map<String, String> includeOldParameters = new HashMap<String, String>();
        includeOldParameters.put("document", "A.B");
        MacroBlock includeMacroBlock2 = new MacroBlock("include", includeOldParameters, false);

        Map<String, String> displayParameters = new HashMap<String, String>();
        displayParameters.put("reference", "A.B");
        MacroBlock displayMacroBlock = new MacroBlock("display", displayParameters, false);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(Arrays.<Block>asList(includeMacroBlock1, includeMacroBlock2, displayMacroBlock));

        ResourceReference macroResourceReference = new ResourceReference("A.B", ResourceType.DOCUMENT);

        when(this.resourceReferenceResolver.resolve(macroResourceReference, null, documentReference)).thenReturn(
            oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");

        this.mocker.getComponentUnderTest().renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.Y", includeMacroBlock1.getParameter("reference"));
        assertEquals("X.Y", includeMacroBlock2.getParameter("document"));
        assertEquals("X.Y", displayMacroBlock.getParameter("reference"));
        verifyDocumentSave(document, "Renamed back-links.", false);
    }

    @Test
    public void renameLinksFromLinksAndMacros() throws Exception
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

        Map<String, String> includeParameters = new HashMap<String, String>();
        includeParameters.put("reference", "A.B");
        MacroBlock includeMacroBlock = new MacroBlock("include", includeParameters, false);

        ResourceReference resourceReference = new ResourceReference("A.B", ResourceType.DOCUMENT);
        LinkBlock documentLinkBlock = new LinkBlock(Collections.<Block>emptyList(), resourceReference, false);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(Arrays.<Block>asList(includeMacroBlock, documentLinkBlock));

        when(this.resourceReferenceResolver.resolve(resourceReference, null, documentReference)).thenReturn(
            oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");

        this.mocker.getComponentUnderTest().renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.Y", includeMacroBlock.getParameter("reference"));
        assertEquals("X.Y", documentLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, documentLinkBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false);
    }

    private void verifyDocumentSave(XWikiDocument document, String comment, boolean minorEdit) throws Exception
    {
        // Verify we preserve the content author.
        verify(document).setContentDirty(false);
        // Verify the version is going to be incremented.
        verify(document).setMetaDataDirty(true);
        verify(this.xcontext.getWiki()).saveDocument(document, comment, minorEdit, this.xcontext);
    }

    @Test
    public void renameLinksAndTranslations() throws Exception
    {
        DocumentReference baseDocumentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument baseDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(baseDocumentReference, this.xcontext)).thenReturn(baseDocument);
        when(baseDocument.getDocumentReference()).thenReturn(baseDocumentReference);
        this.mocker.registerMockComponent(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString());

        when(baseDocument.getTranslationLocales(xcontext)).thenReturn(Arrays.asList(Locale.FRENCH, Locale.ENGLISH));
        DocumentReference frenchDocumentReference = new DocumentReference("wiki", "Space", "Page", Locale.FRENCH);
        XWikiDocument frenchDocument = mock(XWikiDocument.class);
        when(baseDocument.getTranslatedDocument(Locale.FRENCH, xcontext)).thenReturn(frenchDocument);
        when(frenchDocument.getDocumentReference()).thenReturn(frenchDocumentReference);

        DocumentReference englishDocumentReference = new DocumentReference("wiki", "Space", "Page", Locale.ENGLISH);
        XWikiDocument englishDocument = mock(XWikiDocument.class);
        when(baseDocument.getTranslatedDocument(Locale.ENGLISH, xcontext)).thenReturn(englishDocument);
        when(englishDocument.getDocumentReference()).thenReturn(englishDocumentReference);

        // From a terminal document to another terminal document.
        DocumentReference oldLinkTarget = new DocumentReference("wiki", "A", "B");
        DocumentReference newLinkTarget = new DocumentReference("wiki", "X", "Y");

        List<XWikiDocument> documentsToUpdate = Arrays.asList(baseDocument, frenchDocument, englishDocument);

        for (XWikiDocument xWikiDocument : documentsToUpdate) {
            DocumentReference documentReference = xWikiDocument.getDocumentReference();
            when(xWikiDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
            XDOM xdom = mock(XDOM.class);
            when(xWikiDocument.getXDOM()).thenReturn(xdom);

            ResourceReference linkReference = new ResourceReference("A.B", ResourceType.DOCUMENT);
            LinkBlock linkBlock = new LinkBlock(Collections.<Block>emptyList(), linkReference, false);
            when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).thenReturn(Arrays.<Block>asList(linkBlock));

            when(this.resourceReferenceResolver.resolve(linkReference, null, documentReference)).thenReturn(oldLinkTarget);
            when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);

            when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");
        }
        this.mocker.getComponentUnderTest().renameLinks(baseDocumentReference, oldLinkTarget, newLinkTarget);

        for (XWikiDocument xWikiDocument : documentsToUpdate) {
            verifyDocumentSave(xWikiDocument, "Renamed back-links.", false);
            LinkBlock linkBlock = (LinkBlock) xWikiDocument.getXDOM().
                getBlocks(mock(BlockMatcher.class), Block.Axes.DESCENDANT).get(0);
            assertEquals("X.Y", linkBlock.getReference().getReference());
            assertEquals(ResourceType.DOCUMENT, linkBlock.getReference().getType());
        }
    }
}
