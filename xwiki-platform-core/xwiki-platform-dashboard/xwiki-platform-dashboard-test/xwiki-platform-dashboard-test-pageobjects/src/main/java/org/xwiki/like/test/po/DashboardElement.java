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
package org.xwiki.like.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents a dashboard (rendered by the dashboard macro), in view or in edit mode.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class DashboardElement extends BaseElement
{
    /**
     * The gadget containers (i.e. the columns of the dashboard), in display order.
     */
    protected static final By COLUMNS = By.cssSelector(".dashboard .gadget-container");

    /**
     * @return the number of columns of the dashboard
     */
    public int getColumnCount()
    {
        return getDriver().findElementsWithoutWaiting(COLUMNS).size();
    }

    /**
     * @return the titles of the gadgets of each column of the dashboard, the columns and the gadgets of each column
     *     being in display order; the titles are returned as written, whatever the case they are displayed with
     */
    public List<List<String>> getGadgetTitles()
    {
        return getDriver().findElementsWithoutWaiting(COLUMNS).stream()
            .map(column -> getDriver().findElementsWithoutWaiting(column, By.cssSelector(".gadget .gadget-title"))
                .stream().map(DashboardElement::getTitle).toList())
            .toList();
    }

    /**
     * @param gadgetTitle the title element of a gadget
     * @return the title text as written, since the edit mode styles the gadget titles in upper case
     */
    protected static String getTitle(WebElement gadgetTitle)
    {
        return gadgetTitle.getDomProperty("textContent").trim();
    }
}
