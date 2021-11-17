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
package org.xwiki.netflux.internal.rest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.netflux.EntityChannelStore;
import org.xwiki.netflux.rest.model.jaxb.EntityChannel;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultPageChannelsResource}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultPageChannelsResourceTest
{
    @InjectMockComponents
    private DefaultPageChannelsResource resource;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private EntityChannelStore channelStore;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private final String wikiName = "test";

    private final String spaces = "Path/spaces/To";

    private final String pageName = "Page";

    private final DocumentReference documentReference =
        new DocumentReference(wikiName, Arrays.asList("Path", "To"), pageName);

    @BeforeEach
    void setUp()
    {
        when(this.authorization.hasAccess(Right.EDIT, documentReference)).thenReturn(true);
        when(this.entityReferenceSerializer.serialize(documentReference)).thenReturn("test:Path.To.Page");
    }

    @Test
    void getUnauthorized() throws Exception
    {
        when(this.authorization.hasAccess(Right.EDIT, documentReference)).thenReturn(false);
        try {
            this.resource.getChannels(wikiName, spaces, pageName, Collections.emptyList(), false);
            fail("Access rights were not checked!");
        } catch (WebApplicationException e) {
            assertEquals(Status.UNAUTHORIZED.getStatusCode(), e.getResponse().getStatus());
        }
    }

    @Test
    void getAllChannels() throws Exception
    {
        org.xwiki.netflux.EntityChannel channel =
            new org.xwiki.netflux.EntityChannel(documentReference, Arrays.asList("en", "content"), "qwerty");
        channel.setUserCount(13);
        when(this.channelStore.getChannels(documentReference)).thenReturn(Collections.singletonList(channel));

        List<EntityChannel> restChannels =
            this.resource.getChannels(wikiName, spaces, pageName, Collections.singletonList(null), false);

        assertEquals(1, restChannels.size());
        assertEquals("DOCUMENT", restChannels.get(0).getEntityReference().getType());
        assertEquals("test:Path.To.Page", restChannels.get(0).getEntityReference().getValue());
        assertEquals(channel.getKey(), restChannels.get(0).getKey());
        assertEquals(channel.getPath(), restChannels.get(0).getPath());
        assertEquals(channel.getUserCount(), restChannels.get(0).getUserCount());
    }

    @Test
    void getChannelsByPath() throws Exception
    {
        List<String> path = Arrays.asList("en", "content");
        org.xwiki.netflux.EntityChannel channel =
            new org.xwiki.netflux.EntityChannel(documentReference, path, "qwerty");
        when(this.channelStore.getChannel(documentReference, path)).thenReturn(Optional.of(channel));

        List<EntityChannel> restChannels =
            this.resource.getChannels(wikiName, spaces, pageName, Arrays.asList("fr/content", "en/content"), false);

        assertEquals(1, restChannels.size());
        assertEquals(channel.getKey(), restChannels.get(0).getKey());
    }

    @Test
    void createChannels() throws Exception
    {
        List<String> frPath = Arrays.asList("fr", "content");
        org.xwiki.netflux.EntityChannel frChannel =
            new org.xwiki.netflux.EntityChannel(documentReference, frPath, "12345");
        when(this.channelStore.getChannels(documentReference, frPath)).thenReturn(Collections.singletonList(frChannel));

        List<String> enPath = Arrays.asList("en", "content");
        org.xwiki.netflux.EntityChannel enChannel =
            new org.xwiki.netflux.EntityChannel(documentReference, enPath, "54321");
        when(this.channelStore.createChannel(documentReference, enPath)).thenReturn(enChannel);

        List<EntityChannel> restChannels =
            this.resource.getChannels(wikiName, spaces, pageName, Arrays.asList("fr/content/", "en/content"), true);

        assertEquals(2, restChannels.size());
        assertEquals(frChannel.getKey(), restChannels.get(0).getKey());
        assertEquals(enChannel.getKey(), restChannels.get(1).getKey());
    }
}
