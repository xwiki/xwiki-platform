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
package org.xwiki.user.rest.internal.resources;

import java.net.URI;

import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.user.rest.UserReferenceModelSerializer;
import org.xwiki.user.rest.model.jaxb.User;
import org.xwiki.user.rest.resources.CurrentUserResource;

import com.xpn.xwiki.XWikiException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

/**
 * @since 18.2.0RC1
 * @version $Id$
 */
@Component
@Named("org.xwiki.user.rest.internal.resources.CurrentUserResourceImpl")
public class CurrentUserResourceImpl extends XWikiResource implements CurrentUserResource
{
    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Inject
    private UserReferenceSerializer<String> stringUserReferenceSerializer;

    @Inject
    private Provider<UserReferenceModelSerializer> userReferenceModelSerializerProvider;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Override
    public User getUser(String wikiName, boolean preferences) throws XWikiRestException
    {
        URI baseUri = this.uriInfo.getBaseUri();

        // Fail if we don't have a serializer for the current user store.
        UserReferenceModelSerializer userReferenceModelSerializer = this.userReferenceModelSerializerProvider.get();
        if (userReferenceModelSerializer == null) {
            throw new ServerErrorException(Response.status(Response.Status.NOT_IMPLEMENTED).entity(
                this.contextualLocalizationManager.getTranslationPlain(
                    "rest.exception.userResource.unsupportedStore")).build());
        }

        DocumentReference userDocumentReference = this.xcontextProvider.get().getUserReference();
        try {
            UserReference userReference;
            if (userDocumentReference != null) {
                userReference = this.userReferenceResolver.resolve(userDocumentReference);
            } else {
                // The resource was accessed by guest.
                userReference = GuestUserReference.INSTANCE;
            }

            return userReferenceModelSerializer.toRestUser(baseUri,
                this.stringUserReferenceSerializer.serialize(userReference), userReference, preferences);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
