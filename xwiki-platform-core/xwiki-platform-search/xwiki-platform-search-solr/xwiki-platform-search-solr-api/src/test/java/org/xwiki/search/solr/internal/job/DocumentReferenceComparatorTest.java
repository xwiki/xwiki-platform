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

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

/**
 * Unit tests for {@link DocumentReferenceComparator}.
 * 
 * @version $Id$
 * @since 5.4.5
 */
public class DocumentReferenceComparatorTest
{
    @Test
    public void compare()
    {
        DocumentReferenceComparator comparator = new DocumentReferenceComparator();

        DocumentReference reference = new DocumentReference("wiki", "Space", "Page");
        assertEquals(0, comparator.compare(reference, new DocumentReference(reference)));

        assertEquals(0, comparator.compare(new DocumentReference(reference, Locale.FRENCH), new DocumentReference(
            reference, Locale.FRENCH)));

        assertTrue(comparator.compare(new DocumentReference(reference, Locale.ROOT), new DocumentReference(reference,
            Locale.GERMAN)) < 0);

        assertTrue(comparator.compare(new DocumentReference(reference, Locale.ITALIAN), reference) > 0);

        assertTrue(comparator.compare(reference, new DocumentReference("wiki", "Space", "APage")) > 0);

        assertTrue(comparator.compare(reference, new DocumentReference("wiki", "XSpace", "APage")) < 0);

        assertTrue(comparator.compare(reference, new DocumentReference("aWiki", "ASpace", "APage")) > 0);
    }
}
