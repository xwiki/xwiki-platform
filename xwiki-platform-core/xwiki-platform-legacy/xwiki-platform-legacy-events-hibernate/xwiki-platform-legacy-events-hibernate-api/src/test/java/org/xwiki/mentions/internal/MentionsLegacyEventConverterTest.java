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
package org.xwiki.mentions.internal;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.store.internal.LegacyEvent;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test of {@link MentionsLegacyEventConverter}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@ComponentTest
public class MentionsLegacyEventConverterTest
{
    @InjectMockComponents
    private MentionsLegacyEventConverter activityPubLegacyEventConverter;

    @MockComponent
    private EventFactory eventFactory;

    @MockComponent
    private EntityReferenceResolver<String> resolver;

    /**
     * Test {@link MentionsLegacyEventConverter#convertEventToLegacyActivity(Event)} when 
     * no {@link MentionsRecordableEventConverter#MENTIONS_PARAMETER_KEY} is defined in the converted 
     * {@link DefaultEvent}.
     */
    @Test
    void convertEventToLegacyActivityWithoutActivityParameterKey()
    {
        DefaultEvent e = new DefaultEvent();
        LegacyEvent actual = this.activityPubLegacyEventConverter.convertEventToLegacyActivity(e);
        assertEquals("", actual.getParam3());
    }

    /**
     * Test {@link MentionsLegacyEventConverter#convertEventToLegacyActivity(Event)} when 
     * an {@link MentionsRecordableEventConverter#MENTIONS_PARAMETER_KEY} is defined in the converted 
     * {@link DefaultEvent}.
     */
    @Test
    void convertEventToLegacyActivityWithActivityParameterKey()
    {
        DefaultEvent e = new DefaultEvent();
        Map<String, String> parameters = new HashMap<>();
        parameters.put(MentionsRecordableEventConverter.MENTIONS_PARAMETER_KEY, "randomValue");
        e.setParameters(parameters);
        LegacyEvent actual = this.activityPubLegacyEventConverter.convertEventToLegacyActivity(e);
        assertEquals("randomValue", actual.getParam3());
    }

    /**
     * Test {@link MentionsLegacyEventConverter#convertLegacyActivityToEvent(LegacyEvent)}.
     * When the third param of the {@link LegacyEvent} is not set, the
     * {@link MentionsRecordableEventConverter#MENTIONS_PARAMETER_KEY} param of the returned  {@link DefaultEvent}
     * must be null.
     */
    @Test
    void convertLegacyActivityToEventWithoutActivityParameterKey()
    {
        this.mockCreateRawEvent();
        this.mockEntityReferenceResolve();

        LegacyEvent e = new LegacyEvent();
        Event actual = this.activityPubLegacyEventConverter.convertLegacyActivityToEvent(e);
        assertFalse(actual.getParameters().containsKey(MentionsRecordableEventConverter.MENTIONS_PARAMETER_KEY));
    }

    /**
     * Test {@link MentionsLegacyEventConverter#convertLegacyActivityToEvent(LegacyEvent)}.
     * The third param of the {@link LegacyEvent} must be set in the
     * {@link MentionsRecordableEventConverter#MENTIONS_PARAMETER_KEY} of the returned  {@link DefaultEvent}.
     */
    @Test
    void convertLegacyActivityToEventWithActivityParameterKey()
    {
        this.mockCreateRawEvent();
        this.mockEntityReferenceResolve();
        LegacyEvent e = new LegacyEvent();
        e.setParam3("randomValue2");
        Event actual = this.activityPubLegacyEventConverter.convertLegacyActivityToEvent(e);
        assertEquals("randomValue2", actual.getParameters().get(MentionsRecordableEventConverter.MENTIONS_PARAMETER_KEY));
    }

    /**
     * Mock {@link EventFactory#createRawEvent()} and return a {@link DefaultEvent}.
     */
    private void mockCreateRawEvent()
    {
        when(this.eventFactory.createRawEvent()).thenReturn(new DefaultEvent());
    }

    /**
     * Mocl {@link EntityReferenceResolver#resolve(Object, EntityType, Object...)} and returns a minimal
     * {@link EntityReference}.
     */
    private void mockEntityReferenceResolve()
    {
        EntityReference parent = new SpaceReference("parentTest", "spaceTest1");
        EntityReference test = new EntityReference("test", EntityType.DOCUMENT, parent);
        when(this.resolver.resolve(any(), any())).thenReturn(test);
    }
}