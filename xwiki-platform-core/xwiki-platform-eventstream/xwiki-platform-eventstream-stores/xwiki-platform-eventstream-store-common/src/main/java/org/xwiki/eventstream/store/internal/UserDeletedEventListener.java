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
package org.xwiki.eventstream.store.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStore;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Clean notifications associated to deleted users.
 * 
 * @version $Id$
 * @since 12.6.1
 * @since 12.7RC1
 */
@Component
@Singleton
public class UserDeletedEventListener extends AbstractEventListener
{
    /**
     * Name of the listener.
     */
    public static final String NAME = "org.xwiki.eventstream.store.internal.UserDeletedEventListener";

    @Inject
    private EventStore eventStore;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Default constructor.
     */
    public UserDeletedEventListener()
    {
        super(NAME,
            new XObjectDeletedEvent(BaseObjectReference.any(XWikiUsersDocumentInitializer.CLASS_REFERENCE_STRING)));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        EntityReference userReference =
            ((XObjectDeletedEvent) event).getReference().extractReference(EntityType.DOCUMENT);
        String entity = this.serializer.serialize(userReference);

        this.eventStore.deleteEventStatuses(entity, null);
    }
}
