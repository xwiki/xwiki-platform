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
package org.xwiki.internal.web;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Checks if the current user has the required rights for the given page template.
 *
 * @version $Id$
 * @since 17.10.2
 */
@Component(roles = PageTemplateRequiredRightsChecker.class)
@Singleton
public class PageTemplateRequiredRightsChecker
{
    @Inject
    private DocumentRequiredRightsManager documentRequiredRightsManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    @Inject
    private Logger logger;

    /**
     * Checks if the current user has the rights that are enforced by the page template in the given location.
     *
     * @param templatePage the page template
     * @param documentOrSpaceReference the document or space reference where the template shall be used
     * @return {@code true} if the user has the required rights, {@code false} otherwise
     */
    public boolean hasRequiredRights(DocumentReference templatePage, EntityReference documentOrSpaceReference)
    {
        try {
            Optional<DocumentRequiredRights> requiredRights =
                this.documentRequiredRightsManager.getRequiredRights(templatePage);
            if (requiredRights.isEmpty() || !requiredRights.get().enforce()) {
                return true;
            }

            DocumentReference userReference = this.xWikiContextProvider.get().getUserReference();
            for (DocumentRequiredRight requiredRight : requiredRights.get().rights()) {
                EntityReference scopeReference;
                // If the scope is document, we still check at the space level when we got a space reference as we're
                // creating a new document and for this, the user needs to have the right at the space level.
                if (requiredRight.scope() == EntityType.DOCUMENT) {
                    scopeReference = documentOrSpaceReference;
                } else {
                    scopeReference = documentOrSpaceReference.extractReference(requiredRight.scope());
                }
                if (!this.authorizationManager.hasAccess(requiredRight.right(), userReference, scopeReference)) {
                    return false;
                }
            }
            return true;
        } catch (AuthorizationException e) {
            this.logger.warn("There was an error getting the required rights for document [{}], root cause: [{}]",
                templatePage, ExceptionUtils.getRootCauseMessage(e));
            return false;
        }
    }
}
