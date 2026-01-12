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
package org.xwiki.tool.security.extension.internal;

import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

/**
 * "Disable" the default Observation Manager to prevent initializing and calling various listener which are not needed
 * in this context.
 * 
 * @version $Id$
 * @since 18.0.0RC1
 * @since 17.10.3
 */
@Component
@Singleton
public class VoidObservationManager implements ObservationManager
{
    @Override
    public void addListener(EventListener eventListener)
    {
        // Do nothing
    }

    @Override
    public void removeListener(String listenerName)
    {
        // Do nothing
    }

    @Override
    public void addEvent(String listenerName, Event event)
    {
        // Do nothing
    }

    @Override
    public void removeEvent(String listenerName, Event event)
    {
        // Do nothing
    }

    @Override
    public EventListener getListener(String listenerName)
    {
        return null;
    }

    @Override
    public void notify(Event event, Object source, Object data)
    {
        // Do nothing
    }

    @Override
    public void notify(Event event, Object source)
    {
        // Do nothing
    }
}
