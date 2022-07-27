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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents the page breadcrumb.
 *
 * @version $Id$
 * @since 7.2M3
 */
public class BreadcrumbElement extends BaseElement
{
    private WebElement container;

    public BreadcrumbElement(WebElement container)
    {
        this.container = container;
    }

    public List<String> getPath()
    {
        List<WebElement> pathElements = getDriver().findElementsWithoutWaiting(this.container, By.tagName("li"));
        List<String> path = new ArrayList<>();
        for (WebElement pathElement : pathElements) {
            path.add(pathElement.getText());
        }
        return path;
    }

    public String getPathAsString()
    {
        return StringUtils.join(getPath(), '/');
    }

    public void clickPathElement(String linkText)
    {
        this.container.findElement(By.linkText(linkText)).click();
    }

    public boolean hasPathElement(String text, boolean isCurrent, boolean withLink)
    {
        List<WebElement> result;
        if (isCurrent) {
            result = getDriver().findElementsWithoutWaiting(this.container,
                By.xpath("li[contains(@class, 'active')]/a[. = '" + text + "']"));
        } else {
            if (withLink) {
                result = getDriver().findElementsWithoutWaiting(this.container, By.xpath("//a[. = '" + text + "']"));
            } else {
                // if the user has not the right to see the parent, there is no link to the parent, only a <li> with the
                // name of the document
                result = getDriver().findElementsWithoutWaiting(this.container, By.xpath("//li[. = '" + text + "']"));
            }
        }
        return result.size() > 0;
    }

    public void expand()
    {
        // Remember the id of the container so that we can retrieve it when it will be removed and re-inserted.
        final String containerId = this.container.getAttribute("id");

        clickPathElement("…");

        // Expanding the breadcrumb remove the current container and replace it by an updated one (with the full
        // hierarchy inside). So we wait until the container is re-inserted without any "ellipsis" item.
        getDriver().waitUntilElementDisappears(By.cssSelector("#" + containerId + " .ellipsis"));

        // Update the internal reference
        this.container = getDriver().findElement(By.id(containerId));
    }

    public boolean canBeExpanded()
    {
        return hasPathElement("…", false, true);
    }
}
