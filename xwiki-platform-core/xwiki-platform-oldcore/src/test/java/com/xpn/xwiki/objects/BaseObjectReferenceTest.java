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
package com.xpn.xwiki.objects;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Validate {@link BaseObjectReference}.
 * 
 * @version $Id$
 */
public class BaseObjectReferenceTest extends AbstractBridgedComponentTestCase
{
    private DocumentReference document;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.document = new DocumentReference("wiki", "space", "page");
    }

    @Test
    public void testSerialize() throws Exception
    {
        BaseObjectReference reference =
            new BaseObjectReference(new DocumentReference("wiki", "space", "class"), 42, this.document);

        Assert.assertEquals("wiki:space.class[42]", reference.getName());

        reference = new BaseObjectReference(new DocumentReference("wiki", "space", "class"), null, this.document);

        Assert.assertEquals("wiki:space.class", reference.getName());
    }

    @Test
    public void testSerializeEscape() throws Exception
    {
        BaseObjectReference reference =
            new BaseObjectReference(new DocumentReference("wiki", "space", "class[42]"), null, this.document);

        Assert.assertEquals("wiki:space.class\\[42]", reference.getName());

        reference =
            new BaseObjectReference(new DocumentReference("wiki", "space", "class\\\\[42]"), null, this.document);

        Assert.assertEquals("wiki:space.class\\\\\\\\\\[42]", reference.getName());
    }

    @Test
    public void testUnserialize() throws Exception
    {
        BaseObjectReference reference =
            new BaseObjectReference(new EntityReference("wiki:space.class[42]", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class"), reference.getXClassReference());
        Assert.assertEquals(42, (int) reference.getObjectNumber());

        reference = new BaseObjectReference(new EntityReference("wiki:space.class", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class"), reference.getXClassReference());
        Assert.assertNull(reference.getObjectNumber());
    }

    @Test
    public void testUnserializeEscape() throws Exception
    {
        BaseObjectReference reference =
            new BaseObjectReference(new EntityReference("wiki:space.class\\[42]", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class[42]"), reference.getXClassReference());
        Assert.assertNull(reference.getObjectNumber());

        reference =
            new BaseObjectReference(new EntityReference("wiki:space.class\\\\[42]", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class\\"), reference.getXClassReference());
        Assert.assertEquals(42, (int) reference.getObjectNumber());

        reference =
            new BaseObjectReference(new EntityReference("wiki:space.class\\\\\\[42]", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class\\[42]"), reference.getXClassReference());
        Assert.assertNull(reference.getObjectNumber());

        reference =
            new BaseObjectReference(new EntityReference("wiki:space.class[word]", EntityType.OBJECT, this.document));

        Assert.assertEquals(new DocumentReference("wiki", "space", "class[word]"), reference.getXClassReference());
        Assert.assertNull(reference.getObjectNumber());
    }
}
