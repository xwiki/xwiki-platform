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
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.netflux.internal.EntityChange;
import org.xwiki.netflux.internal.EntityChange.ScriptLevel;
import org.xwiki.netflux.internal.RemoteUser;
import org.xwiki.netflux.internal.User;
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
 * @since 17.10.1
 * @since 18.0.0RC1
 */
@Component
@Singleton
@Named("RemoteCommandEventConverter")
public class RemoteCommandEventConverter extends AbstractEventConverter
{
    private static final String PROP_INSTANCE = "instance";

    private static final String PROP_NAME = "id";

    private static final String PROP_CHANNEL = "channel";

    private static final String PROP_CHANGE_AUTHOR = "changeAuthor";

    private static final String PROP_CHANGE_ENTITYREFERENCE = "changeEntityReference";

    private static final String PROP_CHANGE_SCRIPTLEVEL = "changeScriptLevel";

    private static final String PROP_CHANGE_TIMESTAMP = "timestamp";

    @Inject
    private RemoteObservationManagerConfiguration configuration;

    @Inject
    private UserReferenceSerializer<String> userSerializer;

    @Inject
    private UserReferenceResolver<String> userResolver;

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (localEvent.getEvent() instanceof NetfluxUserJoinEvent joinEvent) {
            remoteEvent.setEvent(joinEvent);

            User user = (User) localEvent.getSource();
            Map<String, String> remoteSource = new HashMap<>();
            remoteSource.put(PROP_INSTANCE, this.configuration.getId());
            remoteSource.put(PROP_NAME, user.getName());
            remoteEvent.setSource((Serializable) remoteSource);

            return true;
        } else if (localEvent.getEvent() instanceof EntityChannelScriptAuthorChangeEvent scriptEvent) {
            remoteEvent.setEvent(scriptEvent);

            EntityChange change = (EntityChange) localEvent.getSource();
            Map<String, Serializable> remoteSource = new HashMap<>();
            remoteSource.put(PROP_CHANNEL, scriptEvent.getChannel());
            remoteSource.put(PROP_CHANGE_ENTITYREFERENCE, change.getEntityReference());
            remoteSource.put(PROP_CHANGE_AUTHOR, serializeUserReference(change.getAuthor()));
            remoteSource.put(PROP_CHANGE_SCRIPTLEVEL, change.getScriptLevel());
            remoteSource.put(PROP_CHANGE_TIMESTAMP, change.getTimestamp());
            remoteEvent.setSource((Serializable) remoteSource);

            return true;
        }

        // TODO: To limit conflict, make all NetfluxMessageUserEvent events go through the cluster leader

        return false;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (remoteEvent.getEvent() instanceof NetfluxUserJoinEvent joinEvent) {
            localEvent.setEvent(joinEvent);

            Map<String, String> user = (Map<String, String>) remoteEvent.getSource();
            localEvent.setSource(new RemoteUser(user.get(PROP_NAME), user.get(PROP_INSTANCE)));

            return true;
        } else if (remoteEvent.getEvent() instanceof EntityChannelScriptAuthorChangeEvent scriptEvent) {
            localEvent.setEvent(scriptEvent);

            Map<String, Serializable> change = (Map<String, Serializable>) remoteEvent.getSource();
            localEvent.setSource(new EntityChange((EntityReference) change.get(PROP_CHANGE_ENTITYREFERENCE),
                unzerializeUserReference((String) change.get(PROP_CHANGE_AUTHOR)),
                (ScriptLevel) change.get(PROP_CHANGE_SCRIPTLEVEL), (long) change.get(PROP_CHANGE_TIMESTAMP)));

            return true;
        }

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
