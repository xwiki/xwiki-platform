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

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Validate {@link SpaceReferenceConverter} component.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({DefaultConverterManager.class, ContextComponentManagerProvider.class, EnumConverter.class,
    ConvertUtilsConverter.class})
public class SpaceReferenceConverterTest
{
    @InjectMockComponents
    private SpaceReferenceConverter spaceReferenceConverter;

    private ConverterManager converterManager;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<String> mockStringResolver;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<EntityReference> mockReferenceResolver;

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
        when(this.mockStringResolver.resolve("wiki:space", EntityType.SPACE))
            .thenReturn(new EntityReference("space", EntityType.SPACE, new WikiReference("wiki")));
        assertEquals(new SpaceReference("wiki", "space"),
            this.converterManager.convert(SpaceReference.class, "wiki:space"));
    }

    @Test
    public void testConvertFromReference()
    {
        EntityReference reference;

        reference = new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI)));
        when(this.mockReferenceResolver.resolve(reference, EntityType.SPACE))
            .thenReturn(new EntityReference("space", EntityType.SPACE, new WikiReference("wiki")));
        assertEquals(new SpaceReference("wiki", "space"),
            this.converterManager.convert(SpaceReference.class, reference));
    }

    @Test
    public void testConvertFromNull()
    {
        assertNull(this.converterManager.convert(SpaceReference.class, null));
    }

    @Test
    public void testConvertToString()
    {
        when(this.mockSerialier.serialize(new SpaceReference("wiki", "space"))).thenReturn("wiki:space");
        assertEquals("wiki:space", this.converterManager.convert(String.class, new SpaceReference("wiki", "space")));
    }
}
