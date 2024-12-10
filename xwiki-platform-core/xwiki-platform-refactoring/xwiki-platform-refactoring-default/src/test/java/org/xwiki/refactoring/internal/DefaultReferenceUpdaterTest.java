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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.job.Job;
import org.xwiki.job.JobContext;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
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
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.MacroRefactoring;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultReferenceUpdater}.
 *
 * @version $Id$
 */
@ComponentTest
// @formatter:off
@ComponentList({
    ResourceReferenceRenamer.class,
    DefaultReferenceRenamer.class
})
// @formatter:on
class DefaultReferenceUpdaterTest
{
    @MockComponent
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @MockComponent
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    @MockComponent
    private EntityReferenceResolver<ResourceReference> resourceReferenceResolver;

    @MockComponent
    private PageReferenceResolver<EntityReference> defaultReferencePageReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @MockComponent
    Provider<MacroRefactoring> macroRefactoringProvider;

    @MockComponent
    private ContentParser contentParser;

    @MockComponent
    private JobContext jobContext;

    @MockComponent
    @Named("relative")
    private EntityReferenceResolver<ResourceReference> relativeEntityReferenceResolver;

    @Mock
    private Job job;

    @Mock
    private Request jobrequest;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named("xwiki/2.1")
    private BlockRenderer blockRenderer;

    @MockComponent
    private JobProgressManager progressManager;

    @InjectMockComponents
    private DefaultReferenceUpdater updater;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension();

    @Mock
    private XWikiContext xcontext;

    private MutableRenderingContext mutableRenderingContext;

    int logIndex = 0;

    @BeforeComponent
    void setup(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        mockitoComponentManager.registerComponent(ComponentManager.class, "context", mockitoComponentManager);
        this.mutableRenderingContext = mock(MutableRenderingContext.class);
        this.componentManager.registerComponent(RenderingContext.class, this.mutableRenderingContext);
    }

    @BeforeEach
    void beforeEach() throws Exception
    {
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(xwiki);
        when(xwiki.exists(any(DocumentReference.class), eq(this.xcontext))).thenReturn(true);

        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.componentManagerProvider.get()).thenReturn(this.componentManager);

        when(this.jobContext.getCurrentJob()).thenReturn(this.job);
        when(this.job.getRequest()).thenReturn(jobrequest);
        when(this.jobrequest.isVerbose()).thenReturn(true);
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
            .thenReturn(Map.of(documentReference, List.of(baseObject)));

        when(this.contentParser.parse("areacontent", Syntax.XWIKI_2_1, documentReference)).thenReturn(xdom);
    }

    @Test
    void updateRelativeLinks() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("wiki", "A", "B");
        AttachmentReference oldImageTargetAttachment = new AttachmentReference("attachment.txt", oldReference);
        DocumentReference newReference = new DocumentReference("wiki", "X", "Y");
        AttachmentReference newImageTargetAttachment = new AttachmentReference("attachment.txt", newReference);
        AttachmentReference absoluteTargetAttachment = new AttachmentReference("attachment.txt",
            new DocumentReference("wiki", "Main", "WebHome"));

        XWikiDocument newDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(newReference, this.xcontext)).thenReturn(newDocument);
        when(newDocument.getDocumentReference()).thenReturn(newReference);
        when(newDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        // Setup document content
        ResourceReference docLinkReference = new ResourceReference("C", ResourceType.DOCUMENT);
        LinkBlock docLinkBlock = new LinkBlock(List.of(), docLinkReference, false);
        ResourceReference spaceLinkReference = new ResourceReference("Z", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(List.of(), spaceLinkReference, false);
        ResourceReference imageReference = new AttachmentResourceReference("attachment.txt");
        ImageBlock imageBlock = new ImageBlock(imageReference, false);
        ResourceReference absoluteDocLinkResourceReference = new ResourceReference("xwiki:C.WebHome",
            ResourceType.DOCUMENT);
        LinkBlock absoluteDocLinkBlock =
            new LinkBlock(List.of(), absoluteDocLinkResourceReference, false);
        when(newDocument.getXDOM())
            .thenReturn(new XDOM(List.of(docLinkBlock, spaceLinkBlock, imageBlock, absoluteDocLinkBlock)));

        // Setup object content
        ResourceReference xobjectDocLinkReference = new ResourceReference("C", ResourceType.DOCUMENT);
        LinkBlock xobjectDocLinkBlock = new LinkBlock(List.of(), xobjectDocLinkReference, false);
        ResourceReference xobjectSpaceLinkReference = new ResourceReference("Z", ResourceType.SPACE);
        LinkBlock xobjectSpaceLinkBlock =
            new LinkBlock(List.of(), xobjectSpaceLinkReference, false);
        ResourceReference xobjectImageReference = new AttachmentResourceReference("attachment.txt");
        ImageBlock xobjectImageBlock = new ImageBlock(xobjectImageReference, false);
        setTextarea(newDocument,
            new XDOM(List.of(xobjectDocLinkBlock, xobjectSpaceLinkBlock, xobjectImageBlock)));

        DocumentReference originalDocLinkReference = new DocumentReference("WebHome",
            new SpaceReference("C", oldReference.getLastSpaceReference()));
        DocumentReference absoluteDocLinkReference = new DocumentReference("WebHome", new SpaceReference("wiki", "C"));
        EntityReference relativeDocLinkReference = new EntityReference("WebHome", EntityType.DOCUMENT,
            new EntityReference("C", EntityType.SPACE));
        when(this.resourceReferenceResolver.resolve(docLinkReference, null))
            .thenReturn(absoluteDocLinkReference);
        when(this.relativeEntityReferenceResolver.resolve(docLinkReference, null, null))
            .thenReturn(relativeDocLinkReference);
        when(this.resourceReferenceResolver.resolve(absoluteDocLinkResourceReference, null))
            .thenReturn(absoluteDocLinkReference);
        when(this.relativeEntityReferenceResolver.resolve(absoluteDocLinkResourceReference, null, null))
            .thenReturn(absoluteDocLinkReference);
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, oldReference))
            .thenReturn(originalDocLinkReference);
        when(this.resourceReferenceResolver.resolve(absoluteDocLinkResourceReference, null, oldReference))
            .thenReturn(absoluteDocLinkReference);
        when(this.resourceReferenceResolver.resolve(xobjectDocLinkReference, null))
            .thenReturn(absoluteDocLinkReference);
        when(this.relativeEntityReferenceResolver.resolve(xobjectDocLinkReference, null, null))
            .thenReturn(relativeDocLinkReference);
        when(this.resourceReferenceResolver.resolve(xobjectDocLinkReference, null, oldReference))
            .thenReturn(originalDocLinkReference);
        when(this.resourceReferenceResolver.resolve(imageReference, null))
            .thenReturn(absoluteTargetAttachment);
        when(this.resourceReferenceResolver.resolve(imageReference, null, oldReference))
            .thenReturn(oldImageTargetAttachment);
        when(this.relativeEntityReferenceResolver.resolve(imageReference, null, null))
            .thenReturn(relativeDocLinkReference);
        DocumentReference newDocLinkReference = new DocumentReference("WebHome",
            new SpaceReference("C", newReference.getLastSpaceReference()));
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, newReference))
            .thenReturn(newDocLinkReference);
        when(this.resourceReferenceResolver.resolve(xobjectDocLinkReference, null, newReference))
            .thenReturn(newDocLinkReference);
        when(this.resourceReferenceResolver.resolve(imageReference, null, newReference))
            .thenReturn(newImageTargetAttachment);
        when(this.resourceReferenceResolver.resolve(absoluteDocLinkResourceReference, null, newReference))
            .thenReturn(new DocumentReference("WebHome", new SpaceReference("wiki", "E")));

        SpaceReference originalSpaceReference = new SpaceReference("wiki", "Z");
        EntityReference relativeSpaceReference = new EntityReference("Z", EntityType.SPACE);
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null))
            .thenReturn(originalSpaceReference);
        when(this.relativeEntityReferenceResolver.resolve(spaceLinkReference, null, null))
            .thenReturn(relativeSpaceReference);
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, oldReference))
            .thenReturn(originalSpaceReference);
        when(this.resourceReferenceResolver.resolve(xobjectSpaceLinkReference, null))
            .thenReturn(originalSpaceReference);
        when(this.resourceReferenceResolver.resolve(xobjectSpaceLinkReference, null, oldReference))
            .thenReturn(originalSpaceReference);
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, newReference))
            .thenReturn(originalSpaceReference);
        when(this.resourceReferenceResolver.resolve(xobjectSpaceLinkReference, null, newReference))
            .thenReturn(originalSpaceReference);

        when(this.compactEntityReferenceSerializer.serialize(originalDocLinkReference, newReference)).thenReturn("A.C");
        when(this.defaultEntityReferenceSerializer.serialize(absoluteDocLinkReference, newReference))
            .thenReturn("wiki:C.WebHome");

        updater.update(newReference, oldReference, newReference);

        // Document link block is updated.
        assertEquals("A.C", docLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, docLinkBlock.getReference().getType());
        assertEquals("wiki:C.WebHome", absoluteDocLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, absoluteDocLinkBlock.getReference().getType());
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
        LinkBlock docLinkBlock = new LinkBlock(List.of(), docLinkReference, false);

        ResourceReference spaceLinkReference = new ResourceReference("Z", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(List.of(), spaceLinkReference, false);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(List.of(docLinkBlock, spaceLinkBlock));

        DocumentReference originalDocLinkReference = new DocumentReference("C", oldReference.getLastSpaceReference());
        DocumentReference absoluteDocLinkReference = new DocumentReference("C", new SpaceReference("xwiki", "Main"));
        EntityReference relativeDocLinkReference = new EntityReference("C", EntityType.DOCUMENT,
            new EntityReference("B", EntityType.SPACE));
        EntityReference relativeSpaceLinkReference = new EntityReference("Z", EntityType.SPACE);
        when(this.resourceReferenceResolver.resolve(docLinkReference, null)).thenReturn(absoluteDocLinkReference);
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, oldReference))
            .thenReturn(originalDocLinkReference);
        DocumentReference newDocLinkReference = new DocumentReference("C", newReference.getLastSpaceReference());
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, newReference))
            .thenReturn(newDocLinkReference);
        when(this.relativeEntityReferenceResolver.resolve(docLinkReference, null, null))
            .thenReturn(relativeDocLinkReference);

        SpaceReference originalSpaceReference = new SpaceReference("wiki1", "Z");
        SpaceReference absoluteSpaceReference = new SpaceReference("xwiki", "Z");
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null)).thenReturn(absoluteSpaceReference);
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, oldReference))
            .thenReturn(originalSpaceReference);
        SpaceReference newSpaceReference = new SpaceReference("wiki2", "Z");
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, newReference))
            .thenReturn(newSpaceReference);
        when(this.relativeEntityReferenceResolver.resolve(spaceLinkReference, null, null))
            .thenReturn(relativeSpaceLinkReference);

        when(this.compactEntityReferenceSerializer.serialize(originalDocLinkReference, newReference))
            .thenReturn("wiki1:A.C");
        when(this.compactEntityReferenceSerializer.serialize(originalSpaceReference, newReference))
            .thenReturn("wiki1:Z");

        updater.update(newReference, oldReference, newReference);

        // Document link block is updated.
        assertEquals("wiki1:A.C", docLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, docLinkBlock.getReference().getType());
        // Space link is also updated, since they were referring entities on a different wiki.
        assertEquals("wiki1:Z", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.SPACE, spaceLinkBlock.getReference().getType());
        verifyDocumentSave(newDocument, "Updated the relative links.", true, true);
    }

    @Test
    void update() throws Exception
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
        LinkBlock linkBlock = new LinkBlock(List.of(), linkReference, false);
        ResourceReference absoluteLinkReference = new ResourceReference("xwiki:A.B.WebHome", ResourceType.DOCUMENT);
        LinkBlock absoluteLinkBlock = new LinkBlock(List.of(), absoluteLinkReference, false);
        XDOM xdom = new XDOM(List.of(linkBlock, absoluteLinkBlock));
        when(document.getXDOM()).thenReturn(xdom);

        // Setup object content
        ResourceReference xobjectLinkReference = new ResourceReference("A.B", ResourceType.DOCUMENT);
        LinkBlock xobjectLinkBlock = new LinkBlock(List.of(), xobjectLinkReference, false);
        XDOM xobjectXDOM = new XDOM(List.of(xobjectLinkBlock));
        setTextarea(document, xobjectXDOM);

        EntityReference relativeLinkReference = new EntityReference("B", EntityType.DOCUMENT,
            new EntityReference("A", EntityType.SPACE));
        EntityReference relativeAbsoluteLinkReference = new EntityReference("WebHome", EntityType.DOCUMENT,
            new EntityReference("B", EntityType.SPACE, new EntityReference("B", EntityType.SPACE,
                new EntityReference("xwiki", EntityType.WIKI))));

        when(this.resourceReferenceResolver.resolve(linkReference, null)).thenReturn(oldLinkTarget);
        when(this.resourceReferenceResolver.resolve(linkReference, null, documentReference)).thenReturn(oldLinkTarget);
        when(this.resourceReferenceResolver.resolve(absoluteLinkReference, null)).thenReturn(oldLinkTarget);
        when(this.resourceReferenceResolver.resolve(absoluteLinkReference, null, documentReference))
            .thenReturn(oldLinkTarget);
        when(this.resourceReferenceResolver.resolve(xobjectLinkReference, null))
            .thenReturn(oldLinkTarget);
        when(this.resourceReferenceResolver.resolve(xobjectLinkReference, null, documentReference))
            .thenReturn(oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.relativeEntityReferenceResolver.resolve(xobjectLinkReference,null, null))
            .thenReturn(relativeLinkReference);
        when(this.relativeEntityReferenceResolver.resolve(linkReference, null, null))
            .thenReturn(relativeLinkReference);
        when(this.relativeEntityReferenceResolver.resolve(absoluteLinkReference, null, null))
            .thenReturn(relativeAbsoluteLinkReference);

        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");
        when(this.defaultEntityReferenceSerializer.serialize(newLinkTarget, documentReference))
            .thenReturn("xwiki:X.Y.WebHome");

        updater.update(documentReference, oldLinkTarget, newLinkTarget);

        assertEquals("X.Y", linkBlock.getReference().getReference());
        assertEquals("xwiki:X.Y.WebHome", absoluteLinkBlock.getReference().getReference());
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
        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).thenReturn(List.of(imageBlock));

        EntityReference relativeReference = new EntityReference("attachment.txt", EntityType.ATTACHMENT,
            new EntityReference("B", EntityType.DOCUMENT, new EntityReference("A", EntityType.SPACE)));

        when(this.resourceReferenceResolver.resolve(imageReference, null)).thenReturn(oldImageTargetAttachment);
        when(this.resourceReferenceResolver.resolve(imageReference, null, documentReference))
            .thenReturn(oldImageTargetAttachment);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldImageTargetAttachment))
            .thenReturn(oldImageTarget);
        when(this.relativeEntityReferenceResolver.resolve(imageReference, null, null)).thenReturn(relativeReference);

        when(this.compactEntityReferenceSerializer.serialize(newImageTargetAttachment, documentReference))
            .thenReturn("X.Y@attachment.txt");

        updater.update(documentReference, oldImageTarget, newImageTarget);

        assertEquals("X.Y@attachment.txt", imageBlock.getReference().getReference());
        assertEquals(ResourceType.ATTACHMENT, imageBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false, false);
    }

    @Test
    void renameAttachment() throws Exception
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
        LinkBlock linkBlock = new LinkBlock(List.of(), linkReference, false);
        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).thenReturn(List.of(linkBlock));

        EntityReference relativeReference = new EntityReference("attachment.txt", EntityType.ATTACHMENT,
            new EntityReference("B", EntityType.DOCUMENT, new EntityReference("A", EntityType.SPACE)));

        when(this.resourceReferenceResolver.resolve(linkReference, null))
            .thenReturn(oldLinkTargetAttachment);
        when(this.resourceReferenceResolver.resolve(linkReference, null, documentReference))
            .thenReturn(oldLinkTargetAttachment);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTargetAttachment)).thenReturn(oldLinkTarget);
        when(this.relativeEntityReferenceResolver.resolve(linkReference, null, null)).thenReturn(relativeReference);

        when(this.compactEntityReferenceSerializer.serialize(newLinkTargetAttachment, documentReference))
            .thenReturn("X.Y@attachment.txt");

        updater.update(documentReference, oldLinkTarget, newLinkTarget);

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
        LinkBlock documentLinkBlock = new LinkBlock(List.of(), docLinkReference, false);
        EntityReference relativeReference = new EntityReference("WebHome", EntityType.DOCUMENT,
            new EntityReference("A", EntityType.SPACE));

        ResourceReference spaceLinkReference = new ResourceReference("A", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(List.of(), spaceLinkReference, false);
        EntityReference relativeSpaceReference = new EntityReference("A", EntityType.SPACE);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(List.of(documentLinkBlock, spaceLinkBlock));

        // Doc link
        when(this.resourceReferenceResolver.resolve(docLinkReference, null))
            .thenReturn(oldLinkTarget);
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, documentReference))
            .thenReturn(oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.WebHome");
        when(this.relativeEntityReferenceResolver.resolve(docLinkReference, null, null)).thenReturn(relativeReference);

        // Space link
        SpaceReference spaceReference = oldLinkTarget.getLastSpaceReference();
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null))
            .thenReturn(spaceReference);
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, documentReference))
            .thenReturn(spaceReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(spaceReference)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget.getLastSpaceReference(), documentReference))
            .thenReturn("X");
        when(this.relativeEntityReferenceResolver.resolve(spaceLinkReference, null, null))
            .thenReturn(relativeSpaceReference);

        updater.update(documentReference, oldLinkTarget, newLinkTarget);

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
        LinkBlock documentLinkBlock = new LinkBlock(List.of(), docLinkReference, false);

        EntityReference relativeReference = new EntityReference("WebHome", EntityType.DOCUMENT,
            new EntityReference("A", EntityType.SPACE));

        ResourceReference spaceLinkReference = new ResourceReference("A", ResourceType.SPACE);
        LinkBlock spaceLinkBlock = new LinkBlock(List.of(), spaceLinkReference, false);

        EntityReference relativeSpaceReference = new EntityReference("A", EntityType.SPACE);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(List.of(documentLinkBlock, spaceLinkBlock));

        // Doc link
        when(this.resourceReferenceResolver.resolve(docLinkReference, null))
            .thenReturn(oldLinkTarget);
        when(this.resourceReferenceResolver.resolve(docLinkReference, null, documentReference))
            .thenReturn(oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");
        when(this.relativeEntityReferenceResolver.resolve(docLinkReference, null, null)).thenReturn(relativeReference);

        // Space link
        SpaceReference spaceReference = oldLinkTarget.getLastSpaceReference();
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null))
            .thenReturn(spaceReference);
        when(this.resourceReferenceResolver.resolve(spaceLinkReference, null, documentReference))
            .thenReturn(spaceReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(spaceReference)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget.getLastSpaceReference(), documentReference))
            .thenReturn("X");
        when(this.relativeEntityReferenceResolver.resolve(spaceLinkReference, null, null))
            .thenReturn(relativeSpaceReference);

        updater.update(documentReference, oldLinkTarget, newLinkTarget);

        // Note that both resulting renamed back-links are of type document. (i.e. the space link was converted to a doc
        // link)
        assertEquals("X.Y", documentLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, documentLinkBlock.getReference().getType());
        assertEquals("X.Y", spaceLinkBlock.getReference().getReference());
        assertEquals(ResourceType.DOCUMENT, spaceLinkBlock.getReference().getType());
        verifyDocumentSave(document, "Renamed back-links.", false, false);
    }

    @Test
    void updateFromMacros(MockitoComponentManager componentManager) throws Exception
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
        includeMacroBlock1.setParent(xdom);

        Map<String, String> includeOldParameters = new HashMap<String, String>();
        includeOldParameters.put("document", "A.B");
        MacroBlock includeMacroBlock2 = new MacroBlock("include", includeOldParameters, false);
        includeMacroBlock2.setParent(xdom);

        Map<String, String> displayParameters = new HashMap<String, String>();
        displayParameters.put("reference", "A.B");
        MacroBlock displayMacroBlock = new MacroBlock("display", displayParameters, false);
        displayMacroBlock.setParent(xdom);

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(List.of(includeMacroBlock1, includeMacroBlock2, displayMacroBlock));

        ResourceReference macroResourceReference = new ResourceReference("A.B", ResourceType.DOCUMENT);

        when(this.resourceReferenceResolver.resolve(macroResourceReference, null))
            .thenReturn(oldLinkTarget);
        when(this.resourceReferenceResolver.resolve(macroResourceReference, null, documentReference))
            .thenReturn(oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");

        MacroRefactoring includeMacroRefactoring =
            componentManager.registerMockComponent(MacroRefactoring.class, "include");
        MacroRefactoring displayMacroRefactoring =
            componentManager.registerMockComponent(MacroRefactoring.class, "display");
        when(displayMacroRefactoring.replaceReference(any(), any(), any(DocumentReference.class), any(), anyBoolean()
            , any()))
            .thenReturn(Optional.of(displayMacroBlock));
        when(this.documentAccessBridge.getDocumentInstance(documentReference)).thenReturn(document);
        updater.update(documentReference, oldLinkTarget, newLinkTarget);

        verify(includeMacroRefactoring).replaceReference(includeMacroBlock1, documentReference, oldLinkTarget,
            newLinkTarget, false, Map.of(oldLinkTarget, newLinkTarget));
        verify(includeMacroRefactoring).replaceReference(includeMacroBlock2, documentReference, oldLinkTarget,
            newLinkTarget, false, Map.of(oldLinkTarget, newLinkTarget));
        verify(displayMacroRefactoring).replaceReference(displayMacroBlock, documentReference, oldLinkTarget,
            newLinkTarget, false, Map.of(oldLinkTarget, newLinkTarget));
        verify(this.mutableRenderingContext, times(3)).push(any(), any(), eq(Syntax.XWIKI_2_1), any(), anyBoolean(),
            any());
        verify(this.mutableRenderingContext, times(3)).pop();
        verifyDocumentSave(document, "Renamed back-links.", false, false);
    }

    @Test
    void updateAttachments() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        DocumentReference sourceDocument = new DocumentReference("wiki", "Space", "Source");
        AttachmentReference oldLinkTarget = new AttachmentReference("oldname.txt", sourceDocument);
        DocumentReference targetDocument = new DocumentReference("wiki", "Space", "Target");
        AttachmentReference newLinkTarget = new AttachmentReference("newname.txt", targetDocument);
        ResourceReference resourceReference = new ResourceReference("oldname.txt", ResourceType.ATTACHMENT);
        LinkBlock documentLinkBlock = new LinkBlock(List.of(), resourceReference, false);

        XWikiDocument document = mock(XWikiDocument.class);
        XDOM xdom = mock(XDOM.class);

        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getTranslationLocales(this.xcontext)).thenReturn(List.of());
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        when(document.getXDOM()).thenReturn(xdom);
        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).thenReturn(List.of(documentLinkBlock));
        when(this.resourceReferenceResolver.resolve(resourceReference, null)).thenReturn(
            new AttachmentReference("oldname.txt", new DocumentReference("wiki", "Main", "WebHome")));
        when(this.resourceReferenceResolver.resolve(resourceReference, null, documentReference)).thenReturn(
            oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn(
            "newname.txt");

        updater.update(documentReference, oldLinkTarget, newLinkTarget);

        verify(this.progressManager).pushLevelProgress(1, updater);
        verify(document).setContent(xdom);
        verify(document).setContentDirty(false);
        verify(document).setMetaDataDirty(true);
        verify(this.xcontext.getWiki()).saveDocument(document, "Renamed back-links.", false, this.xcontext);
        verify(this.progressManager).popLevelProgress(updater);
        assertEquals(1, this.logCapture.size());
        assertEquals(
            "The links from [null] that were targeting [Attachment wiki:Space.Source@oldname.txt] have "
                + "been updated to target [Attachment wiki:Space.Target@newname.txt].", this.logCapture.getMessage(0));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void updateFromLinksAndMacros(MockitoComponentManager componentManager) throws Exception
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

        Map<String, String> includeParameters = new HashMap<>();
        includeParameters.put("reference", "A.B");
        MacroBlock includeMacroBlock = new MacroBlock("include", includeParameters, false);

        ResourceReference resourceReference = new ResourceReference("A.B", ResourceType.DOCUMENT);
        LinkBlock documentLinkBlock = new LinkBlock(List.of(), resourceReference, false);
        EntityReference relativeReference = new EntityReference("B", EntityType.DOCUMENT,
            new EntityReference("A", EntityType.SPACE));

        when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT)))
            .thenReturn(List.of(includeMacroBlock, documentLinkBlock));

        when(this.resourceReferenceResolver.resolve(resourceReference, null))
            .thenReturn(oldLinkTarget);
        when(this.resourceReferenceResolver.resolve(resourceReference, null, documentReference))
            .thenReturn(oldLinkTarget);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
        when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");
        when(this.relativeEntityReferenceResolver.resolve(resourceReference, null, null)).thenReturn(relativeReference);

        MacroRefactoring includeMacroRefactoring =
            componentManager.registerMockComponent(MacroRefactoring.class, "include");
        when(this.documentAccessBridge.getDocumentInstance(documentReference)).thenReturn(document);
        updater.update(documentReference, oldLinkTarget, newLinkTarget);

        verify(includeMacroRefactoring).replaceReference(includeMacroBlock, documentReference, oldLinkTarget,
            newLinkTarget, false, Map.of(oldLinkTarget, newLinkTarget));
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
        if (relative) {
            assertEquals("Updated the relative links from [{}].", logEvent.getMessage());
        } else {
            assertEquals("The links from [{}] that were targeting [{}] have been updated to target [{}].",
                logEvent.getMessage());
        }
        assertEquals(Level.INFO, logEvent.getLevel());
    }

    @Test
    void updateAndTranslations() throws Exception
    {
        DocumentReference baseDocumentReference = new DocumentReference("wiki", "Space", "Page");
        XWikiDocument baseDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(baseDocumentReference, this.xcontext)).thenReturn(baseDocument);
        when(baseDocument.getDocumentReference()).thenReturn(baseDocumentReference);

        when(baseDocument.getTranslationLocales(xcontext)).thenReturn(List.of(Locale.FRENCH, Locale.ENGLISH));
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

        List<XWikiDocument> documentsToUpdate = List.of(baseDocument, frenchDocument, englishDocument);

        for (XWikiDocument xWikiDocument : documentsToUpdate) {
            DocumentReference documentReference = xWikiDocument.getDocumentReference();
            when(xWikiDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
            XDOM xdom = mock(XDOM.class);
            when(xWikiDocument.getXDOM()).thenReturn(xdom);

            ResourceReference linkReference = new ResourceReference("A.B", ResourceType.DOCUMENT);
            EntityReference relativeReference = new EntityReference("B", EntityType.DOCUMENT,
                new EntityReference("A", EntityType.SPACE));
            LinkBlock linkBlock = new LinkBlock(List.of(), linkReference, false);
            when(xdom.getBlocks(any(), eq(Block.Axes.DESCENDANT))).thenReturn(List.of(linkBlock));

            when(this.resourceReferenceResolver.resolve(linkReference, null))
                .thenReturn(oldLinkTarget);
            when(this.resourceReferenceResolver.resolve(linkReference, null, documentReference))
                .thenReturn(oldLinkTarget);
            when(this.defaultReferenceDocumentReferenceResolver.resolve(oldLinkTarget)).thenReturn(oldLinkTarget);
            when(this.compactEntityReferenceSerializer.serialize(newLinkTarget, documentReference)).thenReturn("X.Y");
            when(this.relativeEntityReferenceResolver.resolve(linkReference, null, null))
                .thenReturn(relativeReference);
        }
        updater.update(baseDocumentReference, oldLinkTarget, newLinkTarget);

        for (XWikiDocument xWikiDocument : documentsToUpdate) {
            verifyDocumentSave(xWikiDocument, "Renamed back-links.", false, false);
            LinkBlock linkBlock =
                (LinkBlock) xWikiDocument.getXDOM().getBlocks(mock(BlockMatcher.class), Block.Axes.DESCENDANT).get(0);
            assertEquals("X.Y", linkBlock.getReference().getReference());
            assertEquals(ResourceType.DOCUMENT, linkBlock.getReference().getType());
        }
    }

    @Test
    void updateBlockRendererNotFound() throws XWikiException
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
        updater.update(newReference, oldReference, newReference);

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("We can't rename the links from [null] because there is no renderer available for its "
                         + "syntax [Markdown 1.1].", this.logCapture.getMessage(0));
    }
}
