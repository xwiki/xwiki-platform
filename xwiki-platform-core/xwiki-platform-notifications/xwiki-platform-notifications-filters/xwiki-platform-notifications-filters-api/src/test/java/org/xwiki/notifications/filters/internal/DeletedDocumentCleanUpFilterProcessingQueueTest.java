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
package org.xwiki.notifications.filters.internal;

import java.util.List;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.event.CleaningFilterEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DeletedDocumentCleanUpFilterProcessingQueue}.
 *
 * @version $Id$
 * @since 16.0.0RC1
 * @since 15.10.2
 */
@ComponentTest
class DeletedDocumentCleanUpFilterProcessingQueueTest
{
    @InjectMockComponents
    private DeletedDocumentCleanUpFilterProcessingQueue cleanUpFilterProcessingQueue;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @BeforeComponent
    void beforeComponent() throws ComponentLookupException
    {
        ExecutionContextManager executionContextManager = mock(ExecutionContextManager.class);
        Execution execution = mock(Execution.class);

        when(this.componentManager.getInstance(ExecutionContextManager.class)).thenReturn(executionContextManager);
        when(this.componentManager.getInstance(Execution.class)).thenReturn(execution);
    }

    @Test
    void addCleanUpTask() throws NotificationException, InterruptedException
    {
        DocumentReference userReference = mock(DocumentReference.class, "userReference");
        DocumentReference deletedDocReference = mock(DocumentReference.class, "deletedDoc");

        String serializedFullRef = "foowiki:Space.DeletedDocument";
        when(this.entityReferenceSerializer.serialize(deletedDocReference)).thenReturn(serializedFullRef);

        NotificationFilterPreference filterPref1 = mock(NotificationFilterPreference.class, "filterPref1");
        NotificationFilterPreference filterPref2 = mock(NotificationFilterPreference.class, "filterPref2");
        NotificationFilterPreference filterPref3 = mock(NotificationFilterPreference.class, "filterPref3");
        NotificationFilterPreference filterPref4 = mock(NotificationFilterPreference.class, "filterPref4");

        when(filterPref1.getId()).thenReturn("filterPref1");
        when(filterPref2.getId()).thenReturn("filterPref2");
        when(filterPref3.getId()).thenReturn("filterPref3");
        when(filterPref4.getId()).thenReturn("filterPref4");

        when(filterPref2.getPageOnly()).thenReturn(serializedFullRef);
        when(filterPref4.getPageOnly()).thenReturn(serializedFullRef);

        when(this.notificationFilterPreferenceManager.getFilterPreferences(userReference)).thenReturn(List.of(
            filterPref1,
            filterPref2,
            filterPref3,
            filterPref4
        ));

        this.cleanUpFilterProcessingQueue.addCleanUpTask(userReference, deletedDocReference);
        Thread.sleep(100);

        verify(this.observationManager).notify(any(CleaningFilterEvent.class), eq(deletedDocReference),
            eq(Set.of(filterPref2, filterPref4)));
        verify(this.notificationFilterPreferenceManager)
            .deleteFilterPreferences(userReference, Set.of("filterPref2", "filterPref4"));

        when(this.entityReferenceSerializer.serialize(deletedDocReference)).thenReturn("otherRef");
        when(filterPref1.getPageOnly()).thenReturn("otherRef");

        doAnswer(invocationOnMock -> {
            CleaningFilterEvent event = invocationOnMock.getArgument(0);
            event.cancel();
            return null;
        }).when(this.observationManager)
            .notify(any(CleaningFilterEvent.class), eq(deletedDocReference), eq(Set.of(filterPref1)));

        this.cleanUpFilterProcessingQueue.addCleanUpTask(userReference, deletedDocReference);
        Thread.sleep(100);

        verify(this.observationManager).notify(any(CleaningFilterEvent.class), eq(deletedDocReference),
            eq(Set.of(filterPref1)));
        verify(this.notificationFilterPreferenceManager, never())
            .deleteFilterPreferences(userReference, Set.of("filterPref1"));
    }
}