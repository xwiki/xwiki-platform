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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.UntypedRecordableEvent;

/**
 * This event converter is used to properly store a {@link UntypedRecordableEvent} in database.
 * It overrides {@link DefaultRecordableEventConverter}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
@Component
@Singleton
@Named(DefaultUntypedRecordableEventConverter.NAME)
public class DefaultUntypedRecordableEventConverter extends DefaultRecordableEventConverter
{
    /**
     * Name of the event converter.
     */
    public static final String NAME = "Untyped Recordable Event Converter";

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        Event convertedEvent = super.convert(recordableEvent, source, data);
        convertedEvent.setType(((UntypedRecordableEvent) recordableEvent).getEventType());
        return convertedEvent;
    }

    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return Arrays.asList(new DefaultUntypedRecordableEvent());
    }
}
