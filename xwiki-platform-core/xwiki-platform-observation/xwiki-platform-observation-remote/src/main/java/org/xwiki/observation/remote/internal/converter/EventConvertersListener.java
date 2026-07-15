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
package org.xwiki.observation.remote.internal.converter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.event.ComponentDescriptorAddedEvent;
import org.xwiki.component.event.ComponentDescriptorEvent;
import org.xwiki.component.event.ComponentDescriptorRemovedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.converter.LocalEventConverter;
import org.xwiki.observation.remote.converter.RemoteEventConverter;

/**
 * Invalidate the cache of the event converters when components are added, updated or removed.
 * 
 * @version $Id$
 * @since 18.3.0RC1
 * @since 17.10.8
 */
@Component
@Singleton
@Named(EventConvertersListener.NAME)
public class EventConvertersListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.observation.remote.internal.converter.EventConvertersListener";

    @Inject
    private EventConverters eventConverters;

    /**
     * Default constructor.
     */
    public EventConvertersListener()
    {
        super(NAME, new ComponentDescriptorAddedEvent(), new ComponentDescriptorRemovedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        ComponentDescriptorEvent componentDescriptorEvent = (ComponentDescriptorEvent) event;

        if (componentDescriptorEvent.getRoleType() == LocalEventConverter.class) {
            this.eventConverters.resetLocalEventConverters();
        } else if (componentDescriptorEvent.getRoleType() == RemoteEventConverter.class) {
            this.eventConverters.resetRemoteEventConverters();
        }
    }
}
