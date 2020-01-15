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
package org.xwiki.model.reference;

import javax.inject.Named;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

/**
 * Validate {@link AttachmentReferenceConverter} component.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(value = { DefaultConverterManager.class, ContextComponentManagerProvider.class, EnumConverter.class,
    ConvertUtilsConverter.class })
public class AttachmentReferenceConverterTest
{
    @InjectMockComponents
    private AttachmentReferenceConverter documentReferenceConverter;

    private ConverterManager converterManager;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<String> mockStringResolver;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<EntityReference> mockReferenceResolver;

    @MockComponent
    @Named("compact")
    private EntityReferenceSerializer<String> mockSerialier;

    @BeforeEach
    public void setup(MockitoComponentManager componentManager) throws ComponentLookupException
    {
        this.converterManager = componentManager.getInstance(ConverterManager.class);
    }

    @Test
    public void testConvertFromString()
    {
        AttachmentReference attachmentReference = new AttachmentReference("file.txt",
            new DocumentReference("wiki", "space", "page"));
        Mockito.when(this.mockStringResolver.resolve("wiki:space.page@file.txt")).thenReturn(attachmentReference);
        Assert.assertEquals(attachmentReference,
            this.converterManager.convert(AttachmentReference.class, "wiki:space.page@file.txt"));

        attachmentReference = new AttachmentReference("file.txt",
            new DocumentReference("currentwiki", "space", "page"));
        Mockito.when(this.mockStringResolver.resolve("space.page@file.txt")).thenReturn(attachmentReference);
        Assert.assertEquals(attachmentReference,
            this.converterManager.convert(AttachmentReference.class, "space.page@file.txt"));

        attachmentReference = new AttachmentReference("file.txt",
            new DocumentReference("currentwiki", "currentspace", "page"));
        Mockito.when(this.mockStringResolver.resolve("page@file.txt")).thenReturn(attachmentReference);
        Assert.assertEquals(attachmentReference,
            this.converterManager.convert(AttachmentReference.class, "page@file.txt"));
    }

    @Test
    public void testConvertFromReference()
    {
        EntityReference reference;

        reference =
            new EntityReference("file.txt", EntityType.ATTACHMENT, new EntityReference("page", EntityType.DOCUMENT,
                new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))));

        AttachmentReference attachmentReference = new AttachmentReference("file.txt",
            new DocumentReference("wiki", "space", "page"));
        Mockito.when(this.mockReferenceResolver.resolve(reference)).thenReturn(attachmentReference);
        Assert.assertEquals(attachmentReference,
            this.converterManager.convert(AttachmentReference.class, reference));

        reference =
            new EntityReference("file.txt", EntityType.ATTACHMENT, new EntityReference("page", EntityType.DOCUMENT,
                new EntityReference("space", EntityType.SPACE)));
        attachmentReference = new AttachmentReference("file.txt",
            new DocumentReference("currentwiki", "space", "page"));
        Mockito.when(this.mockReferenceResolver.resolve(reference)).thenReturn(attachmentReference);
        Assert.assertEquals(attachmentReference,
            this.converterManager.convert(AttachmentReference.class, reference));

        reference =
            new EntityReference("file.txt", EntityType.ATTACHMENT, new EntityReference("page", EntityType.DOCUMENT));
        attachmentReference = new AttachmentReference("file.txt",
            new DocumentReference("currentwiki", "currentspace", "page"));
        Mockito.when(this.mockReferenceResolver.resolve(reference)).thenReturn(attachmentReference);
        Assert.assertEquals(attachmentReference,
            this.converterManager.convert(AttachmentReference.class, reference));
    }

    @Test
    public void testConvertFromNull()
    {
        Assert.assertNull(this.converterManager.convert(AttachmentReference.class, null));
    }

    @Test
    public void testConvertToString()
    {
        AttachmentReference attachmentReference = new AttachmentReference("file.txt",
            new DocumentReference("wiki", "space", "page"));
        Mockito.when(this.mockSerialier.serialize(attachmentReference)).thenReturn("wiki:space.page@file.txt");
        Assert.assertEquals("wiki:space.page@file.txt",
            this.converterManager.convert(String.class, attachmentReference));

        attachmentReference = new AttachmentReference("file.txt",
            new DocumentReference("currentwiki", "space", "page"));
        Mockito.when(this.mockSerialier.serialize(attachmentReference)).thenReturn("space.page@file.txt");
        Assert.assertEquals("space.page@file.txt",
            this.converterManager.convert(String.class, attachmentReference));
    }
}