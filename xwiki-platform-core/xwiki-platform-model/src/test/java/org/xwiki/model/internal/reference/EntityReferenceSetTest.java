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

/**
 * Validate {@link EntityReferenceSet}.
 * 
 * @version $Id$
 */
public class EntityReferenceSetTest
{
    @Test
    public void testIncludeWiki()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.includes(new EntityReference("name", EntityType.WIKI));

        Assert.assertTrue(set.matches(new EntityReference("name", EntityType.WIKI)));

        Assert.assertFalse(set.matches(new EntityReference("notname", EntityType.WIKI)));

        set.includes(new EntityReference("othername", EntityType.WIKI));

        Assert.assertTrue(set.matches(new EntityReference("name", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("othername", EntityType.WIKI)));

        Assert.assertFalse(set.matches(new EntityReference("notname", EntityType.WIKI)));
    }

    @Test
    public void testIncludeSpace()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.includes(new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("space", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));

        Assert.assertFalse(set.matches(new EntityReference("space", EntityType.SPACE, new EntityReference("notwiki",
            EntityType.WIKI))));
        Assert.assertFalse(set.matches(new EntityReference("notspace", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));
    }

    @Test
    public void testIncludeDocument()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.includes(new EntityReference("document", EntityType.DOCUMENT, new EntityReference("space",
            EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))));

        Assert.assertTrue(set.matches(new EntityReference("document", EntityType.DOCUMENT, new EntityReference("space",
            EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI)))));

        Assert.assertFalse(set.matches(new EntityReference("document", EntityType.DOCUMENT, new EntityReference(
            "space", EntityType.SPACE, new EntityReference("notwiki", EntityType.WIKI)))));
        Assert.assertFalse(set.matches(new EntityReference("document", EntityType.DOCUMENT, new EntityReference(
            "notspace", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI)))));
        Assert.assertFalse(set.matches(new EntityReference("notdocument", EntityType.DOCUMENT, new EntityReference(
            "space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI)))));
    }

    @Test
    public void testExcludeWiki()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.excludes(new EntityReference("name", EntityType.WIKI));

        Assert.assertFalse(set.matches(new EntityReference("name", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("othername", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("notname", EntityType.WIKI)));

        set.excludes(new EntityReference("othername", EntityType.WIKI));

        Assert.assertFalse(set.matches(new EntityReference("name", EntityType.WIKI)));

        Assert.assertFalse(set.matches(new EntityReference("othername", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("notname", EntityType.WIKI)));
    }
}
