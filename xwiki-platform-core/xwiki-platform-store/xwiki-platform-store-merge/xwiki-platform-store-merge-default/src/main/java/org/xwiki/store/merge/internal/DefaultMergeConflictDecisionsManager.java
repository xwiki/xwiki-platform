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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.internal.DefaultConflictDecision;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
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
    private Cache<Map<String, Conflict>> conflictsCache;

    private Map<String, List<ConflictDecision>> decisionMap;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private EntityReferenceSerializer<String> stringEntityReferenceSerializer;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public DefaultMergeConflictDecisionsManager()
    {
        this.decisionMap = new HashMap<>();
    }

    private void initCache()
    {
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setConfigurationId("xwiki.store.merge.conflictDecisionManager");
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(1000);
        lru.setLifespan(3600);
        cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);
        try {
            conflictsCache = cacheManager.createNewCache(cacheConfiguration);
        } catch (CacheException e) {
            logger.error("Error when creating the cache of conflicts.", e);
        }
    }

    private Cache<Map<String, Conflict>> getConflictsCache()
    {
        if (this.conflictsCache == null) {
            initCache();
        }
        return this.conflictsCache;
    }

    private String getIdentifier(DocumentReference documentReference, EntityReference userReference)
    {
        return String.format("%s_%s", stringEntityReferenceSerializer.serialize(documentReference),
            stringEntityReferenceSerializer.serialize(userReference));
    }

    @Override
    public List<ConflictDecision> getConflictDecisionList(DocumentReference documentReference,
        EntityReference userReference)
    {
        return decisionMap.get(getIdentifier(documentReference, userReference));
    }

    @Override
    public void setConflictDecisionList(List<ConflictDecision> conflictDecisionList,
        DocumentReference documentReference, EntityReference userReference)
    {
        decisionMap.put(getIdentifier(documentReference, userReference), conflictDecisionList);
    }

    @Override
    public void removeConflictDecisionList(DocumentReference documentReference,
        EntityReference userReference)
    {
        decisionMap.remove(getIdentifier(documentReference, userReference));
    }

    @Override
    public void recordConflicts(DocumentReference documentReference, EntityReference userReference,
        List<Conflict<?>> conflicts)
    {
        Map<String, Conflict> conflictMap = new HashMap<>();
        if (conflicts != null) {
            for (Conflict conflict : conflicts) {
                conflictMap.put(conflict.getReference(), conflict);
            }
        }
        getConflictsCache().set(getIdentifier(documentReference, userReference), conflictMap);
    }

    private void addDecision(String identifier, ConflictDecision conflictDecision)
    {
        List<ConflictDecision> conflictDecisionList;
        if (decisionMap.containsKey(identifier)) {
            conflictDecisionList = decisionMap.get(identifier);
        } else {
            conflictDecisionList = new ArrayList<>();
            decisionMap.put(identifier, conflictDecisionList);
        }
        conflictDecisionList.add(conflictDecision);
    }

    @Override
    public <E> boolean recordDecision(DocumentReference documentReference, EntityReference userReference,
        String conflictReference, ConflictDecision.DecisionType decisionType, List<E> customDecision)
    {
        String entryId = getIdentifier(documentReference, userReference);
        Map<String, Conflict> conflictMap = getConflictsCache().get(entryId);

        boolean decisionRecorded = false;
        if (conflictMap != null) {
            Conflict conflict = conflictMap.get(conflictReference);
            if (conflict != null) {
                ConflictDecision conflictDecision = new DefaultConflictDecision(conflict);
                conflictDecision.setType(decisionType);
                if (customDecision != null && !customDecision.isEmpty()) {
                    conflictDecision.setCustom(customDecision);
                }
                this.addDecision(entryId, conflictDecision);
                decisionRecorded = true;
            } else {
                logger.error("Cannot find a conflict with reference [{}] for document identifier [{}]",
                    conflictReference, entryId);
            }
        } else {
            logger.error("Cannot find any conflict for document identifier [{}]", entryId);
        }

        return decisionRecorded;
    }
}
