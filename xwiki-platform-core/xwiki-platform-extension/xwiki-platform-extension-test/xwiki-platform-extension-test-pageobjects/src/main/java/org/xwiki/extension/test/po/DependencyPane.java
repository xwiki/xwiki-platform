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
package org.xwiki.extension.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.extension.ExtensionId;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Displays information about a dependency of an extension.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class DependencyPane extends BaseElement
{
    /**
     * The dependency container.
     */
    private final WebElement container;

    /**
     * Creates a new instance.
     * 
     * @param container the dependency container
     */
    public DependencyPane(WebElement container)
    {
        this.container = container;
    }

    /**
     * @return the extension status (loading, core, installed, remote, remote-installed, remote-core,
     *         remote-installed-incompatible, remote-core-incompatible)
     */
    public String getStatus()
    {
        String[] classNames = container.getAttribute("class").split("\\s+");
        if (classNames.length < 2) {
            return null;
        }
        return classNames[1].substring("extension-item-".length());
    }

    /**
     * @return the extension status message
     */
    public String getStatusMessage()
    {
        List<WebElement> found = getDriver().findElementsWithoutWaiting(container, By.className("extension-status"));
        return found.size() > 0 ? found.get(0).getText() : null;
    }

    /**
     * @return the extension link that can be clicked to view more details
     */
    public WebElement getLink()
    {
        By linkLocator = By.cssSelector("a.extension-link");
        List<WebElement> found = getDriver().findElementsWithoutWaiting(container, linkLocator);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * @return the extension name
     */
    public String getName()
    {
        WebElement link = getLink();
        if (link != null) {
            return link.getText();
        }
        // Unknown dependency, with no link.
        String innerText = container.getText();
        return innerText.substring(0, innerText.indexOf(getVersion()));
    }

    /**
     * @return the extension version
     */
    public String getVersion()
    {
        return getDriver().findElementWithoutWaiting(container, By.className("extension-version")).getText();
    }

    /**
     * @return the extension id
     */
    public ExtensionId getId()
    {
        return new ExtensionId(getName(), getVersion());
    }
}
