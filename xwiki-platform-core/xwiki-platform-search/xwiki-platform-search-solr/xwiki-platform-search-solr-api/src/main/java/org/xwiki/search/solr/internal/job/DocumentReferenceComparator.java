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
import java.util.List;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Compares document references by looking at the entity names in the parent chain.
 * 
 * @version $Id$
 * @since 5.4.5
 */
public class DocumentReferenceComparator implements Comparator<DocumentReference>
{
    @Override
    public int compare(DocumentReference alice, DocumentReference bob)
    {
        List<EntityReference> aliceChain = alice.getReversedReferenceChain();
        List<EntityReference> bobChain = bob.getReversedReferenceChain();
        for (int i = 0; i < aliceChain.size(); i++) {
            int diff = aliceChain.get(i).getName().compareTo(bobChain.get(i).getName());
            if (diff != 0) {
                return diff;
            }
        }
        String aliceLocale = alice.getLocale() != null ? alice.getLocale().toString() : "";
        String bobLocale = bob.getLocale() != null ? bob.getLocale().toString() : "";
        return aliceLocale.compareTo(bobLocale);
    }
}
