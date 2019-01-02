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
package org.xwiki.refactoring.job;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.question.EntitySelection;

/**
 * Represents a question asked to the user when he tries to refactor some pages containing used XClass.
 *
 * @version $Id$
 * @since 10.10RC1
 */
public class XClassBreakingQuestion
{
    /**
     * The map of entities concerned by the refactoring, where the entity reference is the key.
     */
    private Map<EntityReference, EntitySelection> concernedEntities;

    /**
     * The map of objects that might be impacted by a deletion.
     * The key is represented by the local name of the page containing the XClass.
     */
    private Map<EntityReference, Set<EntityReference>> impactedObjects;

    /**
     * The list of pages containing an used XClass.
     */
    private Set<EntitySelection> xclassPages;

    /**
     * The list of pages that does not contain an used XClass.
     */
    private Set<EntitySelection> freePages;

    /**
     * If true, the refactoring is forbidden because the user is not an advanced user.
     */
    private boolean refactoringForbidden;

    /**
     * If true, some objects might be not displayed.
     */
    private boolean objectsPotentiallyHidden;

    /**
     * @param concernedEntities the map of entities concerned by the refactoring
     */
    public XClassBreakingQuestion(Map<EntityReference, EntitySelection> concernedEntities)
    {
        this.concernedEntities = concernedEntities;
        this.impactedObjects = new HashMap<>();
        this.freePages = new HashSet<>();
        this.xclassPages = new HashSet<>();
    }

    /**
     * @param entitySelection an entity to mark as not containing an used XClass
     */
    public void markAsFreePage(EntitySelection entitySelection)
    {
        this.freePages.add(entitySelection);
    }

    /**
     * Mark an entity as containing an used XClass and specify which objects is using it.
     *
     * @param entitySelection The entity containing an used XClass
     * @param documentObjectName The name of the page containing the objects for this XClass
     */
    public void markImpactedObject(EntitySelection entitySelection, EntityReference documentObjectName)
    {
        DocumentReference xclassDocReference = (DocumentReference) entitySelection.getEntityReference();

        // we don't want the locale for the XClass reference
        if (xclassDocReference.getLocale() != null) {
            xclassDocReference = new DocumentReference(xclassDocReference, (Locale) null);
        }

        if (!this.impactedObjects.containsKey(xclassDocReference)) {
            this.impactedObjects.put(xclassDocReference, new LinkedHashSet<>());
            this.xclassPages.add(entitySelection);
        }

        this.impactedObjects.get(xclassDocReference).add(documentObjectName);
    }

    /**
     * @return the map of impacted objects by the refactoring.
     */
    public Map<EntityReference, Set<EntityReference>> getImpactedObjects()
    {
        return impactedObjects;
    }

    /**
     * Unselect all entities.
     */
    public void unselectAll()
    {
        for (EntitySelection entitySelection : concernedEntities.values()) {
            if (entitySelection.getState() == EntitySelection.State.UNKNOWN) {
                entitySelection.setSelected(false);
            }
        }
    }

    /**
     * @return the list of entities containing used XClass.
     */
    public Set<EntitySelection> getXClassPages()
    {
        return xclassPages;
    }

    /**
     * @return the list of entities that do not contain used XClass.
     */
    public Set<EntitySelection> getFreePages()
    {
        return freePages;
    }

    /**
     * @param documents the documents to set as selected
     */
    public void setSelectedDocuments(Set<DocumentReference> documents)
    {
        for (DocumentReference document : documents) {
            EntitySelection entitySelection = concernedEntities.get(document);
            if (entitySelection != null) {
                entitySelection.setSelected(true);
            }
        }
    }

    /**
     * Select all pages that do not belong to any extension.
     */
    public void selectAllFreePages()
    {
        for (EntitySelection entitySelection : freePages) {
            entitySelection.setSelected(true);
        }
    }

    /**
     * Same as {@link #selectAllFreePages()} but compatible with properties system.
     *
     * @param select true if all free pages should be selected
     */
    public void setSelectAllFreePages(boolean select)
    {
        if (select) {
            selectAllFreePages();
        }
    }

    /**
     * @return true if the refactoring is forbidden (default for simple users).
     */
    public boolean isRefactoringForbidden()
    {
        return refactoringForbidden;
    }

    /**
     * Specify if the refactoring should be doable by the user or not.
     * @param refactoringForbidden true means the refactoring will be forbidden.
     */
    public void setRefactoringForbidden(boolean refactoringForbidden)
    {
        this.refactoringForbidden = refactoringForbidden;
    }

    /**
     * @return true if there might have objects not retrieved because the request limit is reached.
     */
    public boolean isObjectsPotentiallyHidden()
    {
        return objectsPotentiallyHidden;
    }

    /**
     * Specify if the request limit has been reached.
     * @param objectsPotentiallyHidden true means that a supplementary warning will be displayed to specify that some
     *  impacted objects may not be displayed.
     */
    public void setObjectsPotentiallyHidden(boolean objectsPotentiallyHidden)
    {
        this.objectsPotentiallyHidden = objectsPotentiallyHidden;
    }

    /**
     * @return the entities involved in the refactoring.
     */
    public Map<EntityReference, EntitySelection> getConcernedEntities()
    {
        return concernedEntities;
    }
}
