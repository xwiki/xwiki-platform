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

import javax.script.ScriptContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
public class UntypedEventListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<UntypedEventListener> mocker =
            new MockitoComponentMockingRule<>(UntypedEventListener.class);

    /**
     * Inner classed used to verify what is sent to the observation manager
     */
    class MyAnswer implements Answer
    {
        private DefaultUntypedRecordableEvent sentEvent;

        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable
        {
            sentEvent = invocationOnMock.getArgument(0);
            return null;
        }

        public DefaultUntypedRecordableEvent getSentEvent()
        {
            return sentEvent;
        }
    }

    private MyAnswer answer;

    private ObservationManager observationManager;

    private ScriptContextManager scriptContextManager;

    private ComponentManager componentManager;

    private ModelBridge modelBridge;

    private TemplateManager templateManager;

    private Template template;

    private XDOM xdom;

    private ScriptContext scriptContext;

    private BlockRenderer renderer;

    @Before
    public void setUp() throws Exception
    {
        componentManager = mocker.registerMockComponent(ComponentManager.class, "context");
        modelBridge = mocker.registerMockComponent(ModelBridge.class);
        renderer = mocker.getInstance(BlockRenderer.class, "html/5.0");

        scriptContextManager = mocker.registerMockComponent(ScriptContextManager.class);
        scriptContext = mock(ScriptContext.class);
        when(scriptContextManager.getCurrentScriptContext()).thenReturn(scriptContext);

        templateManager = mocker.getInstance(TemplateManager.class);
        template = mock(Template.class);
        xdom = mock(XDOM.class);
        when(templateManager.createStringTemplate(anyString(), any(), any(), any())).thenReturn(template);
        when(templateManager.execute(template)).thenReturn(xdom);
    }

    @Before
    public void watchObservationManager() throws Exception
    {
        observationManager = mocker.registerMockComponent(ObservationManager.class);
        answer = new MyAnswer();
        doAnswer(answer).when(observationManager).notify(any(DefaultUntypedRecordableEvent.class), anyString(), any());
    }

    private UntypedRecordableEventDescriptor mockDescriptor() throws Exception
    {
        UntypedRecordableEventDescriptor descriptor = mock(UntypedRecordableEventDescriptor.class);
        when(descriptor.getEventTriggers()).thenReturn(Arrays.asList(DocumentUpdatedEvent.class.getCanonicalName()));
        when(descriptor.getEventType()).thenReturn("myCustomEvent");
        when(modelBridge.checkXObjectPresence(any(), any())).thenReturn(true);
        when(componentManager.getInstanceList(any())).thenReturn(Arrays.asList(descriptor));

        return descriptor;
    }

    @Test
    public void onEventWithBlankValidationAndBlankTarget() throws Exception
    {
        // Mocks
        mockDescriptor();

        // Test
        mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager, times(1)).notify(any(DefaultUntypedRecordableEvent.class), any(), any());

        assertNotNull(answer.getSentEvent());
        assertTrue(answer.getSentEvent().getTarget().isEmpty());
        assertEquals("myCustomEvent", answer.getSentEvent().getEventType());
    }

    @Test
    public void onEventWithWrongEvent() throws Exception
    {
        // Mocks
        mockDescriptor();

        // Test
        mocker.getComponentUnderTest().onEvent(new ApplicationReadyEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager, never()).notify(any(), any(), any());
        assertNull(answer.getSentEvent());
    }

    @Test
    public void onEventWithoutXObject() throws Exception
    {
        // Mocks
        mockDescriptor();

        // Same than onEventWithBlankValidationAndBlankTarget() but with no XObject
        when(modelBridge.checkXObjectPresence(any(), any())).thenReturn(false);

        // Test
        Object source = mock(Object.class);
        mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(), source, null);

        // Verify
        verify(this.observationManager, never()).notify(any(), any(), any());
        assertNull(answer.getSentEvent());
    }

    @Test
    public void onEventWithCorrectValidation() throws Exception
    {
        // Mocks
        UntypedRecordableEventDescriptor descriptor = mockDescriptor();
        when(descriptor.getValidationExpression()).thenReturn("someVelocityCode");
        when(scriptContext.getAttribute("xreturn")).thenReturn(true);

        // Test
        Object source = mock(Object.class);
        mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(), source, null);

        // Verify
        verify(this.observationManager, times(1)).notify(any(DefaultUntypedRecordableEvent.class), any(), any());
        assertNotNull(answer.getSentEvent());
        assertEquals("myCustomEvent", answer.getSentEvent().getEventType());
    }

    @Test
    public void onEventWithIncorrectValidation() throws Exception
    {
        // Mocks
        UntypedRecordableEventDescriptor descriptor = mockDescriptor();
        when(descriptor.getValidationExpression()).thenReturn("someVelocityCode");
        when(scriptContext.getAttribute("xreturn")).thenReturn(false);

        // Test
        mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager, never()).notify(any(), any(), any());
        assertNull(answer.getSentEvent());
    }

    @Test
    public void onEventWithCorrectValidation2() throws Exception
    {
        // Mocks
        UntypedRecordableEventDescriptor descriptor = mockDescriptor();
        when(descriptor.getValidationExpression()).thenReturn("someVelocityCode");
        doAnswer(invocationOnMock -> {
            WikiPrinter wikiPrinter = invocationOnMock.getArgument(1);
            wikiPrinter.println("    true ");
            return null;
        }).when(renderer).render(any(XDOM.class), any(WikiPrinter.class));

        // Test
        mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager, times(1)).notify(any(DefaultUntypedRecordableEvent.class), any(), any());
        assertNotNull(answer.getSentEvent());
        assertEquals("myCustomEvent", answer.getSentEvent().getEventType());
    }

    @Test
    public void onEventWithIncorrectValidation2() throws Exception
    {
        // Mocks
        UntypedRecordableEventDescriptor descriptor = mockDescriptor();
        when(descriptor.getValidationExpression()).thenReturn("someVelocityCode");

        // Test
        mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager, never()).notify(any(), any(), any());
        assertNull(answer.getSentEvent());
    }

    @Test
    public void onEventWithTarget() throws Exception
    {
        // Mocks
        UntypedRecordableEventDescriptor descriptor = mockDescriptor();
        when(descriptor.getTargetExpression()).thenReturn("someVelocityCode");
        // Target velocity
        when(scriptContext.getAttribute("xreturn")).thenReturn(Arrays.asList("UserA", "UserB"));

        // Test
        mocker.getComponentUnderTest().onEvent(new DocumentUpdatedEvent(), mock(Object.class), null);

        // Verify
        verify(this.observationManager, times(1)).notify(
                any(DefaultUntypedRecordableEvent.class), any(), any());
        assertNotNull(answer.getSentEvent());
        assertNotNull(answer.getSentEvent().getTarget());
        assertTrue(answer.getSentEvent().getTarget().contains("UserA"));
        assertTrue(answer.getSentEvent().getTarget().contains("UserB"));
        assertEquals(2, answer.getSentEvent().getTarget().size());
    }
}
