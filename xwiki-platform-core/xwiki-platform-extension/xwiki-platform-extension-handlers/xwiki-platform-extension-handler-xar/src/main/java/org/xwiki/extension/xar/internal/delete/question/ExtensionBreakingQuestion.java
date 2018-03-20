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
package org.xwiki.extension.xar.internal.delete.question;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.stability.Unstable;

/**
 * @version $Id$
 * @since 9.1RC1
 */
@Unstable
public class ExtensionBreakingQuestion
{
    /**
     * The map of entities concerned by the refactoring, where the entity reference if the key.
     */
    private Map<EntityReference, EntitySelection> concernedEntities;

    /**
     * Map of extensions to select, where the extension id is the key.
     */
    private Map<String, ExtensionSelection> extensions = new HashMap<>();

    /**
     * List of pages that do not belong to any extensions.
     */
    private List<EntitySelection> freePages = new ArrayList<>();

    /**
     * Construct an ExtensionBreakingQuestion.
     * 
     * @param concernedEntities the entities concerned by the refactoring
     */
    public ExtensionBreakingQuestion(Map<EntityReference, EntitySelection> concernedEntities)
    {
        this.concernedEntities = concernedEntities;
    }

    /**
     * @param entityReference the reference of an entity
     * @return the EntitySelection corresponding to the entity, or null if the entity is not concerned by the
     *         refactoring
     */
    public EntitySelection get(EntityReference entityReference)
    {
        return concernedEntities.get(entityReference);
    }

    /**
     * @return the map of entities concerned by the refactoring, where the entity reference if the key.
     */
    public Map<EntityReference, EntitySelection> getConcernedEntities()
    {
        return concernedEntities;
    }

    /**
     * @return the map of extensions to select, where the extension id is the key
     */
    public Map<String, ExtensionSelection> getExtensions()
    {
        return extensions;
    }

    /**
     * @return pages that do not belong to any extension
     */
    public List<EntitySelection> getFreePages()
    {
        return freePages;
    }

    /**
     * Select all the pages from all extensions.
     */
    public void selectAllExtensions()
    {
        for (ExtensionSelection extension : extensions.values()) {
            extension.selectAllPages();
        }
    }

    /**
     * Same as {@link #selectAllExtensions()} but compatible with properties system.
     * 
     * @param select true if all extensions should be selected
     * @since 10.2RC1
     */
    public void setSelectAllExtensions(boolean select)
    {
        if (select) {
            selectAllExtensions();
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
     * @param entitySelection entity selection of a page that do not belong to any extension
     */
    public void markAsFreePage(EntitySelection entitySelection)
    {
        freePages.add(entitySelection);
    }

    /**
     * Mark an entity selection representing a page that it belongs to an extension.
     * 
     * @param entitySelection entity selection of the page
     * @param extension extension that contain the page
     */
    public void pageBelongsToExtension(EntitySelection entitySelection, XarInstalledExtension extension)
    {
        ExtensionSelection extensionSelection = getExtension(extension.getId().getId());
        if (extensionSelection == null) {
            extensionSelection = new ExtensionSelection(extension);
            extensions.put(extension.getId().getId(), extensionSelection);
        }
        extensionSelection.addPage(entitySelection);
    }

    /**
     * @param extensionId id of the extension
     * @return the ExtensionSelection corresponding to the extension, or null of the extension is not concerned y the
     *         refactoring action
     */
    public ExtensionSelection getExtension(String extensionId)
    {
        return extensions.get(extensionId);
    }

    /**
     * Unselect all entities.
     */
    public void unselectAll()
    {
        for (EntitySelection entitySelection : concernedEntities.values()) {
            entitySelection.setSelected(false);
        }
    }

    /**
     * @param extensionIds the ids of the extensions to select
     * @since 10.2RC1
     */
    public void setSelectedExtensions(Set<String> extensionIds)
    {
        for (String extensionId : extensionIds) {
            ExtensionSelection extensionSelection = getExtension(extensionId);
            if (extensionSelection != null) {
                extensionSelection.selectAllPages();
            }
        }
    }

    /**
     * @param documents the documents to set as selected
     * @since 10.2RC1
     */
    public void setSelectedDocuments(Set<DocumentReference> documents)
    {
        for (DocumentReference document : documents) {
            EntitySelection entitySelection = concernedEntities.get(document);
            entitySelection.setSelected(true);
        }
    }
}
