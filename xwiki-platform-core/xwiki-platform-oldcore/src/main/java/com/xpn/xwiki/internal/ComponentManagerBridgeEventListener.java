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

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.EventListener;
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
// We need this listener to be initialized very because it's responsible for making the ComponentManager available for
// non component code (not setting 0 just in case something would really like to be the absolute first listener)
@Priority(10)
public class ComponentManagerBridgeEventListener extends AbstractEventListener implements EventListener, Initializable
{
    /**
     * Provided to {@link Utils#setComponentManager(ComponentManager)}.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The default constructor.
     */
    public ComponentManagerBridgeEventListener()
    {
        super("Component Manager Bridge Listener");
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Inject the component manager as soon as possible
        Utils.setComponentManager(this.componentManager);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // The work is done in the listener initialization
    }
}
