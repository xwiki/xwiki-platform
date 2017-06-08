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
package org.xwiki.test.wysiwyg.framework;

import org.openqa.selenium.By;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * The list of suggestions provided by the tree finder.
 * 
 * @version $Id$
 */
public class TreeSuggestionsPane
{
    private final XWikiWebDriver driver;

    /**
     * Creates a new instance that uses the given web driver.
     * 
     * @param driver the web driver
     */
    public TreeSuggestionsPane(XWikiWebDriver driver)
    {
        this.driver = driver;
    }

    /**
     * Select the space with the given name from the list of suggestions.
     * 
     * @param spaceName the space name
     */
    public void selectSpace(String spaceName)
    {
        select(spaceName, "space");
    }

    /**
     * Select the page with the given title from the list of suggestions.
     * 
     * @param pageTitle the page title
     */
    public void selectPage(String pageTitle)
    {
        select(pageTitle, "document");
    }

    /**
     * Select the attachment with the given name from the list of suggestions.
     * 
     * @param fileName the attachment file name
     */
    public void selectAttachment(String fileName)
    {
        select(fileName, "attachment");
    }

    /**
     * Select the suggestion with the given value and type.
     * 
     * @param value the suggestion value
     * @param type the suggestion type
     */
    private void select(String value, String type)
    {
        this.driver.findElementByXPath(getSuggestionSelector(value, type)).click();
    }

    /**
     * @param spaceName the space name
     * @return {@code true} if there is a suggestion matching the given space name, {@code false} otherwise
     */
    public boolean hasSpace(String spaceName)
    {
        return hasSuggestion(spaceName, "space");
    }

    /**
     * @param pageTitle the page title
     * @return {@code true} if there is a suggestion matching the given page title, {@code false} otherwise
     */
    public boolean hasPage(String pageTitle)
    {
        return hasSuggestion(pageTitle, "document");
    }

    /**
     * @param fileName the file name
     * @return {@code true} if there is a suggestion matching the given file name, {@code false} otherwise
     */
    public boolean hasAttachment(String fileName)
    {
        return hasSuggestion(fileName, "attachment");
    }

    /**
     * @param value the suggestion value
     * @param type the suggestion type
     * @return {@code true} if there is a suggestion with the given value and type
     */
    private boolean hasSuggestion(String value, String type)
    {
        return this.driver.hasElementWithoutWaiting(By.xpath(getSuggestionSelector(value, type)));
    }

    private String getSuggestionSelector(String value, String type)
    {
        return String.format("//div[contains(@class, 'xtree-finder-suggestions')]"
            + "//div[contains(@class, 'suggestItem') and contains(@class, '%s')]"
            + "//div[@class = 'value' and contains(., '%s')]", type, value);
    }
}
