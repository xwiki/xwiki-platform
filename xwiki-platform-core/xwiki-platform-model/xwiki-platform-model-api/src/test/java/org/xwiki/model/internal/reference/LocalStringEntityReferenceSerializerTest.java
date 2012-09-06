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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Unit tests for {@link LocalStringEntityReferenceSerializer}.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class LocalStringEntityReferenceSerializerTest
{
    private EntityReferenceSerializer<String> serializer;

    private EntityReferenceResolver<String> resolver;

    @Before
    public void setUp()
    {
        this.serializer = new LocalStringEntityReferenceSerializer();
        this.resolver = new DefaultStringEntityReferenceResolver();
    }

    @Test
    public void testSerializeDocumentReference() throws Exception
    {
        EntityReference reference = resolver.resolve("wiki:space.page", EntityType.DOCUMENT);
        Assert.assertEquals("space.page", serializer.serialize(reference));
    }
    
    @Test
    public void testSerializeSpaceReferenceWithChild()
    {
        EntityReference reference = resolver.resolve("wiki:space.page", EntityType.DOCUMENT);
        Assert.assertEquals("space", serializer.serialize(reference.getParent()));
    }
}
