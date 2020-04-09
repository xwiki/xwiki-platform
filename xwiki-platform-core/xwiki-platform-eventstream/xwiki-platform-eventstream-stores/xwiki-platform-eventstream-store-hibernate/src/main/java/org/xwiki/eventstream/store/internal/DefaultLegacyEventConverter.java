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
package org.xwiki.eventstream.store.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;

/**
 * Default implementation of {@link LegacyEventConverter}.
 * By default this converter looks for a component {@link LegacyEventConverter} with a hint corresponding to the type of
 * the event to apply the conversion.
 * If none is found, it applies default behaviour from {@link AbstractLegacyEventConverter}.
 *
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Singleton
public class DefaultLegacyEventConverter extends AbstractLegacyEventConverter
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    private LegacyEventConverter getConverterForType(String type)
    {
        // check that the type is not empty or default to avoid stackoverflow
        if (!StringUtils.isEmpty(type) && !"default".equals(type)
            && this.componentManager.hasComponent(LegacyEventConverter.class, type)) {
            try {
                return this.componentManager.getInstance(LegacyEventConverter.class, type);
            } catch (ComponentLookupException e) {
                logger.error("Error while initializing LegacyEventConverter with hint [{}]. "
                    + "Fallback on default converter.", type, e);
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public LegacyEvent convertEventToLegacyActivity(Event e)
    {
        LegacyEventConverter converter = this.getConverterForType(e.getType());
        LegacyEvent result;
        if (converter == null) {
            result = super.convertEventToLegacyActivity(e);
        } else {
            result = converter.convertEventToLegacyActivity(e);
        }
        return result;
    }

    @Override
    public Event convertLegacyActivityToEvent(LegacyEvent e)
    {
        LegacyEventConverter converter = this.getConverterForType(e.getType());
        Event result;
        if (converter == null) {
            result = super.convertLegacyActivityToEvent(e);
        } else {
            result = converter.convertLegacyActivityToEvent(e);
        }
        return result;
    }

    @Override
    public LegacyEventStatus convertEventStatusToLegacyActivityStatus(EventStatus eventStatus)
    {
        LegacyEventStatus result = null;
        if (eventStatus.getEvent() != null) {
            LegacyEventConverter converter = this.getConverterForType(eventStatus.getEvent().getType());
            if (converter != null) {
                result = converter.convertEventStatusToLegacyActivityStatus(eventStatus);
            }
        }

        if (result == null) {
            result = super.convertEventStatusToLegacyActivityStatus(eventStatus);
        }
        return result;
    }

    @Override
    public EventStatus convertLegacyActivityStatusToEventStatus(LegacyEventStatus eventStatus)
    {
        EventStatus result = null;
        if (eventStatus.getActivityEvent() != null) {
            LegacyEventConverter converter = this.getConverterForType(eventStatus.getActivityEvent().getType());
            if (converter != null) {
                result = converter.convertLegacyActivityStatusToEventStatus(eventStatus);
            }
        }

        if (result == null) {
            result = super.convertLegacyActivityStatusToEventStatus(eventStatus);
        }
        return result;
    }
}
