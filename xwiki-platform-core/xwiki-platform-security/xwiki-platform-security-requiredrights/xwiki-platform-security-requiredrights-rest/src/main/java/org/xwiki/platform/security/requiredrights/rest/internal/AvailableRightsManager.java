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
package org.xwiki.platform.security.requiredrights.rest.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.requiredrights.rest.model.jaxb.AvailableRight;
import org.xwiki.security.requiredrights.rest.model.jaxb.ObjectFactory;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Compute the available rights for the current user on a given document.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component(roles = AvailableRightsManager.class)
@Singleton
public class AvailableRightsManager
{
    private static final List<DocumentRequiredRight> CONSIDERED_RIGHTS = List.of(
        // The "None" option isn't really a right and thus represented as "null" here.
        new DocumentRequiredRight(null, EntityType.DOCUMENT),
        new DocumentRequiredRight(Right.SCRIPT, EntityType.DOCUMENT),
        new DocumentRequiredRight(Right.ADMIN, EntityType.WIKI),
        new DocumentRequiredRight(Right.PROGRAM, null)
    );

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @Inject
    private ContextualLocalizationManager localizationManager;

    private final ObjectFactory factory = new ObjectFactory();

    /**
     * Compute the available rights for the current user on the given document.
     *
     * @param analysisResults the analysis results to compute which rights are required
     * @param documentReference the document reference to check rights on
     * @return the list of available rights for the current user on the given document
     */
    public List<AvailableRight> computeAvailableRights(List<RequiredRightAnalysisResult> analysisResults,
        DocumentReference documentReference)
    {
        int maximumRequiredRight = 0;
        Set<Integer> maybeRequiredRights = new HashSet<>();
        for (RequiredRightAnalysisResult analysisResult : analysisResults) {
            for (RequiredRight requiredRight : analysisResult.getRequiredRights()) {
                int index = getIndexInConsideredRights(requiredRight);

                if (index == -1) {
                    continue;
                }

                if (requiredRight.isManualReviewNeeded()) {
                    maybeRequiredRights.add(index);
                } else {
                    maximumRequiredRight = Math.max(maximumRequiredRight, index);
                }
            }
        }

        int finalMaximumRequiredRight = maximumRequiredRight;
        maybeRequiredRights.removeIf(maybeIndex -> maybeIndex <= finalMaximumRequiredRight);

        DocumentReference userReference = this.userReferenceSerializer.serialize(CurrentUserReference.INSTANCE);

        boolean hasEdit = this.authorizationManager.hasAccess(Right.EDIT, userReference, documentReference);

        List<AvailableRight> availableRights = new ArrayList<>(CONSIDERED_RIGHTS.size());
        for (int i = 0; i < CONSIDERED_RIGHTS.size(); i++) {
            DocumentRequiredRight consideredRight = CONSIDERED_RIGHTS.get(i);
            boolean maybeRequired = maybeRequiredRights.contains(i);
            boolean hasRight = hasEdit && (consideredRight.right() == null
                || this.authorizationManager.hasAccess(consideredRight.right(), userReference,
                documentReference.extractReference(consideredRight.scope())));
            availableRights.add(this.factory.createAvailableRight()
                .withRight(Objects.toString(consideredRight.right(), ""))
                .withScope(Objects.toString(consideredRight.scope(), null))
                .withDefinitelyRequiredRight(i == maximumRequiredRight)
                .withMaybeRequiredRight(maybeRequired)
                .withHasRight(hasRight)
                .withDisplayName(getDisplayName(consideredRight.right()))
            );
        }

        return availableRights;
    }

    private String getDisplayName(Right right)
    {
        String translationKey = "security.requiredrights.rest.right." + (right == null ? "none" : right.getName());
        return this.localizationManager.getTranslationPlain(translationKey);
    }

    private static int getIndexInConsideredRights(RequiredRight requiredRight)
    {
        int index = -1;
        // Find the first considered right where scope and right match.
        for (int i = 0; i < CONSIDERED_RIGHTS.size(); i++) {
            DocumentRequiredRight consideredRight = CONSIDERED_RIGHTS.get(i);
            if (consideredRight.right() == requiredRight.getRight()
                && consideredRight.scope() == requiredRight.getEntityType())
            {
                index = i;
                break;
            }
        }
        return index;
    }
}
