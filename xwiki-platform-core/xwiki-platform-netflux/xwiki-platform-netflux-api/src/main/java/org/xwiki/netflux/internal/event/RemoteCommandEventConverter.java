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
package org.xwiki.netflux.internal.event;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Inject;

import org.xwiki.netflux.internal.RemoteUser;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.converter.AbstractEventConverter;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Component in charge of adapting local Netflux event to remote instances.
 * 
 * @version $Id$
 * @since 17.10.0RC1
 */
public class RemoteCommandEventConverter extends AbstractEventConverter
{
    private static final String PROP_INSTANCE = "instance";

    private static final String PROP_NAME = "id";

    private static final String PROP_CHANNEL = "channel";

    @Inject
    private RemoteObservationManagerConfiguration configuration;

    @Inject
    private UserReferenceSerializer<String> userSerializer;

    @Inject
    private UserReferenceResolver<String> userResolver;

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (localEvent.getEvent() instanceof NetfluxUserJoinedEvent joinEvent) {
            remoteEvent.setEvent(joinEvent);

            Map<String, String> user = new HashMap<>();
            user.put(PROP_INSTANCE, this.configuration.getId());
            user.put(PROP_NAME, joinEvent.getUser());

            remoteEvent.setSource((Serializable) user);

            return true;
        } else if (localEvent.getEvent() instanceof EntityChannelCreatedEvent channelEvent) {            
            remoteEvent.setEvent(channelEvent);

            Map<String, String> user = new HashMap<>();
            user.put(PROP_INSTANCE, this.configuration.getId());
            user.put(PROP_CHANNEL, channelEvent.getChannel());

            remoteEvent.setSource((Serializable) user);

            return true;
        }

        // TODO: To limit conflict, make all NetfluxMessageUserEvent events go through the cluster leader

        return false;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        Map<String, String> user = (Map<String, String>) remoteEvent.getSource();

        localEvent.setEvent((Event) remoteEvent.getEvent());
        localEvent.setSource(new RemoteUser(user.get(PROP_NAME), user.get(PROP_INSTANCE)));

        return false;
    }

    private UserReference unzerializeUserReference(String reference)
    {
        return this.userResolver.resolve(reference);
    }

    private String serializeUserReference(UserReference userReference)
    {
        return this.userSerializer.serialize(userReference);
    }
}
