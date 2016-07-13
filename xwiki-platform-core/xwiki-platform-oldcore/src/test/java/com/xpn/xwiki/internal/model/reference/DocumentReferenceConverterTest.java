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
package com.xpn.xwiki.internal.model.reference;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.converter.Converter;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ColorConverter;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Validate {@link ColorConverter} component.
 * 
 * @version $Id$
 */
@ComponentList(value = { DefaultConverterManager.class, ContextComponentManagerProvider.class, EnumConverter.class,
ConvertUtilsConverter.class })
public class DocumentReferenceConverterTest
{
    @Rule
    public MockitoComponentMockingRule<Converter<DocumentReference>> mocker =
        new MockitoComponentMockingRule<Converter<DocumentReference>>(DocumentReferenceConverter.class);

    private ConverterManager converterManager;

    private DocumentReferenceResolver<String> mockStringResolver;

    private DocumentReferenceResolver<EntityReference> mockReferenceResolver;

    private EntityReferenceSerializer<String> mockSerialier;

    @Before
    public void setUp() throws Exception
    {
        this.converterManager = mocker.getInstance(ConverterManager.class);

        this.mockStringResolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        this.mockReferenceResolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_REFERENCE, "currentgetdocument");
        this.mockSerialier = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "compact");
    }

    @Test
    public void testConvertFromString()
    {
        Mockito.when(this.mockStringResolver.resolve("wiki:space.page")).thenReturn(
            new DocumentReference("wiki", "space", "page"));
        Assert.assertEquals(new DocumentReference("wiki", "space", "page"),
            this.converterManager.convert(DocumentReference.class, "wiki:space.page"));

        Mockito.when(this.mockStringResolver.resolve("space.page")).thenReturn(
            new DocumentReference("currentwiki", "space", "page"));
        Assert.assertEquals(new DocumentReference("currentwiki", "space", "page"),
            this.converterManager.convert(DocumentReference.class, "space.page"));

        Mockito.when(this.mockStringResolver.resolve("page")).thenReturn(
            new DocumentReference("currentwiki", "currentspace", "page"));
        Assert.assertEquals(new DocumentReference("currentwiki", "currentspace", "page"),
            this.converterManager.convert(DocumentReference.class, "page"));
    }

    @Test
    public void testConvertFromReference()
    {
        EntityReference reference;

        reference =
            new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE,
                new EntityReference("wiki", EntityType.WIKI)));
        Mockito.when(this.mockReferenceResolver.resolve(reference)).thenReturn(
            new DocumentReference("wiki", "space", "page"));
        Assert.assertEquals(new DocumentReference("wiki", "space", "page"),
            this.converterManager.convert(DocumentReference.class, reference));

        reference = new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE));
        Mockito.when(this.mockReferenceResolver.resolve(reference)).thenReturn(
            new DocumentReference("currentwiki", "space", "page"));
        Assert.assertEquals(new DocumentReference("currentwiki", "space", "page"),
            this.converterManager.convert(DocumentReference.class, reference));

        reference = new EntityReference("page", EntityType.DOCUMENT);
        Mockito.when(this.mockReferenceResolver.resolve(reference)).thenReturn(
            new DocumentReference("currentwiki", "currentspace", "page"));
        Assert.assertEquals(new DocumentReference("currentwiki", "currentspace", "page"),
            this.converterManager.convert(DocumentReference.class, reference));
    }

    @Test
    public void testConvertFromNull()
    {
        Assert.assertNull(this.converterManager.convert(DocumentReference.class, null));
    }

    @Test
    public void testConvertToString()
    {
        Mockito.when(this.mockSerialier.serialize(new DocumentReference("wiki", "space", "page"))).thenReturn(
            "wiki:space.page");
        Assert.assertEquals("wiki:space.page",
            this.converterManager.convert(String.class, new DocumentReference("wiki", "space", "page")));

        Mockito.when(this.mockSerialier.serialize(new DocumentReference("wiki", "space", "page"))).thenReturn(
            "space.page");
        Assert.assertEquals("space.page",
            this.converterManager.convert(String.class, new DocumentReference("wiki", "space", "page")));
    }
}
