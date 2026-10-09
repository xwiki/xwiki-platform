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
package org.xwiki.flamingo.skin.test.po;

import java.util.Arrays;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

/**
 * Represents the compact location picker: a space suggestion input with a button opening a document tree to browse
 * for locations.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class CompactLocationPickerElement extends BaseElement
{
    private static final By CHECKBOX = By.className("jstree-checkbox");

    private final WebElement container;

    /**
     * @param container the element wrapping the suggestion input and the browse button
     */
    public CompactLocationPickerElement(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the first compact location picker displayed in the content of the current page
     */
    public static CompactLocationPickerElement getFirstInPageContent()
    {
        return new CompactLocationPickerElement(getUtil().getDriver().findElement(
            By.cssSelector("#xwikicontent .location-picker-compact")));
    }

    /**
     * @return the suggestion input holding the value of the picker
     */
    public SuggestInputElement getSuggestInput()
    {
        return new SuggestInputElement(this.container.findElement(By.cssSelector("select.suggest-spaces")));
    }

    /**
     * Clicks on the suggestion input, like a user who wants to type a location.
     *
     * @return this picker
     */
    public CompactLocationPickerElement clickSuggestInput()
    {
        getSuggestInputControl().click();
        return this;
    }

    /**
     * @return {@code true} if the text input of the suggestion input has the focus, {@code false} otherwise
     */
    public boolean isSuggestInputFocused()
    {
        return getSuggestInputControl().findElement(By.tagName("input"))
            .equals(getDriver().switchTo().activeElement());
    }

    /**
     * @return the accessible label of the button opening the document tree
     */
    public String getBrowseLabel()
    {
        return getDropDownToggle().getAttribute("aria-label");
    }

    /**
     * @return {@code true} if the drop down holding the document tree is open, {@code false} otherwise
     */
    public boolean isDropDownOpen()
    {
        return hasClass(getDropDown(), "open");
    }

    /**
     * Opens the drop down holding the document tree, if it isn't already, and waits for the tree to be loaded.
     *
     * @return the document tree used to browse for locations
     */
    public TreeElement openDropDown()
    {
        if (!isDropDownOpen()) {
            getDropDownToggle().click();
            getDriver().waitUntilCondition(driver -> isDropDownOpen());
        }
        return getTree().waitForIt();
    }

    /**
     * Closes the drop down holding the document tree, if it's open.
     *
     * @return this picker
     */
    public CompactLocationPickerElement closeDropDown()
    {
        if (isDropDownOpen()) {
            getDropDownToggle().click();
            getDriver().waitUntilCondition(driver -> !isDropDownOpen());
        }
        return this;
    }

    /**
     * Picks a location from the document tree, when the picker accepts a single location, and waits for the drop down
     * to close.
     *
     * @param nodeId the id of the tree node backing the location, e.g. {@code document:xwiki:Space.WebHome}
     * @return this picker
     */
    public CompactLocationPickerElement pick(String nodeId)
    {
        openDropDown().getNode(nodeId).select();
        getDriver().waitUntilCondition(driver -> !isDropDownOpen());
        return this;
    }

    /**
     * Checks or unchecks a location in the document tree, when the picker accepts multiple locations.
     *
     * @param nodeId the id of the tree node backing the location, e.g. {@code document:xwiki:Space.WebHome}
     * @param checked {@code true} to check the location, {@code false} to uncheck it
     * @return this picker
     */
    public CompactLocationPickerElement setChecked(String nodeId, boolean checked)
    {
        openDropDown();
        if (isChecked(nodeId) != checked) {
            getCheckbox(nodeId).click();
            getDriver().waitUntilCondition(driver -> isChecked(nodeId) == checked);
        }
        return this;
    }

    /**
     * @param nodeId the id of a tree node
     * @return {@code true} if the given node is checked in the document tree, {@code false} otherwise
     */
    public boolean isChecked(String nodeId)
    {
        return hasClass(getTreeNode(nodeId).getLabelElement(), "jstree-checked");
    }

    /**
     * @param nodeId the id of a tree node
     * @return {@code true} if the given node is selected in the document tree, {@code false} otherwise
     */
    public boolean isSelected(String nodeId)
    {
        return getTreeNode(nodeId).isSelected();
    }

    /**
     * @param nodeId the id of a tree node
     * @return {@code true} if the given node displays a checkbox, {@code false} otherwise
     */
    public boolean hasCheckbox(String nodeId)
    {
        // The tree doesn't generate any checkbox when the picker accepts a single location.
        return getDriver().findElementsWithoutWaiting(getTreeNode(nodeId).getLabelElement(),
            CHECKBOX).stream().anyMatch(WebElement::isDisplayed);
    }

    private WebElement getCheckbox(String nodeId)
    {
        return getTreeNode(nodeId).getLabelElement().findElement(CHECKBOX);
    }

    private TreeNodeElement getTreeNode(String nodeId)
    {
        return getTree().getNode(nodeId);
    }

    private TreeElement getTree()
    {
        return new TreeElement(getDropDown().findElement(By.className("location-tree")));
    }

    private WebElement getSuggestInputControl()
    {
        // Wait for the suggestion input to be ready before looking for the element it generates.
        getSuggestInput();
        return this.container.findElement(By.cssSelector(".ts-wrapper > .ts-control"));
    }

    private WebElement getDropDown()
    {
        return this.container.findElement(By.className("location-picker-dropdown"));
    }

    private boolean hasClass(WebElement element, String className)
    {
        return Arrays.asList(element.getAttribute("class").split("\\s+")).contains(className);
    }

    private WebElement getDropDownToggle()
    {
        return getDropDown().findElement(By.className("dropdown-toggle"));
    }
}
