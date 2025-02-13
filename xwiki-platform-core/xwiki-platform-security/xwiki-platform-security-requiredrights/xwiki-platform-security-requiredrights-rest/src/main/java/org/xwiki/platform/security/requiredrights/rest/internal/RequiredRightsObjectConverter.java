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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AbstractLocalizedEntityReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.requiredrights.rest.model.jaxb.AvailableRight;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRight;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRightsAnalysisResult;
import org.xwiki.security.requiredrights.rest.model.jaxb.ObjectFactory;
import org.xwiki.security.requiredrights.rest.model.jaxb.RequiredRight;
import org.xwiki.security.requiredrights.rest.model.jaxb.RequiredRightAnalysisResult;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Convert required rights objects to the REST API data types.
 *
 * @version $Id$
 * @since 17.1.0RC1
 */
@Component(roles = RequiredRightsObjectConverter.class)
@Singleton
public class RequiredRightsObjectConverter
{
    private static final List<org.xwiki.security.authorization.requiredrights.DocumentRequiredRight>
        CONSIDERED_RIGHTS = List.of(
        new org.xwiki.security.authorization.requiredrights.DocumentRequiredRight(null, EntityType.DOCUMENT),
        new org.xwiki.security.authorization.requiredrights.DocumentRequiredRight(Right.SCRIPT, EntityType.DOCUMENT),
        new org.xwiki.security.authorization.requiredrights.DocumentRequiredRight(Right.ADMIN, EntityType.WIKI),
        new org.xwiki.security.authorization.requiredrights.DocumentRequiredRight(Right.PROGRAM, null)
    );

    // TODO: localize (and make non-static again).
    private static final List<String> DISPLAY_NAMES = List.of("None", "Script", "Wiki Admin", "Programming");

    @Inject
    @Named("html/5.0")
    private BlockRenderer htmlRenderer;

    @Inject
    @Named("withtype")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    private final ObjectFactory factory = new ObjectFactory();

    /**
     * Convert the required rights objects to the REST API data types.
     *
     * @param currentRights the current required rights of the document
     * @param analysisResults the result of the required rights analysis
     * @param documentReference the reference of the considered document
     * @return the REST API data type
     */
    public DocumentRightsAnalysisResult toDocumentRightsAnalysisResult(
        org.xwiki.security.authorization.requiredrights.DocumentRequiredRights currentRights,
        List<org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult> analysisResults,
        DocumentReference documentReference)
    {
        org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights jaxbDocumentRights =
            convertDocumentRequiredRights(currentRights);

        List<AvailableRight> availableRights = computeAvailableRights(analysisResults, documentReference);

        List<org.xwiki.security.requiredrights.rest.model.jaxb.RequiredRightAnalysisResult> jaxbAnalysisResults =
            convertRequiredRightAnalysisResults(analysisResults);

        return this.factory.createDocumentRightsAnalysisResult()
            .withAnalysisResults(jaxbAnalysisResults)
            .withCurrentRights(jaxbDocumentRights)
            .withAvailableRights(availableRights);
    }

    private List<RequiredRightAnalysisResult> convertRequiredRightAnalysisResults(
        List<org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult> analysisResults)
    {
        return analysisResults.stream()
            .map(analysisResult -> {
                String summaryHTML = getHTML(analysisResult.getSummaryMessage());
                String detailsHTML = getHTML(analysisResult.getDetailedMessage());

                String locale;
                if (analysisResult.getEntityReference()
                    instanceof AbstractLocalizedEntityReference localizedEntityReference
                    && localizedEntityReference.getLocale() != null)
                {
                    locale = localizedEntityReference.getLocale().toString();
                } else {
                    locale = null;
                }
                return this.factory.createRequiredRightAnalysisResult()
                    .withSummaryMessageHTML(summaryHTML)
                    .withDetailedMessageHTML(detailsHTML)
                    .withEntityReference(
                        this.entityReferenceSerializer.serialize(analysisResult.getEntityReference()))
                    .withLocale(locale)
                    .withRequiredRights(
                        analysisResult.getRequiredRights().stream().map(this::mapRequiredRight).toList());
            })
            .toList();
    }

    private List<AvailableRight> computeAvailableRights(
        List<org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult> analysisResults,
        DocumentReference documentReference)
    {
        int maximumRequiredRight = 0;
        Set<Integer> maybeRequiredRights = new HashSet<>();
        for (org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult analysisResult : analysisResults) {
            for (org.xwiki.platform.security.requiredrights.RequiredRight requiredRight
                : analysisResult.getRequiredRights()) {
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
            org.xwiki.security.authorization.requiredrights.DocumentRequiredRight consideredRight =
                CONSIDERED_RIGHTS.get(i);
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
                .withDisplayName(DISPLAY_NAMES.get(i))
            );
        }

        return availableRights;
    }

    private static int getIndexInConsideredRights(
        org.xwiki.platform.security.requiredrights.RequiredRight requiredRight)
    {
        int index = -1;
        // Find the first considered right where scope and right match.
        for (int i = 0; i < CONSIDERED_RIGHTS.size(); i++) {
            org.xwiki.security.authorization.requiredrights.DocumentRequiredRight consideredRight =
                CONSIDERED_RIGHTS.get(i);
            if (consideredRight.right() == requiredRight.getRight()
                && (consideredRight.scope() == requiredRight.getEntityType())) {
                index = i;
                break;
            }
        }
        return index;
    }

    private DocumentRequiredRights convertDocumentRequiredRights(
        org.xwiki.security.authorization.requiredrights.DocumentRequiredRights currentRights)
    {
        return this.factory.createDocumentRequiredRights()
            .withEnforce(currentRights.enforce())
            .withRights(
                currentRights.rights()
                    .stream()
                    .map(this::mapDocumentRight)
                    .toList()
            );
    }

    private DocumentRequiredRight mapDocumentRight(
        org.xwiki.security.authorization.requiredrights.DocumentRequiredRight right)
    {
        if (right == null) {
            return null;
        } else {
            return this.factory.createDocumentRequiredRight()
                .withRight(right.right().toString())
                .withScope(right.scope() != null ? right.scope().toString() : null);
        }
    }

    private RequiredRight mapRequiredRight(
        org.xwiki.platform.security.requiredrights.RequiredRight right)
    {
        return this.factory.createRequiredRight()
            .withRight(right.getRight().toString())
            .withEntityType(right.getEntityType() != null ? right.getEntityType().toString() : null)
            .withManualReviewNeeded(right.isManualReviewNeeded());
    }

    private String getHTML(Block block)
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        this.htmlRenderer.render(block, printer);
        return printer.toString();
    }

}
