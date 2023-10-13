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
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;

/**
 * Takes a list of {@link RequiredRightAnalysisResult} and filter them according to a set of {@link DocumentAuthors}.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component(roles = RequiredRightsAddedFilter.class)
@Singleton
public class RequiredRightsAddedFilter
{
    @Inject
    private AuthorizationManager authorizationManager;

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
    public List<RequiredRightAnalysisResult> filter(DocumentAuthors authors,
        List<RequiredRightAnalysisResult> resultList)
    {
        if (resultList == null || resultList.isEmpty()) {
            return List.of();
        }
        DocumentReference userReference = this.contextProvider.get().getUserReference();
        DocumentReference contentAuthorReference = this.userReferenceSerializer.serialize(authors.getContentAuthor());
        DocumentReference effectiveMetadataAuthorReference =
            this.userReferenceSerializer.serialize(authors.getEffectiveMetadataAuthor());
        return resultList.stream().filter(analysis -> {
            EntityReference analyzedEntityReference = analysis.getEntityReference();
            return analysis.getRequiredRights().stream().allMatch(requiredRight -> {
                Right right = requiredRight.getRight();
                EntityType entityType = requiredRight.getEntityType();
                EntityReference extractedEntityReference = analyzedEntityReference.extractReference(entityType);
                DocumentReference authorReference;
                if (analyzedEntityReference.getType() == EntityType.OBJECT
                    || analyzedEntityReference.getType() == EntityType.OBJECT_PROPERTY)
                {
                    authorReference = effectiveMetadataAuthorReference;
                } else {
                    authorReference = contentAuthorReference;
                }
                if (Objects.equals(userReference, authorReference)) {
                    return false;
                }
                return this.authorizationManager.hasAccess(right, userReference, extractedEntityReference)
                    != this.authorizationManager.hasAccess(right, authorReference, extractedEntityReference);
            });
        }).collect(Collectors.toList());
    }
}
