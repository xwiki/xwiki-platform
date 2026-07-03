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

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;

import static org.xwiki.model.EntityType.OBJECT;
import static org.xwiki.model.EntityType.OBJECT_PROPERTY;

/**
 * Takes a list of {@link RequiredRightAnalysisResult} and filter them according to a set of {@link DocumentAuthors}.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component(roles = RequiredRightsChangedFilter.class)
@Singleton
public class RequiredRightsChangedFilter
{
    private static final Set<EntityType> OBJECT_ENTITY_TYPES = Set.of(OBJECT, OBJECT_PROPERTY);

    @Inject
    private DocumentAuthorizationManager authorizationManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    /**
     * Takes a list of {@link RequiredRightAnalysisResult} and filter them according to a set of
     * {@link DocumentAuthors}.
     *
     * @param authors the document authors to use for the filtering
     * @param resultList the list of required rights analysis result to filter
     * @return the filtered list of required rights results, according to the provided document authors
     */
    public RequiredRightsChangedResult filter(DocumentAuthors authors, List<RequiredRightAnalysisResult> resultList)
    {
        RequiredRightsChangedResult requiredRightsChangedResult = new RequiredRightsChangedResult();
        if (resultList == null || resultList.isEmpty()) {
            return requiredRightsChangedResult;
        }
        DocumentReference userReference = this.contextProvider.get().getUserReference();
        DocumentReference contentAuthorReference = this.userReferenceSerializer.serialize(authors.getContentAuthor());
        DocumentReference effectiveMetadataAuthorReference =
            this.userReferenceSerializer.serialize(authors.getEffectiveMetadataAuthor());
        for (RequiredRightAnalysisResult analysis : resultList) {
            EntityReference analyzedEntityReference = analysis.getEntityReference();
            for (RequiredRight requiredRight : analysis.getRequiredRights()) {
                Right right = requiredRight.getRight();
                EntityType entityType = requiredRight.getEntityType();
                EntityReference documentEntityReference = analyzedEntityReference.extractReference(EntityType.DOCUMENT);
                DocumentReference documentReference = new DocumentReference(documentEntityReference);
                DocumentReference authorReference;
                if (OBJECT_ENTITY_TYPES.contains(analyzedEntityReference.getType())) {
                    authorReference = effectiveMetadataAuthorReference;
                } else {
                    authorReference = contentAuthorReference;
                }
                if (!Objects.equals(userReference, authorReference)) {
                    boolean currentUserHasAccess = this.authorizationManager.hasAccess(right, entityType, userReference,
                        documentReference);
                    boolean authorHasAccess = this.authorizationManager.hasAccess(right, entityType, authorReference,
                        documentReference);
                    if (currentUserHasAccess != authorHasAccess) {
                        requiredRightsChangedResult.add(analysis, right, currentUserHasAccess,
                            requiredRight.isManualReviewNeeded());
                    }
                }
            }
        }

        return requiredRightsChangedResult;
    }
}
