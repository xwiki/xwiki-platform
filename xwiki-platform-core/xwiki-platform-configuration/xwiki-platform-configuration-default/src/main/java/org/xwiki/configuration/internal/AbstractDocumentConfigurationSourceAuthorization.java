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
package org.xwiki.configuration.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.configuration.ConfigurationRight;
import org.xwiki.configuration.ConfigurationSourceAuthorization;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Common code to implement {@link ConfigurationSourceAuthorization} for Document-based {@code ConfigurationSource}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
public abstract class AbstractDocumentConfigurationSourceAuthorization implements ConfigurationSourceAuthorization
{
    @Inject
    protected AuthorizationManager authorizationManager;

    @Inject
    @Named("document")
    protected UserReferenceSerializer<DocumentReference> bridgeSerializer;

    @Override
    public boolean hasAccess(String key, UserReference userReference, ConfigurationRight right)
    {
        // Convert the user reference to a DocumentReference since currently the AuthorizationManager API doesn't use
        // the new user API. Remove this when it does.
        DocumentReference userDocumentReference = getUserDocumentReference(userReference);

        // READ:
        // - We require that the passed user has view rights on the configuration document to read from it.
        // WRITE:
        // - We require edit rights on the configuration document to be able to modify it.
        Right entityRight = getEntityRight(right);
        return this.authorizationManager.hasAccess(entityRight, userDocumentReference, getDocumentReference());
    }

    protected DocumentReference getUserDocumentReference(UserReference userReference)
    {
        return this.bridgeSerializer.serialize(userReference);
    }

    protected Right getEntityRight(ConfigurationRight right)
    {
        return right == ConfigurationRight.READ ? Right.VIEW : Right.EDIT;
    }

    /**
     * @return the reference of the document containing the configuration, used to verify that the user has edit
     *         permissions on it
     */
    protected abstract DocumentReference getDocumentReference();
}
