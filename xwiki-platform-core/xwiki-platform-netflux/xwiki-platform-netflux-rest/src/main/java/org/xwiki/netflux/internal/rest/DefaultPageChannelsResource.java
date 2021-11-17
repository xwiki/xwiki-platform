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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.netflux.EntityChannelStore;
import org.xwiki.netflux.rest.PageChannelsResource;
import org.xwiki.netflux.rest.model.jaxb.EntityChannel;
import org.xwiki.netflux.rest.model.jaxb.EntityReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Default implementation of {@link PageChannelsResource}.
 * 
 * @version $Id$
 * @since 13.9RC1
 */
@Component
@Named("org.xwiki.netflux.internal.rest.DefaultPageChannelsResource")
@Singleton
public class DefaultPageChannelsResource extends XWikiResource implements PageChannelsResource
{
    private static final String PATH_SEPARATOR = "/";

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private EntityChannelStore channelStore;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Override
    public List<EntityChannel> getChannels(String wikiName, String spaceNames, String pageName, List<String> paths,
        Boolean create) throws XWikiRestException
    {
        DocumentReference documentReference = new DocumentReference(pageName, getSpaceReference(spaceNames, wikiName));
        if (!this.authorization.hasAccess(Right.EDIT, documentReference)) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        // Workaround for https://github.com/restlet/restlet-framework-java/issues/922 (JaxRs multivalue
        // query-params gives list with null element).
        List<String> actualPaths = paths.stream().filter(Objects::nonNull).collect(Collectors.toList());

        return getChannels(documentReference, actualPaths, create).stream().map(this::toRestChannel)
            .collect(Collectors.toList());
    }

    private List<org.xwiki.netflux.EntityChannel> getChannels(org.xwiki.model.reference.EntityReference entityReference,
        List<String> paths, boolean create)
    {
        if (paths.isEmpty()) {
            // Return all channels.
            return this.channelStore.getChannels(entityReference);
        } else {
            // Return only matching channels, creating missing ones if asked.
            return paths.stream().flatMap(path -> this.getChannels(entityReference, path, create))
                .collect(Collectors.toList());
        }
    }

    private Stream<org.xwiki.netflux.EntityChannel> getChannels(
        org.xwiki.model.reference.EntityReference entityReference, String path, boolean create)
    {
        List<String> pathElements = decodePath(path);
        if (path.endsWith(PATH_SEPARATOR)) {
            // Handle as a path prefix.
            return this.channelStore.getChannels(entityReference, pathElements).stream();
        } else {
            // Handle as full path.
            Optional<org.xwiki.netflux.EntityChannel> channel =
                this.channelStore.getChannel(entityReference, pathElements);
            if (channel.isPresent()) {
                return channel.map(Stream::of).orElseGet(Stream::empty);
            } else if (create && !path.isEmpty()) {
                return Stream.of(this.channelStore.createChannel(entityReference, pathElements));
            } else {
                return Stream.empty();
            }
        }
    }

    private List<String> decodePath(String path)
    {
        return Stream.of(path.split(PATH_SEPARATOR)).map(pathElement -> {
            try {
                return URLDecoder.decode(pathElement, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Shouldn't happen.
                return pathElement;
            }
        }).collect(Collectors.toList());
    }

    private EntityChannel toRestChannel(org.xwiki.netflux.EntityChannel channel)
    {
        String entityReferenceString = this.entityReferenceSerializer.serialize(channel.getEntityReference());
        EntityReference entityReference = new EntityReference().withType(channel.getEntityReference().getType().name())
            .withValue(entityReferenceString);
        return new EntityChannel().withEntityReference(entityReference).withPath(channel.getPath())
            .withKey(channel.getKey()).withUserCount(channel.getUserCount());
    }
}
