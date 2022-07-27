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
package org.xwiki.container.servlet.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.container.servlet.events.SessionCreatedEvent;
import org.xwiki.container.servlet.events.SessionDestroyedEvent;
import org.xwiki.observation.ObservationManager;

/**
 * Manager in charge of keeping track of the {@link HttpSession}.
 * One of the role of this component is to properly call {@link HttpSession#invalidate()} on all sessions before
 * disposal of the component: this ensures that all listeners relying on the session disposal can be executed.
 *
 * @version $Id$
 * @since 14.5
 * @since 14.4.1
 */
@Component(roles = HttpSessionManager.class)
@Singleton
public class HttpSessionManager implements Initializable, Disposable, HttpSessionListener
{
    @Inject
    private ObservationManager observationManager;

    private ConcurrentLinkedQueue<HttpSession> sessionsList;

    @Override
    public void initialize() throws InitializationException
    {
        this.sessionsList = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        for (HttpSession httpSession : this.sessionsList) {
            if (!httpSession.isNew()) {
                httpSession.invalidate();
            }
        }
    }

    @Override
    public void sessionCreated(HttpSessionEvent se)
    {
        HttpSession session = se.getSession();
        this.sessionsList.add(session);
        this.observationManager.notify(new SessionCreatedEvent(), session, null);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se)
    {
        HttpSession session = se.getSession();
        this.sessionsList.remove(session);
        this.observationManager.notify(new SessionDestroyedEvent(), session, null);
    }

    /**
     * @return the list of created sessions not yet destroyed.
     */
    protected List<HttpSession> getSessionList()
    {
        return new ArrayList<>(this.sessionsList);
    }
}
