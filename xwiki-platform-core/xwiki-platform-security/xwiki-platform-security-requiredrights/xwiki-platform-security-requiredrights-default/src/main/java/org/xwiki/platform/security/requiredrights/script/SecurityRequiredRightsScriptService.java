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
package org.xwiki.platform.security.requiredrights.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Script service for required rights.
 *
 * @version $Id$
 * @since 16.10.0RC1
 */
@Named("security.requiredrights")
@Singleton
@Component
@Unstable
public class SecurityRequiredRightsScriptService implements ScriptService
{
    @Inject
    private RequiredRightAnalyzer<XWikiDocument> requiredRightAnalyzer;

    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * An operation that removes or adds rights to a document.
     *
     * @param increasesRights if more rights are granted due to this change
     * @param rightToRemove the right to replace
     * @param rightToAdd the right to add
     * @param requiresManualReview if the analysis is certain that the right is needed/not needed or the user needs to
     * manually review the analysis result to determine if the right is actually needed/not needed
     */
    public record RightOperation(boolean increasesRights, DocumentRequiredRight rightToRemove,
                                 DocumentRequiredRight rightToAdd, boolean requiresManualReview)
    {
    }

    /**
     * Analyze the required rights of the document referenced by the given reference.
     *
     * @param documentReference the reference of the document to analyze
     * @return the list of analysis results
     * @throws AccessDeniedException if the document cannot be accessed
     * @throws RequiredRightsException if there is a problem loading or analyzing the document
     */
    public List<RequiredRightAnalysisResult> analyzeDocument(DocumentReference documentReference) throws
        AccessDeniedException, RequiredRightsException
    {
        this.contextualAuthorizationManager.checkAccess(Right.VIEW, documentReference);

        XWikiDocument document;
        try {
            XWikiContext context = this.xWikiContextProvider.get();
            document = context.getWiki().getDocument(documentReference, context);
        } catch (XWikiException e) {
            throw new RequiredRightsException("Failed to load document", e);
        }

        return this.requiredRightAnalyzer.analyze(document);
    }

    /**
     * Suggests operations to change the required rights of the given document.
     *
     * @param document the document to suggest operations for
     * @return the suggested operations
     * @throws AccessDeniedException if the document cannot be accessed
     * @throws RequiredRightsException if there is a problem loading or analyzing the document
     */
    public List<RightOperation> getSuggestedOperations(Document document)
        throws AccessDeniedException, RequiredRightsException
    {
        List<RequiredRightAnalysisResult> requiredRightAnalysisResults =
            analyzeDocument(document.getDocumentReference());

        Set<DocumentRequiredRight> currentRights = document.getRequiredRights().rights();

        // For each analysis result, find if it is covered by one of the current rights. If not, suggest adding a
        // right. Then, consolidate all suggestions to only keep the "highest" right.
        // If there are rights that require manual review, don't consider them for the consolidation but suggest
        // those rights separately.
        // For each current right, find out if it is actually required according to the analysis result. If not,
        // suggest removing it. Also, when, e.g., programming right is currently configured but just script right is
        // required, suggest replacing programming by script right.
        // Start simple: only act on the hierarchy none - script - wiki admin - programming right.
        DocumentRequiredRight configuredRight = currentRights.stream()
            .filter(this::isConsideredRight)
            .reduce(null, SecurityRequiredRightsScriptService::getMorePowerfulRight);

        DocumentRequiredRight definitelyRequiredRight =
            getMostPowerfulConsideredRequiredRight(requiredRightAnalysisResults, false);
        DocumentRequiredRight maybeRequiredRight =
            getMostPowerfulConsideredRequiredRight(requiredRightAnalysisResults, true);

        return getRightOperations(configuredRight, definitelyRequiredRight, maybeRequiredRight);
    }

    private DocumentRequiredRight getMostPowerfulConsideredRequiredRight(
        List<RequiredRightAnalysisResult> requiredRightAnalysisResults, boolean manualReviewNeeded)
    {
        return requiredRightAnalysisResults.stream()
            .flatMap(result -> result.getRequiredRights().stream())
            .filter(result -> result.isManualReviewNeeded() == manualReviewNeeded)
            .map(RequiredRight::toDocumentRequiredRight)
            .filter(this::isConsideredRight)
            .reduce(null, SecurityRequiredRightsScriptService::getMorePowerfulRight);
    }

    private List<RightOperation> getRightOperations(DocumentRequiredRight configuredRight,
        DocumentRequiredRight definitelyRequiredRight, DocumentRequiredRight maybeRequiredRight)
    {
        List<RightOperation> operations = new ArrayList<>();

        // The page definitely needs more rights? Suggest adding that right, replacing the current one.
        if (isNewRightMorePowerFull(configuredRight, definitelyRequiredRight)) {
            operations.add(new RightOperation(true, configuredRight, definitelyRequiredRight,
                false));
        }

        // Suggest adding a possibly required right, but only if it is more powerful than the definitely required right.
        if (isNewRightMorePowerFull(configuredRight, maybeRequiredRight)
            && isNewRightMorePowerFull(definitelyRequiredRight, maybeRequiredRight))
        {
            operations.add(new RightOperation(true, configuredRight, maybeRequiredRight, true));
        }

        // Suggest removing a right, but only if we've currently configured more than is required.
        if (isNewRightMorePowerFull(definitelyRequiredRight, configuredRight)) {
            // If the maybe required right is more powerful than the definitely required right, review is required.
            boolean maybeIsHigherThanDefinitely = isNewRightMorePowerFull(definitelyRequiredRight, maybeRequiredRight);
            operations.add(new RightOperation(false, configuredRight, definitelyRequiredRight,
                maybeIsHigherThanDefinitely));

            // If the maybe required right is higher than the definitely required right but the maybe required right
            // is still lower than the configured right, we can suggest lowering to the maybe required right.
            if (maybeIsHigherThanDefinitely && isNewRightMorePowerFull(maybeRequiredRight, configuredRight)) {
                operations.add(new RightOperation(false, configuredRight, maybeRequiredRight,
                    false));
            }
        }
        return operations;
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
