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

package org.xwiki.extension.handler.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.handler.ExtensionInitializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

/**
 * An application started listener to initialize extensions.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("ExtensionApplicationStartedListener")
public class ExtensionApplicationStartedListener implements EventListener
{
    /**
     * The list of events observed.
     */
    private static final List<Event> EVENTS = Collections.<Event> singletonList(new ApplicationStartedEvent());

    /**
     * The extension initializer.
     */
    @Inject
    private ExtensionInitializer extensionInitializer;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return "ExtensionApplicationStartedListener";
    }

    @Override
    public void onEvent(Event arg0, Object arg1, Object arg2)
    {
        extensionInitializer.initialize();
    }
}
