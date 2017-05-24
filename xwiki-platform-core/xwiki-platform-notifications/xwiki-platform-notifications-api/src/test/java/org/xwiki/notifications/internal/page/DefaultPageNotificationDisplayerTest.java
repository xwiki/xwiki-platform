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
package org.xwiki.notifications.internal.page;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.text.StringUtils;
import org.xwiki.velocity.VelocityManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class DefaultPageNotificationDisplayerTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultPageNotificationDisplayer> mocker =
            new MockitoComponentMockingRule<>(DefaultPageNotificationDisplayer.class);

    private PageNotificationEventDescriptorManager pageNotificationEventDescriptorManager;

    private TemplateManager templateManager;

    private VelocityManager velocityManager;

    private VelocityContext velocityContext;

    private CompositeEvent eventNotification;

    @Before
    public void setUp() throws Exception
    {
        this.pageNotificationEventDescriptorManager =
                mocker.registerMockComponent(PageNotificationEventDescriptorManager.class);

        this.templateManager = mocker.registerMockComponent(TemplateManager.class);
        this.mockTemplateManager();

        this.velocityManager = mocker.registerMockComponent(VelocityManager.class);
        this.velocityContext = mock(VelocityContext.class);
        when(this.velocityManager.getCurrentVelocityContext()).thenReturn(this.velocityContext);

        this.eventNotification = mock(CompositeEvent.class);
    }

    /**
     * Mock the test template manager in order to add default comportments, used later in the tests.
     *
     * @throws Exception
     */
    private void mockTemplateManager() throws Exception
    {
        XDOM defaultReturnElement = mock(XDOM.class);
        when(defaultReturnElement.toString()).thenReturn("Default XDOM");

        XDOM customTemplateReturnElement = mock(XDOM.class);
        when(customTemplateReturnElement.toString()).thenReturn("Custom Template XDOM");

        Template template = mock(Template.class);

        when(templateManager.getTemplate("notification/custom.vm")).thenReturn(template);
        when(templateManager.executeNoException("notification/default.vm")).thenReturn(defaultReturnElement);
        when(templateManager.executeNoException(template)).thenReturn(customTemplateReturnElement);
    }

    private void setUpEventNotificationMock(String typeName)
    {
        Event event1 = mock(Event.class);
        when(event1.getType()).thenReturn(typeName);

        when(this.eventNotification.getEvents()).thenReturn(Arrays.asList(event1));
    }

    private PageNotificationEventDescriptor setUpEventDescriptorMock(String eventName, String eventTemplate)
    {
        PageNotificationEventDescriptor eventDescriptor = mock(PageNotificationEventDescriptor.class);
        when(eventDescriptor.getNotificationTemplate()).thenReturn(eventTemplate);
        when(eventDescriptor.getEventName()).thenReturn(eventName);

        return eventDescriptor;
    }

    @Test
    public void getSupportedEventsWithNoSupportedEvents() throws Exception
    {
        when(pageNotificationEventDescriptorManager.getDescriptorList()).thenReturn(new ArrayList<>());
        assertTrue(mocker.getComponentUnderTest().getSupportedEvents().isEmpty());
    }

    @Test
    public void getSupportEventsWithMultipleEvents() throws Exception
    {
        PageNotificationEventDescriptor eventDescriptor1 = mock(PageNotificationEventDescriptor.class);
        when(eventDescriptor1.getEventType()).thenReturn("eventType1");
        PageNotificationEventDescriptor eventDescriptor2 = mock(PageNotificationEventDescriptor.class);
        when(eventDescriptor2.getEventType()).thenReturn("eventType2");

        when(pageNotificationEventDescriptorManager.getDescriptorList())
                .thenReturn(Arrays.asList(eventDescriptor1, eventDescriptor2));

        List<String> result = mocker.getComponentUnderTest().getSupportedEvents();
        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList("eventType1", "eventType2")));
    }

    @Test
    public void renderNotificationWithoutTemplate() throws Exception
    {
        setUpEventNotificationMock("eventNotificationType");

        PageNotificationEventDescriptor eventDescriptor1 =
                setUpEventDescriptorMock("eventDescriptorName", StringUtils.EMPTY);

        when(this.pageNotificationEventDescriptorManager.getDescriptorByType("eventNotificationType"))
                .thenReturn(eventDescriptor1);

        assertEquals("Default XDOM",
                mocker.getComponentUnderTest().renderNotification(this.eventNotification)
                        .toString());
    }

    @Test
    public void renderNotificationWithCustomEventName() throws Exception
    {
        setUpEventNotificationMock("customType");

        PageNotificationEventDescriptor eventDescriptor1 =
                setUpEventDescriptorMock("custom", StringUtils.EMPTY);

        when(this.pageNotificationEventDescriptorManager.getDescriptorByType("customType"))
                .thenReturn(eventDescriptor1);

        assertEquals("Custom Template XDOM",
                mocker.getComponentUnderTest().renderNotification(this.eventNotification).toString());
    }
}
