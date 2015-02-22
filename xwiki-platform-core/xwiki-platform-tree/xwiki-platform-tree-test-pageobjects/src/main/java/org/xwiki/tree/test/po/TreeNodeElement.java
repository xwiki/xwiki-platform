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
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page object used to interact with a tree node.
 * 
 * @version $Id$
 * @since 6.3RC1
 */
public class TreeNodeElement extends BaseElement
{
    /**
     * The class HTML attribute.
     */
    private static final String CLASS = "class";

    /**
     * The tree that contains this node.
     */
    private WebElement treeElement;

    /**
     * The element that represents the tree node.
     */
    private By locator;

    /**
     * Creates a new instance that can be used to interact with the tree node represented by the specified element.
     * 
     * @param treeElement the tree that contains this node
     * @param locator the element that represents the tree node
     */
    public TreeNodeElement(WebElement treeElement, By locator)
    {
        this.treeElement = treeElement;
        this.locator = locator;
    }

    /**
     * The element that represents this tree node is often replaced so we cannot cache it.
     * 
     * @return the element that represents this tree node
     */
    private WebElement getElement()
    {
        return this.treeElement.findElement(this.locator);
    }

    /**
     * @return the node label (as plain text)
     */
    public String getLabel()
    {
        return getLabelElement().getText();
    }

    private WebElement getLabelElement()
    {
        String labelId = getElement().getAttribute("aria-labelledby");
        return getElement().findElement(By.id(labelId));
    }

    /**
     * @return the list of child nodes
     */
    public List<TreeNodeElement> getChildren()
    {
        List<TreeNodeElement> children = new ArrayList<>();
        for (WebElement childElement : getElement().findElements(By.xpath("./ul/li"))) {
            children.add(new TreeNodeElement(this.treeElement, By.id(childElement.getAttribute("id"))));
        }
        return children;
    }

    /**
     * @return {@code true} if this node is a leaf (no children), {@code false} otherwise
     */
    public boolean isLeaf()
    {
        return getElement().getAttribute(CLASS).contains("jstree-leaf");
    }

    /**
     * @return {@code true} if this node is opened (children are visible), {@code false} otherwise
     */
    public boolean isOpen()
    {
        return Boolean.valueOf(getElement().getAttribute("aria-expanded"));
    }

    /**
     * @return {@code true} if this node is selected, {@code false} otherwise
     */
    public boolean isSelected()
    {
        return Boolean.valueOf(getElement().getAttribute("aria-selected"));
    }

    /**
     * Opens this tree node.
     * 
     * @return this tree node
     */
    public TreeNodeElement open()
    {
        if (!isOpen()) {
            getToggleElement().click();
        }
        return this;
    }

    /**
     * Closes this tree node.
     * 
     * @return this tree node
     */
    public TreeNodeElement close()
    {
        if (isOpen()) {
            getToggleElement().click();
        }
        return this;
    }

    private WebElement getToggleElement()
    {
        return getElement().findElement(By.xpath("./i[contains(@class, 'jstree-ocl')]"));
    }

    /**
     * Selects this tree node by clicking on its label.
     * 
     * @return this tree node
     */
    public TreeNodeElement select()
    {
        if (!isSelected()) {
            getLabelElement().click();
        }
        return this;
    }

    /**
     * Wait as long as this node is in loading state.
     * 
     * @return this tree node
     */
    public TreeNodeElement waitForIt()
    {
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return !Boolean.valueOf(getElement().getAttribute("aria-busy"));
            }
        });
        return this;
    }
}
