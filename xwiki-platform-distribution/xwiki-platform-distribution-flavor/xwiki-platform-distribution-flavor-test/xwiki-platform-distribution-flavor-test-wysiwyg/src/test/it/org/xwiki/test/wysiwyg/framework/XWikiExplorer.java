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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * Utility class used to write integration tests involving the XWiki Explorer tree.
 * 
 * @version $Id$
 */
public class XWikiExplorer
{
    private static final String FINDER_LOCATOR =
        "//input[@class = 'xtree-finder' and following-sibling::div[contains(@class, 'xExplorer')]]";

    private static final By SELECTED_NODE = By
        .xpath("//div[contains(@class, 'xExplorer')]//li[@role = 'treeitem' and @aria-selected = 'true']");

    private final XWikiWebDriver driver;

    /**
     * Creates a new XWikiExplorer that uses the passed web driver.
     * 
     * @param driver the web driver
     */
    public XWikiExplorer(XWikiWebDriver driver)
    {
        this.driver = driver;
    }

    /**
     * Open the node that corresponds to the specified space.
     * 
     * @param spaceName the space name
     * @return this
     */
    public XWikiExplorer openSpace(String spaceName)
    {
        return open(getSpaceNodeId(spaceName));
    }

    /**
     * Open the node that corresponds to the specified page.
     * 
     * @param spaceName the space name
     * @param pageName the page name
     * @return this
     */
    public XWikiExplorer openPage(String spaceName, String pageName)
    {
        return open(getDocumentNodeId(spaceName, pageName));
    }

    /**
     * Open the node that corresponds to the 'Attachments' meta node of the specified page.
     * 
     * @param spaceName the space name
     * @param pageName the page name
     * @return this
     */
    public XWikiExplorer openAttachments(String spaceName, String pageName)
    {
        return open(getAttachmentsNodeId(spaceName, pageName));
    }

    private XWikiExplorer open(String nodeId)
    {
        // Select the node first in order to scroll it into view. Clicking on the toggle to expand the node has no
        // effect otherwise (Selenium doesn't manage to scroll the node into view when the toggle is clicked).
        select(nodeId);

        String toggleLocatorFormat =
            "//div[contains(@class, 'xExplorer')]//li[@id = '%s'"
                + " and @aria-expanded = '%s']/i[contains(@class, 'jstree-ocl')]";
        this.driver.findElementByXPath(String.format(toggleLocatorFormat, nodeId, false)).click();
        this.driver.waitUntilElementIsVisible(By.xpath(String.format(toggleLocatorFormat, nodeId, true)));

        return this;
    }

    /**
     * Select the node that corresponds to the specified space.
     * 
     * @param spaceName the space name
     * @return this
     */
    public XWikiExplorer selectSpace(String spaceName)
    {
        return select(getSpaceNodeId(spaceName));
    }

    /**
     * Select the node that corresponds to the 'New page...' meta node of the specified space.
     * 
     * @param spaceName the space name
     * @return this
     */
    public XWikiExplorer selectNewPage(String spaceName)
    {
        return select(getNewDocumentNodeId(spaceName));
    }

    /**
     * Select the node that corresponds to the specified page.
     * 
     * @param spaceName the space name
     * @param pageName the page name
     * @return this
     */
    public XWikiExplorer selectPage(String spaceName, String pageName)
    {
        return select(getDocumentNodeId(spaceName, pageName));
    }

    /**
     * Toggles the selection of the specified page by clicking the label of the corresponding node while holding the
     * Control key down.
     * 
     * @param spaceName the space name
     * @param pageName the page name
     * @return
     */
    public XWikiExplorer togglePageSelection(String spaceName, String pageName)
    {
        return toggleSelection(getDocumentNodeId(spaceName, pageName));
    }

    /**
     * Select the node that corresponds to the 'Upload file...' meta node of the specified page.
     * 
     * @param spaceName the space name
     * @param pageName the page name
     * @return this
     */
    public XWikiExplorer selectNewAttachment(String spaceName, String pageName)
    {
        return select(String.format("addAttachment:xwiki:%s.%s", spaceName, pageName));
    }

    /**
     * Select the node that corresponds to the specified attachment.
     * 
     * @param spaceName the space name
     * @param pageName the page name
     * @param fileName the file name
     * @return this
     */
    public XWikiExplorer selectAttachment(String spaceName, String pageName, String fileName)
    {
        return select(getAttachmentNodeId(spaceName, pageName, fileName));
    }

    private XWikiExplorer select(String nodeId)
    {
        this.driver.findElementByXPath(getNodeLocator(nodeId) + "/a[@class = 'jstree-anchor']").click();
        return this;
    }

    private XWikiExplorer toggleSelection(String nodeId)
    {
        WebElement node =
            this.driver.findElementByXPath(getNodeLocator(nodeId) + "/a[contains(@class, 'jstree-anchor')]");
        new Actions(this.driver).keyDown(Keys.CONTROL).click(node).keyUp(Keys.CONTROL).perform();
        return this;
    }

    /**
     * @param spaceName the space name
     * @return {@code true} if the node corresponding to the specified space is present in the tree
     */
    public boolean hasSpace(String spaceName)
    {
        return hasNode(getSpaceNodeId(spaceName));
    }

    /**
     * @param spaceName the space name
     * @param pageName the page name
     * @return {@code true} if the node corresponding to the specified page is present in the tree
     */
    public boolean hasPage(String spaceName, String pageName)
    {
        return hasNode(getDocumentNodeId(spaceName, pageName));
    }

    /**
     * @param spaceName the space name
     * @param pageName the page name
     * @param fileName the file name
     * @return {@code true} if the node corresponding to the specified attachment is present in the tree
     */
    public boolean hasAttachment(String spaceName, String pageName, String fileName)
    {
        return hasNode(getAttachmentNodeId(spaceName, pageName, fileName));
    }

    private boolean hasNode(String nodeId)
    {
        return this.driver.hasElementWithoutWaiting(By.xpath(getNodeLocator(nodeId)));
    }

    /**
     * Types the given space name into the finder and selects the first suggestions that matches the space name.
     * 
     * @param spaceName the space name
     * @return this
     */
    public XWikiExplorer findAndSelectSpace(String spaceName)
    {
        find(spaceName).selectSpace(spaceName);
        return waitForNodeSelected();
    }

    /**
     * Types the given page title into the finder and selects the first suggestions that matches the page title.
     * 
     * @param pageTitle the page title
     * @return this
     */
    public XWikiExplorer findAndSelectPage(String pageTitle)
    {
        find(pageTitle).selectPage(pageTitle);
        return waitForNodeSelected();
    }

    /**
     * Types the given file name into the finder and selects the first suggestions that matches the file name.
     * 
     * @param fileName the file name
     * @return this
     */
    public XWikiExplorer findAndSelectAttachment(String fileName)
    {
        find(fileName).selectAttachment(fileName);
        return waitForNodeSelected();
    }

    /**
     * Type the given text into the finder and wait for suggestions.
     * 
     * @param text the text to find
     * @return the suggestions page
     */
    public TreeSuggestionsPane find(String text)
    {
        WebElement finder = this.driver.findElementByXPath(FINDER_LOCATOR);
        finder.clear();
        finder.sendKeys(text);
        // Wait for the list of suggestions.
        this.driver.waitUntilElementIsVisible(By
            .xpath("//div[contains(@class, 'xtree-finder-suggestions')]//ul[contains(@class, 'suggestList')]"));
        return new TreeSuggestionsPane(this.driver);
    }

    /**
     * Wait for the tree to load.
     * 
     * @return this
     */
    public XWikiExplorer waitForIt()
    {
        // The editor attempts to open the tree to a specified node. If the node is found then it is selected. Otherwise
        // the value of the finder is set to the node id. Thus we wait for a node to be selected or for the finder to
        // have a non-empty value.
        this.driver.waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver input)
            {
                List<WebElement> finders =
                    XWikiExplorer.this.driver.findElementsWithoutWaiting(By.xpath(FINDER_LOCATOR));
                return (!finders.isEmpty() && !StringUtils.isEmpty(finders.get(0).getAttribute("value")))
                    || XWikiExplorer.this.driver.hasElementWithoutWaiting(SELECTED_NODE);
            }
        });
        return this;
    }

    /**
     * Waits for the specified page to be selected.
     * 
     * @param spaceName the name of the space containing the page
     * @param pageName the name of the page to wait for
     * @return this
     */
    public XWikiExplorer waitForPageSelected(String spaceName, String pageName)
    {
        return waitForNodeSelected(getDocumentNodeId(spaceName, pageName));
    }

    /**
     * Waits for the "New page.." node under the specified space to be selected.
     * 
     * @param spaceName the name of the space where the new page would be created
     * @return this
     */
    public XWikiExplorer waitForNewPageSelected(String spaceName)
    {
        return waitForNodeSelected(getNewDocumentNodeId(spaceName));
    }

    /**
     * Waits for the specified attachment to be selected.
     * 
     * @param spaceName the name of the space containing the page
     * @param pageName the name of the page who has the attachment
     * @param fileName the attachment file name
     * @return this
     */
    public XWikiExplorer waitForAttachmentSelected(String spaceName, String pageName, String fileName)
    {
        return waitForNodeSelected(getAttachmentNodeId(spaceName, pageName, fileName));
    }

    /**
     * Waits for the attachments node of the specified page to be selected. The attachments node is the parent node for
     * all the attachments of a page.
     * 
     * @param spaceName the space containing the page
     * @param pageName the name of the page whose attachments node is selected
     * @return this
     */
    public XWikiExplorer waitForAttachmentsSelected(String spaceName, String pageName)
    {
        return waitForNodeSelected(getAttachmentsNodeId(spaceName, pageName));
    }

    private XWikiExplorer waitForNodeSelected()
    {
        this.driver.waitUntilElementIsVisible(SELECTED_NODE);
        return this;
    }

    private XWikiExplorer waitForNodeSelected(String nodeId)
    {
        this.driver.waitUntilElementIsVisible(By.xpath(String.format(
            "//div[contains(@class, 'xExplorer')]//li[@id = '%s' and @aria-selected = 'true']", nodeId)));
        return this;
    }

    public XWikiExplorer waitForFinderValue(final String value)
    {
        this.driver.waitUntilElementHasAttributeValue(By.xpath(FINDER_LOCATOR), "value", value);
        return this;
    }

    private String getDocumentNodeId(String spaceName, String pageName)
    {
        return String.format("document:xwiki:%s.%s", spaceName, pageName);
    }

    private String getNewDocumentNodeId(String spaceName)
    {
        return String.format("addDocument:xwiki:%s.WebHome", spaceName);
    }

    private String getSpaceNodeId(String spaceName)
    {
        return "space:xwiki:" + spaceName;
    }

    private String getAttachmentNodeId(String spaceName, String pageName, String fileName)
    {
        return String.format("attachment:xwiki:%s.%s@%s", spaceName, pageName, fileName);
    }

    private String getAttachmentsNodeId(String spaceName, String pageName)
    {
        return String.format("attachments:xwiki:%s.%s", spaceName, pageName);
    }

    private String getNodeLocator(String nodeId)
    {
        return String.format("//div[contains(@class, 'xExplorer')]//li[@id = '%s']", nodeId);
    }
}
