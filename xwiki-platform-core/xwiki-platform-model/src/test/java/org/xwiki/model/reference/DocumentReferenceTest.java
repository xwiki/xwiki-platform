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
 * Unit tests for {@link org.xwiki.model.reference.DocumentReference}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class DocumentReferenceTest
{
    @Test
    public void testConstructors()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        Assert.assertEquals(reference, new DocumentReference(new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI)))));
        Assert.assertEquals(reference, new DocumentReference("wiki", Arrays.asList("space"), "page"));
        Assert.assertEquals(reference, new DocumentReference("page", new SpaceReference("space", new WikiReference(
            "wiki"))));
    }

    @Test
    public void testInvalidType()
    {
        try {
            new DocumentReference(new EntityReference("page", EntityType.SPACE));
            Assert.fail("Should have thrown an exception here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("Invalid type [SPACE] for a document reference", expected.getMessage());
        }
    }

    @Test
    public void testInvalidNullParent()
    {
        try {
            new DocumentReference("page", null);
            Assert.fail("Should have thrown an exception here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("Invalid parent reference [null] in a document reference", expected.getMessage());
        }
    }

    @Test
    public void testInvalidParentType()
    {
        try {
            new DocumentReference(new EntityReference("page", EntityType.DOCUMENT, new WikiReference("wiki")));
            Assert.fail("Should have thrown an exception here");
        } catch (IllegalArgumentException expected) {
            Assert.assertEquals("Invalid parent reference [Wiki wiki] in a document reference", expected.getMessage());
        }
    }

    @Test
    public void testGetWikiReference()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        Assert.assertEquals(new WikiReference("wiki"), reference.getWikiReference());
    }

    @Test
    public void testGetLastSpaceReferenceWhenOneSpace()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        SpaceReference spaceReference = documentReference.getLastSpaceReference();
        Assert.assertEquals(new SpaceReference("space", new WikiReference("wiki")), spaceReference);
    }

    @Test
    public void testGetLastSpaceReferenceWhenMultipleSpaces()
    {
        DocumentReference reference = new DocumentReference("wiki", Arrays.asList("space1", "space2"), "page");
        Assert.assertEquals(new SpaceReference("space2", new SpaceReference("space1", new WikiReference("wiki"))),
            reference.getLastSpaceReference());
    }

    @Test
    public void testSpaceReferences()
    {
        DocumentReference reference1 = new DocumentReference("wiki", "space", "page");
        List<SpaceReference> spaceRefs = reference1.getSpaceReferences();
        Assert.assertEquals(1, spaceRefs.size());
        Assert.assertEquals(new SpaceReference("space", new WikiReference("wiki")), spaceRefs.get(0));

        DocumentReference reference2 = new DocumentReference("wiki", Arrays.asList("space1", "space2"), "page");
        List<SpaceReference> spaceRefs2 = reference2.getSpaceReferences();
        Assert.assertEquals(2, spaceRefs2.size());
        Assert.assertEquals(new SpaceReference("space1", new WikiReference("wiki")), spaceRefs2.get(0));
        Assert.assertEquals(new SpaceReference("space2", new SpaceReference("space1", new WikiReference("wiki"))),
            spaceRefs2.get(1));
    }

    @Test
    public void testToString()
    {
        DocumentReference reference1 = new DocumentReference("wiki", "space", "page");
        Assert.assertEquals("wiki:space.page", reference1.toString());

        DocumentReference reference2 = new DocumentReference("wiki", "space", "page", Locale.FRANCE);
        Assert.assertEquals("wiki:space.page(fr_FR)", reference2.toString());

        DocumentReference reference3 = new DocumentReference("wiki", "space", "page", "en");
        Assert.assertEquals("wiki:space.page(en)", reference3.toString());
    }

    @Test
    public void testCreateDocumentReferenceFromLocalDocumentReference()
    {
        Assert.assertEquals("wiki:space.page", new DocumentReference(new LocalDocumentReference("space", "page"),
            new WikiReference("wiki")).toString());
    }
}
