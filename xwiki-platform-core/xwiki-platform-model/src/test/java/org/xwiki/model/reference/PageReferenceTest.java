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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.EntityType;

/**
 * Unit tests for {@link org.xwiki.model.reference.PageReference}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class PageReferenceTest
{
    @Test
    public void testConstructors()
    {
        PageReference reference = new PageReference("wiki", "space", "page");
        Assert.assertEquals(reference, new PageReference(new EntityReference("page", EntityType.PAGE,
            new EntityReference("space", EntityType.PAGE, new EntityReference("wiki", EntityType.WIKI, null)))));
        Assert.assertEquals(reference, new PageReference("wiki", Arrays.asList("space", "page")));
        Assert.assertEquals(reference,
            new PageReference("page", new SpaceReference("space", new WikiReference("wiki"))));
    }

    @Test
    public void testInvalidType()
    {
        try {
            new PageReference(new EntityReference("page", EntityType.PAGE));
            Assert.fail("Should have thrown an exception here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("Invalid type [SPACE] for a document reference", expected.getMessage());
        }
    }

    @Test
    public void testInvalidNullParent()
    {
        try {
            new PageReference("page", null);
            Assert.fail("Should have thrown an exception here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("Invalid parent reference [null] in a document reference", expected.getMessage());
        }
    }

    @Test
    public void testInvalidParentType()
    {
        try {
            new PageReference(new EntityReference("page", EntityType.DOCUMENT, new WikiReference("wiki")));
            Assert.fail("Should have thrown an exception here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("Invalid parent reference [Wiki wiki] in a document reference", expected.getMessage());
        }
    }

    @Test
    public void testGetWikiReference()
    {
        PageReference reference = new PageReference("wiki", "space", "page");
        Assert.assertEquals(new WikiReference("wiki"), reference.getWikiReference());
    }

    @Test
    public void testSpaceReferences()
    {
        PageReference reference1 = new PageReference("wiki", "space", "page");
        List<SpaceReference> spaceRefs = reference1.getSpaceReferences();
        Assert.assertEquals(1, spaceRefs.size());
        Assert.assertEquals(new SpaceReference("space", new WikiReference("wiki")), spaceRefs.get(0));

        PageReference reference2 = new PageReference("wiki", Arrays.asList("space1", "space2"), "page");
        List<SpaceReference> spaceRefs2 = reference2.getSpaceReferences();
        Assert.assertEquals(2, spaceRefs2.size());
        Assert.assertEquals(new SpaceReference("space1", new WikiReference("wiki")), spaceRefs2.get(0));
        Assert.assertEquals(new SpaceReference("space2", new SpaceReference("space1", new WikiReference("wiki"))),
            spaceRefs2.get(1));
    }

    @Test
    public void testToString()
    {
        PageReference reference1 = new PageReference("wiki", "space", "page");
        Assert.assertEquals("wiki:space.page", reference1.toString());

        PageReference reference2 = new PageReference("wiki", "space", "page", Locale.FRANCE);
        Assert.assertEquals("wiki:space.page(fr_FR)", reference2.toString());

        PageReference reference3 = new PageReference("wiki", "space", "page", "en");
        Assert.assertEquals("wiki:space.page(en)", reference3.toString());
    }

    @Test
    public void testCreatePageReferenceFromLocalPageReference()
    {
        Assert.assertEquals("wiki:space.page",
            new PageReference(new LocalPageReference("space", "page"), new WikiReference("wiki")).toString());
    }
}
