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

import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.EntityType;

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

        set.includes(new EntityReference("wiki", EntityType.WIKI));

        Assert.assertTrue(set.matches(new EntityReference("wiki", EntityType.WIKI)));
        Assert.assertFalse(set.matches(new EntityReference("notwiki", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("space", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));
        Assert.assertFalse(set.matches(new EntityReference("space", EntityType.SPACE, new EntityReference("notwiki",
            EntityType.WIKI))));

        set.includes(new EntityReference("otherwiki", EntityType.WIKI));

        Assert.assertTrue(set.matches(new EntityReference("wiki", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("otherwiki", EntityType.WIKI)));

        Assert.assertFalse(set.matches(new EntityReference("notwiki", EntityType.WIKI)));
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

        set.includes(new EntityReference("otherspace", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("space", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));

        Assert.assertTrue(set.matches(new EntityReference("otherspace", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));

        Assert.assertFalse(set.matches(new EntityReference("notspace", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));
    }

    @Test
    public void testIncludePartial()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.includes(new EntityReference("space", EntityType.SPACE));

        Assert.assertTrue(set.matches(new EntityReference("space", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));
        Assert.assertTrue(set.matches(new EntityReference("space", EntityType.SPACE)));

        Assert.assertFalse(set.matches(new EntityReference("notspace", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));
        Assert.assertFalse(set.matches(new EntityReference("notspace", EntityType.SPACE)));
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

        set.excludes(new EntityReference("wiki", EntityType.WIKI));

        Assert.assertFalse(set.matches(new EntityReference("wiki", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("otherwiki", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("notwiki", EntityType.WIKI)));

        set.excludes(new EntityReference("otherwiki", EntityType.WIKI));

        Assert.assertFalse(set.matches(new EntityReference("wiki", EntityType.WIKI)));

        Assert.assertFalse(set.matches(new EntityReference("otherwiki", EntityType.WIKI)));

        Assert.assertTrue(set.matches(new EntityReference("notwiki", EntityType.WIKI)));
    }

    @Test
    public void testExcludeSpace()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.excludes(new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI)));

        Assert.assertFalse(set.matches(new EntityReference("space", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));

        Assert.assertTrue(set.matches(new EntityReference("wiki", EntityType.WIKI)));
        Assert.assertTrue(set.matches(new EntityReference("space", EntityType.SPACE, new EntityReference("otherwiki",
            EntityType.WIKI))));
        Assert.assertTrue(set.matches(new EntityReference("otherspace", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));
    }

    @Test
    public void testExcludePartial()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.excludes(new EntityReference("space", EntityType.SPACE));

        Assert.assertFalse(set.matches(new EntityReference("space", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));
        Assert.assertFalse(set.matches(new EntityReference("space", EntityType.SPACE)));

        Assert.assertTrue(set.matches(new EntityReference("notspace", EntityType.SPACE, new EntityReference("wiki",
            EntityType.WIKI))));
        Assert.assertTrue(set.matches(new EntityReference("notspace", EntityType.SPACE)));
    }

    @Test
    public void testIncludeLocale()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.includes(new DocumentReference("wiki", "space", "document", Locale.ENGLISH));

        Assert.assertTrue(set.matches(new DocumentReference("wiki", "space", "document")));
        Assert.assertTrue(set.matches(new DocumentReference("wiki", "space", "document", Locale.ENGLISH)));

        Assert.assertFalse(set.matches(new DocumentReference("wiki", "space", "document", Locale.FRENCH)));
        Assert.assertFalse(set.matches(new DocumentReference("wiki", "space", "document", Locale.ROOT)));

        set.includes(new DocumentReference("wiki", "space", "document", Locale.FRENCH));

        Assert.assertTrue(set.matches(new DocumentReference("wiki", "space", "document")));
        Assert.assertTrue(set.matches(new DocumentReference("wiki", "space", "document", Locale.ENGLISH)));
        Assert.assertTrue(set.matches(new DocumentReference("wiki", "space", "document", Locale.FRENCH)));

        Assert.assertFalse(set.matches(new DocumentReference("wiki", "space", "document", Locale.ROOT)));
    }

    @Test
    public void testExcludeLocale()
    {
        EntityReferenceSet set = new EntityReferenceSet();

        set.excludes(new DocumentReference("wiki", "space", "document", Locale.ENGLISH));

        Assert.assertTrue(set.matches(new DocumentReference("wiki", "space", "document")));

        Assert.assertFalse(set.matches(new DocumentReference("wiki", "space", "document", Locale.ENGLISH)));

        Assert.assertTrue(set.matches(new DocumentReference("wiki", "space", "document", Locale.FRENCH)));
        Assert.assertTrue(set.matches(new DocumentReference("wiki", "space", "document", Locale.ROOT)));

        set.excludes(new DocumentReference("wiki", "space", "document", Locale.FRENCH));

        Assert.assertTrue(set.matches(new DocumentReference("wiki", "space", "document")));

        Assert.assertFalse(set.matches(new DocumentReference("wiki", "space", "document", Locale.ENGLISH)));
        Assert.assertFalse(set.matches(new DocumentReference("wiki", "space", "document", Locale.FRENCH)));

        Assert.assertTrue(set.matches(new DocumentReference("wiki", "space", "document", Locale.ROOT)));
    }
}
