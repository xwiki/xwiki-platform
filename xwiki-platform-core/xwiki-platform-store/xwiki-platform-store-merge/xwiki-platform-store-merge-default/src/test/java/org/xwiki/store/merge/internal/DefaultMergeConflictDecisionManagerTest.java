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
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.AdditionalAnswers;
import org.xwiki.cache.infinispan.internal.InfinispanCacheFactory;
import org.xwiki.cache.internal.DefaultCacheManager;
import org.xwiki.cache.internal.DefaultCacheManagerConfiguration;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.Delta;
import org.xwiki.diff.internal.DefaultChunk;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultMergeConflictDecisionsManager}.
 *
 * @version $Id$
 * @since 11.8RC1
 */
@ComponentList({
    DefaultCacheManager.class,
    DefaultCacheManagerConfiguration.class,
    InfinispanCacheFactory.class })
@ComponentTest
public class DefaultMergeConflictDecisionManagerTest
{
    @InjectMockComponents
    private DefaultMergeConflictDecisionsManager mergeConflictDecisionsManager;

    @MockComponent
    private ConfigurationSource configurationSource;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @BeforeEach
    public void setup()
    {
        doAnswer(AdditionalAnswers.returnsArgAt(1)).when(configurationSource).getProperty(anyString(), anyString());
    }

    @Test
    public void recordDecisions()
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");
        EntityReference userReference = new DocumentReference("xwiki", "XWiki", "User");
        assertNull(mergeConflictDecisionsManager.getConflictDecisionList(documentReference, userReference));

        List<Conflict<?>> conflicts = new ArrayList<>();
        Conflict<?> conflict1 = mock(Conflict.class);
        when(conflict1.getReference()).thenReturn("conflict1");
        Delta delta = mock(Delta.class);
        when(delta.getNext()).thenReturn(new DefaultChunk(18, Arrays.asList("some", "elements")));
        when(conflict1.getDeltaCurrent()).thenReturn(delta);
        conflicts.add(conflict1);

        Conflict<?> conflict2 = mock(Conflict.class);
        when(conflict2.getReference()).thenReturn("conflict2");
        when(conflict2.getIndex()).thenReturn(42);
        conflicts.add(conflict2);

        mergeConflictDecisionsManager.recordConflicts(documentReference, userReference, conflicts);
        assertNull(mergeConflictDecisionsManager.getConflictDecisionList(documentReference, userReference));

        assertTrue(mergeConflictDecisionsManager.recordDecision(documentReference, userReference, "conflict1",
            ConflictDecision.DecisionType.CURRENT, null));
        // this referenced conflict does not exist
        assertFalse(mergeConflictDecisionsManager.recordDecision(documentReference, userReference, "unexisting",
            ConflictDecision.DecisionType.PREVIOUS, null));
        assertEquals("Cannot find a conflict with reference [unexisting] for document identifier [null_null]",
            logCapture.getMessage(0));
        assertTrue(mergeConflictDecisionsManager.recordDecision(documentReference, userReference, "conflict2",
            ConflictDecision.DecisionType.CUSTOM, Arrays.asList("Someting", "Custom")));

        List<ConflictDecision> conflictDecisionList =
            mergeConflictDecisionsManager.getConflictDecisionList(documentReference, userReference);
        assertEquals(2, conflictDecisionList.size());
        assertEquals(ConflictDecision.DecisionType.CURRENT, conflictDecisionList.get(0).getType());
        assertEquals(ConflictDecision.DecisionType.CUSTOM, conflictDecisionList.get(1).getType());

        assertSame(conflict1, conflictDecisionList.get(0).getConflict());
        assertSame(conflict2, conflictDecisionList.get(1).getConflict());

        assertEquals(18, conflictDecisionList.get(0).getChunk().getIndex());
        assertEquals(Arrays.asList("some", "elements"), conflictDecisionList.get(0).getChunk().getElements());

        assertEquals(42, conflictDecisionList.get(1).getChunk().getIndex());
        assertEquals(Arrays.asList("Someting", "Custom"), conflictDecisionList.get(1).getChunk().getElements());

        mergeConflictDecisionsManager.removeConflictDecisionList(documentReference, userReference);
        assertNull(mergeConflictDecisionsManager.getConflictDecisionList(documentReference, userReference));
    }
}
