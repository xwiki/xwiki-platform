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
import org.xwiki.test.ui.po.BaseElement;

/**
 * Displays an extension.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ExtensionPane extends BaseElement
{
    /**
     * The extension name locator.
     */
    private static final By EXTENSION_NAME = By.className("extension-name");

    /**
     * The element that wraps the extension display.
     */
    private final WebElement container;

    /**
     * Creates a new instance.
     * 
     * @param container the element that wraps the extension display
     */
    public ExtensionPane(WebElement container)
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
        By xpath = By.xpath("*[@class = 'extension-header']//*[@class = 'extension-status']");
        List<WebElement> found = getUtil().findElementsWithoutWaiting(getDriver(), container, xpath);
        return found.size() > 0 ? found.get(0).getText() : null;
    }

    /**
     * @return the extension name
     */
    public String getName()
    {
        String nameAndVersion = getUtil().findElementWithoutWaiting(getDriver(), container, EXTENSION_NAME).getText();
        return nameAndVersion.substring(0, nameAndVersion.length() - getVersion().length());
    }

    /**
     * @return the extension version
     */
    public String getVersion()
    {
        By xpath = By.xpath("*[@class = 'extension-header']//*[@class = 'extension-version']");
        return getUtil().findElementWithoutWaiting(getDriver(), container, xpath).getText();
    }

    /**
     * @return the extension authors
     */
    public String getAuthors()
    {
        List<WebElement> found =
            getUtil().findElementsWithoutWaiting(getDriver(), container, By.className("extension-authors"));
        return found.size() > 0 ? found.get(0).getText() : null;
    }

    /**
     * @return the extension summary
     */
    public String getSummary()
    {
        List<WebElement> found =
            getUtil().findElementsWithoutWaiting(getDriver(), container, By.className("extension-description"));
        return found.size() > 0 ? found.get(0).getText() : null;
    }

    /**
     * Clicks on the show details button.
     * 
     * @return the extension pane showing the extension details
     */
    public ExtensionPane showDetails()
    {
        List<WebElement> found = getUtil().findElementsWithoutWaiting(getDriver(), container, By.name("showDetails"));
        if (found.size() > 0) {
            found.get(0).click();
            return this;
        } else {
            // Wait until the extension body is not loading.
            return clickAndWaitUntilElementIsVisible(By.name("actionShowDetails"), "/*[@class = 'extension-body']");
        }
    }

    /**
     * Clicks on the specified button and waits for the specified element to be visible.
     * 
     * @param buttonLocator the button to be clicked
     * @param xpathSuffix the XPath suffix inside the 'extension-item' element
     * @return the new extension pane, after the specified element became visible
     */
    private ExtensionPane clickAndWaitUntilElementIsVisible(By buttonLocator, String xpathSuffix)
    {
        String nameAndVersion = getUtil().findElementWithoutWaiting(getDriver(), container, EXTENSION_NAME).getText();
        getUtil().findElementWithoutWaiting(getDriver(), container, buttonLocator).click();
        waitUntilElementIsVisible(By.xpath(String.format("//*[contains(@class, 'extension-item') and "
            + "descendant::*[contains(@class, 'extension-name') and . = '%s']]%s", nameAndVersion, xpathSuffix)));
        // We have to create a new extension pane because the DOM has changed.
        return new ExtensionPane(getUtil().findElementWithoutWaiting(
            getDriver(),
            By.xpath(String.format("//*[contains(@class, 'extension-item') and"
                + " descendant::*[contains(@class, 'extension-name') and . = '%s']]", nameAndVersion))));
    }

    /**
     * Clicks on the hide details button.
     * 
     * @return the extension pane that doesn't show the extension details
     */
    public ExtensionPane hideDetails()
    {
        getUtil().findElementWithoutWaiting(getDriver(), container, By.name("hideDetails")).click();
        return this;
    }

    /**
     * Clicks on the specified button and waits for a confirmation or for the job/action to be done.
     * 
     * @param buttonLocator the button to be clicked
     * @return the extension pane showing the confirmation or the job log
     */
    private ExtensionPane clickAndWaitForConfirmationOrJobDone(By buttonLocator)
    {
        // Wait until the progress section contains a confirmation button or no loading log items.
        return clickAndWaitUntilElementIsVisible(buttonLocator, "/*[@class = 'extension-body']/*"
            + "[@class = 'extension-body-progress extension-body-section' and "
            + "(descendant::input[@name = 'confirm'] or "
            + "not(descendant::div[contains(@class, 'extension-log-item-loading')]))]");
    }

    /**
     * Clicks on the install button and waits for the install plan to be computed.
     * 
     * @return the extension pane displaying the install plan
     */
    public ExtensionPane install()
    {
        return clickAndWaitForConfirmationOrJobDone(By
            .xpath(".//input[@name = 'actionInstall' and @value = 'Install']"));
    }

    /**
     * Clicks on the uninstall button and waits for the uninstall plan to be computed.
     * 
     * @return the extension pane displaying the uninstall plan
     */
    public ExtensionPane uninstall()
    {
        return clickAndWaitForConfirmationOrJobDone(By.name("actionUninstall"));
    }

    /**
     * Clicks on the upgrade button and waits for the upgrade plan to be computed.
     * 
     * @return the extension pane displaying the upgrade plan
     */
    public ExtensionPane upgrade()
    {
        return clickAndWaitForConfirmationOrJobDone(By
            .xpath(".//input[@name = 'actionInstall' and @value = 'Upgrade']"));
    }

    /**
     * Clicks on the downgrade button and waits for the downgrade plan to be computed.
     * 
     * @return the extension pane displaying the downgrade plan
     */
    public ExtensionPane downgrade()
    {
        return clickAndWaitForConfirmationOrJobDone(By
            .xpath(".//input[@name = 'actionInstall' and @value = 'Downgrade']"));
    }

    /**
     * Confirms the current action and wait for it to be performed.
     * 
     * @return the extension pane displaying the extension after the current action has been performed
     */
    public ExtensionPane confirm()
    {
        return clickAndWaitForConfirmationOrJobDone(By.name("confirm"));
    }

    /**
     * Clicks on the specified tab and returns the corresponding section if it's available.
     * 
     * @param label the tab label
     * @return the element that wraps the section corresponding to the specified tab
     */
    private WebElement clickTab(String label)
    {
        By tabXPath = By.xpath(".//*[@class = 'innerMenu']//a[. = '" + label + "']");
        List<WebElement> found = getUtil().findElementsWithoutWaiting(getDriver(), container, tabXPath);
        if (found.size() == 0) {
            return null;
        }
        found.get(0).click();
        String sectionAnchor = found.get(0).getAttribute("href").substring(1);
        By sectionXPath =
            By.xpath(".//div[contains(@class, 'extension-body-section') and preceding-sibling::*[1][@id = '"
                + sectionAnchor + "']]");
        return getUtil().findElementWithoutWaiting(getDriver(), container, sectionXPath);
    }

    /**
     * Selects the extension description tab.
     * 
     * @return the extension description section
     */
    public ExtensionDescriptionPane openDescriptionSection()
    {
        WebElement section = clickTab("Description");
        return section != null ? new ExtensionDescriptionPane(section) : null;
    }

    /**
     * Selects the extension dependencies tab.
     * 
     * @return the extension dependencies section
     */
    public ExtensionDependenciesPane openDependenciesSection()
    {
        WebElement section = clickTab("Dependencies");
        return section != null ? new ExtensionDependenciesPane(section) : null;
    }

    /**
     * Selects the extension progress tab.
     * 
     * @return the extension progress section
     */
    public ExtensionProgressPane openProgressSection()
    {
        WebElement section = clickTab("Progress");
        return section != null ? new ExtensionProgressPane(section) : null;
    }

    /**
     * @return the progress bar pane
     */
    public ProgressBarPane getProgressBar()
    {
        return new ProgressBarPane(getUtil().findElementWithoutWaiting(getDriver(), container,
            By.className("ui-progress")));
    }
}
