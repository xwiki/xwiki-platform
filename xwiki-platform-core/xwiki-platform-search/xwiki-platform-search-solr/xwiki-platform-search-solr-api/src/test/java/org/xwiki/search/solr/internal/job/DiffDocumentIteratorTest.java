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
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.job.DiffDocumentIterator.Action;

/**
 * Unit tests for {@link DiffDocumentIterator}.
 * 
 * @version $Id$
 * @since 5.4.5
 */
public class DiffDocumentIteratorTest
{
    public static class DocumentIteratorStub<T> implements DocumentIterator<T>
    {
        private int size;

        private Iterator<Pair<DocumentReference, T>> iterator;

        public DocumentIteratorStub(List<Pair<DocumentReference, T>> list)
        {
            size = list.size();
            iterator = list.iterator();
        }

        @Override
        public boolean hasNext()
        {
            return iterator.hasNext();
        }

        @Override
        public Pair<DocumentReference, T> next()
        {
            return iterator.next();
        }

        @Override
        public void remove()
        {
            iterator.remove();
        }

        @Override
        public void setRootReference(EntityReference rootReference)
        {
        }

        @Override
        public long size()
        {
            return size;
        }
    }

    @Test
    public void iterate()
    {
        List<Pair<DocumentReference, String>> previous = new ArrayList<Pair<DocumentReference, String>>();
        previous.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("chess", "B", "M"), "2.3"));
        previous.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("chess", "E", "A"), "5.1"));
        previous.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("xwiki", "A", "S"), "1.1"));
        DocumentIterator<String> previousIterator = new DocumentIteratorStub<String>(previous);

        List<Pair<DocumentReference, String>> next = new ArrayList<Pair<DocumentReference, String>>();
        next.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("chess", "B", "L"), "1.2"));
        next.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("chess", "B", "M"), "4.7"));
        next.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("xwiki", "A", "S"), "1.1"));
        next.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("xwiki", "B", "P"), "2.4"));
        DocumentIterator<String> nextIterator = new DocumentIteratorStub<String>(next);

        DiffDocumentIterator<String> iterator = new DiffDocumentIterator<String>(previousIterator, nextIterator);

        assertEquals(4, iterator.size());

        List<Pair<DocumentReference, Action>> actualResult = new ArrayList<Pair<DocumentReference, Action>>();
        while (iterator.hasNext()) {
            actualResult.add(iterator.next());
        }

        List<Pair<DocumentReference, Action>> expectedResult = new ArrayList<Pair<DocumentReference, Action>>();
        expectedResult.add(new ImmutablePair<DocumentReference, Action>(next.get(0).getKey(), Action.ADD));
        expectedResult.add(new ImmutablePair<DocumentReference, Action>(next.get(1).getKey(), Action.UPDATE));
        expectedResult.add(new ImmutablePair<DocumentReference, Action>(previous.get(1).getKey(), Action.DELETE));
        expectedResult.add(new ImmutablePair<DocumentReference, Action>(next.get(2).getKey(), Action.SKIP));
        expectedResult.add(new ImmutablePair<DocumentReference, Action>(next.get(3).getKey(), Action.ADD));

        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void deleteAll()
    {
        List<Pair<DocumentReference, String>> previous = new ArrayList<Pair<DocumentReference, String>>();
        previous.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("wiki", "A", "B"), "3.1"));
        previous.add(new ImmutablePair<DocumentReference, String>(new DocumentReference("wiki", "X", "Y"), "5.2"));
        DocumentIterator<String> previousIterator = new DocumentIteratorStub<String>(previous);

        List<Pair<DocumentReference, String>> next = Collections.emptyList();
        DocumentIterator<String> nextIterator = new DocumentIteratorStub<String>(next);

        DiffDocumentIterator<String> iterator = new DiffDocumentIterator<String>(previousIterator, nextIterator);

        List<Pair<DocumentReference, Action>> actualResult = new ArrayList<Pair<DocumentReference, Action>>();
        while (iterator.hasNext()) {
            actualResult.add(iterator.next());
        }

        List<Pair<DocumentReference, Action>> expectedResult = new ArrayList<Pair<DocumentReference, Action>>();
        expectedResult.add(new ImmutablePair<DocumentReference, Action>(previous.get(0).getKey(), Action.DELETE));
        expectedResult.add(new ImmutablePair<DocumentReference, Action>(previous.get(1).getKey(), Action.DELETE));
        assertEquals(expectedResult, actualResult);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void setRootReference()
    {
        DocumentIterator<String> previous = mock(DocumentIterator.class, "previous");
        DocumentIterator<String> next = mock(DocumentIterator.class, "next");
        DiffDocumentIterator<String> iterator = new DiffDocumentIterator<String>(previous, next);

        WikiReference rootReference = new WikiReference("foo");
        iterator.setRootReference(rootReference);

        verify(previous).setRootReference(rootReference);
        verify(next).setRootReference(rootReference);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void remove()
    {
        DocumentIterator<String> previous = mock(DocumentIterator.class, "previous");
        DocumentIterator<String> next = mock(DocumentIterator.class, "next");
        DiffDocumentIterator<String> iterator = new DiffDocumentIterator<String>(previous, next);
        try {
            iterator.remove();
            fail();
        } catch (Exception e) {
            if (!(e instanceof UnsupportedOperationException)) {
                fail();
            }
        }
    }
}
