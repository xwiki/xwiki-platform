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
import java.util.List;

import javax.inject.Named;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.UntypedRecordableEventDescriptor;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
@ComponentTest
class UntypedEventListenerTest
{
    @InjectMockComponents
    private UntypedEventListener listener;

    /**
     * Inner classed used to verify what is sent to the observation manager
     */
    class MyAnswer implements Answer
    {
        private DefaultUntypedRecordableEvent sentEvent;

        @Override
        public Object answer(InvocationOnMock invocationOnMock)
        {
            this.sentEvent = invocationOnMock.getArgument(0);
            return null;
        }

        public DefaultUntypedRecordableEvent getSentEvent()
        {
            return this.sentEvent;
        }
    }

    private MyAnswer answer;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    private TemplateManager templateManager;

    @Mock
    private ScriptContext scriptContext;

    @MockComponent
    @Named("html/5.0")
    private BlockRenderer renderer;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(this.scriptContext);

        Template template = mock(Template.class);
        when(this.templateManager.createStringTemplate(anyString(), any(), any(), any())).thenReturn(template);
        when(this.templateManager.execute(template)).thenReturn(mock(XDOM.class));
    }

    @BeforeEach
    void watchObservationManager()
    {
        this.answer = new MyAnswer();
        doAnswer(this.answer).when(this.observationManager)
            .notify(any(DefaultUntypedRecordableEvent.class), anyString(), any());
    }

    private UntypedRecordableEventDescriptor mockDescriptor() throws Exception
    {
        UntypedRecordableEventDescriptor descriptor = mock(UntypedRecordableEventDescriptor.class);
        when(descriptor.getEventTriggers()).thenReturn(List.of(DocumentUpdatedEvent.class.getCanonicalName()));
        when(descriptor.getEventType()).thenReturn("myCustomEvent");
        when(this.modelBridge.checkXObjectPresence(any(), any())).thenReturn(true);
        when(this.componentManager.getInstanceList(any())).thenReturn(List.of(descriptor));

        return descriptor;
    }

    @Test
    void onEventWithBlankValidationAndBlankTarget() throws Exception
    {
        // Mocks
        mockDescriptor();

        // Test
        this.listener.onEvent(new DocumentUpdatedEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager).notify(any(DefaultUntypedRecordableEvent.class), any(), any());

        assertNotNull(this.answer.getSentEvent());
        assertTrue(this.answer.getSentEvent().getTarget().isEmpty());
        assertEquals("myCustomEvent", this.answer.getSentEvent().getEventType());
    }

    @Test
    void onEventWithWrongEvent() throws Exception
    {
        // Mocks
        mockDescriptor();

        // Test
        this.listener.onEvent(new ApplicationReadyEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager, never()).notify(any(), any(), any());
        assertNull(this.answer.getSentEvent());
    }

    @Test
    void onEventWithoutXObject() throws Exception
    {
        // Mocks
        mockDescriptor();

        // Same than onEventWithBlankValidationAndBlankTarget() but with no XObject
        when(this.modelBridge.checkXObjectPresence(any(), any())).thenReturn(false);

        // Test
        Object source = mock(Object.class);
        this.listener.onEvent(new DocumentUpdatedEvent(), source, null);

        // Verify
        verify(this.observationManager, never()).notify(any(), any(), any());
        assertNull(this.answer.getSentEvent());
    }

    @Test
    void onEventWithCorrectValidation() throws Exception
    {
        // Mocks
        UntypedRecordableEventDescriptor descriptor = mockDescriptor();
        when(descriptor.getValidationExpression()).thenReturn("someVelocityCode");
        when(this.scriptContext.getAttribute("xreturn")).thenReturn(true);

        // Test
        Object source = mock(Object.class);
        this.listener.onEvent(new DocumentUpdatedEvent(), source, null);

        // Verify
        verify(this.observationManager).notify(any(DefaultUntypedRecordableEvent.class), any(), any());
        assertNotNull(this.answer.getSentEvent());
        assertEquals("myCustomEvent", this.answer.getSentEvent().getEventType());
    }

    @Test
    void onEventWithIncorrectValidation() throws Exception
    {
        // Mocks
        UntypedRecordableEventDescriptor descriptor = mockDescriptor();
        when(descriptor.getValidationExpression()).thenReturn("someVelocityCode");
        when(this.scriptContext.getAttribute("xreturn")).thenReturn(false);

        // Test
        this.listener.onEvent(new DocumentUpdatedEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager, never()).notify(any(), any(), any());
        assertNull(this.answer.getSentEvent());
    }

    @Test
    void onEventWithCorrectValidation2() throws Exception
    {
        // Mocks
        UntypedRecordableEventDescriptor descriptor = mockDescriptor();
        when(descriptor.getValidationExpression()).thenReturn("someVelocityCode");
        doAnswer(invocationOnMock -> {
            WikiPrinter wikiPrinter = invocationOnMock.getArgument(1);
            wikiPrinter.println("    true ");
            return null;
        }).when(this.renderer).render(any(XDOM.class), any(WikiPrinter.class));

        // Test
        this.listener.onEvent(new DocumentUpdatedEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager).notify(any(DefaultUntypedRecordableEvent.class), any(), any());
        assertNotNull(this.answer.getSentEvent());
        assertEquals("myCustomEvent", this.answer.getSentEvent().getEventType());
    }

    @Test
    void onEventWithIncorrectValidation2() throws Exception
    {
        // Mocks
        UntypedRecordableEventDescriptor descriptor = mockDescriptor();
        when(descriptor.getValidationExpression()).thenReturn("someVelocityCode");

        // Test
        this.listener.onEvent(new DocumentUpdatedEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager, never()).notify(any(), any(), any());
        assertNull(this.answer.getSentEvent());
    }

    @Test
    void onEventWithTarget() throws Exception
    {
        // Mocks
        UntypedRecordableEventDescriptor descriptor = mockDescriptor();
        when(descriptor.getTargetExpression()).thenReturn("someVelocityCode");
        // Target velocity
        when(this.scriptContext.getAttribute("xreturn")).thenReturn(Arrays.asList("UserA", "UserB"));

        // Test
        this.listener.onEvent(new DocumentUpdatedEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager)
            .notify(any(DefaultUntypedRecordableEvent.class), any(), any());
        assertNotNull(this.answer.getSentEvent());
        assertNotNull(this.answer.getSentEvent().getTarget());
        assertTrue(this.answer.getSentEvent().getTarget().contains("UserA"));
        assertTrue(this.answer.getSentEvent().getTarget().contains("UserB"));
        assertEquals(2, this.answer.getSentEvent().getTarget().size());
    }
}
