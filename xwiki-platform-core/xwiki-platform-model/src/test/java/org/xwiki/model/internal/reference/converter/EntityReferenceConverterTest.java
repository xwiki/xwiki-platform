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
package org.xwiki.model.internal.reference.converter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.properties.converter.Converter;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Validate {@link EntityReferenceConverter} component.
 * 
 * @version $Id$
 */
@AllComponents
public class EntityReferenceConverterTest
{
    @Rule
    public MockitoComponentMockingRule<Converter<EntityReference>> mocker =
        new MockitoComponentMockingRule<>(EntityReferenceConverter.class);

    private ConverterManager converterManager;

    @Before
    public void setUp() throws Exception
    {
        this.converterManager = this.mocker.getInstance(ConverterManager.class);
    }

    @Test
    public void convertDocumentFromString()
    {
        EntityReference reference;

        reference =
            new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE,
                new EntityReference("wiki", EntityType.WIKI)));
        assertEquals(reference, this.converterManager.convert(EntityReference.class, "document:wiki:space.page"));

        reference = new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE));
        assertEquals(reference, this.converterManager.convert(EntityReference.class, "document:space.page"));
        assertEquals(reference, this.converterManager.convert(EntityReference.class, "space.page"));

        reference = new EntityReference("page", EntityType.DOCUMENT);
        assertEquals(reference, this.converterManager.convert(EntityReference.class, "document:page"));
        assertEquals(reference, this.converterManager.convert(EntityReference.class, "page"));
    }

    @Test
    public void convertSpaceFromString()
    {
        EntityReference reference;

        reference = new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI));
        assertEquals(reference, this.converterManager.convert(EntityReference.class, "space:wiki:space"));

        reference = new EntityReference("space", EntityType.SPACE);
        assertEquals(reference, this.converterManager.convert(EntityReference.class, "space:space"));
    }

    @Test
    public void convertWikiFromString()
    {
        EntityReference reference = new EntityReference("dev", EntityType.WIKI);
        assertEquals(reference, this.converterManager.convert(EntityReference.class, "wiki:dev"));
    }

    @Test
    public void convertFromNull()
    {
        assertNull(this.converterManager.convert(EntityReference.class, null));
    }

    @Test
    public void convertToString()
    {
        EntityReference reference;

        reference =
            new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space", EntityType.SPACE,
                new EntityReference("wiki", EntityType.WIKI)));
        assertEquals("document:wiki:space.page", this.converterManager.convert(String.class, reference));
    }
}
