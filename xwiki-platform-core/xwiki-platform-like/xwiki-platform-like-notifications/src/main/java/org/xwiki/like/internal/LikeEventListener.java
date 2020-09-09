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
package org.xwiki.like.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.like.LikeEvent;
import org.xwiki.like.events.LikeRecordableEvent;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

/**
 * Listener to transform generic {@link LikeEvent} into {@link LikeRecordableEvent} that generate a notification.
 *
 * @version $Id$
 * @since 12.7RC1
 */
@Component
@Singleton
@Named(LikeEventListener.NAME)
public class LikeEventListener extends AbstractEventListener
{
    /**
     * Listener name.
     */
    public static final String NAME = "LikeEventListener";

    private static final List<Event> EVENT_LIST = Collections.singletonList(new LikeEvent());

    @Inject
    private ObservationManager observationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public LikeEventListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (data != null) {
            EntityReference likedEntity = (EntityReference) data;

            try {
                DocumentModelBridge documentInstance =
                    this.documentAccessBridge.getDocumentInstance(likedEntity);
                this.observationManager.notify(new LikeRecordableEvent(),
                    LikeEventDescriptor.EVENT_SOURCE, documentInstance);
            } catch (Exception e) {
                this.logger.error("Error while sending event about like on [{}]", likedEntity, e);
            }
        }
    }
}
