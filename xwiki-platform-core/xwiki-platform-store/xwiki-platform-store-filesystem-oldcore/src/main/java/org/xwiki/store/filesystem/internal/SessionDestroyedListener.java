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
package org.xwiki.store.filesystem.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.servlet.events.SessionDestroyedEvent;
import org.xwiki.observation.event.AbstractLocalEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.store.TemporaryAttachmentSessionsManager;

/**
 * Listener in charge of cleaning the {@link TemporaryAttachmentSessionsManager} whenever a session is destroyed.
 *
 * @version $Id$
 * @since 14.5RC1
 * @since 14.4.1
 */
@Component
@Singleton
@Named(SessionDestroyedListener.NAME)
public class SessionDestroyedListener extends AbstractLocalEventListener
{
    static final String NAME = "org.xwiki.store.filesystem.internal.SessionDestroyedListener";

    private static final List<Event> EVENT_LIST = Collections.singletonList(new SessionDestroyedEvent());

    @Inject
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    /**
     * Default constructor.
     */
    public SessionDestroyedListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void processLocalEvent(Event event, Object source, Object data)
    {
        this.temporaryAttachmentSessionsManager.removeUploadedAttachments((String) source);
    }
}
