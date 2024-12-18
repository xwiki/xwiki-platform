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

import java.util.Comparator;
import java.util.Objects;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Compares the list of document references from two document iterators.
 * 
 * @param <T> the type of data used to determine if a document is up to date
 * @version $Id$
 * @since 5.4.5
 */
public class DiffDocumentIterator<T> extends AbstractDocumentIterator<DiffDocumentIterator.Action>
{
    /**
     * The action that must be taken in order to move from the previous version to the next version.
     */
    public enum Action
    {
        /** This document hasn't changed so you can skip it. */
        SKIP,

        /** This document is new so you must add it. */
        ADD,

        /** This document doesn't exist anymore so you must delete it. */
        DELETE,

        /** This document has been modified so you must update it. */
        UPDATE
    }

    /**
     * The document iterator that corresponds to the previous state (the store that needs to be updated).
     */
    private final DocumentIterator<T> previous;

    /**
     * The document iterator that corresponds to the next state (the store that is used as a reference point).
     */
    private final DocumentIterator<T> next;

    /**
     * Used to compare document references.
     */
    private final Comparator<DocumentReference> documentReferenceComparator = getComparator();

    /**
     * The last entry taken from the {@link #previous} iterator.
     */
    private Pair<DocumentReference, T> previousEntry;

    /**
     * The last entry taken from the {@link #next} iterator.
     */
    private Pair<DocumentReference, T> nextEntry;

    /**
     * The last compare result between {@link #previousEntry} and {@link #nextEntry}.
     */
    private int diff;

    /**
     * Initializes this iterator with the two iterators to compare.
     * 
     * @param previous the document iterator that corresponds to the previous state (the store that needs to be updated)
     * @param next the document iterator that corresponds to the next state (the store that is used as a reference
     *            point)
     */
    public DiffDocumentIterator(DocumentIterator<T> previous, DocumentIterator<T> next)
    {
        this.previous = previous;
        this.next = next;
    }

    @Override
    public void setRootReference(EntityReference rootReference)
    {
        previous.setRootReference(rootReference);
        next.setRootReference(rootReference);
    }

    @Override
    public boolean hasNext()
    {
        return previous.hasNext() || next.hasNext();
    }

    @Override
    public Pair<DocumentReference, Action> next()
    {
        DocumentReference documentReference;
        Action action;
        if (next.hasNext() && previous.hasNext()) {
            if (diff >= 0) {
                nextEntry = next.next();
            }
            if (diff <= 0) {
                previousEntry = previous.next();
            }
            diff = documentReferenceComparator.compare(previousEntry.getKey(), nextEntry.getKey());
            if (diff == 0) {
                documentReference = nextEntry.getKey();
                // Compare the document version.
                if (nextEntry.getValue().equals(previousEntry.getValue())) {
                    action = Action.SKIP;
                } else {
                    action = Action.UPDATE;
                }
            } else if (diff > 0) {
                documentReference = nextEntry.getKey();
                action = Action.ADD;
            } else {
                documentReference = previousEntry.getKey();
                action = Action.DELETE;
            }
        } else if (next.hasNext()) {
            documentReference = next.next().getKey();
            action = Action.ADD;
        } else {
            documentReference = previous.next().getKey();
            action = Action.DELETE;
        }

        return new ImmutablePair<DocumentReference, Action>(documentReference, action);
    }

    /**
     * Get the comparator used for comparing document references. This method is public for testing purposes.
     *
     * @return the comparator for comparing document references
     */
    public static Comparator<DocumentReference> getComparator()
    {
        EntityReferenceSerializer<String> localEntityReferenceSerializer =
            new LocalStringEntityReferenceSerializer(new DefaultSymbolScheme());
        return Comparator.comparing(DocumentReference::getWikiReference)
            // Compare by the whole space as string as this is also what we compare in the database and in Solr.
            .thenComparing(documentReference -> localEntityReferenceSerializer.serialize(documentReference.getParent()))
            .thenComparing(DocumentReference::getName)
            .thenComparing(documentReference -> Objects.toString(documentReference.getLocale(), ""));
    }

    @Override
    public long size()
    {
        return Math.max(previous.size(), next.size());
    }
}
