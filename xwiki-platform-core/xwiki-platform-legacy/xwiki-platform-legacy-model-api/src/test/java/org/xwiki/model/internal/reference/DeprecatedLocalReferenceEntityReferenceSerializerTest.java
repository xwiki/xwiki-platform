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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * Unit tests for {@link DeprecatedLocalReferenceEntityReferenceSerializer}.
 * 
 * @version $Id$
 */
public class DeprecatedLocalReferenceEntityReferenceSerializerTest
{
    private EntityReferenceSerializer<EntityReference> serializer;

    @Before
    public void setUp()
    {
        this.serializer = new DeprecatedLocalReferenceEntityReferenceSerializer();
    }

    @Test
    public void testSerializeDocumentReference() throws Exception
    {
        EntityReference reference = this.serializer.serialize(new DocumentReference("wiki", "space", "page"));

        Assert.assertEquals(EntityType.DOCUMENT, reference.getType());
        Assert.assertEquals("page", reference.getName());
        Assert.assertEquals(EntityType.SPACE, reference.getParent().getType());
        Assert.assertEquals("space", reference.getParent().getName());
        Assert.assertNull(reference.getParent().getParent());
    }

    @Test
    public void testSerializeSpaceReferenceWithChild()
    {
        EntityReference reference = this.serializer.serialize(new SpaceReference("space", new WikiReference("wiki")));

        Assert.assertEquals(EntityType.SPACE, reference.getType());
        Assert.assertEquals("space", reference.getName());
        Assert.assertNull(reference.getParent());
    }
}
