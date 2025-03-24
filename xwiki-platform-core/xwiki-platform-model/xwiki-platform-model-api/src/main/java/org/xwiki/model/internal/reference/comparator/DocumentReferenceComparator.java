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
package org.xwiki.model.internal.reference.comparator;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Compares document references by looking at the entity names in the parent chain and at the document locale.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class DocumentReferenceComparator implements Comparator<DocumentReference>
{
    /**
     * Whether to ignore the "WebHome" suffix or not.
     */
    private final boolean asNestedPages;

    /**
     * Default constructor.
     */
    public DocumentReferenceComparator()
    {
        this(false);
    }

    /**
     * Creates a new instance.
     * 
     * @param asNestedPages {@code true} to ignore the "WebHome" suffix (i.e. to put the space home page before the
     *            space child pages), {@code false} otherwise
     */
    public DocumentReferenceComparator(boolean asNestedPages)
    {
        this.asNestedPages = asNestedPages;
    }

    @Override
    public int compare(DocumentReference alice, DocumentReference bob)
    {
        int diff = compareReferenceChains(getActualReference(alice), getActualReference(bob));
        if (diff == 0) {
            // Both references have the same number of components and they all match. Use the locale as a tie-breaker.
            diff = compareLocales(alice, bob);
        }
        return diff;
    }

    private EntityReference getActualReference(DocumentReference documentReference)
    {
        if (this.asNestedPages && "WebHome".equals(documentReference.getName())) {
            return documentReference.getLastSpaceReference();
        } else {
            return documentReference;
        }
    }

    private int compareReferenceChains(EntityReference alice, EntityReference bob)
    {
        Iterator<EntityReference> aliceIterator = getPath(alice).iterator();
        Iterator<EntityReference> bobIterator = getPath(bob).iterator();
        // The number of components in an entity reference can vary (e.g. for nested pages).
        while (aliceIterator.hasNext() && bobIterator.hasNext()) {
            int diff = compareSiblingEntities(aliceIterator.next(), bobIterator.next());
            if (diff != 0) {
                return diff;
            }
        }
        if (aliceIterator.hasNext()) {
            // Alice's path is longer.
            return 1;
        } else if (bobIterator.hasNext()) {
            // Bob's path is longer.
            return -1;
        }
        return 0;
    }

    /**
     * @param entityReference the entity reference for which to get the path
     * @return the path from the root of the entity hierarchy to the specified entity (including it)
     */
    protected List<EntityReference> getPath(EntityReference entityReference)
    {
        return entityReference.getReversedReferenceChain();
    }

    /**
     * Compares two entities that have the same parent.
     *
     * @param alice the first entity
     * @param bob the second entity
     * @return a negative integer, zero, or a positive integer as the first entity is less than, equal to, or greater
     *         than the second entity
     */
    protected int compareSiblingEntities(EntityReference alice, EntityReference bob)
    {
        return alice.getName().compareTo(bob.getName());
    }

    private int compareLocales(DocumentReference alice, DocumentReference bob)
    {
        String aliceLocale = alice.getLocale() != null ? alice.getLocale().toString() : "";
        String bobLocale = bob.getLocale() != null ? bob.getLocale().toString() : "";
        return aliceLocale.compareTo(bobLocale);
    }
}
