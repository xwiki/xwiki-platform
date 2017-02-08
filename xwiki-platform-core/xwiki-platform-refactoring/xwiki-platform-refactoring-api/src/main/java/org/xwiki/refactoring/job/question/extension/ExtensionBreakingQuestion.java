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
package org.xwiki.refactoring.job.question.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.question.EntityQuestion;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.stability.Unstable;

/**
 * @version $Id$
 * @since 9.1RC1
 */
@Unstable
public class ExtensionBreakingQuestion extends EntityQuestion
{
    /**
     * Map of extensions to select, where the extension id is the key.
     */
    private Map<String, ExtensionSelection> extensions = new HashMap<>();

    /**
     * List of pages that do not belong to any extensions.
     */
    private List<EntitySelection> freePages = new ArrayList<>();

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
     * Select all pages that do not belong to any extension.
     */
    public void selectAllFreePages()
    {
        for (EntitySelection entitySelection : freePages) {
            entitySelection.setSelected(true);
        }
    }

    /**
     * Add the reference of a page concerned by the refactoring action.
     * @param entityReference reference of the page
     */
    public void addFreePage(EntityReference entityReference)
    {
        freePages.add(addEntity(entityReference));
    }

    /**
     * Add the reference of a page concerned by the refactoring action, that belong to a particular extension.
     * @param extension extension that contain the page
     * @param pageReference reference of the page
     */
    public void addPageFromExtension(XarInstalledExtension extension, EntityReference pageReference)
    {
        ExtensionSelection extensionSelection = getExtension(extension.getId().getId());
        if (extensionSelection == null) {
            extensionSelection = new ExtensionSelection(extension);
            extensions.put(extension.getId().getId(), extensionSelection);
        }
        extensionSelection.addPage(this.addEntity(pageReference));
    }

    /**
     * @param extensionId id of the extension
     * @return the ExtensionSelection corresponding to the extension, or null of the extension is not concerned y the
     *   refactoring action
     */
    public ExtensionSelection getExtension(String extensionId)
    {
        return extensions.get(extensionId);
    }
}
