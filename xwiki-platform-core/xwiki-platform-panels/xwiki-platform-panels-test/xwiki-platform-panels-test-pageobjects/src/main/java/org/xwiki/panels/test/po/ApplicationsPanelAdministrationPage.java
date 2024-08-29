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
package org.xwiki.panels.test.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.test.ui.po.SortableElement;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.assertTrue;

/**
 * Represents the applications panel administration page (PanelsCode.ApplicationsPanelConfiguration).
 *
 * @version $Id$
 * @since 7.1M1
 * @since 7.0.1
 * @since 6.4.4
 */
public class ApplicationsPanelAdministrationPage extends ViewPage
{
    @FindBy(id = "displayedPanels")
    private WebElement displayedPanels;

    @FindBy(id = "blacklistedPanels")
    private WebElement blacklistedPanels;

    @FindBy(id = "bt-revert")
    private WebElement revertButton;

    @FindBy(id = "bt-save")
    private WebElement saveButton;

    private final SortableElement sortableDisplayedPanels = new SortableElement(this.displayedPanels);

    public static ApplicationsPanelAdministrationPage gotoPage()
    {
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        assertTrue(administrationPage.hasSection("panels.applications"));
        administrationPage.clickSection("Look & Feel", "Applications Panel");
        return new ApplicationsPanelAdministrationPage();
    }
    
    public List<String> getApplicationsInBar()
    {
        return getApplicationsInPanel(displayedPanels);
    }

    public List<String> getApplicationsNotInBar()
    {
        return getApplicationsInPanel(blacklistedPanels);
    }
    
    private List<String> getApplicationsInPanel(WebElement panel)
    {
        List<String> results = new ArrayList<>();
        for (WebElement elem : getDriver().findElementsWithoutWaiting(panel,
                By.xpath("div[contains(@class, 'panel-body')]/ul"
                        + "/li[contains(@class, 'draggableApp')]//span[contains(@class, 'application-label')]"))) {
            results.add(elem.getText());
        }

        return results;
    }

    public void addApplicationInBar(String appName)
    {
        moveAppToPanel(appName, displayedPanels, blacklistedPanels);
    }
    
    public void removeApplicationFromBar(String appName)
    {
        moveAppToPanel(appName, blacklistedPanels, displayedPanels);
    }

    private void moveAppToPanel(String appName, WebElement panel, WebElement fromPanel)
    {
        By appSelector = By.linkText(appName);
        WebElement app = fromPanel.findElement(appSelector);
        WebElement destination = panel.findElement(By.tagName("ul"));
        getDriver().dragAndDrop(app, destination);

        getDriver().waitUntilCondition(webDriver ->
            getDriver().hasElementWithoutWaiting(panel, appSelector)
                    && !getDriver().hasElementWithoutWaiting(fromPanel, appSelector)
        );
    }

    public void moveAppBefore(String appName, String appBeforeName)
    {
        if (appName.equals(appBeforeName)) {
            // do nothing
            return;
        }

        this.sortableDisplayedPanels.moveBefore(By.linkText(appName), By.linkText(appBeforeName));
    }

    public void revert()
    {
        revertButton.click();   
    }

    public void save()
    {
        saveButton.click();
    }
    
    public boolean hasSuccessNotification()
    {
        WebElement notification = getDriver().findElement(By.className("xnotification-done"));
        return notification != null && "The configuration has been saved.".equals(notification.getText());
    }
}
