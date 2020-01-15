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
package org.xwiki.model.internal.reference;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.InvalidEntityReferenceException;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.test.TestConstants;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultReferenceEntityReferenceResolver}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
@ComponentTest
@ComponentList({ DefaultSymbolScheme.class })
public class DefaultReferenceEntityReferenceResolverTest implements TestConstants
{
    @MockComponent
    private EntityReferenceProvider referenceProvider;

    @InjectMockComponents
    private DefaultReferenceEntityReferenceResolver resolver;

    @BeforeComponent
    public void beforeComponent()
    {
        when(this.referenceProvider.getDefaultReference(EntityType.WIKI)).thenReturn(DEFAULT_WIKI_REFERENCE);
        when(this.referenceProvider.getDefaultReference(EntityType.SPACE)).thenReturn(DEFAULT_SPACE_REFERENCE);
        when(this.referenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(DEFAULT_DOCUMENT_REFERENCE);
        when(this.referenceProvider.getDefaultReference(EntityType.ATTACHMENT)).thenReturn(DEFAULT_ATTACHMENT_REFERENCE);
        when(this.referenceProvider.getDefaultReference(EntityType.OBJECT)).thenReturn(DEFAULT_OBJECT_REFERENCE);
        when(this.referenceProvider.getDefaultReference(EntityType.OBJECT_PROPERTY))
            .thenReturn(DEFAULT_OBJECT_PROPERTY_REFERENCE);
        when(this.referenceProvider.getDefaultReference(EntityType.PAGE)).thenReturn(DEFAULT_PAGE_REFERENCE);
        when(this.referenceProvider.getDefaultReference(EntityType.PAGE_ATTACHMENT))
        .thenReturn(DEFAULT_PAGE_ATTACHMENT_REFERENCE);
        when(this.referenceProvider.getDefaultReference(EntityType.PAGE_OBJECT))
            .thenReturn(DEFAULT_PAGE_OBJECT_REFERENCE);
        when(this.referenceProvider.getDefaultReference(EntityType.PAGE_OBJECT_PROPERTY))
            .thenReturn(DEFAULT_PAGE_OBJECT_PROPERTY_REFERENCE);
    }

    @Test
    public void resolveDocumentReferenceWhenMissingParents()
    {
        EntityReference partialReference = new EntityReference("document", EntityType.DOCUMENT);

        EntityReference reference = this.resolver.resolve(partialReference, EntityType.DOCUMENT);

        assertNotSame(partialReference, reference);
        assertEquals(DEFAULT_SPACE, reference.getParent().getName());
        assertEquals(EntityType.SPACE, reference.getParent().getType());
        assertEquals(DEFAULT_WIKI, reference.getParent().getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    }

    @Test
    public void resolveAttachmentReferenceWhenMissingParents()
    {
        EntityReference reference =
            this.resolver.resolve(new EntityReference("filename", EntityType.ATTACHMENT), EntityType.ATTACHMENT);

        assertEquals(DEFAULT_DOCUMENT, reference.getParent().getName());
        assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        assertEquals(DEFAULT_SPACE, reference.getParent().getParent().getName());
        assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        assertEquals(DEFAULT_WIKI, reference.getParent().getParent().getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }

    @Test
    public void resolveDocumentReferenceWhenMissingParentBetweenReferences()
    {
        EntityReference partialReference =
            new EntityReference("document", EntityType.DOCUMENT, new EntityReference("wiki", EntityType.WIKI));

        EntityReference reference = this.resolver.resolve(partialReference, EntityType.DOCUMENT);

        assertNotSame(partialReference, reference);
        assertEquals(DEFAULT_SPACE, reference.getParent().getName());
        assertEquals(EntityType.SPACE, reference.getParent().getType());
        assertNotSame(partialReference.getParent().getParent(), reference.getParent().getParent());
        assertEquals("wiki", reference.getParent().getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    }

    @Test
    public void resolveAttachmentReferenceWhenMissingParentBetweenReferences()
    {
        EntityReference reference = this.resolver.resolve(
            new EntityReference("filename", EntityType.ATTACHMENT, new EntityReference("wiki", EntityType.WIKI)),
            EntityType.ATTACHMENT);

        assertEquals(DEFAULT_DOCUMENT, reference.getParent().getName());
        assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        assertEquals(DEFAULT_SPACE, reference.getParent().getParent().getName());
        assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        assertEquals("wiki", reference.getParent().getParent().getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }

    @Test
    public void resolveDocumentReferenceWhenInvalidReference()
    {
        try {
            this.resolver.resolve(new EntityReference("document", EntityType.DOCUMENT,
                new EntityReference("filename", EntityType.ATTACHMENT)), EntityType.DOCUMENT);

            fail("Should have thrown an exception here");
        } catch (InvalidEntityReferenceException expected) {
            assertEquals("Invalid reference [Document filename???document]", expected.getMessage());
        }
    }

    @Test
    public void resolveDocumentReferenceWhenTypeIsSpace()
    {
        EntityReference reference =
            this.resolver.resolve(new EntityReference("space", EntityType.SPACE), EntityType.DOCUMENT);

        assertEquals(EntityType.DOCUMENT, reference.getType());
        assertEquals(DEFAULT_DOCUMENT, reference.getName());
        assertEquals(EntityType.SPACE, reference.getParent().getType());
        assertEquals("space", reference.getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
        assertEquals(DEFAULT_WIKI, reference.getParent().getParent().getName());
    }

    @Test
    public void resolveSpaceReferenceWhenTypeIsDocument()
    {
        EntityReference reference =
            this.resolver.resolve(new EntityReference("document", EntityType.DOCUMENT), EntityType.SPACE);

        assertEquals(EntityType.SPACE, reference.getType());
        assertEquals(DEFAULT_SPACE, reference.getName());
        assertEquals(EntityType.WIKI, reference.getParent().getType());
        assertEquals(DEFAULT_WIKI, reference.getParent().getName());
    }

    /**
     * Tests that a relative object reference is resolved correctly and completed with the default document parent.
     */
    @Test
    public void resolveObjectReferenceWhenMissingParents()
    {
        EntityReference reference =
            resolver.resolve(new EntityReference("object", EntityType.OBJECT), EntityType.OBJECT);
        assertEquals(EntityType.OBJECT, reference.getType());
        assertEquals("object", reference.getName());
        assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        assertEquals(DEFAULT_DOCUMENT, reference.getParent().getName());
        assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        assertEquals(DEFAULT_SPACE, reference.getParent().getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
        assertEquals(DEFAULT_WIKI, reference.getParent().getParent().getParent().getName());
    }

    /**
     * Tests that a relative object property is resolved correctly and completed with the default object parent.
     */
    @Test
    public void resolveObjectPropertyReferenceWhenMissingParents()
    {
        EntityReference reference =
            resolver.resolve(new EntityReference("property", EntityType.OBJECT_PROPERTY), EntityType.OBJECT_PROPERTY);
        assertEquals(EntityType.OBJECT_PROPERTY, reference.getType());
        assertEquals("property", reference.getName());
        assertEquals(EntityType.OBJECT, reference.getParent().getType());
        assertEquals(DEFAULT_OBJECT, reference.getParent().getName());
        assertEquals(EntityType.DOCUMENT, reference.getParent().getParent().getType());
        assertEquals(DEFAULT_DOCUMENT, reference.getParent().getParent().getName());
        assertEquals(EntityType.SPACE, reference.getParent().getParent().getParent().getType());
        assertEquals(DEFAULT_SPACE, reference.getParent().getParent().getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getParent().getType());
        assertEquals(DEFAULT_WIKI, reference.getParent().getParent().getParent().getParent().getName());
    }

    /**
     * Test that a document reference, when resolved as an object reference, is correctly completed with the default
     * values for object name.
     */
    @Test
    public void resolveObjectReferenceWhenTypeIsDocument()
    {
        EntityReference reference = resolver.resolve(
            new EntityReference("document", EntityType.DOCUMENT,
                new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))),
            EntityType.OBJECT);
        assertEquals(EntityType.OBJECT, reference.getType());
        assertEquals(DEFAULT_OBJECT, reference.getName());
        assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        assertEquals("document", reference.getParent().getName());
        assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        assertEquals("space", reference.getParent().getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
        assertEquals("wiki", reference.getParent().getParent().getParent().getName());
    }

    /**
     * Test that a document reference, when resolved as a property reference, is correctly completed with the default
     * values for object and property name.
     */
    @Test
    public void resolveObjectPropertyReferenceWhenTypeIsDocument()
    {
        EntityReference reference = resolver.resolve(
            new EntityReference("document", EntityType.DOCUMENT,
                new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))),
            EntityType.OBJECT_PROPERTY);
        assertEquals(EntityType.OBJECT_PROPERTY, reference.getType());
        assertEquals(DEFAULT_OBJECT_PROPERTY, reference.getName());
        assertEquals(EntityType.OBJECT, reference.getParent().getType());
        assertEquals(DEFAULT_OBJECT, reference.getParent().getName());
        assertEquals(EntityType.DOCUMENT, reference.getParent().getParent().getType());
        assertEquals("document", reference.getParent().getParent().getName());
        assertEquals(EntityType.SPACE, reference.getParent().getParent().getParent().getType());
        assertEquals("space", reference.getParent().getParent().getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getParent().getType());
        assertEquals("wiki", reference.getParent().getParent().getParent().getParent().getName());
    }

    @Test
    public void resolveDocumentReferenceWhenNullReference()
    {
        EntityReference reference = this.resolver.resolve(null, EntityType.DOCUMENT);

        assertEquals(EntityType.DOCUMENT, reference.getType());
        assertEquals(DEFAULT_DOCUMENT, reference.getName());
        assertEquals(EntityType.SPACE, reference.getParent().getType());
        assertEquals(DEFAULT_SPACE, reference.getParent().getName());
        assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
        assertEquals(DEFAULT_WIKI, reference.getParent().getParent().getName());
    }

    @Test
    public void resolvePageReferenceWhenTypeIsDocument()
    {
        EntityReference reference =
            this.resolver.resolve(new EntityReference("document", EntityType.DOCUMENT), EntityType.PAGE);

        assertEquals(new PageReference(DEFAULT_WIKI, DEFAULT_SPACE, "document"), reference);

        reference = this.resolver.resolve(new EntityReference(DEFAULT_DOCUMENT, EntityType.DOCUMENT), EntityType.PAGE);

        assertEquals(new PageReference(DEFAULT_WIKI, DEFAULT_SPACE), reference);
    }

    @Test
    public void resolvePageReferenceWhenTypeIsSpace()
    {
        EntityReference reference =
            this.resolver.resolve(new EntityReference("space", EntityType.SPACE), EntityType.PAGE);

        assertEquals(new PageReference(DEFAULT_WIKI, "space"), reference);
    }

    @Test
    public void resolveDocumentReferenceWhenTypeIsPage()
    {
        EntityReference reference =
            this.resolver.resolve(new EntityReference("page", EntityType.PAGE), EntityType.DOCUMENT);

        assertEquals(new DocumentReference(DEFAULT_WIKI, "page", DEFAULT_DOCUMENT), reference);

        reference = this.resolver.resolve(
            new EntityReference("page1", EntityType.PAGE, new EntityReference("page2", EntityType.PAGE)),
            EntityType.DOCUMENT);

        assertEquals(new DocumentReference(DEFAULT_WIKI, Arrays.asList("page2", "page1"), DEFAULT_DOCUMENT), reference);
    }

    @Test
    public void resolveSpaceReferenceWhenTypeIsPage()
    {
        EntityReference reference =
            this.resolver.resolve(new EntityReference("page", EntityType.PAGE), EntityType.SPACE);

        assertEquals(new SpaceReference(DEFAULT_WIKI, Arrays.asList("page")), reference);

        reference = this.resolver.resolve(
            new EntityReference("page1", EntityType.PAGE, new EntityReference("page2", EntityType.PAGE)),
            EntityType.SPACE);

        assertEquals(new SpaceReference(DEFAULT_WIKI, Arrays.asList("page2", "page1")), reference);
    }
}
