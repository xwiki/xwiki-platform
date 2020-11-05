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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.BlockMatcher;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.render.LinkedResourceHelper;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultLinkRefactoring}.
 *
 * @version $Id$
 */
@ComponentTest
// @formatter:off
@ComponentList({
    LinkedResourceHelper.class,
    ReferenceRenamer.class
})
// @formatter:on
class DefaultLinkRefactoringTest
{
    @MockComponent
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    @MockComponent
    private EntityReferenceResolver<ResourceReference> resourceReferenceResolver;

    @MockComponent
    private PageReferenceResolver<EntityReference> defaultReferencePageReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @MockComponent
    private ContentParser contentParser;

    @InjectComponentManager
    private ComponentManager componentManager;

    @MockComponent
    @Named("xwiki/2.1")
    private BlockRenderer blockRenderer;

    @InjectMockComponents
    private DefaultLinkRefactoring refactoring;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension();

    private XWikiContext xcontext = mock(XWikiContext.class);

    int logIndex = 0;

    @BeforeEach
    void beforeEach()
    {
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(xwiki);

        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.componentManagerProvider.get()).thenReturn(this.componentManager);
    }

    private void setTextarea(XWikiDocument document, XDOM xdom)
        throws XWikiException, ParseException, MissingParserException
    {
        DocumentReference documentReference = document.getDocumentReference();

        BaseClass baseClass = mock(BaseClass.class);
        TextAreaClass textAreaClass = new TextAreaClass();
        textAreaClass.setName("area");
        when(baseClass.getProperties()).thenReturn(new Object[] { textAreaClass });

        when(this.xcontext.getWiki().getXClass(documentReference, this.xcontext)).thenReturn(baseClass);

        BaseObject baseObject = mock(BaseObject.class);
        when(baseObject.getXClass(any())).thenReturn(baseClass);
        LargeStringProperty property = mock(LargeStringProperty.class);
        when(baseObject.getField("area")).thenReturn(property);
        when(property.getValue()).thenReturn("areacontent");
        when(property.getReference())
            .thenReturn(new ObjectPropertyReference("area", new ObjectReference("areaobject", documentReference)));

        when(document.getXObjects())
            .thenReturn(Collections.singletonMap(documentReference, Collections.singletonList(baseObject)));

        when(this.contentParser.parse("areacontent", Syntax.XWIKI_2_1, documentReference)).thenReturn(xdom);
    }

    @Test
    void updateRelativeLinks() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("wiki", "A", "B");
        AttachmentReference oldImageTargetAttachment = new AttachmentReference("attachment.txt", oldReference);
        DocumentReference newReference = new DocumentReference("wiki", "X", "Y");
        AttachmentReference newImageTargetAttachment = new AttachmentReference("attachment.txt", newReference);

        XWikiDocument newDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(newReference, this.xcontext)).thenReturn(newDocument);
        when(newDocument.getDocumentReference()).thenReturn(newReference);
        when(newDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        // Setup document content
        ResourceReference docLinkReference = new ResourceReference("C", ResourceType.DOCUMENT);
        LinkBlock docLinkBlock = new LinkBlock(Collections.emptyList(), docLinkReference, false);
        ResourceReference spaceLinkReference = new ResourceReference("Z", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(Collections.emptyList(), spaceLinkReference, false);
        ResourceReference imageReference = new AttachmentResourceReference("attachment.txt");
        ImageBlock imageBlock = new ImageBlock(imageReference, false);
        when(newDocument.getXDOM())
            .thenReturn(new XDOM(Arrays.asList(docLinkBlock, spaceLinkBlock, imageBlock)));

        // Setup object content
        ResourceReference xobjectDocLinkReference = new ResourceReference("C", ResourceType.DOCUMENT);
        LinkBlock xobjectDocLinkBlock = new LinkBlock(Collections.emptyList(), xobjectDocLinkReference, false);
        ResourceReference xobjectSpaceLinkReference = new ResourceReference("Z", ResourceType.SPACE);
        LinkBlock xobjectSpaceLinkBlock =
            new LinkBlock(Collections.emptyList(), xobjectSpaceLinkReference, false);
        ResourceReference xobjectImageReference = new AttachmentResourceReference("attachment.txt");
        ImageBlock xobjectImageBlock = new ImageBlock(imageReference, false);
        setTextarea(newDocument,
            new XDOM(Arrays.asList(xobjectDocLinkBlock, xobjectSpaceLinkBlock, xobjectImageBlock)));

        DocumentReference originalDocLinkReference = new DocumentReference("C", oldReference.getLastSpaceReference());
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, oldReference))
            .thenReturn(originalDocLinkReference);
        when(this.resourceReferenceResolver.resolve(xobjectDocLinkReference, null, oldReference))
            .thenReturn(originalDocLinkReference);
        when(this.resourceReferenceResolver.resolve(imageReference, null, oldReference))
            .thenReturn(oldImageTargetAttachment);
        DocumentReference newDocLinkReference = new DocumentReference("C", newReference.getLastSpaceReference());
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, newReference))
            .thenReturn(newDocLinkReference);
        when(this.resourceReferenceResolver.resolve(xobjectDocLinkReference, null, newReference))
            .thenReturn(newDocLinkReference);
        when(this.resourceReferenceResolver.resolve(imageReference, null, newReference))
            .thenReturn(newImageTargetAttachment);

        SpaceReference originalSpaceReference = new SpaceReference("wiki", "Z");
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, oldReference))
            .thenReturn(originalSpaceReference);
        when(this.resourceReferenceResolver.resolve(xobjectSpaceLinkReference, null, oldReference))
            .thenReturn(originalSpaceReference);
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, newReference))
            .thenReturn(originalSpaceReference);
        when(this.resourceReferenceResolver.resolve(xobjectSpaceLinkReference, null, newReference))
            .thenReturn(originalSpaceReference);

        when(this.compactEntityReferenceSerializer.serialize(originalDocLinkReference, newReference)).thenReturn("A.C");

        this.refactoring.updateRelativeLinks(oldReference, newReference);

        // Document link block is updated.
        assertEquals("A.C", docLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, docLinkBlock.getReference().getType());
        // Space link block stays the same, since they were on the same wiki.
        assertEquals("Z", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.SPACE, spaceLinkBlock.getReference().getType());
        // Image link relative to the document stay the same
        assertEquals("attachment.txt", imageBlock.getReference().getReference());

        // XObject Document link block is updated.
        assertEquals("A.C", xobjectDocLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, xobjectDocLinkBlock.getReference().getType());
        // XObject Space link block stays the same, since they were on the same wiki.
        assertEquals("Z", xobjectSpaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.SPACE, xobjectSpaceLinkBlock.getReference().getType());
        // Image link relative to the document stay the same
        assertEquals("attachment.txt", xobjectImageBlock.getReference().getReference());

        verifyDocumentSave(newDocument, "Updated the relative links.", true, true);
    }

    @Test
    void updateRelativeLinksAcrossWikis() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("wiki1", "A", "B");
        DocumentReference newReference = new DocumentReference("wiki2", "X", "Y");

        XWikiDocument newDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(newReference, this.xcontext)).thenReturn(newDocument);
        when(newDocument.getDocumentReference()).thenReturn(newReference);
        when(newDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        XDOM xdom = mock(XDOM.class);
        when(newDocument.getXDOM()).thenReturn(xdom);

        ResourceReference docLinkReference = new ResourceReference("C", ResourceType.DOCUMENT);
        LinkBlock docLinkBlock = new LinkBlock(Collections.emptyList(), docLinkReference, false);

        ResourceReference spaceLinkReference = new ResourceReference("Z", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(Collections.emptyList(), spaceLinkReference, false);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(Arrays.asList(docLinkBlock, spaceLinkBlock));

        DocumentReference originalDocLinkReference = new DocumentReference("C", oldReference.getLastSpaceReference());
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, oldReference))
            .thenReturn(originalDocLinkReference);
        DocumentReference newDocLinkReference = new DocumentReference("C", newReference.getLastSpaceReference());
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, newReference))
            .thenReturn(newDocLinkReference);

        SpaceReference originalSpaceReference = new SpaceReference("wiki1", "Z");
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, oldReference))
            .thenReturn(originalSpaceReference);
        SpaceReference newSpaceReference = new SpaceReference("wiki2", "Z");
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, newReference))
            .thenReturn(newSpaceReference);

        when(this.compactEntityReferenceSerializer.serialize(originalDocLinkReference, newReference))
            .thenReturn("wiki1:A.C");
        when(this.compactEntityReferenceSerializer.serialize(originalSpaceReference, newReference))
            .thenReturn("wiki1:Z");

        this.refactoring.updateRelativeLinks(oldReference, newReference);

        // Document link block is updated.
        assertEquals("wiki1:A.C", docLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, docLinkBlock.getReference().getType());
        // Space link is also updated, since they were referring entities on a different wiki.
        assertEquals("wiki1:Z", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.SPACE, spaceLinkBlock.getReference().getType());
        verifyDocumentSave(newDocument, "Updated the relative links.", true, true);
    }

    @Test
    void renameLinks() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        // From a terminal document to another terminal document.
        DocumentReference oldLinkTarget = new DocumentReference("wiki", "A", "B");
        DocumentReference newLinkTarget = new DocumentReference("wiki", "X", "Y");

        // Setup document content
        ResourceReference linkReference = new ResourceReference("A.B", ResourceType.DOCUMENT);
        LinkBlock linkBlock = new LinkBlock(Collections.emptyList(), linkReference, false);
        XDOM xdom = new XDOM(Collections.singletonList(linkBlock));
        when(document.getXDOM()).thenReturn(xdom);

        // Setup object content
        ResourceReference xobjectLinkReference = new ResourceReference("A.B", ResourceType.DOCUMENT);
        LinkBlock xobjectLinkBlock = new LinkBlock(Collections.emptyList(), xobjectLinkReference, false);
        XDOM xobjectXDOM = new XDOM(Collections.singletonList(xobjectLinkBlock));
        setTextarea(document, xobjectXDOM);

        when(this.resourceReferenceResolver.resolve(linkReference, null, documentReference)).thenReturn(oldLinkTarget);
        when(this.resourceReferenceResolver.resolve(xobjectLinkReference, null, documentReference))
            .thenReturn(oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);

        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");

        this.refactoring.renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.Y", linkBlock.getReference().getReference());
        assertEquals("X.Y", xobjectLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, linkBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false, false);
    }

    @Test
    void renameImage() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        // From a terminal document to another terminal document.
        DocumentReference oldImageTarget = new DocumentReference("wiki", "A", "B");
        AttachmentReference oldImageTargetAttachment = new AttachmentReference("attachment.txt", oldImageTarget);
        DocumentReference newImageTarget = new DocumentReference("wiki", "X", "Y");
        AttachmentReference newImageTargetAttachment = new AttachmentReference("attachment.txt", newImageTarget);

        XDOM xdom = mock(XDOM.class);
        when(document.getXDOM()).thenReturn(xdom);

        ResourceReference imageReference = new AttachmentResourceReference("A.B@attachment.txt");
        ImageBlock imageBlock = new ImageBlock(imageReference, false);
        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).thenReturn(Arrays.asList(imageBlock));

        when(this.resourceReferenceResolver.resolve(imageReference, null, documentReference))
            .thenReturn(oldImageTargetAttachment);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldImageTargetAttachment))
            .thenReturn(oldImageTarget);

        when(this.compactEntityReferenceSerializer.serialize(newImageTargetAttachment, documentReference))
            .thenReturn("X.Y@attachment.txt");

        this.refactoring.renameLinks(documentReference, oldImageTarget, newImageTarget);

        assertEquals("X.Y@attachment.txt", imageBlock.getReference().getReference());
        assertEquals(ResourceType.ATTACHMENT, imageBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false, false);
    }

    @Test
    public void renameAttachment() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        // From a terminal document to another terminal document.
        DocumentReference oldLinkTarget = new DocumentReference("wiki", "A", "B");
        AttachmentReference oldLinkTargetAttachment = new AttachmentReference("attachment.txt", oldLinkTarget);
        DocumentReference newLinkTarget = new DocumentReference("wiki", "X", "Y");
        AttachmentReference newLinkTargetAttachment = new AttachmentReference("attachment.txt", newLinkTarget);

        XDOM xdom = mock(XDOM.class);
        when(document.getXDOM()).thenReturn(xdom);

        ResourceReference linkReference = new AttachmentResourceReference("A.B@attachment.txt");
        LinkBlock linkBlock = new LinkBlock(Collections.emptyList(), linkReference, false);
        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).thenReturn(Arrays.asList(linkBlock));

        when(this.resourceReferenceResolver.resolve(linkReference, null, documentReference))
            .thenReturn(oldLinkTargetAttachment);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTargetAttachment)).thenReturn(oldLinkTarget);

        when(this.compactEntityReferenceSerializer.serialize(newLinkTargetAttachment, documentReference))
            .thenReturn("X.Y@attachment.txt");

        this.refactoring.renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.Y@attachment.txt", linkBlock.getReference().getReference());
        assertEquals(ResourceType.ATTACHMENT, linkBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false, false);
    }

    @Test
    void renameNonTerminalDocumentLinks() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        // From a non-terminal document to another non-terminal document.
        DocumentReference oldLinkTarget = new DocumentReference("wiki", "A", "WebHome");
        DocumentReference newLinkTarget = new DocumentReference("wiki", "X", "WebHome");

        XDOM xdom = mock(XDOM.class);
        when(document.getXDOM()).thenReturn(xdom);

        ResourceReference docLinkReference = new ResourceReference("A.WebHome", ResourceType.DOCUMENT);
        LinkBlock documentLinkBlock = new LinkBlock(Collections.emptyList(), docLinkReference, false);

        ResourceReference spaceLinkReference = new ResourceReference("A", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(Collections.emptyList(), spaceLinkReference, false);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(Arrays.asList(documentLinkBlock, spaceLinkBlock));

        // Doc link
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, documentReference))
            .thenReturn(oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.WebHome");

        // Space link
        SpaceReference spaceReference = oldLinkTarget.getLastSpaceReference();
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, documentReference))
            .thenReturn(spaceReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(spaceReference)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget.getLastSpaceReference(), documentReference))
            .thenReturn("X");

        this.refactoring.renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.WebHome", documentLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, documentLinkBlock.getReference().getType());
        assertEquals("X", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.SPACE, spaceLinkBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false, false);
    }

    @Test
    void renameNonTerminalToTerminalDocumentLinks() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        // From a non-terminal document to a terminal document.
        DocumentReference oldLinkTarget = new DocumentReference("wiki", "A", "WebHome");
        DocumentReference newLinkTarget = new DocumentReference("wiki", "X", "Y");

        XDOM xdom = mock(XDOM.class);
        when(document.getXDOM()).thenReturn(xdom);

        ResourceReference docLinkReference = new ResourceReference("A.WebHome", ResourceType.DOCUMENT);
        LinkBlock documentLinkBlock = new LinkBlock(Collections.emptyList(), docLinkReference, false);

        ResourceReference spaceLinkReference = new ResourceReference("A", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(Collections.emptyList(), spaceLinkReference, false);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(Arrays.asList(documentLinkBlock, spaceLinkBlock));

        // Doc link
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, documentReference))
            .thenReturn(oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");

        // Space link
        SpaceReference spaceReference = oldLinkTarget.getLastSpaceReference();
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, documentReference))
            .thenReturn(spaceReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(spaceReference)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget.getLastSpaceReference(), documentReference))
            .thenReturn("X");

        this.refactoring.renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        // Note that both resulting renamed back-links are of type document. (i.e. the space link was converted to a doc
        // link)
        assertEquals("X.Y", documentLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, documentLinkBlock.getReference().getType());
        assertEquals("X.Y", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, spaceLinkBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false, false);
    }

    @Test
    void renameLinksFromMacros() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

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
            .thenReturn(Arrays.asList(includeMacroBlock1, includeMacroBlock2, displayMacroBlock));

        ResourceReference macroResourceReference = new ResourceReference("A.B", ResourceType.DOCUMENT);

        when(this.resourceReferenceResolver.resolve(macroResourceReference, null, documentReference))
            .thenReturn(oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");

        this.refactoring.renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.Y", includeMacroBlock1.getParameter("reference"));
        assertEquals("X.Y", includeMacroBlock2.getParameter("document"));
        assertEquals("X.Y", displayMacroBlock.getParameter("reference"));
        verifyDocumentSave(document, "Renamed back-links.", false, false);
    }

    @Test
    void renameLinksFromLinksAndMacros() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        // From a terminal document to another terminal document.
        DocumentReference oldLinkTarget = new DocumentReference("wiki", "A", "B");
        DocumentReference newLinkTarget = new DocumentReference("wiki", "X", "Y");

        XDOM xdom = mock(XDOM.class);
        when(document.getXDOM()).thenReturn(xdom);

        Map<String, String> includeParameters = new HashMap<String, String>();
        includeParameters.put("reference", "A.B");
        MacroBlock includeMacroBlock = new MacroBlock("include", includeParameters, false);

        ResourceReference resourceReference = new ResourceReference("A.B", ResourceType.DOCUMENT);
        LinkBlock documentLinkBlock = new LinkBlock(Collections.emptyList(), resourceReference, false);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(Arrays.asList(includeMacroBlock, documentLinkBlock));

        when(this.resourceReferenceResolver.resolve(resourceReference, null, documentReference))
            .thenReturn(oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");

        this.refactoring.renameLinks(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.Y", includeMacroBlock.getParameter("reference"));
        assertEquals("X.Y", documentLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, documentLinkBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false, false);
    }

    private void verifyDocumentSave(XWikiDocument document, String comment, boolean minorEdit, boolean relative)
        throws Exception
    {
        // Verify we preserve the content author.
        verify(document).setContentDirty(false);
        // Verify the version is going to be incremented.
        verify(document).setMetaDataDirty(true);
        verify(this.xcontext.getWiki()).saveDocument(document, comment, minorEdit, this.xcontext);
        ILoggingEvent logEvent = this.logCapture.getLogEvent(this.logIndex++);
        assertEquals(Level.INFO, logEvent.getLevel());
        if (relative) {
            assertEquals("Updated the relative links from [{}].", logEvent.getMessage());
        } else {
            assertEquals("The links from [{}] that were targeting [{}] have been updated to target [{}].",
                logEvent.getMessage());
        }
    }

    @Test
    void renameLinksAndTranslations() throws Exception
    {
        DocumentReference baseDocumentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument baseDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(baseDocumentReference, this.xcontext)).thenReturn(baseDocument);
        when(baseDocument.getDocumentReference()).thenReturn(baseDocumentReference);

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
            LinkBlock linkBlock = new LinkBlock(Collections.emptyList(), linkReference, false);
            when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).thenReturn(Arrays.asList(linkBlock));

            when(this.resourceReferenceResolver.resolve(linkReference, null, documentReference))
                .thenReturn(oldLinkTarget);
            when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);

            when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");
        }
        this.refactoring.renameLinks(baseDocumentReference, oldLinkTarget, newLinkTarget);

        for (XWikiDocument xWikiDocument : documentsToUpdate) {
            verifyDocumentSave(xWikiDocument, "Renamed back-links.", false, false);
            LinkBlock linkBlock =
                (LinkBlock) xWikiDocument.getXDOM().getBlocks(mock(BlockMatcher.class), Block.Axes.DESCENDANT).get(0);
            assertEquals("X.Y", linkBlock.getReference().getReference());
            assertEquals(ResourceType.DOCUMENT, linkBlock.getReference().getType());
        }
    }

    @Test
    void renameLinksBlockRendererNotFound() throws XWikiException
    {
        DocumentReference newReference = new DocumentReference("xwiki", "XWiki", "new");

        XWiki xWiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(xWiki);
        XWikiDocument document = mock(XWikiDocument.class);
        when(xWiki.getDocument(newReference, this.xcontext)).thenReturn(document);

        ComponentManager componentManager = mock(ComponentManager.class);
        when(this.componentManagerProvider.get()).thenReturn(componentManager);
        when(document.getSyntax()).thenReturn(Syntax.MARKDOWN_1_1);
        when(componentManager.hasComponent(BlockRenderer.class, Syntax.MARKDOWN_1_1.toIdString()))
            .thenReturn(false);

        DocumentReference oldReference = new DocumentReference("xwiki", "XWiki", "old");
        this.refactoring.updateRelativeLinks(oldReference, newReference);

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("We can't rename the links from [null] because there is no renderer available for its "
                         + "syntax [Markdown 1.1].", this.logCapture.getMessage(0));
    }
}
