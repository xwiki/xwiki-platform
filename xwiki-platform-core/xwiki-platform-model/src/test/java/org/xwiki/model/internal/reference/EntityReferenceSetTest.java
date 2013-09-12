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
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSet;

public class EntityReferenceSetTest
{
    @Test
    public void testAnd()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.and(new EntityReference("name", EntityType.WIKI));

        Assert.assertTrue(set.matches(new EntityReference("name", EntityType.WIKI)));

        Assert.assertFalse(set.matches(new EntityReference("notname", EntityType.WIKI)));
    }

    @Test
    public void testAndNot()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.andnot(new EntityReference("name", EntityType.WIKI));

        Assert.assertFalse(set.matches(new EntityReference("name", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("notname", EntityType.WIKI)));
    }

    @Test
    public void testOr()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.or(new EntityReference("name", EntityType.WIKI));

        Assert.assertTrue(set.matches(new EntityReference("name", EntityType.WIKI)));

        Assert.assertFalse(set.matches(new EntityReference("notname", EntityType.WIKI)));
    }

    @Test
    public void testOrnot()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.ornot(new EntityReference("name", EntityType.WIKI));

        Assert.assertFalse(set.matches(new EntityReference("name", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("notname", EntityType.WIKI)));
    }

    @Test
    public void testAndAnd()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.and(new EntityReference("name1", EntityType.WIKI));
        set.and(new EntityReference("name2", EntityType.WIKI));

        Assert.assertFalse(set.matches(new EntityReference("name1", EntityType.WIKI)));
        Assert.assertFalse(set.matches(new EntityReference("name2", EntityType.WIKI)));
        Assert.assertFalse(set.matches(new EntityReference("notname", EntityType.WIKI)));
    }

    @Test
    public void testAndAndnot()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.and(new EntityReference("name1", EntityType.WIKI));
        set.andnot(new EntityReference("name2", EntityType.WIKI));

        Assert.assertTrue(set.matches(new EntityReference("name1", EntityType.WIKI)));
        Assert.assertFalse(set.matches(new EntityReference("name2", EntityType.WIKI)));
        Assert.assertFalse(set.matches(new EntityReference("notname", EntityType.WIKI)));
    }

    @Test
    public void testAndOr()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.and(new EntityReference("name1", EntityType.WIKI));
        set.or(new EntityReference("name2", EntityType.WIKI));

        Assert.assertTrue(set.matches(new EntityReference("name1", EntityType.WIKI)));
        Assert.assertTrue(set.matches(new EntityReference("name2", EntityType.WIKI)));
        Assert.assertFalse(set.matches(new EntityReference("notname", EntityType.WIKI)));
    }

    @Test
    public void testAndOrnot()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.and(new EntityReference("name1", EntityType.WIKI));
        set.ornot(new EntityReference("name2", EntityType.WIKI));

        Assert.assertTrue(set.matches(new EntityReference("name1", EntityType.WIKI)));
        Assert.assertFalse(set.matches(new EntityReference("name2", EntityType.WIKI)));
        Assert.assertTrue(set.matches(new EntityReference("notname", EntityType.WIKI)));
    }
}
