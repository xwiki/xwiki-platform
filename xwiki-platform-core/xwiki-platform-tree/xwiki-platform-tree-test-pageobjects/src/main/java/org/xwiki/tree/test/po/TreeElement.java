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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
     * Open the tree to the specified node and select it.
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
        waitForNodeSelected(nodeId);

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
        // Wait for the loading animation container. This element is generated from JavaScript when the tree is
        // being initialized, so its presence guarantees that the tree initialization has started.
        getDriver().waitUntilElementIsVisible(this.element, By.cssSelector(".jstree-container-ul"));
        // Wait for the root node to be loaded.
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                // The tree element is marked as busy while the tree nodes are being loaded.
                return !Boolean.valueOf(element.getAttribute("aria-busy"));
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
    public TreeElement waitForNodeSelected(String nodeId)
    {
        String selectedNodeXPath = String.format(".//*[@id = '%s' and @aria-selected = 'true']", nodeId);
        getDriver().waitUntilElementIsVisible(this.element, By.xpath(selectedNodeXPath));
        return this;
    }

    protected WebElement getElement()
    {
        return this.element;
    }

    /**
     * @return the list of selected node IDs.
     */
    public List<String> getSelectedNodeIDs()
    {
        if (getDriver() instanceof JavascriptExecutor) {
            List<String> selectedNodeIDs =
                (List<String>) ((JavascriptExecutor) getDriver()).executeScript(
                    "return jQuery.jstree.reference(jQuery(arguments[0])).get_selected()", this.element);

            return selectedNodeIDs;
        }

        return new ArrayList<String>();
    }

    /**
     * @return a list of loaded node IDs.
     */
    public List<String> getNodeIDs()
    {
        if (getDriver() instanceof JavascriptExecutor) {
            String[] selectedNodeIDs =
                (String[]) ((JavascriptExecutor) getDriver()).executeScript(
                    "return jQuery.jstree.reference(jQuery(arguments[0])).get_json('#', "
                        + "{flat:true, no_data:true, no_state:true})" + ".map(function(element) {return element.id});",
                    this.element);

            return Arrays.asList(selectedNodeIDs);
        }

        return new ArrayList<String>();
    }

    /**
     * @return the list of top level nodes
     */
    public List<TreeNodeElement> getTopLevelNodes()
    {
        return getDriver()
            .findElementsWithoutWaiting(this.element,
                By.cssSelector(".jstree-node[aria-level=\"1\"]:not(.jstree-hidden)"))
            .stream().map(nodeElement -> By.id(nodeElement.getAttribute("id")))
            .map(nodeLocator -> new TreeNodeElement(this.element, nodeLocator)).collect(Collectors.toList());
    }
}
