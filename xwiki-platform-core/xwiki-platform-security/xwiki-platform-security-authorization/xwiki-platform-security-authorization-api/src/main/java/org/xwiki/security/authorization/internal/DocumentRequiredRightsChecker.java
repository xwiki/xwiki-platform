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
package org.xwiki.security.authorization.internal;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;

/**
 * Check if a user has all required rights defined for an entity.
 *
 * @version $Id$
 * @since 16.10.0RC1
 */
@Component(roles = DocumentRequiredRightsChecker.class)
@Singleton
public class DocumentRequiredRightsChecker
{
    @Inject
    private Provider<AuthorizationManager> authorizationManagerProvider;

    @Inject
    private DocumentRequiredRightsManager documentRequiredRightsManager;

    /**
     * Check if a user has all required rights defined for an entity.
     *
     * @param userReference the user reference
     * @param entityReference the entity reference
     * @return true if the user has all required rights, false otherwise
     * @throws AuthorizationException if an error occurs while loading the required rights
     */
    public boolean hasRequiredRights(DocumentReference userReference, EntityReference entityReference)
        throws AuthorizationException
    {
        DocumentReference documentReference = getDocumentReference(entityReference);

        if (documentReference == null) {
            return true;
        }

        AuthorizationManager authorizationManager = this.authorizationManagerProvider.get();

        DocumentRequiredRights documentRequiredRights =
            this.documentRequiredRightsManager.getRequiredRights(documentReference)
                .orElse(DocumentRequiredRights.EMPTY);
        if (documentRequiredRights.enforce()) {
            for (DocumentRequiredRight requiredRight : documentRequiredRights.rights()) {
                if (!authorizationManager.hasAccess(requiredRight.right(), userReference,
                    documentReference.extractReference(requiredRight.scope())))
                {
                    return false;
                }
            }
        }

        return true;
    }

    private DocumentReference getDocumentReference(EntityReference entityReference)
    {
        if (entityReference != null) {
            if (entityReference instanceof DocumentReference documentReference) {
                return documentReference;
            }

            EntityReference documentEntityReference = entityReference.extractReference(EntityType.DOCUMENT);

            if (documentEntityReference != null) {
                return new DocumentReference(documentEntityReference);
            }
        }

        return null;
    }
}
