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
package org.xwiki.tree.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page object used to interact with the generic tree widget available in XWiki.
 * 
 * @version $Id$
 * @since 6.3RC1
 */
public class TreeElement extends BaseElement
{
    /**
     * The element that represents the tree.
     */
    private WebElement element;

    /**
     * Creates a new instance that can be used to interact with the tree represented by the given element.
     * 
     * @param element the element that represents the tree
     */
    public TreeElement(WebElement element)
    {
        this.element = element;
    }

    /**
     * @param nodeId the node identifier
     * @return the node with the specified identifier
     */
    public TreeNodeElement getNode(String nodeId)
    {
        return new TreeNodeElement(this.element, By.id(nodeId));
    }

    /**
     * @param nodeId the node identifier
     * @return {@code true} if the specified node is present (loaded), {@code false} otherwise
     */
    public boolean hasNode(String nodeId)
    {
        // We cannot use By.id(nodeId) because findElements returns 0 elements if the id contains special characters
        // such as backslash (which is used to escape special characters in an entity reference which the node id can
        // be). Such an element id is technically invalid but the browsers are handling it fine.
        // See https://code.google.com/p/selenium/issues/detail?id=8173
        return getDriver().findElementsWithoutWaiting(this.element,
            By.xpath(".//*[@id = '" + nodeId + "']")).size() > 0;
    }

    /**
     * Open the tree to the specified node.
     * 
     * @param nodeId the node to open to
     * @return this tree
     */
    public TreeElement openTo(String nodeId)
    {
        if (getDriver() instanceof JavascriptExecutor) {
            ((JavascriptExecutor) getDriver()).executeScript(
                "jQuery.jstree.reference(jQuery(arguments[0])).openTo(arguments[1])", this.element, nodeId);
        }
        getDriver().waitUntilElementIsVisible(By.id(nodeId));
        return this;
    }

    /**
     * Clear the selection in the tree.
     * 
     * @return this tree
     * @since 7.2M3
     */
    public TreeElement clearSelection()
    {
        if (getDriver() instanceof JavascriptExecutor) {
            ((JavascriptExecutor) getDriver()).executeScript(
                "jQuery.jstree.reference(jQuery(arguments[0])).deselect_all()", this.element);
        }
        return this;
    }

    /**
     * Wait as long as the tree in busy (loading).
     * 
     * @return this tree
     */
    public TreeElement waitForIt()
    {
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                // We are ready when the DOM element is marked as ready (not busy) and when the jQuery plugin is
                // properly registered (otherwise we can not interact with it).
                return !Boolean.valueOf(element.getAttribute("aria-busy"))
                    && Boolean.TRUE.equals(getDriver().executeJavascript("return jQuery.jstree != null"));
            }
        });
        return this;
    }

    /**
     * Waits for the specified node to be selected.
     * 
     * @param nodeId the id of the node to wait for
     * @return this tree
     * @since 7.2
     */
    protected TreeElement waitForNodeSelected(String nodeId)
    {
        String selectedNodeXPath = String.format(".//*[@id = '%s' and @aria-selected = 'true']", nodeId);
        getDriver().waitUntilElementIsVisible(this.element, By.xpath(selectedNodeXPath));
        return this;
    }

    protected WebElement getElement()
    {
        return this.element;
    }
}
