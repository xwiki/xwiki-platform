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
package com.xpn.xwiki.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.web.Utils;

/**
 * This is a temporary bridge to allow non components to call Utils.getComponent() and get a component instance without
 * having to pass around a XWiki Context (in order to retrieve the Servlet Context to get the component manager from an
 * attribute).
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
@Singleton
@Named("componentManagerBridge")
public class ComponentManagerBridgeEventListener implements EventListener
{
    /**
     * Provided to {@link Utils#setComponentManager(ComponentManager)}.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see EventListener#getEvents()
     */
    public List<Event> getEvents()
    {
        return Collections.singletonList((Event) new ApplicationStartedEvent());
    }

    /**
     * {@inheritDoc}
     * 
     * @see EventListener#getName()
     */
    public String getName()
    {
        return "Component Manager Bridge Listener";
    }

    /**
     * {@inheritDoc}
     * 
     * @see EventListener#onEvent(Event, Object, Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        Utils.setComponentManager(this.componentManager);
    }
}
