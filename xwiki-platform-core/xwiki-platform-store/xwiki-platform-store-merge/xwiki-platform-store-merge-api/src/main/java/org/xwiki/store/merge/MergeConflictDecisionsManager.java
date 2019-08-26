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
package org.xwiki.store.merge;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.diff.MergeConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Manage the decisions taken to solve the merge conflicts.
 * For more information about the conflict decision and how they are used, read the documentation of
 * {@link org.xwiki.diff.DiffManager#merge(List, List, List, MergeConfiguration)}.
 *
 * @version $Id$
 * @since 11.8RC1
 */
@Role
@Unstable
public interface MergeConflictDecisionsManager
{
    /**
     * Retrieve the list of {@link ConflictDecision} recorded for the given document and user.
     *
     * @param documentReference the document reference for which to retrieve the decisions.
     * @param userReference the user who made the decisions.
     * @return a list of {@link ConflictDecision} or null if no decision has been recorded for this pair of
     *          document and user.
     */
    List<ConflictDecision> getConflictDecisionList(DocumentReference documentReference,
        EntityReference userReference);

    /**
     * Record a list of decisions for a given document and a given user.
     * This pair document, user is used to properly identify the decisions made.
     *
     * @param conflictDecisionList the decisions to record.
     * @param documentReference the document on which the decisions should apply.
     * @param userReference the user who made the decisions.
     */
    void setConflictDecisionList(List<ConflictDecision> conflictDecisionList, DocumentReference documentReference,
        EntityReference userReference);

    /**
     * Remove a record of decisions.
     *
     * @param documentReference the document for which to remove the record.
     * @param userReference the user who made the decisions.
     */
    void removeConflictDecisionList(DocumentReference documentReference, EntityReference userReference);
}
