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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.InvalidEntityReferenceException;

/**
 * Unit tests for {@link DefaultReferenceEntityReferenceResolver}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class DefaultReferenceEntityReferenceResolverTest
{
    private EntityReferenceResolver<EntityReference> resolver;

    private Mockery mockery = new Mockery();

    @Before
    public void setUp()
    {
        this.resolver = new DefaultReferenceEntityReferenceResolver();
        final EntityReferenceValueProvider mockValueProvider = this.mockery.mock(EntityReferenceValueProvider.class);
        ReflectionUtils.setFieldValue(this.resolver, "provider", mockValueProvider);

        this.mockery.checking(new Expectations()
        {
            {
                allowing(mockValueProvider).getDefaultValue(EntityType.SPACE);
                will(returnValue("defspace"));
                allowing(mockValueProvider).getDefaultValue(EntityType.WIKI);
                will(returnValue("defwiki"));
                allowing(mockValueProvider).getDefaultValue(EntityType.DOCUMENT);
                will(returnValue("defpage"));
                allowing(mockValueProvider).getDefaultValue(EntityType.OBJECT); 
                will(returnValue("defobject"));
                allowing(mockValueProvider).getDefaultValue(EntityType.OBJECT_PROPERTY); 
                will(returnValue("defproperty"));
            }
        });
    }

    @Test
    public void testResolveDocumentReferenceWhenMissingParents()
    {
        EntityReference partialReference = new EntityReference("page", EntityType.DOCUMENT);

        EntityReference reference = this.resolver.resolve(partialReference, EntityType.DOCUMENT);

        Assert.assertNotSame(partialReference, reference);
        Assert.assertEquals("defspace", reference.getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertEquals("defwiki", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    }

    @Test
    public void testResolveAttachmentReferenceWhenMissingParents()
    {
        EntityReference reference =
                this.resolver.resolve(new EntityReference("filename", EntityType.ATTACHMENT), EntityType.ATTACHMENT);

        Assert.assertEquals("defpage", reference.getParent().getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        Assert.assertEquals("defspace", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        Assert.assertEquals("defwiki", reference.getParent().getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }

    @Test
    public void testResolveDocumentReferenceWhenMissingParentBetweenReferences()
    {
        EntityReference partialReference =
                new EntityReference("page", EntityType.DOCUMENT, new EntityReference("wiki", EntityType.WIKI));

        EntityReference reference = this.resolver.resolve(partialReference, EntityType.DOCUMENT);

        Assert.assertNotSame(partialReference, reference);
        Assert.assertEquals("defspace", reference.getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertNotSame(partialReference.getParent().getParent(), reference.getParent().getParent());
        Assert.assertEquals("wiki", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    }

    @Test
    public void testResolveAttachmentReferenceWhenMissingParentBetweenReferences()
    {
        EntityReference reference =
                this.resolver.resolve(new EntityReference("filename", EntityType.ATTACHMENT, new EntityReference(
                    "wiki", EntityType.WIKI)), EntityType.ATTACHMENT);

        Assert.assertEquals("defpage", reference.getParent().getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        Assert.assertEquals("defspace", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        Assert.assertEquals("wiki", reference.getParent().getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    }

    @Test
    public void testResolveDocumentReferenceWhenInvalidReference()
    {
        try {
            this.resolver.resolve(new EntityReference("page", EntityType.DOCUMENT, new EntityReference("filename",
                EntityType.ATTACHMENT)), EntityType.DOCUMENT);
            Assert.fail("Should have thrown an exception here");
        } catch (InvalidEntityReferenceException expected) {
            Assert.assertEquals("Invalid reference [Document filename.page]", expected.getMessage());
        }
    }

    @Test
    public void testResolveDocumentReferenceWhenTypeIsSpace()
    {
        EntityReference reference =
                this.resolver.resolve(new EntityReference("space", EntityType.SPACE), EntityType.DOCUMENT);

        Assert.assertEquals(EntityType.DOCUMENT, reference.getType());
        Assert.assertEquals("defpage", reference.getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertEquals("space", reference.getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
        Assert.assertEquals("defwiki", reference.getParent().getParent().getName());
    }

    @Test
    public void testResolveSpaceReferenceWhenTypeIsDocument()
    {
        EntityReference reference =
                this.resolver.resolve(new EntityReference("page", EntityType.DOCUMENT), EntityType.SPACE);

        Assert.assertEquals(EntityType.SPACE, reference.getType());
        Assert.assertEquals("defspace", reference.getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getType());
        Assert.assertEquals("defwiki", reference.getParent().getName());
    }

    /**
     * Tests that a relative object reference is resolved correctly and completed with the default document parent.
     */
    @Test
    public void testResolveObjectReferenceWhenMissingParents()
    {
        EntityReference reference =
            resolver.resolve(new EntityReference("object", EntityType.OBJECT), EntityType.OBJECT);
        Assert.assertEquals(EntityType.OBJECT, reference.getType());
        Assert.assertEquals("object", reference.getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        Assert.assertEquals("defpage", reference.getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        Assert.assertEquals("defspace", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
        Assert.assertEquals("defwiki", reference.getParent().getParent().getParent().getName());
    }

    /**
     * Tests that a relative object property is resolved correctly and completed with the default object parent.
     */
    @Test
    public void testResolveObjectPropertyReferenceWhenMissingParents()
    {
        EntityReference reference =
            resolver.resolve(new EntityReference("property", EntityType.OBJECT_PROPERTY), EntityType.OBJECT_PROPERTY);
        Assert.assertEquals(EntityType.OBJECT_PROPERTY, reference.getType());
        Assert.assertEquals("property", reference.getName());        
        Assert.assertEquals(EntityType.OBJECT, reference.getParent().getType());
        Assert.assertEquals("defobject", reference.getParent().getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getParent().getType());
        Assert.assertEquals("defpage", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getParent().getType());
        Assert.assertEquals("defspace", reference.getParent().getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getParent().getType());
        Assert.assertEquals("defwiki", reference.getParent().getParent().getParent().getParent().getName());
    }

    /**
     * Test that a document reference, when resolved as an object reference, is correctly completed with the default
     * values for object name.
     */
    @Test
    public void testResolveObjectReferenceWhenTypeIsDocument()
    {
        EntityReference reference =
            resolver.resolve(new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space",
                EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))), EntityType.OBJECT);
        Assert.assertEquals(EntityType.OBJECT, reference.getType());
        Assert.assertEquals("defobject", reference.getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
        Assert.assertEquals("page", reference.getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
        Assert.assertEquals("space", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
        Assert.assertEquals("wiki", reference.getParent().getParent().getParent().getName());
    }

    /**
     * Test that a document reference, when resolved as a property reference, is correctly completed with the default
     * values for object and property name.
     */
    @Test
    public void testResolveObjectPropertyReferenceWhenTypeIsDocument()
    {
        EntityReference reference =
            resolver.resolve(new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space",
                EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))), EntityType.OBJECT_PROPERTY);
        Assert.assertEquals(EntityType.OBJECT_PROPERTY, reference.getType());
        Assert.assertEquals("defproperty", reference.getName());
        Assert.assertEquals(EntityType.OBJECT, reference.getParent().getType());
        Assert.assertEquals("defobject", reference.getParent().getName());
        Assert.assertEquals(EntityType.DOCUMENT, reference.getParent().getParent().getType());
        Assert.assertEquals("page", reference.getParent().getParent().getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getParent().getParent().getType());
        Assert.assertEquals("space", reference.getParent().getParent().getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getParent().getType());
        Assert.assertEquals("wiki", reference.getParent().getParent().getParent().getParent().getName());
    }    

    @Test
    public void testResolveDocumentReferenceWhenNullReference()
    {
        EntityReference reference = this.resolver.resolve(null, EntityType.DOCUMENT);

        Assert.assertEquals(EntityType.DOCUMENT, reference.getType());
        Assert.assertEquals("defpage", reference.getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertEquals("defspace", reference.getParent().getName());
        Assert.assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
        Assert.assertEquals("defwiki", reference.getParent().getParent().getName());
    }
}
