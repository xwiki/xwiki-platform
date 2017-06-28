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
package org.xwiki.notifications.internal;

import javax.script.ScriptContext;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationDisplayer;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationDisplayerComponent}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public class NotificationDisplayerComponentTest
{
    private BaseObjectReference objectReference;

    private DocumentReference authorReference;

    private TemplateManager templateManager;

    private ScriptContextManager scriptContextManager;

    private ComponentManager componentManager;

    private NotificationDisplayer defaultNotificationDisplayer;

    private NotificationDisplayerComponent componentUnderTest;

    @Before
    public void setUp() throws Exception
    {
        this.objectReference = mock(BaseObjectReference.class);
        this.authorReference = mock(DocumentReference.class);

        this.templateManager = mock(TemplateManager.class);
        when(this.templateManager.createStringTemplate(any(), any())).thenReturn(mock(Template.class));

        this.scriptContextManager = mock(ScriptContextManager.class);
        ScriptContext scriptContext = mock(ScriptContext.class);
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(scriptContext);

        this.componentManager = mock(ComponentManager.class);
        this.defaultNotificationDisplayer = mock(DefaultNotificationDisplayer.class);
        when(this.componentManager.getInstance(NotificationDisplayer.class)).thenReturn(defaultNotificationDisplayer);
    }

    private void instantiateComponent(BaseObject baseObject) throws Exception
    {
        componentUnderTest = new NotificationDisplayerComponent(objectReference, authorReference, templateManager,
                scriptContextManager, componentManager, baseObject);
    }

    private BaseObject mockBaseObject(String notificationTemplate, String eventType) throws Exception
    {
        BaseObject baseObject = mock(BaseObject.class);
        when(baseObject.getStringValue(NotificationDisplayerDocumentInitializer.NOTIFICATION_TEMPLATE))
                .thenReturn(notificationTemplate);
        when(baseObject.getStringValue(NotificationDisplayerDocumentInitializer.EVENT_TYPE))
                .thenReturn(eventType);
        return baseObject;
    }

    @Test
    public void parameters() throws Exception
    {
        this.instantiateComponent(this.mockBaseObject("Notification Template", "Event Type"));

        assertEquals("Event Type", componentUnderTest.getSupportedEvents().get(0));
        assertEquals(this.authorReference, componentUnderTest.getAuthorReference());
        assertEquals(NotificationDisplayer.class, componentUnderTest.getRoleType());
        assertEquals("Event Type", componentUnderTest.getRoleHint());
        assertEquals(WikiComponentScope.WIKI, componentUnderTest.getScope());
        assertEquals(this.objectReference, componentUnderTest.getEntityReference());
    }

    @Test
    public void renderNotificationWithBlankTemplate() throws Exception
    {
        this.instantiateComponent(this.mockBaseObject("", "Event Type"));

        CompositeEvent compositeEvent = mock(CompositeEvent.class);
        this.componentUnderTest.renderNotification(compositeEvent);

        verify(defaultNotificationDisplayer, times(1)).renderNotification(compositeEvent);
    }

    @Test
    public void renderNotificationWithTemplate() throws Exception
    {
        this.instantiateComponent(this.mockBaseObject("Some Template", "Event Type"));

        CompositeEvent compositeEvent = mock(CompositeEvent.class);
        this.componentUnderTest.renderNotification(compositeEvent);

        verify(templateManager, times(1)).getXDOM(any(Template.class));
    }
}
