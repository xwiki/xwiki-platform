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
package org.xwiki.eventstream.internal;

import java.util.Arrays;
import java.util.Collections;

import javax.script.ScriptContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.UntypedRecordableEventDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Units tests for {@link UntypedEventListener}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
public class UntypedEventListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<UntypedEventListener> mocker =
            new MockitoComponentMockingRule<>(UntypedEventListener.class);

    private ObservationManager observationManager;

    private DocumentReferenceResolver documentReferenceResolver;

    private ScriptContextManager scriptContextManager;

    private ComponentManager componentManager;

    private ModelBridge modelBridge;

    private DocumentUpdatingEvent registeredEvent1;
    private DocumentUpdatedEvent registeredEvent2;
    private DocumentDeletedEvent registeredEvent3;
    private DocumentCreatingEvent registeredEvent4;
    private DocumentCreatedEvent registeredEvent5;

    private LocalDocumentReference tagClassLocalReference;
    private LocalDocumentReference randomClassLocalReference;

    @Before
    public void setUp() throws Exception
    {
        observationManager = mocker.registerMockComponent(ObservationManager.class);
        componentManager = mocker.registerMockComponent(ComponentManager.class, "wiki");
        modelBridge = mocker.registerMockComponent(ModelBridge.class);

        scriptContextManager = mocker.registerMockComponent(ScriptContextManager.class);
        when(scriptContextManager.getCurrentScriptContext()).thenReturn(mock(ScriptContext.class));

        documentReferenceResolver = mocker.registerMockComponent(DocumentReferenceResolver.class);

        // registeredEvent{1, 2, 3, 4, 5, 6} are events that will pass the «Event Triggers» tests
        registeredEvent1 = mock(DocumentUpdatingEvent.class);
        registeredEvent2 = mock(DocumentUpdatedEvent.class);
        registeredEvent3 = mock(DocumentDeletedEvent.class);
        registeredEvent4 = mock(DocumentCreatingEvent.class);
        registeredEvent5 = mock(DocumentCreatedEvent.class);

        tagClassLocalReference = mock(LocalDocumentReference.class);
        randomClassLocalReference = mock(LocalDocumentReference.class);
    }

    /**
     * Mocks the component manager in order to add default comportments.
     *
     * @throws Exception
     */
    private void mockUntypedRecordableEventDescriptorManager() throws Exception
    {
        UntypedRecordableEventDescriptor descriptor1 = mock(UntypedRecordableEventDescriptor.class);
        when(descriptor1.getEventTriggers()).thenReturn(Arrays.asList(
                registeredEvent1.getClass().getCanonicalName(),
                DocumentUpdatedEvent.class.getCanonicalName()
        ));

        UntypedRecordableEventDescriptor descriptor2 = mock(UntypedRecordableEventDescriptor.class);
        when(descriptor2.getEventTriggers()).thenReturn(Arrays.asList(
                registeredEvent2.getClass().getCanonicalName(),
                ApplicationStoppedEvent.class.getCanonicalName()
        ));

        UntypedRecordableEventDescriptor descriptor3 = mock(UntypedRecordableEventDescriptor.class);
        when(descriptor3.getEventTriggers()).thenReturn(Arrays.asList(
                registeredEvent3.getClass().getCanonicalName()
        ));

        UntypedRecordableEventDescriptor descriptor4 = mock(UntypedRecordableEventDescriptor.class);
        when(descriptor4.getEventTriggers()).thenReturn(Arrays.asList(
                registeredEvent4.getClass().getCanonicalName(),
                DocumentUpdatedEvent.class.getCanonicalName()
        ));

        UntypedRecordableEventDescriptor descriptor5 = mock(UntypedRecordableEventDescriptor.class);
        when(descriptor5.getEventTriggers()).thenReturn(Arrays.asList(
                registeredEvent5.getClass().getCanonicalName()
        ));

        when(descriptor1.getValidationExpression()).thenReturn(" ");
        when(descriptor1.getObjectTypes()).thenReturn(Collections.EMPTY_LIST);

        when(descriptor2.getValidationExpression()).thenReturn(
                "{{velocity}} #if(1==1) true #else false #end {{/velocity}}");
        when(descriptor2.getObjectTypes()).thenReturn(Arrays.asList("XWiki.TagClass"));

        when(descriptor3.getValidationExpression()).thenReturn(
                "{{velocity}} #if(1!=1) true #else false #end {{/velocity}}");
        when(descriptor3.getObjectTypes()).thenReturn(Collections.EMPTY_LIST);

        when(descriptor4.getValidationExpression()).thenReturn("  ");
        when(descriptor4.getObjectTypes()).thenReturn(Arrays.asList("XWiki.TagClass"));

        when(descriptor5.getValidationExpression()).thenReturn(
                "{{velocity}}\n#if(2==2) true #else false #end\n{{/velocity}}");
        when(descriptor5.getObjectTypes()).thenReturn(Collections.EMPTY_LIST);

        when(this.componentManager.getInstanceList(any())).thenReturn(Arrays.asList(
                descriptor1,
                descriptor2,
                descriptor3,
                descriptor4,
                descriptor5
        ));

        DocumentReference tagClassReference = mock(DocumentReference.class);
        when(tagClassReference.getLocalDocumentReference()).thenReturn(this.tagClassLocalReference);
        when(this.documentReferenceResolver.resolve("XWiki.TagClass")).thenReturn(tagClassReference);

        DocumentReference randomClassReference = mock(DocumentReference.class);
        when(randomClassReference.getLocalDocumentReference()).thenReturn(this.randomClassLocalReference);
        when(this.documentReferenceResolver.resolve("XWiki.AnotherRandomClass"))
                .thenReturn(randomClassReference);
    }

    @Test
    public void onEventWithWrongEvent() throws Exception
    {
        Object source = mock(Object.class);
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);

        mockUntypedRecordableEventDescriptorManager();

        mocker.getComponentUnderTest().onEvent(event, source, null);

        verify(this.observationManager, never()).notify(any(), any(), any());
    }

    @Test
    public void onEventWithCorrectEventAndDefinedXObjectAndBlankValidation() throws Exception
    {
        Object source = mock(Object.class);

        when(this.modelBridge.checkXObjectPresence(any(), any())).thenReturn(true);

        mockUntypedRecordableEventDescriptorManager();

        mocker.getComponentUnderTest().onEvent(registeredEvent4, source, null);

        verify(this.observationManager, times(1)).notify(any(), any(), any());
    }

    @Test
    public void onEventWithCorrectEventAndBlankXObjectAndIncorrectValidation() throws Exception
    {
        Object source = mock(Object.class);

        mockUntypedRecordableEventDescriptorManager();

        mocker.getComponentUnderTest().onEvent(registeredEvent3, source, null);

        verify(this.observationManager, never()).notify(any(), any(), any());
    }
}
