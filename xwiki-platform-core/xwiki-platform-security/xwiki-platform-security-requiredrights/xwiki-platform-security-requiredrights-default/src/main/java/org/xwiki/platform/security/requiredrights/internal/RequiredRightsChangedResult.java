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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.text.XWikiToStringBuilder;

import static java.util.Locale.ROOT;

/**
 * Represent the results of a {@link RequiredRightsChangedFilter} result. The remaining
 * {@link RequiredRightAnalysisResult} are grouped in two categories, "added" for the rights that would be additionally
 * granted to the page if the current user becomes the author, and "removed" for the rights that would be lost if the
 * current user becomes the author
 *
 * @version $Id$
 * @since 15.9RC1
 */
public class RequiredRightsChangedResult
{
    private final List<RequiredRightAnalysisResult> added = new ArrayList<>();

    private final List<RequiredRightAnalysisResult> removed = new ArrayList<>();

    /**
     * Adds a {@link RequiredRightAnalysisResult} to the added list.
     *
     * @param analysis The {@link RequiredRightAnalysisResult} to be added
     */
    public void addToAdded(RequiredRightAnalysisResult analysis)
    {
        this.added.add(analysis);
    }

    /**
     * Adds a {@link RequiredRightAnalysisResult} to the removed list.
     *
     * @param analysis The {@link RequiredRightAnalysisResult} to be added
     */
    public void addToRemoved(RequiredRightAnalysisResult analysis)
    {
        this.removed.add(analysis);
    }

    /**
     * @return {@code true} if there are any result in the added group, {@code false} otherwise
     */
    public boolean hasAdded()
    {
        return !this.added.isEmpty();
    }

    /**
     * @return {@code true} if there are any result in the removed group, {@code false} otherwise
     */
    public boolean hasRemoved()
    {
        return !this.removed.isEmpty();
    }

    /**
     * Converts the "added" group into a map, grouped by entity reference.
     *
     * @return a map representation of the "added" group, where each key is an {@link EntityReference} and each value is
     *     a list of {@link RequiredRightAnalysisResult}.
     */
    public Map<EntityReference, List<RequiredRightAnalysisResult>> getAddedAsMap()
    {
        return makeMap(this.added);
    }

    /**
     * Converts the "removed" group into a map, grouped by entity reference.
     *
     * @return a map representation of the "removed" group, where each key is an {@link EntityReference} and each value
     *     is a list of {@link RequiredRightAnalysisResult}.
     */
    public Map<EntityReference, List<RequiredRightAnalysisResult>> getRemovedAsMap()
    {
        return makeMap(this.removed);
    }

    private static Map<EntityReference, List<RequiredRightAnalysisResult>> makeMap(
        List<RequiredRightAnalysisResult> list)
    {
        Map<EntityReference, List<RequiredRightAnalysisResult>> map = new HashMap<>();
        for (RequiredRightAnalysisResult requiredRightAnalysisResult : list) {
            EntityReference entityReference = requiredRightAnalysisResult.getEntityReference();
            EntityReference extractedDocument = entityReference.extractReference(EntityType.DOCUMENT);
            if (extractedDocument instanceof DocumentReference && !extractedDocument.equals(entityReference)) {
                entityReference = entityReference.replaceParent(extractedDocument,
                    cleanupLocale((DocumentReference) extractedDocument));
            } else if (entityReference instanceof DocumentReference) {
                entityReference = cleanupLocale((DocumentReference) entityReference);
            }
            if (map.containsKey(entityReference)) {
                map.get(entityReference).add(requiredRightAnalysisResult);
            } else {
                map.put(entityReference, new ArrayList<>(List.of(requiredRightAnalysisResult)));
            }
        }
        return map;
    }

    private static DocumentReference cleanupLocale(DocumentReference entityReference)
    {
        DocumentReference documentReference = entityReference;
        if (documentReference.getLocale() == null || documentReference.getLocale().equals(ROOT)) {
            documentReference = documentReference.withoutLocale();
        }
        return documentReference;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RequiredRightsChangedResult that = (RequiredRightsChangedResult) o;

        return new EqualsBuilder()
            .append(this.added, that.added)
            .append(this.removed, that.removed)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.added)
            .append(this.removed)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("added", this.added)
            .append("removed", this.removed)
            .toString();
    }
}
