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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link org.xwiki.model.reference.DocumentReference}.
 * 
 * @version $Id$
 * @since 2.2M1
 */
class DocumentReferenceTest
{
    @Test
    void testConstructors()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        assertEquals(reference, new DocumentReference(new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("space", EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI)))));
        assertEquals(reference, new DocumentReference("wiki", Arrays.asList("space"), "page"));
        assertEquals(reference, new DocumentReference("page", new SpaceReference("space", new WikiReference("wiki"))));
        assertEquals(reference,
            new DocumentReference("page", new SpaceReference("space", new WikiReference("wiki")), (Locale) null));
        assertEquals(reference, new DocumentReference("wiki", "space", "page", (Locale) null));
        assertEquals(reference, new DocumentReference("wiki", "space", "page", (String) null));

        reference = new DocumentReference("wiki", "space", "page", Locale.CANADA);
        assertEquals(reference, new DocumentReference("wiki", "space", "page", "en_CA"));
    }

    @Test
    void testInvalidType()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new DocumentReference(new EntityReference("page", EntityType.SPACE)));

        assertEquals("Invalid type [SPACE] for a document reference", e.getMessage());
    }

    @Test
    void testInvalidNullParent()
    {
        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> new DocumentReference("page", null));

        assertEquals("Invalid parent reference [null] in a document reference", e.getMessage());
    }

    @Test
    void testInvalidParentType()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new DocumentReference(new EntityReference("page", EntityType.DOCUMENT, new WikiReference("wiki"))));

        assertEquals("Invalid parent reference [Wiki wiki] in a document reference", e.getMessage());
    }

    @Test
    void testGetWikiReference()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        assertEquals(new WikiReference("wiki"), reference.getWikiReference());
    }

    @Test
    void testGetLastSpaceReferenceWhenOneSpace()
    {
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        SpaceReference spaceReference = documentReference.getLastSpaceReference();
        assertEquals(new SpaceReference("space", new WikiReference("wiki")), spaceReference);
    }

    @Test
    void testGetLastSpaceReferenceWhenMultipleSpaces()
    {
        DocumentReference reference = new DocumentReference("wiki", Arrays.asList("space1", "space2"), "page");
        assertEquals(new SpaceReference("space2", new SpaceReference("space1", new WikiReference("wiki"))),
            reference.getLastSpaceReference());
    }

    @Test
    void testSpaceReferences()
    {
        DocumentReference reference1 = new DocumentReference("wiki", "space", "page");
        List<SpaceReference> spaceRefs = reference1.getSpaceReferences();
        assertEquals(1, spaceRefs.size());
        assertEquals(new SpaceReference("space", new WikiReference("wiki")), spaceRefs.get(0));

        DocumentReference reference2 = new DocumentReference("wiki", Arrays.asList("space1", "space2"), "page");
        List<SpaceReference> spaceRefs2 = reference2.getSpaceReferences();
        assertEquals(2, spaceRefs2.size());
        assertEquals(new SpaceReference("space1", new WikiReference("wiki")), spaceRefs2.get(0));
        assertEquals(new SpaceReference("space2", new SpaceReference("space1", new WikiReference("wiki"))),
            spaceRefs2.get(1));
    }

    @Test
    void testToString()
    {
        DocumentReference reference1 = new DocumentReference("wiki", "space", "page");
        assertEquals("wiki:space.page", reference1.toString());

        DocumentReference reference2 = new DocumentReference("wiki", "space", "page", Locale.FRANCE);
        assertEquals("wiki:space.page(fr_FR)", reference2.toString());

        DocumentReference reference3 = new DocumentReference("wiki", "space", "page", "en");
        assertEquals("wiki:space.page(en)", reference3.toString());
    }

    @Test
    void testCreateDocumentReferenceFromLocalDocumentReference()
    {
        assertEquals("wiki:space.page",
            new DocumentReference(new LocalDocumentReference("space", "page"), new WikiReference("wiki")).toString());
    }

    @Test
    void testReplaceParent()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "page").replaceParent(
            new EntityReference("space2", EntityType.SPACE, new EntityReference("wiki2", EntityType.WIKI)));

        assertEquals(new DocumentReference("wiki2", "space2", "page"), reference);

        assertSame(reference, reference.replaceParent(reference.getParent()));
    }

    @Test
    void withoutLocale()
    {
        assertEquals(new DocumentReference("wiki", "space", "page"),
            new DocumentReference("wiki", "space", "page", Locale.ENGLISH).withoutLocale());
        assertEquals(new DocumentReference("wiki", "space", "page"),
            new DocumentReference("wiki", "space", "page", Locale.ROOT).withoutLocale());
        assertEquals(new DocumentReference("wiki", "space", "page"),
            new DocumentReference("wiki", "space", "page").withoutLocale());
    }

    @Test
    void valueOf()
    {
        assertEquals(Optional.empty(), DocumentReference.valueOf(null));
        DocumentReference documentReference = new DocumentReference("xwiki", "Foo", "Bar");
        Optional<DocumentReference> obtainedReference = DocumentReference.valueOf(documentReference);
        assertFalse(obtainedReference.isEmpty());
        assertSame(documentReference, obtainedReference.get());

        assertEquals(Optional.empty(), DocumentReference.valueOf(new WikiReference("foo")));
        assertEquals(Optional.empty(), DocumentReference.valueOf(documentReference.getLastSpaceReference()));

        assertEquals(Optional.of(documentReference),
            DocumentReference.valueOf(new ObjectReference("Foo", documentReference)));
    }
}
