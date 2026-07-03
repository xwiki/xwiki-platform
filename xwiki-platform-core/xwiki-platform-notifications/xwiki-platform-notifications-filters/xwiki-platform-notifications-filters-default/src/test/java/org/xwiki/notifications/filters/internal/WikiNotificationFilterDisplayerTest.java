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

import javax.script.ScriptContext;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterDisplayer;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.BaseProperty;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link WikiNotificationFilterDisplayer}.
 *
 * @version $Id$
 */
@ComponentTest
class WikiNotificationFilterDisplayerTest
{
    private static final String FILTER = "filter";

    private static final String FILTER_PREFERENCE = "filterPreference";

    @InjectMockComponents
    private WikiNotificationFilterDisplayer wikiNotificationFilterDisplayer;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private NotificationFilterDisplayer notificationFilterDisplayer;

    @Test
    void initializeAndDisplay() throws Exception
    {
        // initialize part
        DocumentReference authorReference = new DocumentReference("fooWiki", "XWiki", "Foo");
        BaseObject baseObject = mock(BaseObject.class);
        BaseObjectReference baseObjectReference = mock(BaseObjectReference.class);
        when(baseObject.getReference()).thenReturn(baseObjectReference);

        when(baseObject.getListValue(WikiNotificationFilterDisplayerDocumentInitializer.SUPPORTED_FILTERS))
            .thenReturn(List.of("filter1", "filter2", "filter3"));

        BaseProperty baseProperty = mock(BaseProperty.class);
        when(baseObject.get(WikiNotificationFilterDisplayerDocumentInitializer.FILTER_TEMPLATE))
            .thenReturn(baseProperty);
        when(baseProperty.getValue()).thenReturn("template1");

        EntityReference propertyReference = mock(EntityReference.class);
        when(baseProperty.getReference()).thenReturn(propertyReference);
        when(serializer.serialize(propertyReference)).thenReturn("propertyReference");
        DocumentReference baseObjectDocRef = new DocumentReference("otherWiki", "Space", "Page");
        when(baseObject.getDocumentReference()).thenReturn(baseObjectDocRef);

        Template template = mock(Template.class);
        when(templateManager.createStringTemplate("propertyReference", "template1", authorReference,
            baseObjectDocRef)).thenReturn(template);

        wikiNotificationFilterDisplayer.initialize(authorReference, baseObject);
        verify(templateManager).createStringTemplate("propertyReference", "template1", authorReference,
            baseObjectDocRef);

        // display part
        NotificationFilter filter = mock(NotificationFilter.class);
        NotificationFilterPreference filterPreference = mock(NotificationFilterPreference.class);
        XDOM xdom = mock(XDOM.class);
        when(templateManager.execute(template)).thenReturn(xdom);

        // setUpContext
        ScriptContext scriptContext = mock(ScriptContext.class);
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(scriptContext);
        when(scriptContext.getAttribute(FILTER)).thenReturn("oldFilter");
        when(scriptContext.getAttribute(FILTER_PREFERENCE)).thenReturn("oldFilterPref");


        assertEquals(xdom, wikiNotificationFilterDisplayer.display(filter, filterPreference));
        verify(templateManager).execute(template);
        verify(this.scriptContextManager, times(2)).getCurrentScriptContext();
        verify(scriptContext).setAttribute(FILTER, filter, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute(FILTER_PREFERENCE, filterPreference, ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute(FILTER, "oldFilter", ScriptContext.ENGINE_SCOPE);
        verify(scriptContext).setAttribute(FILTER_PREFERENCE, "oldFilterPref", ScriptContext.ENGINE_SCOPE);
    }
}