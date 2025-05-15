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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;

/**
 * Proposes changes for required rights.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Singleton
@Component(roles = RequiredRightsChangeSuggestionManager.class)
public class RequiredRightsChangeSuggestionManager
{
    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * @param documentReference the reference of the document
     * @param currentRights the currently configured rights
     * @param analysisResults the results of the required rights analysis
     * @return the operations suggested for changed the current required rights
     */
    public List<RequiredRightChangeSuggestion> getSuggestedOperations(DocumentReference documentReference,
        DocumentRequiredRights currentRights, List<RequiredRightAnalysisResult> analysisResults)
    {
        // For each analysis result, find if it is covered by one of the current rights. If not, suggest adding a
        // right. Then, consolidate all suggestions to only keep the "highest" right.
        // If there are rights that require manual review, don't consider them for the consolidation but suggest
        // those rights separately.
        // For each current right, find out if it is actually required according to the analysis result. If not,
        // suggest removing it. Also, when, e.g., programming right is currently configured but just script right is
        // required, suggest replacing programming by script right.
        // Start simple: only act on the hierarchy none - script - wiki admin - programming right.
        DocumentRequiredRight configuredRight = currentRights.rights().stream()
            .filter(this::isConsideredRight)
            .reduce(null, RequiredRightsChangeSuggestionManager::getMorePowerfulRight);

        DocumentRequiredRight definitelyRequiredRight =
            getMostPowerfulConsideredRequiredRight(analysisResults, false);
        DocumentRequiredRight maybeRequiredRight =
            getMostPowerfulConsideredRequiredRight(analysisResults, true);

        return getRightOperations(documentReference, configuredRight, definitelyRequiredRight,
            maybeRequiredRight);
    }

    private DocumentRequiredRight getMostPowerfulConsideredRequiredRight(
        List<RequiredRightAnalysisResult> requiredRightAnalysisResults, boolean manualReviewNeeded)
    {
        return requiredRightAnalysisResults.stream()
            .flatMap(result -> result.getRequiredRights().stream())
            .filter(result -> result.isManualReviewNeeded() == manualReviewNeeded)
            .map(RequiredRight::toDocumentRequiredRight)
            .filter(this::isConsideredRight)
            .reduce(null, RequiredRightsChangeSuggestionManager::getMorePowerfulRight);
    }

    private List<RequiredRightChangeSuggestion> getRightOperations(DocumentReference documentReference,
        DocumentRequiredRight configuredRight, DocumentRequiredRight definitelyRequiredRight,
        DocumentRequiredRight maybeRequiredRight)
    {
        List<RequiredRightChangeSuggestion> operations = new ArrayList<>();

        // The page definitely needs more rights? Suggest adding that right, replacing the current one.
        if (isNewRightMorePowerFull(configuredRight, definitelyRequiredRight)) {
            operations.add(buildRightOperation(documentReference, true, configuredRight, definitelyRequiredRight,
                false));
        }

        // Suggest adding a possibly required right, but only if it is more powerful than the definitely required right.
        if (isNewRightMorePowerFull(configuredRight, maybeRequiredRight)
            && isNewRightMorePowerFull(definitelyRequiredRight, maybeRequiredRight))
        {
            operations.add(buildRightOperation(documentReference, true, configuredRight, maybeRequiredRight, true));
        }

        // Suggest removing a right, but only if we've currently configured more than is required.
        if (isNewRightMorePowerFull(definitelyRequiredRight, configuredRight)) {
            // If the maybe required right is more powerful than the definitely required right, review is required.
            boolean maybeIsHigherThanDefinitely = isNewRightMorePowerFull(definitelyRequiredRight, maybeRequiredRight);
            operations.add(buildRightOperation(documentReference, false, configuredRight, definitelyRequiredRight,
                maybeIsHigherThanDefinitely));

            // If the maybe required right is higher than the definitely required right, but the maybe required right
            // is still lower than the configured right, we can suggest lowering to the maybe required right.
            if (maybeIsHigherThanDefinitely && isNewRightMorePowerFull(maybeRequiredRight, configuredRight)) {
                operations.add(buildRightOperation(documentReference, false, configuredRight, maybeRequiredRight,
                    false));
            }
        }
        return operations;
    }

    private RequiredRightChangeSuggestion buildRightOperation(DocumentReference documentReference,
        boolean increasesRights, DocumentRequiredRight rightToRemove, DocumentRequiredRight rightToAdd,
        boolean requiresManualReview)
    {
        boolean hasPermission = this.contextualAuthorizationManager.hasAccess(Right.EDIT, documentReference)
            && (rightToAdd == null || hasAccess(documentReference, rightToAdd));

        return new RequiredRightChangeSuggestion(increasesRights, rightToRemove, rightToAdd, requiresManualReview,
            hasPermission);
    }

    private boolean hasAccess(DocumentReference documentReference, DocumentRequiredRight rightToAdd)
    {
        if (rightToAdd.scope() == null) {
            return this.contextualAuthorizationManager.hasAccess(rightToAdd.right(), null);
        } else {
            return this.contextualAuthorizationManager.hasAccess(rightToAdd.right(),
                documentReference.extractReference(rightToAdd.scope()));
        }
    }

    private static boolean isNewRightMorePowerFull(DocumentRequiredRight existingRight, DocumentRequiredRight newRight)
    {
        return newRight != null && (existingRight == null || (newRight.right().getImpliedRights() != null
            && newRight.right().getImpliedRights().contains(existingRight.right())));
    }

    private static DocumentRequiredRight getMorePowerfulRight(DocumentRequiredRight right1,
        DocumentRequiredRight right2)
    {
        return isNewRightMorePowerFull(right1, right2) ? right2 : right1;
    }

    private boolean isConsideredRight(DocumentRequiredRight documentRequiredRight)
    {
        return List.of(Right.SCRIPT, Right.ADMIN, Right.PROGRAM).contains(documentRequiredRight.right())
            && (!documentRequiredRight.right().equals(Right.ADMIN) || documentRequiredRight.scope() == EntityType.WIKI);
    }
}
