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
package org.xwiki.netflux.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.netflux.EntityChannel;
import org.xwiki.netflux.internal.event.EntityChannelScriptAuthorChangeEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

/**
 * Update the script author for a specific {@link EntityChannel}.
 * 
 * @version $Id$
 * @since 17.10.0RC1
 */
@Component
@Named(EntityChannelScriptAuthorListener.NAME)
@Singleton
public class EntityChannelScriptAuthorListener extends AbstractEventListener
{
    /**
     * The name of this event listener (and its component hint at the same time).
     */
    public static final String NAME = "org.xwiki.netflux.internal.EntityChannelScriptAuthorListener";

    @Inject
    private EntityChannelScriptAuthorTracker tracker;

    @Inject
    private Logger logger;

    /**
     * Setup the listener.
     */
    public EntityChannelScriptAuthorListener()
    {
        super(NAME, new EntityChannelScriptAuthorChangeEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof EntityChannelScriptAuthorChangeEvent changedEvent) {
            EntityChange change = (EntityChange) source;

            this.tracker.setScriptAuthor(changedEvent.getChannel(), change);

            this.logger.debug("Updated the script author associated with the entity channel [{}] to [{}].",
                changedEvent.getChannel(), change);
        }
    }
}
