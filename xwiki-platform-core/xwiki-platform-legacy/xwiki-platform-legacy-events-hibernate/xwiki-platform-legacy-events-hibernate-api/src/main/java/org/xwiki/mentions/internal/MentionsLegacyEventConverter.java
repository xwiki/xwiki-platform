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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.events.MentionEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.store.internal.AbstractLegacyEventConverter;
import org.xwiki.eventstream.store.internal.LegacyEvent;

import static java.util.Collections.singletonMap;

/**
 * We need this custom legacy converter to ensure that the mention notification parameters from the
 * {@link org.xwiki.eventstream.internal.DefaultEvent} are set in the parameter3 of the {@link LegacyEvent}: this is
 * what will be stored.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
@Named(MentionEvent.EVENT_TYPE)
public class MentionsLegacyEventConverter extends AbstractLegacyEventConverter
{
    @Override
    public LegacyEvent convertEventToLegacyActivity(Event e)
    {
        // This code is called once when the event is saved in database
        LegacyEvent event = super.convertEventToLegacyActivity(e);
        if (e.getCustom().containsKey(MentionsRecordableEventConverter.MENTIONS_PARAMETER_KEY)) {
            event.setParam3((String) e.getCustom().get(MentionsRecordableEventConverter.MENTIONS_PARAMETER_KEY));
        }
        return event;
    }

    @Override
    public Event convertLegacyActivityToEvent(LegacyEvent e)
    {
        // this code is called everytime an event is loaded from database
        Event event = super.convertLegacyActivityToEvent(e);
        if (!StringUtils.isEmpty(e.getParam3())) {
            event.setCustom(singletonMap(MentionsRecordableEventConverter.MENTIONS_PARAMETER_KEY, e.getParam3()));
        }
        return event;
    }
}
