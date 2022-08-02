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
package org.xwiki.index.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Represents the livetable for index all docs.
 *
 * @since 11.6RC1
 * @since 11.5
 * @version $Id$
 */
public class AllDocsLivetable extends LiveTableElement
{
    public AllDocsLivetable()
    {
        super("alldocs");
    }

    public void filterColumn(int columnNumber, String filterValue)
    {
        String columnId = String.format("xwiki-livetable-alldocs-filter-%s", columnNumber);
        this.filterColumn(columnId, filterValue);
    }

    public void clickAction(int rowNumber, String actionName)
    {
        WebElement pageRow = this.getRow(rowNumber);
        String xPathLocator = String.format("//a[contains(@class, 'action%s')]", actionName);
        pageRow.findElement(By.xpath(xPathLocator)).click();
    }
}
