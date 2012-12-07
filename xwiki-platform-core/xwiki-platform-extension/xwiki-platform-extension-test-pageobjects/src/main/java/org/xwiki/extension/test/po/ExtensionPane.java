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

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.extension.ExtensionId;
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
     * @return the extension identifier
     */
    public ExtensionId getId()
    {
        return new ExtensionId(getName(), getVersion());
    }

    /**
     * @return the extension name
     */
    public String getName()
    {
        By xpath = By.xpath("*[@class = 'extension-header']//*[@class = 'extension-name']");
        return getUtil().findElementWithoutWaiting(getDriver(), container, xpath).getText();
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
    public List<WebElement> getAuthors()
    {
        return getUtil().findElementsWithoutWaiting(getDriver(), container, By.className("extension-author"));
    }

    /**
     * @return the extension summary
     */
    public String getSummary()
    {
        List<WebElement> found =
            getUtil().findElementsWithoutWaiting(getDriver(), container, By.className("extension-summary"));
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
            WebElement button =
                getUtil().findElementWithoutWaiting(getDriver(), container, By.name("actionShowDetails"));
            return clickAndWaitUntilElementIsVisible(button, "/*[@class = 'extension-body']");
        }
    }

    /**
     * Clicks on the given button and waits for the specified element to be visible.
     * 
     * @param button the button to be clicked
     * @param xpathSuffix the XPath suffix inside the 'extension-item' element
     * @return the new extension pane, after the specified element became visible
     */
    private ExtensionPane clickAndWaitUntilElementIsVisible(WebElement button, String xpathSuffix)
    {
        String xpath = getXPath();
        button.click();
        waitUntilElementIsVisible(By.xpath(xpath + xpathSuffix));
        // We have to create a new extension pane because the DOM has changed.
        return new ExtensionPane(getUtil().findElementWithoutWaiting(getDriver(), By.xpath(xpath)));
    }

    /**
     * @return the XPath used to locate this extension
     */
    private String getXPath()
    {
        String nameAndVersion =
            getUtil().findElementWithoutWaiting(getDriver(), container, By.className("extension-title")).getText();
        return String.format("//*[contains(@class, 'extension-item') and descendant::*["
            + "contains(@class, 'extension-title') and normalize-space(.) = '%s']]", nameAndVersion);
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
     * Clicks on the given button and waits for a confirmation or for the job/action to be done.
     * 
     * @param button the button to be clicked
     * @return the extension pane showing the confirmation or the job log
     */
    private ExtensionPane clickAndWaitForConfirmationOrJobDone(WebElement button)
    {
        // Wait until the extension is not loading or the progress section contains a confirmation button.
        return clickAndWaitUntilElementIsVisible(button, "/*[@class = 'extension-body']/*"
            + "[@class = 'extension-body-progress extension-body-section' and "
            + "(not(ancestor::*[contains(@class, 'loading')]) or "
            + "descendant::input[@name = 'confirm' and not(@disabled)])]");
    }

    /**
     * Clicks on the install button and waits for the install plan to be computed.
     * 
     * @return the extension pane displaying the install plan
     */
    public ExtensionPane install()
    {
        return clickAndWaitForConfirmationOrJobDone(getInstallButton());
    }

    /**
     * @return the install button, if present
     */
    public WebElement getInstallButton()
    {
        return maybeFindElement(By.xpath(".//input[@name = 'actionInstall' and @value = 'Install']"));
    }

    /**
     * @param locator specifies the element to look for
     * @return the specified element, if found, {@code null} otherwise
     */
    private WebElement maybeFindElement(By locator)
    {
        List<WebElement> found = getUtil().findElementsWithoutWaiting(getDriver(), container, locator);
        return found.size() > 0 ? found.get(0) : null;
    }

    /**
     * Clicks on the uninstall button and waits for the uninstall plan to be computed.
     * 
     * @return the extension pane displaying the uninstall plan
     */
    public ExtensionPane uninstall()
    {
        return clickAndWaitForConfirmationOrJobDone(getUninstallButton());
    }

    /**
     * @return the uninstall button, if present
     */
    public WebElement getUninstallButton()
    {
        return maybeFindElement(By.name("actionUninstall"));
    }

    /**
     * Clicks on the upgrade button and waits for the upgrade plan to be computed.
     * 
     * @return the extension pane displaying the upgrade plan
     */
    public ExtensionPane upgrade()
    {
        return clickAndWaitForConfirmationOrJobDone(getUpgradeButton());
    }

    /**
     * @return the upgrade button, if present
     */
    public WebElement getUpgradeButton()
    {
        return maybeFindElement(By.xpath(".//input[@name = 'actionInstall' and @value = 'Upgrade']"));
    }

    /**
     * Clicks on the downgrade button and waits for the downgrade plan to be computed.
     * 
     * @return the extension pane displaying the downgrade plan
     */
    public ExtensionPane downgrade()
    {
        return clickAndWaitForConfirmationOrJobDone(getDowngradeButton());
    }

    /**
     * @return the downgrade button, if present
     */
    public WebElement getDowngradeButton()
    {
        return maybeFindElement(By.xpath(".//input[@name = 'actionInstall' and @value = 'Downgrade']"));
    }

    /**
     * Confirms the current action and wait for it to be performed.
     * 
     * @return the extension pane displaying the extension after the current action has been performed
     */
    public ExtensionPane confirm()
    {
        WebElement button = getUtil().findElementWithoutWaiting(getDriver(), container, By.name("confirm"));
        return clickAndWaitForConfirmationOrJobDone(button);
    }

    /**
     * Clicks on the specified tab and returns the corresponding section if it's available.
     * 
     * @param label the tab label
     * @return the element that wraps the section corresponding to the specified tab
     */
    private WebElement clickTab(String label)
    {
        By tabXPath = By.xpath(".//*[@class = 'innerMenu']//a[normalize-space(.) = '" + label + "']");
        List<WebElement> found = getUtil().findElementsWithoutWaiting(getDriver(), container, tabXPath);
        if (found.size() == 0) {
            return null;
        }
        String sectionAnchor = StringUtils.substringAfterLast(found.get(0).getAttribute("href"), "#");
        found.get(0).click();
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
        List<WebElement> found =
            getUtil().findElementsWithoutWaiting(getDriver(), container, By.className("ui-progress"));
        return found.size() != 1 ? null : new ProgressBarPane(found.get(0));
    }
}
