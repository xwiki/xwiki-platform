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
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link org.xwiki.model.reference.PageReference}.
 * 
 * @version $Id$
 */
public class PageReferenceTest
{
    @Test
    public void testConstructors()
    {
        PageReference reference = new PageReference("wiki", "space", "page");
        assertEquals(reference, new PageReference(new EntityReference("page", EntityType.PAGE,
            new EntityReference("space", EntityType.PAGE, new EntityReference("wiki", EntityType.WIKI)))));
        assertEquals(reference, new PageReference("wiki", Arrays.asList("space", "page")));
        assertEquals(reference, new PageReference("page", new PageReference("space", new WikiReference("wiki"))));
    }

    @Test
    public void testInvalidType()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new PageReference(new EntityReference("page", EntityType.DOCUMENT)));

        assertEquals("Invalid type [DOCUMENT] for a page reference", e.getMessage());
    }

    @Test
    public void testInvalidNullParent()
    {
        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> new PageReference("page", (WikiReference) null));

        assertEquals("Invalid parent reference [null] in a page reference", e.getMessage());
    }

    @Test
    public void testInvalidParentType()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new PageReference(
            new EntityReference("page", EntityType.PAGE, new EntityReference("document", EntityType.DOCUMENT))));

        assertEquals("Invalid parent reference [Document document] in a page reference", e.getMessage());
    }

    @Test
    public void testGetWikiReference()
    {
        PageReference reference = new PageReference("wiki", "space", "page");
        assertEquals(new WikiReference("wiki"), reference.getWikiReference());
    }

    @Test
    public void testToString()
    {
        PageReference reference1 = new PageReference("wiki", "space", "page");
        assertEquals("wiki:space/page", reference1.toString());

        PageReference reference2 = new PageReference("wiki", Arrays.asList("space", "page"), Locale.FRANCE);
        assertEquals("wiki:space/page;fr_FR", reference2.toString());
    }

    @Test
    public void testCreatePageReferenceFromLocalPageReference()
    {
        assertEquals(new PageReference("wiki", "space", "page"),
            new PageReference(new LocalPageReference("space", "page"), new WikiReference("wiki")));
    }

    @Test
    public void testReplaceParent()
    {
        PageReference reference = new PageReference("wiki", "space", "page").replaceParent(
            new EntityReference("space2", EntityType.PAGE, new EntityReference("wiki2", EntityType.WIKI)));

        assertEquals(new PageReference("wiki2", "space2", "page"), reference);

        assertSame(reference, reference.replaceParent(reference.getParent()));
    }
}
