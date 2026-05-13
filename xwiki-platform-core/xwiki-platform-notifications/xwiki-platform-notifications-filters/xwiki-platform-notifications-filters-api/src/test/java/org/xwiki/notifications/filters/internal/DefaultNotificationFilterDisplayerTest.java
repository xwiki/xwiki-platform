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

import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNotificationFilterDisplayer}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@ComponentTest
class DefaultNotificationFilterDisplayerTest
{
    @InjectMockComponents
    private DefaultNotificationFilterDisplayer defaultNotificationFilterDisplayer;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @BeforeEach
    void setUp()
    {
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(mock(ScriptContext.class));
    }

    @Test
    void displayWithCustomTemplate() throws Exception
    {
        Template fakeTemplate = mock(Template.class);
        when(this.templateManager.getTemplate(any(String.class))).thenReturn(fakeTemplate);

        NotificationFilter filter = mock(NotificationFilter.class);
        when(filter.getName()).thenReturn("filterName");

        this.defaultNotificationFilterDisplayer.display(filter, mock(NotificationFilterPreference.class));

        verify(this.templateManager).execute(eq(fakeTemplate));
    }

    @Test
    void displayWithDefaultTemplate() throws Exception
    {
        when(this.templateManager.getTemplate(any(String.class))).thenReturn(null);

        NotificationFilter filter = mock(NotificationFilter.class);
        when(filter.getName()).thenReturn("filterName");

        this.defaultNotificationFilterDisplayer.display(filter, mock(NotificationFilterPreference.class));

        verify(this.templateManager).execute(eq("notification/filters/default.vm"));
    }
}
