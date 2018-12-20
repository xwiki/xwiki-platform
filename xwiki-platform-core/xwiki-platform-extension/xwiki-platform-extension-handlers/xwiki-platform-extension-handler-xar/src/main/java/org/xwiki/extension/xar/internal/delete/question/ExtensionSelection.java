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
import java.util.List;

import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.refactoring.job.question.EntitySelection;

/**
 * Represent an extension that may be broken by a refactoring action.
 *
 * @version $Id$
 * @since 9.1RC1
 */
public class ExtensionSelection
{
    /**
     * The XAR extension to select.
     */
    private XarInstalledExtension extension;

    /**
     * The pages that belong to that extension.
     */
    private List<EntitySelection> pages = new ArrayList<>();

    /**
     * Construct an ExtensionSelection.
     * @param extension the extension concerned by the refactoring
     */
    public ExtensionSelection(XarInstalledExtension extension)
    {
        this.extension = extension;
    }

    /**
     * @return the extension to select
     */
    public XarInstalledExtension getExtension()
    {
        return extension;
    }

    /**
     * @return the pages that belong to the extension
     */
    public List<EntitySelection> getPages()
    {
        return pages;
    }

    /**
     * Select all pages that belong to the extension.
     */
    public void selectAllPages()
    {
        for (EntitySelection page : pages) {
            page.setSelected(true);
        }
    }

    /**
     * Add a page that belong to the extension (should only be used by ExtensionBreakingExtension that also make sure
     * there is only one entity selection per page).
     * @param entitySelection entity selection of the page to add
     */
    protected void addPage(EntitySelection entitySelection)
    {
        pages.add(entitySelection);
    }
}
