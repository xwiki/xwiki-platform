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
package org.xwiki.test.ui.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the actions possible on the XWiki Explorer Tree.
 * 
 * @version $Id$
 * @since 5.1M1
 */
public class EntityTreeElement extends BaseElement
{
    /**
     * The text input below the tree, used to search for entities.
     */
    @FindBy(xpath = "//div[@class = 'listGrid']/following-sibling::input")
    private WebElement input;

    /**
     * Waits for the node corresponding to the specified space to be present but not selected.
     * 
     * @param spaceName the space name
     * @return this
     */
    public EntityTreeElement waitForSpace(String spaceName)
    {
        return waitForSpace(spaceName, false);
    }

    /**
     * Waits for the node corresponding to the specified space to be present.
     * 
     * @param spaceName the space name
     * @param selected {@code true} if the space node should be selected, {@code false} otherwise
     * @return this
     */
    public EntityTreeElement waitForSpace(String spaceName, boolean selected)
    {
        return waitForNodeWithHintAndLabel("Located in xwiki \u00BB " + spaceName, spaceName, selected);
    }

    /**
     * @param spaceName the space name
     * @return {@code true} if the node corresponding to the specified space is present (selected or not), {@code false}
     *         otherwise
     */
    public boolean hasSpace(String spaceName)
    {
        return hasNodeWithHintAndLabel("Located in xwiki \u00BB " + spaceName, spaceName);
    }

    /**
     * Waits for the node corresponding to the specified page to be present but not selected. We assume the page title
     * equals the page name.
     * 
     * @param spaceName the space containing the page
     * @param pageName the page name
     * @return this
     */
    public EntityTreeElement waitForPage(String spaceName, String pageName)
    {
        return waitForPage(spaceName, pageName, false);
    }

    /**
     * Waits for the node corresponding to the specified page to be present but not selected.
     * 
     * @param spaceName the space containing the page
     * @param pageName the page name (used as the node tool tip)
     * @param pageTitle the page title (used as the node label)
     * @return this
     */
    public EntityTreeElement waitForPage(String spaceName, String pageName, String pageTitle)
    {
        return waitForPage(spaceName, pageName, pageTitle, false);
    }

    /**
     * Waits for the node corresponding to the specified page to be present. We assume the page title equals the page
     * name.
     * 
     * @param spaceName the space containing the page
     * @param pageName the page name
     * @param selected {@code true} if the page node should be selected, {@code false} otherwise
     * @return this
     */
    public EntityTreeElement waitForPage(String spaceName, String pageName, boolean selected)
    {
        return waitForPage(spaceName, pageName, pageName, selected);
    }

    /**
     * Waits for the node corresponding to the specified page to be present.
     * 
     * @param spaceName the space containing the page
     * @param pageName the page name (used as the node hint)
     * @param pageTitle the page title (used as the node label)
     * @param selected {@code true} if the page node should be selected, {@code false} otherwise
     * @return this
     */
    public EntityTreeElement waitForPage(String spaceName, String pageName, String pageTitle, boolean selected)
    {
        return waitForNodeWithHintAndLabel(String.format("Located in xwiki \u00BB %s \u00BB %s", spaceName, pageName),
            pageTitle, selected);
    }

    /**
     * @param spaceName the space containing the page
     * @param pageName the page name
     * @return {@code true} if the specified page node is present, selected or not, {@code false} otherwise; we assume
     *         the page title equals the page name
     */
    public boolean hasPage(String spaceName, String pageName)
    {
        return hasPage(spaceName, pageName, pageName);
    }

    /**
     * @param spaceName the space containing the page
     * @param pageName the page name (used as the node hint)
     * @param pageTitle the page title (used as the node label)
     * @return {@code true} if the specified page node is present, selected or not, {@code false} otherwise
     */
    public boolean hasPage(String spaceName, String pageName, String pageTitle)
    {
        return hasNodeWithHintAndLabel(String.format("Located in xwiki \u00BB %s \u00BB %s", spaceName, pageName),
            pageTitle);
    }

    /**
     * Waits for the node corresponding to the specified attachments to be present but not selected.
     * 
     * @param spaceName the space containing the page
     * @param pageName the page that holds the attachment
     * @param fileName the attachment file name
     * @return this
     */
    public EntityTreeElement waitForAttachment(String spaceName, String pageName, String fileName)
    {
        return waitForAttachment(spaceName, pageName, fileName, false);
    }

    /**
     * Waits for the node corresponding to the specified attachment to be present.
     * 
     * @param spaceName the space containing the page
     * @param pageName the page that holds the attachment
     * @param fileName the attachment file name
     * @param selected {@code true} if the attachment node should be selected, {@code false} otherwise
     * @return this
     */
    public EntityTreeElement waitForAttachment(String spaceName, String pageName, String fileName, boolean selected)
    {
        return waitForNodeWithHintAndLabel(String.format("Attached to xwiki \u00BB %s \u00BB %s", spaceName, pageName),
            fileName, selected);
    }

    /**
     * @param spaceName the space containing the page
     * @param pageName the page that holds the attachment
     * @param fileName the attachment file name
     * @return {@code true} if the specified attachment node is present, selected or not, {@code false} otherwise
     */
    public boolean hasAttachment(String spaceName, String pageName, String fileName)
    {
        return hasNodeWithHintAndLabel(String.format("Attached to xwiki \u00BB %s \u00BB %s", spaceName, pageName),
            fileName);
    }

    /**
     * Types the given text in the input box below the tree. As a result the tree will lookup an entity among its nodes
     * (e.g. page, attachment) that matches the given reference.
     * 
     * @param entityReference the text to be typed in the input box below the tree, usually an entity reference
     */
    public void lookupEntity(String entityReference)
    {
        input.clear();
        input.sendKeys(entityReference);
    }

    /**
     * Waits for the node with the specified hint and label to be present.
     * 
     * @param hint the tool tip of the node to wait for
     * @param label the label of the node to wait for
     * @param selected whether the node should be selected or not
     */
    private EntityTreeElement waitForNodeWithHintAndLabel(String hint, String label, boolean selected)
    {
        String className = selected ? "treeCellSelected" : "treeCell";
        String xpath =
            String.format("//td[contains(@class, '%s')]//*[@title = '%s' and . = '%s']", className, hint, label);
        getDriver().waitUntilElementIsVisible(By.xpath(xpath));
        return this;
    }

    /**
     * Checks if the node with the specified hint and label is present.
     * 
     * @param hint the tool tip of the node to look for
     * @param label the label of the node to look for
     * @return {@code true} if the specified node is present, {@code false} otherwise
     */
    private boolean hasNodeWithHintAndLabel(String hint, String label)
    {
        String format = "//td[starts-with(@class, 'treeCell')]//*[@title = '%s' and . = '%s']";
        // Note: make sure to use findElementsWithoutWaiting() to speed up tests. Tests need to call waitFor*() APIs
        // beforehand to ensure elements are visible in the tree.
        return getDriver().findElementsWithoutWaiting(By.xpath(String.format(format, hint, label))).size() == 1;
    }
}
