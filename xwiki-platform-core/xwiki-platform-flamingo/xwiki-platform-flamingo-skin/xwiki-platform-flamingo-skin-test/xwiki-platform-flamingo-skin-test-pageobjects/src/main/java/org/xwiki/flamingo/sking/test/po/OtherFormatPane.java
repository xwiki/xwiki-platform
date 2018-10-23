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
package org.xwiki.flamingo.sking.test.po;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.tree.test.po.TreeElement;

/**
 * Represents the opened panel "Other Formats" in the modal export.
 *
 * @version $Id$
 * @since 10.9RC1
 */
public class OtherFormatPane extends BaseElement
{
    private static final String CONTAINER_TREE_CLASS = "exportModalTreeContainer";
    private static final String FORM_ID = "export-form";
    private static final String TREE_CLASS = "xtree";
    private static final String XAR_EXPORT_LINK_TEXT = "Export as XAR";
    private static final String VALUE_ATTR = "value";

    /**
     * @return true if the tree is displayed.
     */
    public boolean isTreeAvailable()
    {
        return getDriver().hasElement(By.className(CONTAINER_TREE_CLASS));
    }

    /**
     * @return the tree element to manipulate its nodes.
     */
    public TreeElement getTreeElement()
    {
        getDriver().waitUntilElementIsVisible(By.className(TREE_CLASS));
        WebElement element = getDriver()
            .findElement(By.className(CONTAINER_TREE_CLASS))
            .findElement(By.className(TREE_CLASS));

        return new TreeElement(element);
    }

    /**
     * @return true if the export XAR button exists.
     */
    public boolean isExportAsXARButtonAvailable()
    {
        return getDriver().findElementByLinkText(XAR_EXPORT_LINK_TEXT) != null;
    }

    /**
     * Click on the export XAR button.
     */
    public void clickExportAsXARButton()
    {
        getDriver().findElementByLinkText(XAR_EXPORT_LINK_TEXT).click();
    }

    /**
     * @return the main form of the export other format pane.
     */
    public WebElement getForm()
    {
        return getDriver().findElement(By.id(FORM_ID));
    }

    /**
     * @return all the values of the "pages" hidden input fields aggregated in a list.
     */
    public List<String> getPagesValues() {
        return getForm().findElements(By.cssSelector("input[type=hidden][name=pages]")).stream()
            .map(webElement -> webElement.getAttribute(VALUE_ATTR)).collect(Collectors.toList());
    }

    /**
     * @return all the values of the "excludes" hidden input fields aggregated in a list.
     */
    public List<String> getExcludesValues() {
        return getForm()
            .findElements(By.cssSelector("input[type=hidden][name=excludes]")).stream()
            .map(webElement -> webElement.getAttribute(VALUE_ATTR)).collect(Collectors.toList());
    }
}
