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
package org.xwiki.notifications.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SimilarityCalculator}.
 *
 * @version $Id$
 */
public class SimilarityCalculatorTest
{
    private SimilarityCalculator sq;

    @BeforeEach
    public void setUp() throws Exception
    {
        this.sq = new SimilarityCalculator();
    }

    @Test
    void computeSimilarity()
    {
        DocumentReference document =
                new DocumentReference("xwiki", "somewhere", "something");
        String group = "myGroupId";
        String type = "type";

        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);

        when(event1.getDocument()).thenReturn(document);
        when(event2.getDocument()).thenReturn(document);

        when(event1.getGroupId()).thenReturn(group);
        when(event2.getGroupId()).thenReturn(group);

        assertEquals(SimilarityCalculator.SAME_GROUP_ID_AND_DOCUMENT_BUT_DIFFERENT_TYPES,
            this.sq.computeSimilarity(event1, event2));

        when(event2.getGroupId()).thenReturn("somethingElse");

        assertEquals(SimilarityCalculator.NO_SIMILARITY, this.sq.computeSimilarity(event1, event2));

        when(event1.getType()).thenReturn(type);
        when(event2.getType()).thenReturn(type);

        assertEquals(SimilarityCalculator.SAME_DOCUMENT_AND_TYPE, this.sq.computeSimilarity(event1, event2));

        // Even if the group is the same, if they share the same type, we consider not as the same groupid
        when(event2.getGroupId()).thenReturn(group);
        assertEquals(SimilarityCalculator.SAME_DOCUMENT_AND_TYPE, this.sq.computeSimilarity(event1, event2));
    }
}
