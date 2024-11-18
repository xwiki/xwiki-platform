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

import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.PageReferenceResolver;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private Provider<XWikiContext> contextProvider;

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
    void updateResourceReferenceRelative()
    {
        DocumentResourceReference resourceReference = new DocumentResourceReference("Main.WebHome");
        AttachmentReference oldReference =
            new AttachmentReference("file.txt", new DocumentReference("wiki", "space", "page"));
        AttachmentReference newReference =
            new AttachmentReference("file2.txt", new DocumentReference("wiki", "space", "page"));
        when(this.entityReferenceResolver.resolve(resourceReference, null)).thenReturn(oldReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, newReference)).thenReturn(newReference);
        when(this.entityReferenceResolver.resolve(resourceReference, null, oldReference)).thenReturn(oldReference);
        when(this.compactEntityReferenceSerializer.serialize(oldReference, newReference)).thenReturn("file2.txt");

        assertTrue(this.renamer.updateResourceReference(resourceReference,
            oldReference,
            newReference,
            new DocumentReference("xwiki", "Space", "Page"), true, Map.of()));

        verify(this.compactEntityReferenceSerializer).serialize(oldReference, newReference);
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
}
