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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.security.authorization.Right;
import org.xwiki.text.XWikiToStringBuilder;

import static java.util.Locale.ROOT;
import static org.xwiki.security.authorization.Right.PROGRAM;
import static org.xwiki.security.authorization.Right.SCRIPT;

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
    private final Set<RequiredRightAnalysisResult> added = new LinkedHashSet<>();

    private final Set<RequiredRightAnalysisResult> removed = new LinkedHashSet<>();

    private final Map<Right, Boolean> addedRights = new HashMap<>();

    private final Map<Right, Boolean> removedRights = new HashMap<>();

    /**
     * Switch to {@code true} once a first element is added to the results.
     */
    private boolean empty = true;

    /**
     * Adds a pair of {@link RequiredRightAnalysisResult} and one of its {@link Right} to the results.
     *
     * @param analysis the {@link RequiredRightAnalysisResult} to add
     * @param right the {@link Right} associated with the analysis
     * @param added {code true} if the right is granted to the current user, {@code false} if the right is granted
     *     to the document author
     * @param manualReviewNeeded {@code true} if manual review is needed for the right, {@code false} otherwise
     * @since 15.10RC1
     */
    public void add(RequiredRightAnalysisResult analysis, Right right, boolean added,
        boolean manualReviewNeeded)
    {
        this.empty = false;
        if (added) {
            add(this.added, analysis, this.addedRights, right, manualReviewNeeded);
        } else {
            add(this.removed, analysis, this.removedRights, right, manualReviewNeeded);
        }

        // If a script or programming right is added to the removedRights collection, we remove the programming rights
        // if it needs manual review, and a script right with no manual review exists. 
        if ((right == SCRIPT || right == PROGRAM)
            && Objects.equals(this.removedRights.get(right), false)
            && Objects.equals(this.removedRights.get(PROGRAM), true))
        {
            this.removedRights.remove(PROGRAM);
        }
    }

    private void add(Set<RequiredRightAnalysisResult> removed, RequiredRightAnalysisResult analysis,
        Map<Right, Boolean> rightsMap, Right right, boolean manualReviewNeeded)
    {
        removed.add(analysis);
        rightsMap.compute(right,
            (r, manualReviewValue) -> (manualReviewValue == null || manualReviewValue) && manualReviewNeeded);
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
     * @return {@code true} if the result is empty, {@code false} otherwise
     * @since 15.10RC1
     */
    public boolean isEmpty()
    {
        return this.empty;
    }

    /**
     * Converts the "added" group into a map, grouped by entity reference.
     *
     * @return a map representation of the "added" group, where each key is an {@link EntityReference} and each value is
     *     a list of {@link RequiredRightAnalysisResult}.
     */
    public Map<EntityReference, Set<RequiredRightAnalysisResult>> getAddedAsMap()
    {
        return makeMap(this.added);
    }

    /**
     * Converts the "removed" group into a map, grouped by entity reference.
     *
     * @return a map representation of the "removed" group, where each key is an {@link EntityReference} and each value
     *     is a list of {@link RequiredRightAnalysisResult}.
     */
    public Map<EntityReference, Set<RequiredRightAnalysisResult>> getRemovedAsMap()
    {
        return makeMap(this.removed);
    }

    /**
     * Returns a map of added rights as keys, and whether they require manual review as values.
     *
     * @return a map of added rights as keys, and whether they require manual review as values
     * @since 15.10RC1
     */
    public Map<Right, Boolean> getAddedRights()
    {
        return this.addedRights;
    }

    /**
     * Returns a map of removed rights as keys, and whether they require manual review as values.
     *
     * @return a map of removed rights as keys, and whether they require manual review as values
     * @since 15.10RC1
     */
    public Map<Right, Boolean> getRemovedRights()
    {
        return this.removedRights;
    }

    /**
     * @return {@code true} if any of the rights require manual review
     * @since 15.10RC1
     */
    public boolean hasRightWithManualReviewNeeded()
    {
        return this.removedRights.containsValue(true) || this.addedRights.containsValue(true);
    }

    /**
     * @return the set of added required rights analysis results
     * @since 15.10RC1
     */
    public Set<RequiredRightAnalysisResult> getAdded()
    {
        return this.added;
    }

    /**
     * @return the set of removed required rights analysis results
     * @since 15.10RC1
     */
    public Set<RequiredRightAnalysisResult> getRemoved()
    {
        return this.removed;
    }

    private static Map<EntityReference, Set<RequiredRightAnalysisResult>> makeMap(Set<RequiredRightAnalysisResult> list)
    {
        Map<EntityReference, Set<RequiredRightAnalysisResult>> map = new HashMap<>();
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
                map.put(entityReference, new LinkedHashSet<>(Set.of(requiredRightAnalysisResult)));
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
            .append(this.empty, that.empty)
            .append(this.added, that.added)
            .append(this.removed, that.removed)
            .append(this.addedRights, that.addedRights)
            .append(this.removedRights, that.removedRights)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.added)
            .append(this.removed)
            .append(this.addedRights)
            .append(this.removedRights)
            .append(this.empty)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("empty", this.empty)
            .append("added", this.added)
            .append("removed", this.removed)
            .append("addedRights", this.addedRights)
            .append("removedRights", this.removedRights)
            .toString();
    }
}
