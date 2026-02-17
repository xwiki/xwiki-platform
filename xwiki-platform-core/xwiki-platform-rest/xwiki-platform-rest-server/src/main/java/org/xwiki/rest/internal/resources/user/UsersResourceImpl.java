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
package org.xwiki.rest.internal.resources.user;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rest.UserReferenceModelSerializer;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Users;
import org.xwiki.rest.resources.user.UsersResource;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.wiki.user.UserScope;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

import com.xpn.xwiki.XWikiException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

/**
 * @since 18.2.0RC1
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.user.UsersResourceImpl")
public class UsersResourceImpl extends XWikiResource implements UsersResource
{
    @Inject
    private WikiUserManager wikiUserManager;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private Provider<UserReferenceModelSerializer> userReferenceModelSerializerProvider;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Override
    public Users getUsers(String wikiName, Integer start, Integer number) throws XWikiRestException
    {
        Users users = this.objectFactory.createUsers();
        URI baseUri = this.uriInfo.getBaseUri();

        // Fail if we don't have a serializer for the current user store.
        UserReferenceModelSerializer userReferenceModelSerializer = this.userReferenceModelSerializerProvider.get();
        if (userReferenceModelSerializer == null) {
            throw new ServerErrorException(Response.status(Response.Status.NOT_IMPLEMENTED).entity(
                this.contextualLocalizationManager.getTranslationPlain(
                    "rest.exception.userResource.unsupportedStore")).build());
        }

        try {
            UserScope wikiUserScope = this.wikiUserManager.getUserScope(wikiName);
            Collection<String> wikiMembers = this.wikiUserManager.getMembers(wikiName);

            // We use streams to handle pagination.
            users.withUserSummaries(
                wikiMembers.stream().map(userId -> {
                    UserReference userReference = this.userReferenceResolver.resolve(userId,
                        this.getXWikiContext().getWikiReference());

                    if (userReference.isGlobal() && wikiUserScope == UserScope.LOCAL_ONLY
                        || !userReference.isGlobal() && wikiUserScope == UserScope.GLOBAL_ONLY)
                    {
                        return null;
                    } else {
                        try {
                            return userReferenceModelSerializer.toRestUserSummary(baseUri, userId, userReference);
                        } catch (XWikiException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                    .filter(Objects::nonNull)
                    .skip(start)
                    .limit(number != null ? number : wikiMembers.size())
                    .toList()
            );
        } catch (WikiUserManagerException | RuntimeException e) {
            throw new XWikiRestException(e);
        }

        return users;
    }
}
