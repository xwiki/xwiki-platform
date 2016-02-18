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
     * The XPath used to locate an extension action button.
     */
    private static final String ACTION_BUTTON_XPATH = ".//button[@name = 'extensionAction' and @value='%s']";

    /**
     * The name of the "class" attribute of HTML elements.
     */
    private static final String CLASS_ATTRIBUTE = "class";

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
        String[] classNames = container.getAttribute(CLASS_ATTRIBUTE).split("\\s+");
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
        List<WebElement> found = getDriver().findElementsWithoutWaiting(container, xpath);
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
        return getDriver().findElementWithoutWaiting(container, xpath).getText();
    }

    /**
     * @return the extension version
     */
    public String getVersion()
    {
        By xpath = By.xpath("*[@class = 'extension-header']//*[@class = 'extension-version']");
        return getDriver().findElementWithoutWaiting(container, xpath).getText();
    }

    /**
     * @return the extension authors
     */
    public List<WebElement> getAuthors()
    {
        return getDriver().findElementsWithoutWaiting(container, By.className("extension-author"));
    }

    /**
     * @return the extension summary
     */
    public String getSummary()
    {
        List<WebElement> found = getDriver().findElementsWithoutWaiting(container, By.className("extension-summary"));
        return found.size() > 0 ? found.get(0).getText() : null;
    }

    /**
     * Clicks on the show details button.
     * 
     * @return the extension pane showing the extension details
     */
    public ExtensionPane showDetails()
    {
        WebElement showDetailsButton = getShowDetailsButton();
        if (showDetailsButton.getAttribute(CLASS_ATTRIBUTE).contains("visibilityAction")) {
            // Just toggle show/hide details.
            showDetailsButton.click();
            return this;
        } else {
            // Retrieve the details. Wait until the extension body is not loading.
            return clickAndWaitUntilElementIsVisible(showDetailsButton, "/*[@class = 'extension-body']");
        }
    }

    /**
     * @return the button used to show the extension details
     */
    public WebElement getShowDetailsButton()
    {
        return maybeFindElement(By.xpath(String.format(ACTION_BUTTON_XPATH, "showDetails")));
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
        getDriver().waitUntilElementIsVisible(By.xpath(xpath + xpathSuffix));
        // We have to create a new extension pane because the DOM has changed.
        return new ExtensionPane(getDriver().findElementWithoutWaiting(By.xpath(xpath)));
    }

    /**
     * @return the XPath used to locate this extension
     */
    private String getXPath()
    {
        String nameAndVersion =
            getDriver().findElementWithoutWaiting(container, By.className("extension-title")).getText();
        return String.format("//form[contains(@class, 'extension-item') and descendant::*["
            + "contains(@class, 'extension-title') and normalize-space(.) = '%s']]", nameAndVersion);
    }

    /**
     * Clicks on the hide details button.
     * 
     * @return the extension pane that doesn't show the extension details
     */
    public ExtensionPane hideDetails()
    {
        getHideDetailsButton().click();
        return this;
    }

    /**
     * @return the button used to hide the extension details
     */
    public WebElement getHideDetailsButton()
    {
        return maybeFindElement(By.xpath(String.format(ACTION_BUTTON_XPATH, "hideDetails")));
    }

    /**
     * Clicks on the given button and waits for a confirmation or for the job/action to be done.
     * 
     * @param button the button to be clicked
     * @return the extension pane showing the confirmation or the job log
     */
    private ExtensionPane clickAndWaitForConfirmationOrJobDone(WebElement button)
    {
        // Wait until the the continue button is present or the extension is not loading and both the extension body and
        // the progress section are present and not loading.
        return clickAndWaitUntilElementIsVisible(button,
            "[descendant::button[@name = 'extensionAction' and @value = 'continue' and not(@disabled)] or ("
            + "not(contains(@class, 'loading')) and descendant::*[@class = 'extension-body']"
            + "/*[@class = 'extension-body-progress extension-body-section'])]");
    }

    /**
     * Clicks on the install button and waits for the install plan to be computed.
     * 
     * @return the extension pane displaying the install plan
     */
    public ExtensionPane install()
    {
        return maybeOpenActionDropDownMenu().clickAndWaitForConfirmationOrJobDone(getInstallButton());
    }

    /**
     * @return the install button, if present
     */
    public WebElement getInstallButton()
    {
        return maybeFindElement(By.xpath(String.format(ACTION_BUTTON_XPATH, "install")));
    }

    /**
     * @param locator specifies the element to look for
     * @return the specified element, if found, {@code null} otherwise
     */
    private WebElement maybeFindElement(By locator)
    {
        List<WebElement> found = getDriver().findElementsWithoutWaiting(container, locator);
        return found.size() > 0 ? found.get(0) : null;
    }

    /**
     * Clicks on the drop-down toggle, if present, to expand the group of extension action buttons.
     * 
     * @return this
     */
    private ExtensionPane maybeOpenActionDropDownMenu()
    {
        String xpath = ".//*[@class = 'extension-actions']//*[@class = 'dropdown-toggle']";
        List<WebElement> found = getDriver().findElementsWithoutWaiting(container, By.xpath(xpath));
        if (found.size() > 0) {
            found.get(0).click();
        }
        return this;
    }

    /**
     * Clicks on the uninstall button and waits for the uninstall plan to be computed.
     * 
     * @return the extension pane displaying the uninstall plan
     */
    public ExtensionPane uninstall()
    {
        return maybeOpenActionDropDownMenu().clickAndWaitForConfirmationOrJobDone(getUninstallButton());
    }

    /**
     * @return the uninstall button, if present
     */
    public WebElement getUninstallButton()
    {
        return maybeFindElement(By.xpath(String.format(ACTION_BUTTON_XPATH, "uninstall")));
    }

    /**
     * Clicks on the upgrade button and waits for the upgrade plan to be computed.
     * 
     * @return the extension pane displaying the upgrade plan
     */
    public ExtensionPane upgrade()
    {
        return maybeOpenActionDropDownMenu().clickAndWaitForConfirmationOrJobDone(getUpgradeButton());
    }

    /**
     * @return the upgrade button, if present
     */
    public WebElement getUpgradeButton()
    {
        return maybeFindElement(By.xpath(String.format(ACTION_BUTTON_XPATH, "upgrade")));
    }

    /**
     * Clicks on the downgrade button and waits for the downgrade plan to be computed.
     * 
     * @return the extension pane displaying the downgrade plan
     */
    public ExtensionPane downgrade()
    {
        return maybeOpenActionDropDownMenu().clickAndWaitForConfirmationOrJobDone(getDowngradeButton());
    }

    /**
     * @return the downgrade button, if present
     */
    public WebElement getDowngradeButton()
    {
        return maybeFindElement(By.xpath(String.format(ACTION_BUTTON_XPATH, "downgrade")));
    }

    /**
     * Confirms the current action and wait for it to be performed.
     * 
     * @return the extension pane displaying the extension after the current action has been performed
     */
    public ExtensionPane confirm()
    {
        return clickAndWaitForConfirmationOrJobDone(getContinueButton());
    }

    /**
     * @return the button used to continue the current job or to execute a previously computed job plan
     */
    public WebElement getContinueButton()
    {
        return maybeFindElement(By.xpath(String.format(ACTION_BUTTON_XPATH, "continue")));
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
        List<WebElement> found = getDriver().findElementsWithoutWaiting(container, tabXPath);
        if (found.size() == 0) {
            return null;
        }
        String sectionAnchor = StringUtils.substringAfterLast(found.get(0).getAttribute("href"), "#");
        found.get(0).click();
        By sectionXPath =
            By.xpath(".//*[contains(@class, 'extension-body-section') and preceding-sibling::*[1][@id = '"
                + sectionAnchor + "']]");
        return getDriver().findElementWithoutWaiting(container, sectionXPath);
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
        List<WebElement> found = getDriver().findElementsWithoutWaiting(container, By.className("ui-progress"));
        return found.size() != 1 ? null : new ProgressBarPane(found.get(0));
    }
}
