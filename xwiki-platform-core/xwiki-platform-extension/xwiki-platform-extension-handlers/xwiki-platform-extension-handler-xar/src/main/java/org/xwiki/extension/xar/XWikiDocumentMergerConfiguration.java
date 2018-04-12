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
package org.xwiki.extension.xar;

import java.util.EnumMap;
import java.util.Map;

import org.xwiki.extension.xar.question.ConflictQuestion.ConflictType;
import org.xwiki.extension.xar.question.ConflictQuestion.GlobalAction;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;
import org.xwiki.xar.XarEntryType.UpgradeType;

/**
 * @version $Id$
 * @since 10.3RC1
 */
@Unstable
public class XWikiDocumentMergerConfiguration
{
    private final Map<ConflictType, GlobalAction> conflictActions = new EnumMap<>(ConflictType.class);

    private DocumentReference authorReference;

    private UpgradeType type;

    /**
     * Default constructor.
     */
    public XWikiDocumentMergerConfiguration()
    {
        // Default behavior
        setConflictAction(ConflictType.CURRENT_DELETED, GlobalAction.CURRENT);
        setConflictAction(ConflictType.MERGE_SUCCESS, GlobalAction.MERGED);
    }

    /**
     * @return the reference of the user to set as document author
     */
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    /**
     * @param author the reference of the user to set as document author
     */
    public void setAuthorReference(DocumentReference author)
    {
        this.authorReference = author;
    }

    /**
     * @param type the type of conflict
     * @return the action associated to the passed conflict type
     */
    public GlobalAction getConflictAction(ConflictType type)
    {
        return this.conflictActions.get(type);
    }

    /**
     * @param type the type of conflict
     * @param action the action associated to the passed conflict type
     */
    public void setConflictAction(ConflictType type, GlobalAction action)
    {
        setConflictAction(type, action, action);
    }

    /**
     * @param type the type of conflict
     * @param action the action associated to the passed conflict type
     * @param defaultAnswer the action associated to the passed conflict type (if {@code action} is null)
     */
    public void setConflictAction(ConflictType type, GlobalAction action, GlobalAction defaultAnswer)
    {
        this.conflictActions.put(type, action != null ? action : defaultAnswer);
    }

    /**
     * @return the mapping between type of conflict and action
     */
    public Map<ConflictType, GlobalAction> getConflictActions()
    {
        return this.conflictActions;
    }

    /**
     * @param conflictActions the mapping between type of conflict and action
     */
    public void setConflictActions(Map<ConflictType, GlobalAction> conflictActions)
    {
        this.conflictActions.clear();
        this.conflictActions.putAll(conflictActions);
    }

    /**
     * @return the type of upgrade associated with the document
     */
    public UpgradeType getType()
    {
        return this.type;
    }

    /**
     * @param type the type of upgrade associated with the document
     */
    public void setType(UpgradeType type)
    {
        this.type = type;
    }
}
