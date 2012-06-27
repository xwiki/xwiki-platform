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
package org.xwiki.extension.test.po;

import org.openqa.selenium.By;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the extension search results.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class SearchResultsPane extends BaseElement
{
    /**
     * @return the search results pagination
     */
    public PaginationFilterPane getPagination()
    {
        return getUtil().findElementsWithoutWaiting(getDriver(), By.className("paginationFilter")).size() > 0
            ? new PaginationFilterPane() : null;
    }

    /**
     * @return the number of extensions displayed
     */
    public int getDisplayedResultsCount()
    {
        return getUtil().findElementsWithoutWaiting(getDriver(), By.className("extension-item")).size();
    }

    /**
     * @return the message displayed if there are no search results
     */
    public String getNoResultsMessage()
    {
        return getUtil().findElementWithoutWaiting(
            getDriver(),
            By.xpath("//div[contains(@class, 'box') and "
                + "preceding-sibling::div[1][@class = 'extension-search-bar']]")).getText();
    }

    /**
     * Looks for the specified extension on the current results page.
     * 
     * @param name the extension pretty name
     * @param version the extension version
     * @return the pane displaying the specified extension
     */
    public ExtensionPane getExtension(String name, String version)
    {
        String nameAndVersion = name + " " + version;
        By xpath =
            By.xpath("//div[contains(@class, 'extension-item') and "
                + "descendant::h2[contains(@class, 'extension-name') and . = '" + nameAndVersion + "']]");
        return new ExtensionPane(getUtil().findElementWithoutWaiting(getDriver(), xpath));
    }

    /**
     * Looks for the extension with the specified index in the search results.
     * 
     * @param index the 0-based index of the extension in the results
     * @return the pane displaying the specified extension
     */
    public ExtensionPane getExtension(int index)
    {
        int position = index + 1;
        By xpath = By.xpath("//div[contains(@class, 'extension-item')][" + position + "]");
        return new ExtensionPane(getUtil().findElementWithoutWaiting(getDriver(), xpath));
    }
}
