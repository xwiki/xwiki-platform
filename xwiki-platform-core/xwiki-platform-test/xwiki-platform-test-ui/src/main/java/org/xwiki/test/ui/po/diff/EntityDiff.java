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
package org.xwiki.test.ui.po.diff;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the pane that displays the differences between two versions of an entity (e.g. document, attachment,
 * object, class property). For each modified property of the target entity a unified diff is displayed (e.g. the diff
 * for the document title, attachment content or a meta property of a class property).
 * 
 * @version $Id$
 * @since 7.0RC1
 */
public class EntityDiff extends BaseElement
{
    /**
     * The element that wraps and contains the changes for all the entity's properties.
     */
    private final WebElement container;

    /**
     * Create a new instance for the entity specified by the given element.
     * 
     * @param container The element that wraps and contains the changes for all the entity's properties.
     */
    public EntityDiff(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the list of properties that have been modified
     */
    public List<String> getPropertyNames()
    {
        List<String> propertyNames = new ArrayList<>();
        for (WebElement element : getDriver().findElementsWithoutWaiting(this.container, By.className("diff-header"))) {
            propertyNames.add(element.getText().trim());
        }
        return propertyNames;
    }

    /**
     * @param propertyName the name of a modified property of the underlying entity
     * @return the changes made to the specified property (each item in the returned list is a line in the diff)
     */
    public List<String> getDiff(String propertyName)
    {
        WebElement element =
            this.container.findElement(By.xpath(".//div[@class = 'diff-container' and "
                + "parent::dd/preceding-sibling::dt[1][@class = 'diff-header' and normalize-space(.) = '"
                + propertyName + "']]"));
        List<String> diff = new ArrayList<>();
        for (WebElement line : getDriver().findElementsWithoutWaiting(element, By.xpath(".//td[3]"))) {
            if (getDriver().findElementsWithoutWaiting(line, By.tagName("ins")).size() > 0
                || getDriver().findElementsWithoutWaiting(line, By.tagName("del")).size() > 0) {
                diff.add(String.valueOf(getDriver().executeJavascript("return arguments[0].innerHTML", line)));
            } else {
                diff.add(line.getText());
            }
        }
        return diff;
    }
}
