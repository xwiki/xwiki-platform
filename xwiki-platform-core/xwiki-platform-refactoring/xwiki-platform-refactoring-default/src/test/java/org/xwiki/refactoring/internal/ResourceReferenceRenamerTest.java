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

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.SpaceResourceReference;
import org.xwiki.rendering.resolver.DocumentExistenceOverride;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ResourceReferenceRenamer}.
 *
 * @version $Id$
 * @since 14.2RC1
 */
@ComponentTest
class ResourceReferenceRenamerTest
{
    @InjectMockComponents
    private ResourceReferenceRenamer renamer;

    @MockComponent
    private EntityReferenceResolver<ResourceReference> entityReferenceResolver;

    @MockComponent
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @MockComponent
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @MockComponent
    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    @MockComponent
    private PageReferenceResolver<EntityReference> defaultReferencePageReferenceResolver;

    @MockComponent
    @Named("relative")
    private EntityReferenceResolver<ResourceReference> relativeEntityReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private static DocumentExistenceOverride beforeRename(DocumentReference oldReference,
        DocumentReference newReference)
    {
        return new DocumentExistenceOverride(Map.of(oldReference, true, newReference, false));
    }

    @BeforeEach
    void setup() throws XWikiException
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);

        XWiki xWiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xWiki);
        when(xWiki.exists(any(DocumentReference.class), eq(context))).thenReturn(true);
    }

    @Test
    void updateResourceReferenceRelativeOtherWiki()
    {
        DocumentResourceReference resourceReference = new DocumentResourceReference("Main.WebHome");
        AttachmentReference oldReference =
            new AttachmentReference("file.txt", new DocumentReference("wiki", "space", "page"));

        EntityReference relativeReference = new EntityReference("file.txt", EntityType.ATTACHMENT,
            new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE,
                new EntityReference("wiki", EntityType.WIKI))));
        AttachmentReference newReference =
            new AttachmentReference("file2.txt", new DocumentReference("wiki", "space", "page"));
        when(this.entityReferenceResolver.resolve(resourceReference, null, newReference)).thenReturn(newReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, oldReference)).thenReturn(oldReference);
        when(this.relativeEntityReferenceResolver.resolve(resourceReference, null, null)).thenReturn(relativeReference);
        when(this.compactEntityReferenceSerializer.serialize(oldReference, newReference)).thenReturn("file2.txt");

        assertTrue(this.renamer.updateResourceReference(resourceReference,
            oldReference,
            newReference,
            new DocumentReference("xwiki", "Space", "Page"), true, Map.of()));

        verify(this.defaultEntityReferenceSerializer).serialize(oldReference, newReference);
    }

    @Test
    void updateResourceReferenceRelativeSameWiki()
    {
        DocumentResourceReference resourceReference = new DocumentResourceReference("Main.WebHome");
        AttachmentReference oldReference =
            new AttachmentReference("file.txt", new DocumentReference("wiki", "space", "page"));

        EntityReference relativeReference = new EntityReference("file.txt", EntityType.ATTACHMENT,
            new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE)));
        AttachmentReference newReference =
            new AttachmentReference("file2.txt", new DocumentReference("wiki", "space", "page"));
        when(this.entityReferenceResolver.resolve(resourceReference, null, newReference)).thenReturn(newReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, oldReference)).thenReturn(oldReference);
        when(this.relativeEntityReferenceResolver.resolve(resourceReference, null, null)).thenReturn(relativeReference);
        when(this.compactEntityReferenceSerializer.serialize(oldReference, newReference)).thenReturn("file2.txt");

        assertTrue(this.renamer.updateResourceReference(resourceReference,
            oldReference,
            newReference,
            new DocumentReference("xwiki", "Space", "Page"), true, Map.of()));

        verify(this.compactEntityReferenceSerializer).serialize(oldReference, newReference);
    }

    @Test
    void updateResourceReferenceNotRelativeWithAbsoluteImageInMovedDocument()
    {
        // The image is located in a document that is itself moved by the same job (e.g. a child of the renamed page)
        // and targets an attachment of another moved document, using an absolute reference.
        AttachmentResourceReference resourceReference =
            new AttachmentResourceReference("Red.Green.WebHome@image.jpg");
        DocumentReference oldReference = new DocumentReference("WebHome", new SpaceReference("wiki", "Red", "Green"));
        DocumentReference newReference =
            new DocumentReference("WebHome", new SpaceReference("wiki", "Pink", "Green"));
        DocumentReference currentDocumentReference =
            new DocumentReference("WebHome", new SpaceReference("wiki", "Red", "Green", "Blue"));
        AttachmentReference oldAttachmentReference = new AttachmentReference("image.jpg", oldReference);
        AttachmentReference newAttachmentReference = new AttachmentReference("image.jpg", newReference);

        when(this.entityReferenceResolver.resolve(resourceReference, null, currentDocumentReference,
            beforeRename(oldReference, newReference)))
            .thenReturn(oldAttachmentReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, null,
            beforeRename(oldReference, newReference))).thenReturn(oldAttachmentReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldAttachmentReference)).thenReturn(oldReference);
        when(this.relativeEntityReferenceResolver.resolve(resourceReference, null, null))
            .thenReturn(new EntityReference("image.jpg", EntityType.ATTACHMENT,
                new EntityReference("WebHome", EntityType.DOCUMENT, new EntityReference("Green", EntityType.SPACE,
                    new EntityReference("Red", EntityType.SPACE)))));
        when(this.compactEntityReferenceSerializer.serialize(newAttachmentReference, currentDocumentReference))
            .thenReturn("Pink.Green.WebHome@image.jpg");

        assertTrue(this.renamer.updateResourceReference(resourceReference, oldReference, newReference,
            currentDocumentReference, false, Map.of(oldReference, newReference, currentDocumentReference,
                new DocumentReference("WebHome", new SpaceReference("wiki", "Pink", "Green", "Blue")))));
        assertEquals(new AttachmentResourceReference("Pink.Green.WebHome@image.jpg"), resourceReference);
    }

    @Test
    void updateResourceReferenceNotRelativeWithAbsoluteSpaceLinkInMovedDocument()
    {
        // The link is located in a document that is itself moved by the same job (e.g. a child of the renamed page)
        // and targets another moved (non-terminal) document, using an absolute space reference.
        SpaceResourceReference resourceReference = new SpaceResourceReference("Red.Green");
        DocumentReference oldReference = new DocumentReference("WebHome", new SpaceReference("wiki", "Red", "Green"));
        DocumentReference newReference =
            new DocumentReference("WebHome", new SpaceReference("wiki", "Pink", "Green"));
        DocumentReference currentDocumentReference =
            new DocumentReference("WebHome", new SpaceReference("wiki", "Red", "Green", "Blue"));
        SpaceReference oldSpaceReference = oldReference.getLastSpaceReference();
        SpaceReference newSpaceReference = newReference.getLastSpaceReference();

        when(this.entityReferenceResolver.resolve(resourceReference, null, currentDocumentReference,
            beforeRename(oldReference, newReference)))
            .thenReturn(oldSpaceReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, null,
            beforeRename(oldReference, newReference))).thenReturn(oldSpaceReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldSpaceReference)).thenReturn(oldReference);
        when(this.relativeEntityReferenceResolver.resolve(resourceReference, null, null))
            .thenReturn(new EntityReference("Green", EntityType.SPACE, new EntityReference("Red", EntityType.SPACE)));
        when(this.compactEntityReferenceSerializer.serialize(newSpaceReference, currentDocumentReference))
            .thenReturn("Pink.Green");

        assertTrue(this.renamer.updateResourceReference(resourceReference, oldReference, newReference,
            currentDocumentReference, false, Map.of(oldReference, newReference, currentDocumentReference,
                new DocumentReference("WebHome", new SpaceReference("wiki", "Pink", "Green", "Blue")))));
        assertEquals(new SpaceResourceReference("Pink.Green"), resourceReference);
    }

    @Test
    void updateResourceReferenceNotRelative()
    {
        AttachmentResourceReference resourceReference = new AttachmentResourceReference("image.png");
        AttachmentReference oldReference =
            new AttachmentReference("file.txt", new DocumentReference("wiki", "space", "page"));
        AttachmentReference newReference =
            new AttachmentReference("file2.txt", new DocumentReference("wiki", "space", "page"));
        AttachmentReference absoluteReference =
            new AttachmentReference("image.png", new DocumentReference("xwiki", "Main", "WebHome"));
        DocumentReference currentDocumentReference = new DocumentReference("xwiki", "Space", "Page");

        when(this.entityReferenceResolver.resolve(resourceReference, null))
            .thenReturn(absoluteReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, currentDocumentReference))
            .thenReturn(oldReference);
        when(this.compactEntityReferenceSerializer.serialize(newReference, currentDocumentReference))
            .thenReturn("xwiki:Space.Page.file2.txt");

        assertTrue(this.renamer.updateResourceReference(resourceReference, oldReference, newReference,
            currentDocumentReference,
            false, Map.of()));
        assertEquals(new AttachmentResourceReference("xwiki:Space.Page.file2.txt"), resourceReference);
    }

    @Test
    void updateResourceReferenceNotRelativeWithImageOfRenamedTerminalDocument()
    {
        // The image targets an attachment of a renamed terminal document, which doesn't exist anymore: it's resolved
        // as before the rename to find out it targets the renamed document.
        AttachmentResourceReference resourceReference = new AttachmentResourceReference("Space.Page@image.png");
        resourceReference.setTyped(false);
        DocumentReference oldReference = new DocumentReference("xwiki", "Space", "Page");
        DocumentReference newReference = new DocumentReference("xwiki", "NewSpace", "Page");
        DocumentReference currentDocumentReference = new DocumentReference("xwiki", "Other", "Page");
        AttachmentReference oldAttachmentReference = new AttachmentReference("image.png", oldReference);
        AttachmentReference newAttachmentReference = new AttachmentReference("image.png", newReference);

        when(this.entityReferenceResolver.resolve(resourceReference, null, currentDocumentReference,
            beforeRename(oldReference, newReference))).thenReturn(oldAttachmentReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, null,
            beforeRename(oldReference, newReference))).thenReturn(oldAttachmentReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldAttachmentReference)).thenReturn(oldReference);
        when(this.relativeEntityReferenceResolver.resolve(resourceReference, null, null))
            .thenReturn(new EntityReference("image.png", EntityType.ATTACHMENT,
                new EntityReference("Page", EntityType.DOCUMENT, new EntityReference("Space", EntityType.SPACE))));
        when(this.compactEntityReferenceSerializer.serialize(newAttachmentReference, currentDocumentReference))
            .thenReturn("NewSpace.Page@image.png");

        assertTrue(this.renamer.updateResourceReference(resourceReference, oldReference, newReference,
            currentDocumentReference, false, Map.of(oldReference, newReference)));
        AttachmentResourceReference expectedResourceReference =
            new AttachmentResourceReference("NewSpace.Page@image.png");
        expectedResourceReference.setTyped(false);
        assertEquals(expectedResourceReference, resourceReference);
    }

    @Test
    void updateResourceReferenceNotRelativeWithUntypedLinkToRenamedTerminalDocument()
    {
        // The untyped link targets a renamed terminal document, which doesn't exist anymore: it's resolved as before
        // the rename to find out it targets the renamed document.
        DocumentResourceReference resourceReference = new DocumentResourceReference("Space.Page");
        resourceReference.setTyped(false);
        DocumentReference oldReference = new DocumentReference("xwiki", "Space", "Page");
        DocumentReference newReference = new DocumentReference("xwiki", "NewSpace", "Page");
        DocumentReference currentDocumentReference = new DocumentReference("xwiki", "Other", "Page");

        when(this.entityReferenceResolver.resolve(resourceReference, null, currentDocumentReference,
            beforeRename(oldReference, newReference))).thenReturn(oldReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, null,
            beforeRename(oldReference, newReference))).thenReturn(oldReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldReference)).thenReturn(oldReference);
        when(this.relativeEntityReferenceResolver.resolve(resourceReference, null, null))
            .thenReturn(
                new EntityReference("Page", EntityType.DOCUMENT, new EntityReference("Space", EntityType.SPACE)));
        when(this.compactEntityReferenceSerializer.serialize(newReference, currentDocumentReference))
            .thenReturn("NewSpace.Page");

        assertTrue(this.renamer.updateResourceReference(resourceReference, oldReference, newReference,
            currentDocumentReference, false, Map.of(oldReference, newReference)));
        DocumentResourceReference expectedResourceReference = new DocumentResourceReference("NewSpace.Page");
        expectedResourceReference.setTyped(false);
        assertEquals(expectedResourceReference, resourceReference);
    }

    @Test
    void updateResourceReferenceNotRelativeWithUntypedLinkToRenamedSiblingSpace()
    {
        // The untyped link targets a renamed top level space home page from another top level space home page, which
        // the resolver only does when the targeted space home page exists: it's resolved as before the rename to find
        // out it targets the renamed document.
        DocumentResourceReference resourceReference = new DocumentResourceReference("PageA");
        resourceReference.setTyped(false);
        DocumentReference oldReference = new DocumentReference("xwiki", "PageA", "WebHome");
        DocumentReference newReference = new DocumentReference("xwiki", "PageA1", "WebHome");
        DocumentReference currentDocumentReference = new DocumentReference("xwiki", "PageB", "WebHome");

        when(this.entityReferenceResolver.resolve(resourceReference, null, currentDocumentReference,
            beforeRename(oldReference, newReference))).thenReturn(oldReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, null,
            beforeRename(oldReference, newReference)))
            .thenReturn(new DocumentReference("xwiki", List.of("Main", "PageA"), "WebHome"));
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldReference)).thenReturn(oldReference);
        when(this.relativeEntityReferenceResolver.resolve(resourceReference, null, null))
            .thenReturn(new EntityReference("PageA", EntityType.DOCUMENT));
        when(this.compactEntityReferenceSerializer.serialize(newReference, currentDocumentReference))
            .thenReturn("PageA1.WebHome");

        assertTrue(this.renamer.updateResourceReference(resourceReference, oldReference, newReference,
            currentDocumentReference, false, Map.of(oldReference, newReference)));
        DocumentResourceReference expectedResourceReference = new DocumentResourceReference("PageA1.WebHome");
        expectedResourceReference.setTyped(false);
        assertEquals(expectedResourceReference, resourceReference);
    }

    @Test
    void updateResourceReferenceNotRelativeWithImageOfOtherDocument()
    {
        // The image targets an attachment of another document, even when resolved as before the rename.
        AttachmentResourceReference resourceReference = new AttachmentResourceReference("Space.Other@image.png");
        resourceReference.setTyped(false);
        DocumentReference oldReference = new DocumentReference("xwiki", "Space", "Page");
        DocumentReference newReference = new DocumentReference("xwiki", "NewSpace", "Page");
        DocumentReference currentDocumentReference = new DocumentReference("xwiki", "Other", "Page");
        DocumentReference spaceHomeReference = new DocumentReference("xwiki", List.of("Space", "Other"), "WebHome");
        AttachmentReference spaceHomeAttachmentReference = new AttachmentReference("image.png", spaceHomeReference);

        when(this.entityReferenceResolver.resolve(resourceReference, null, currentDocumentReference,
            beforeRename(oldReference, newReference))).thenReturn(spaceHomeAttachmentReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, null,
            beforeRename(oldReference, newReference))).thenReturn(spaceHomeAttachmentReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(spaceHomeAttachmentReference))
            .thenReturn(spaceHomeReference);

        assertFalse(this.renamer.updateResourceReference(resourceReference, oldReference, newReference,
            currentDocumentReference, false, Map.of(oldReference, newReference)));
        assertEquals("Space.Other@image.png", resourceReference.getReference());
    }

    @Test
    void updateResourceReferenceRelativeWithAbsoluteLinkToMovedDocument()
    {
        DocumentResourceReference resourceReference = new DocumentResourceReference("Space.Old.WebHome");
        DocumentReference oldReference = new DocumentReference("wiki", List.of("Space", "Old"), "WebHome");
        DocumentReference newReference = new DocumentReference("wiki", List.of("Space", "New"), "WebHome");

        when(this.entityReferenceResolver.resolve(resourceReference, null, newReference)).thenReturn(oldReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, oldReference)).thenReturn(oldReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null)).thenReturn(oldReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldReference)).thenReturn(oldReference);
        when(this.relativeEntityReferenceResolver.resolve(resourceReference, null, null))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT,
                new EntityReference("Old", EntityType.SPACE, new EntityReference("Space", EntityType.SPACE))));
        when(this.compactEntityReferenceSerializer.serialize(newReference, newReference.getWikiReference()))
            .thenReturn("Space.New.WebHome");

        assertTrue(this.renamer.updateResourceReference(resourceReference, oldReference, newReference, newReference,
            true, Map.of(oldReference, newReference)));
        assertEquals(new DocumentResourceReference("Space.New.WebHome"), resourceReference);
    }

    @Test
    void updateResourceReferenceRelativeWithAbsoluteImageOfMovedDocument()
    {
        AttachmentResourceReference resourceReference =
            new AttachmentResourceReference("Space.Old.WebHome@image.jpg");
        DocumentReference oldReference = new DocumentReference("wiki", List.of("Space", "Old"), "WebHome");
        DocumentReference newReference = new DocumentReference("wiki", List.of("Space", "New"), "WebHome");
        AttachmentReference oldAttachmentReference = new AttachmentReference("image.jpg", oldReference);
        AttachmentReference newAttachmentReference = new AttachmentReference("image.jpg", newReference);

        when(this.entityReferenceResolver.resolve(resourceReference, null, newReference))
            .thenReturn(oldAttachmentReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, oldReference))
            .thenReturn(oldAttachmentReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null)).thenReturn(oldAttachmentReference);
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldAttachmentReference)).thenReturn(oldReference);
        when(this.relativeEntityReferenceResolver.resolve(resourceReference, null, null))
            .thenReturn(new EntityReference("image.jpg", EntityType.ATTACHMENT,
                new EntityReference("WebHome", EntityType.DOCUMENT, new EntityReference("Old", EntityType.SPACE,
                    new EntityReference("Space", EntityType.SPACE)))));
        when(this.compactEntityReferenceSerializer.serialize(newAttachmentReference, newReference.getWikiReference()))
            .thenReturn("Space.New.WebHome@image.jpg");

        assertTrue(this.renamer.updateResourceReference(resourceReference, oldReference, newReference, newReference,
            true, Map.of(oldReference, newReference)));
        assertEquals(new AttachmentResourceReference("Space.New.WebHome@image.jpg"), resourceReference);
    }

    @Test
    void updateResourceReferenceRelativeWithRelativeLinkToMovedDocument()
    {
        DocumentResourceReference resourceReference = new DocumentResourceReference("");
        DocumentReference oldReference = new DocumentReference("wiki", List.of("Space", "Old"), "WebHome");
        DocumentReference newReference = new DocumentReference("wiki", List.of("Space", "New"), "WebHome");

        when(this.entityReferenceResolver.resolve(resourceReference, null, newReference)).thenReturn(newReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, oldReference)).thenReturn(oldReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null))
            .thenReturn(new DocumentReference("wiki", "Main", "WebHome"));
        when(this.defaultReferenceDocumentReferenceResolver.resolve(oldReference)).thenReturn(oldReference);

        assertFalse(this.renamer.updateResourceReference(resourceReference, oldReference, newReference, newReference,
            true, Map.of(oldReference, newReference)));
        assertEquals(new DocumentResourceReference(""), resourceReference);
    }
}
