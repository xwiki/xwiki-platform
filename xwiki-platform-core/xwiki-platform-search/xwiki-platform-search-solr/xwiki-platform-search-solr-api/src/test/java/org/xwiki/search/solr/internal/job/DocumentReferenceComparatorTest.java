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
package org.xwiki.search.solr.internal.job;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DocumentReferenceComparator}.
 * 
 * @version $Id$
 * @since 5.4.5
 */
public class DocumentReferenceComparatorTest
{
    private DocumentReferenceComparator comparator = new DocumentReferenceComparator();

    @Test
    public void compare()
    {
        DocumentReference reference = new DocumentReference("wiki", "Space", "Page");
        assertEquals(0, comparator.compare(reference, new DocumentReference(reference)));

        assertEquals(0, comparator.compare(new DocumentReference(reference, Locale.FRENCH),
            new DocumentReference(reference, Locale.FRENCH)));

        assertTrue(comparator.compare(new DocumentReference(reference, Locale.ROOT),
            new DocumentReference(reference, Locale.GERMAN)) < 0);

        assertTrue(comparator.compare(new DocumentReference(reference, Locale.ITALIAN), reference) > 0);

        assertTrue(comparator.compare(reference, new DocumentReference("wiki", "Space", "APage")) > 0);

        assertTrue(comparator.compare(reference, new DocumentReference("wiki", "XSpace", "APage")) < 0);

        assertTrue(comparator.compare(reference, new DocumentReference("aWiki", "ASpace", "APage")) > 0);
    }

    @Test
    public void compareNestedPages()
    {
        assertTrue(compare(Arrays.asList("math", "Path", "To", "Page"), Arrays.asList("math", "Path", "Page")) > 0);
        assertTrue(compare(Arrays.asList("math", "Path", "Alice"), Arrays.asList("math", "Path", "To", "Bob")) < 0);
        assertTrue(
            compare(Arrays.asList("math", "Users", "Alice", "Files"), Arrays.asList("math", "Users", "Alice")) > 0);
        assertTrue(
            compare(Arrays.asList("math", "Users", "Alice"), Arrays.asList("math", "Users", "Alice", "Files")) < 0);
        assertEquals(0, compare(Arrays.asList("math", "Users", "Alice", "Files"),
            Arrays.asList("math", "Users", "Alice", "Files")));
    }

    private int compare(List<String> alice, List<String> bob)
    {
        assertTrue("Invalid document reference!", alice.size() >= 3);
        assertTrue("Invalid document reference!", bob.size() >= 3);
        DocumentReference aliceReference =
            new DocumentReference(alice.get(0), alice.subList(1, alice.size() - 1), alice.get(alice.size() - 1));
        DocumentReference bobReference =
            new DocumentReference(bob.get(0), bob.subList(1, bob.size() - 1), bob.get(bob.size() - 1));
        return this.comparator.compare(aliceReference, bobReference);
    }
}
