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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.extension.ExtensionId;
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
        return getDriver().hasElementWithoutWaiting(By.className("paginationFilter")) ? new PaginationFilterPane()
            : null;
    }

    /**
     * @return the number of extensions displayed
     */
    public int getDisplayedResultsCount()
    {
        return getDriver().findElementsWithoutWaiting(By.className("extension-item")).size();
    }

    /**
     * @return the message displayed if there are no search results
     */
    public String getNoResultsMessage()
    {
        String xpath =
            "//div[contains(@class, 'infomessage') and preceding-sibling::div[1][@class = 'extension-search-bar']]";
        List<WebElement> found = getDriver().findElementsWithoutWaiting(By.xpath(xpath));
        return found.size() > 0 ? found.get(0).getText() : null;
    }

    /**
     * Looks for the specified extension on the current results page.
     * 
     * @param name the extension pretty name
     * @param version the extension version
     * @return the pane displaying the specified extension, {@code null} if not found
     */
    public ExtensionPane getExtension(String name, String version)
    {
        String nameAndVersion = name + " " + version;
        By xpath =
            By.xpath("//form[contains(@class, 'extension-item') and descendant::*[contains(@class, "
                + "'extension-title') and normalize-space(.) = '" + nameAndVersion + "']]");
        List<WebElement> found = getDriver().findElements(xpath);
        return found.size() == 1 ? new ExtensionPane(found.get(0)) : null;
    }

    /**
     * Looks for the specified extension on the current results page.
     * 
     * @param extensionId the extension identifier
     * @return the pane displaying the specified extension, {@code null} if not found
     */
    public ExtensionPane getExtension(ExtensionId extensionId)
    {
        return getExtension(extensionId.getId(), extensionId.getVersion().getValue());
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
        By xpath = By.xpath("//form[contains(@class, 'extension-item')][" + position + "]");
        return new ExtensionPane(getDriver().findElement(xpath));
    }
}
