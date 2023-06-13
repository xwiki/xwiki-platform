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
package org.xwiki.extension.security.notifications;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.security.ExtensionSecurityEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

/**
 * @version $Id$
 * @since x.y.z
 */
@Component
@Singleton
@Named(ExtensionSecurityListener.ID)
public class ExtensionSecurityListener implements EventListener
{
    /**
     * The hint and name of this listener.
     */
    public static final String ID = "ExtensionSecurityListener";

    @Inject
    private ObservationManager observationManager;

    @Override
    public String getName()
    {
        return ID;
    }

    @Override
    public List<Event> getEvents()
    {
        return List.of(new ExtensionSecurityEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        this.observationManager.notify(new NewSecurityIssueEvent(), this, data);
    }
}
