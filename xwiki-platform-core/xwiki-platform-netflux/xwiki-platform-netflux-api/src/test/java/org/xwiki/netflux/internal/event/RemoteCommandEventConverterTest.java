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

import jakarta.websocket.Session;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.netflux.internal.EntityChange;
import org.xwiki.netflux.internal.EntityChange.ScriptLevel;
import org.xwiki.netflux.internal.LocalUser;
import org.xwiki.netflux.internal.RemoteUser;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.LocalEventConverter;
import org.xwiki.observation.remote.converter.RemoteEventConverter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.user.internal.document.DocumentUserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link RemoteCommandEventConverter}.
 * 
 * @version $Id$
 */
@ComponentTest
class RemoteCommandEventConverterTest
{
    @InjectMockComponents(role = LocalEventConverter.class)
    private RemoteCommandEventConverter localConverter;

    @InjectMockComponents(role = RemoteEventConverter.class)
    private RemoteCommandEventConverter remoteConverter;

    @MockComponent
    private UserReferenceSerializer<String> userSerializer;

    @MockComponent
    private UserReferenceResolver<String> userResolver;

    private LocalEventData convertRemote(Event event, Object source)
    {
        RemoteEventData remoteData = new RemoteEventData();

        if (this.localConverter.toRemote(new LocalEventData(event, source, remoteData), remoteData)) {
            LocalEventData localData = new LocalEventData();

            if (this.remoteConverter.fromRemote(remoteData, localData)) {
                return localData;
            }

            return fail("Failed to convert the remote event");
        }

        return fail("Failed to convert the local event");
    }

    @Test
    void convertRemoteNetfluxUserJoinedEvent()
    {
        LocalUser localUser = new LocalUser(mock(Session.class), "username");
        NetfluxUserJoinEvent event = new NetfluxUserJoinEvent(42, "username", "channel");

        LocalEventData data = convertRemote(event, localUser);

        NetfluxUserJoinEvent convertedEvent = (NetfluxUserJoinEvent) data.getEvent();
        assertEquals(event.getChannel(), convertedEvent.getChannel());
        assertEquals(event.getSequence(), convertedEvent.getSequence());
        assertEquals(event.getUser(), convertedEvent.getUser());

        RemoteUser remoteUser = (RemoteUser) data.getSource();
        assertEquals(localUser.getName(), remoteUser.getName());
    }

    @Test
    void convertEntityChannelScriptAuthorChangeEvent()
    {
        DocumentUserReference userReference = new DocumentUserReference(new DocumentReference("xwiki", "XWiki", "user"), true);
        String userString = "userString";
        when(this.userSerializer.serialize(userReference)).thenReturn(userString);
        when(this.userResolver.resolve(userString)).thenReturn(userReference);
        EntityChange change = new EntityChange(new DocumentReference("wiki", "space", "document"),
            userReference, ScriptLevel.PROGRAMMING);
        EntityChannelScriptAuthorChangeEvent event = new EntityChannelScriptAuthorChangeEvent("channel");

        LocalEventData data = convertRemote(event, change);

        EntityChannelScriptAuthorChangeEvent convertedEvent = (EntityChannelScriptAuthorChangeEvent) data.getEvent();
        assertEquals(event.getChannel(), convertedEvent.getChannel());

        assertEquals(change, data.getSource());
    }
}
