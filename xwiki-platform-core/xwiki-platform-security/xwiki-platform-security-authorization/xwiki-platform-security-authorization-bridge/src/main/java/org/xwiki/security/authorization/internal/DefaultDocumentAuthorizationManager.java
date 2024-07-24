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

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;

/**
 * Default implementation of {@link DocumentAuthorizationManager}.
 *
 * @version $Id$
 * @since 16.10.0RC1
 */
@Component
@Singleton
public class DefaultDocumentAuthorizationManager implements DocumentAuthorizationManager
{
    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private DocumentRequiredRightsManager documentRequiredRightsManager;

    @Inject
    private Logger logger;

    @Override
    public boolean hasAccess(Right right, EntityType level, DocumentReference contextAuthor,
        DocumentReference contextDocument)
    {
        EntityReference reference = contextDocument != null ? contextDocument.extractReference(level) : null;

        try {
            return hasRequiredRight(right, level, contextDocument)
                && this.authorizationManager.hasAccess(right, contextAuthor, reference);
        } catch (Exception e) {
            this.logger.error("Failed to load required rights for user [{}] on [{}].",
                contextAuthor, contextDocument, e);
            return false;
        }
    }

    @Override
    public boolean hasRequiredRight(Right right, EntityType level, DocumentReference contextDocument)
        throws AuthorizationException
    {
        DocumentRequiredRights documentRequiredRights =
            this.documentRequiredRightsManager.getRequiredRights(contextDocument)
                .orElse(DocumentRequiredRights.EMPTY);

        boolean hasAccess;

        if (documentRequiredRights.enforce()) {
            EntityType actualEntityType = getFirstLevelWithRight(right, level, contextDocument);

            hasAccess = documentRequiredRights.rights().stream()
                // Filter required rights of the same level or of an ancestor.
                .filter(requiredRight ->
                    // Same level.
                    Objects.equals(actualEntityType, requiredRight.scope())
                        // Special case for required right on farm level.
                        || requiredRight.scope() == null
                        || (actualEntityType != null && actualEntityType.isAllowedAncestor(requiredRight.scope()))
                )
                .anyMatch(requiredRight ->
                    requiredRight.right().equals(right) || requiredRight.right()
                        .getImpliedRights()
                        .contains(right));
        } else {
            hasAccess = true;
        }

        return hasAccess;
    }

    private static EntityType getFirstLevelWithRight(Right right, EntityType level, DocumentReference contextDocument)
    {
        EntityType actualEntityType = level;
        EntityReference entityReference = contextDocument.extractReference(level);
        do {
            if (Right.getEnabledRights(actualEntityType).contains(right)) {
                break;
            }

            entityReference = entityReference.getParent();
            if (entityReference != null) {
                actualEntityType = entityReference.getType();
            } else {
                actualEntityType = null;
            }
        } while (entityReference != null);
        return actualEntityType;
    }

    @Override
    public void checkAccess(Right right, EntityType level, DocumentReference contextAuthor,
        DocumentReference contextDocument) throws AccessDeniedException
    {
        EntityReference reference = contextDocument != null ? contextDocument.extractReference(level) : null;

        try {
            if (hasRequiredRight(right, level, contextDocument)) {
                this.authorizationManager.checkAccess(right, contextAuthor, reference);
            } else {
                this.logger.info("[{}] right has been denied to user [{}] on entity [{}] based on required rights"
                        + " of document [{}] on level [{}]: security checkpoint", right, contextAuthor, reference,
                    contextDocument, level);
                throw new AccessDeniedException(right, contextAuthor, reference);
            }
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new AccessDeniedException(right, contextAuthor, contextDocument, e);
        }
    }
}
