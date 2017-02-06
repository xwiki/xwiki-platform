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
import java.util.List;

import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.stability.Unstable;

/**
 * Represent an extension that may be broken by a refactoring action.
 *
 * @version $Id$
 * @since 9.1RC1
 */
@Unstable
public class ExtensionSelection
{
    private XarInstalledExtension extension;

    private List<EntitySelection> pages = new ArrayList<>();

    public ExtensionSelection(XarInstalledExtension extension)
    {
        this.extension = extension;
    }

    public XarInstalledExtension getExtension()
    {
        return extension;
    }

    public List<EntitySelection> getPages()
    {
        return pages;
    }

    public void selectAllPages(boolean selectAllPages)
    {
        for (EntitySelection page : pages) {
            page.setSelected(selectAllPages);
        }
    }

    public void addPage(EntitySelection entitySelection)
    {
        pages.add(entitySelection);
    }
}
