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
package org.xwiki.notifications.notifiers.internal;

import java.util.Collections;
import java.util.Date;

import javax.script.ScriptContext;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@ink DefaultNotificationDisplayer}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultNotificationDisplayerTest
{
    @InjectMockComponents
    private DefaultNotificationDisplayer displayer;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private TemplateManager templateManager;

    @Test
    void renderNotificationWhenSpecificTemplateExists() throws Exception
    {
        ScriptContext scriptContext = mock(ScriptContext.class);
        when(this.scriptContextManager.getScriptContext()).thenReturn(scriptContext);

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("some/type");

        Template template = mock(Template.class);
        when(this.templateManager.getTemplate("notification/some.type.vm")).thenReturn(template);

        when(this.templateManager.execute(template)).thenReturn(new XDOM(Collections.emptyList()));

        CompositeEvent compositeEvent = new CompositeEvent(event);
        Block block = this.displayer.renderNotification(compositeEvent);
        assertNotNull(block);
    }

    @Test
    void renderNotificationWhenSpecificTemplateDoesntExists() throws Exception
    {
        ScriptContext scriptContext = mock(ScriptContext.class);
        when(this.scriptContextManager.getScriptContext()).thenReturn(scriptContext);

        Event event = mock(Event.class);
        when(event.getType()).thenReturn("some/type");

        when(this.templateManager.getTemplate("notification/some.type.vm")).thenReturn(null);
        when(this.templateManager.execute("notification/default.vm")).thenReturn(new XDOM(Collections.emptyList()));

        CompositeEvent compositeEvent = new CompositeEvent(event);
        Block block = this.displayer.renderNotification(compositeEvent);
        assertNotNull(block);
    }

    @Test
    void renderNotificationWhenError() throws Exception
    {
        ScriptContext scriptContext = mock(ScriptContext.class);
        when(this.scriptContextManager.getScriptContext()).thenReturn(scriptContext);

        Event event1 = new DefaultEvent();
        event1.setType("type1");
        event1.setId("id1");
        event1.setDate(new Date());

        Event event2 = new DefaultEvent();
        event2.setType("type2");
        event2.setId("id2");
        // Make sure that the second event comes later than the first one so that the order is deterministic
        event2.setDate(DateUtils.addMinutes(new Date(), 5));

        when(this.templateManager.execute("notification/default.vm")).thenThrow(new Exception("error"));

        CompositeEvent compositeEvent = new CompositeEvent(event1);
        compositeEvent.add(event2, 0);

        Throwable exception = assertThrows(NotificationException.class, () -> {
            this.displayer.renderNotification(compositeEvent);
        });
        assertEquals("Failed to render the notification for events [id2,id1]", exception.getMessage());
    }
}
