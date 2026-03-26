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
import org.xwiki.search.solr.internal.job.AbstractDocumentIterator.DocumentIteratorEntry;
import org.xwiki.search.solr.internal.job.DiffDocumentIterator.Action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

    private ImmutablePair<DocumentReference, DocumentIteratorEntry> entry(String wiki, String space, String document,
        long docId, String version)
    {
        return new ImmutablePair<DocumentReference, DocumentIteratorEntry>(new DocumentReference(wiki, space, document),
            new DocumentIteratorEntry(new WikiReference(wiki), docId, version));
    }

    @Test
    public void iterate()
    {
        List<Pair<DocumentReference, DocumentIteratorEntry>> previous =
            new ArrayList<Pair<DocumentReference, DocumentIteratorEntry>>();
        previous.add(entry("chess", "B", "M", 2, "2.3"));
        previous.add(entry("chess", "E", "A", 3, "5.1"));
        previous.add(entry("xwiki", "A", "S", 4, "1.1"));
        DocumentIterator<DocumentIteratorEntry> previousIterator =
            new DocumentIteratorStub<DocumentIteratorEntry>(previous);

        List<Pair<DocumentReference, DocumentIteratorEntry>> next =
            new ArrayList<Pair<DocumentReference, DocumentIteratorEntry>>();
        next.add(entry("chess", "B", "L", 1, "1.2"));
        next.add(entry("chess", "B", "M", 2, "4.7"));
        next.add(entry("xwiki", "A", "S", 4, "1.1"));
        next.add(entry("xwiki", "B", "P", 5, "2.4"));
        DocumentIterator<DocumentIteratorEntry> nextIterator = new DocumentIteratorStub<>(next);

        DiffDocumentIterator iterator = new DiffDocumentIterator(previousIterator, nextIterator);

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
        List<Pair<DocumentReference, DocumentIteratorEntry>> previous =
            new ArrayList<Pair<DocumentReference, DocumentIteratorEntry>>();
        previous.add(entry("wiki", "A", "B", 1, "3.1"));
        previous.add(entry("wiki", "X", "Y", 2, "5.2"));
        DocumentIterator<DocumentIteratorEntry> previousIterator = new DocumentIteratorStub<>(previous);

        List<Pair<DocumentReference, DocumentIteratorEntry>> next = Collections.emptyList();
        DocumentIterator<DocumentIteratorEntry> nextIterator = new DocumentIteratorStub<>(next);

        DiffDocumentIterator iterator = new DiffDocumentIterator(previousIterator, nextIterator);

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
        DocumentIterator<DocumentIteratorEntry> previous = mock(DocumentIterator.class, "previous");
        DocumentIterator<DocumentIteratorEntry> next = mock(DocumentIterator.class, "next");
        DiffDocumentIterator iterator = new DiffDocumentIterator(previous, next);

        WikiReference rootReference = new WikiReference("foo");
        iterator.setRootReference(rootReference);

        verify(previous).setRootReference(rootReference);
        verify(next).setRootReference(rootReference);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void remove()
    {
        DocumentIterator<DocumentIteratorEntry> previous = mock(DocumentIterator.class, "previous");
        DocumentIterator<DocumentIteratorEntry> next = mock(DocumentIterator.class, "next");
        DiffDocumentIterator iterator = new DiffDocumentIterator(previous, next);
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
