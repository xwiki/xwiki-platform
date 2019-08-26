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
package org.xwiki.store.merge.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.annotation.Component;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.store.merge.MergeConflictDecisionsManager;

/**
 * Default implementation of the {@link MergeConflictDecisionsManager}.
 *
 * @version $Id$
 * @since 11.8RC1
 */
@Singleton
@Component
public class DefaultMergeConflictDecisionsManager implements MergeConflictDecisionsManager
{
    private Map<Pair<DocumentReference, EntityReference>, List<ConflictDecision>> decisionMap;

    /**
     * Default constructor.
     */
    public DefaultMergeConflictDecisionsManager()
    {
        this.decisionMap = new HashMap<>();
    }

    @Override
    public List<ConflictDecision> getConflictDecisionList(DocumentReference documentReference,
        EntityReference userReference)
    {
        return decisionMap.get(Pair.of(documentReference, userReference));
    }

    @Override
    public void setConflictDecisionList(List<ConflictDecision> conflictDecisionList,
        DocumentReference documentReference, EntityReference userReference)
    {
        decisionMap.put(Pair.of(documentReference, userReference), conflictDecisionList);
    }

    @Override
    public void removeConflictDecisionList(DocumentReference documentReference,
        EntityReference userReference)
    {
        decisionMap.remove(Pair.of(documentReference, userReference));
    }
}
