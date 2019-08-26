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
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.administration.test.po.AdministrationPage;
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

        WebElement app = displayedPanels.findElement(By.linkText(appName));
        WebElement appBefore = displayedPanels.findElement(By.linkText(appBeforeName));
        Point target = appBefore.getLocation();
        Point source = app.getLocation();

        // The drag & drop of the "sortable" plugin of "jquery-ui" is very sensitive so we need to script the
        // moves of the mouse precisely if we don't want to have flickers.
        Actions actions = new Actions(getDriver().getWrappedDriver());
        // First we hold the app
        actions.clickAndHold(app);
        // Then we move into the position of the targeted app so jquery-ui can register we want to take its place.
        actions.moveByOffset(target.getX() - source.getX(), target.getY() - source.getY());
        // Now we do a little move on top left so jquery-ui understand we want to be *before* the other app and
        // put a blank place instead of the other app.
        actions.moveByOffset(-5, -5);
        // Do it
        actions.perform();

        // Before releasing the click, check that jquery-ui has moved the other app to let the place free.
        getDriver().waitUntilCondition(new ExpectedCondition<Object>()
        {
            @Override
            public Object apply(WebDriver webDriver)
            {
                Point newTarget = appBefore.getLocation();
                Point newSource = app.getLocation();
                return newTarget.getX() > newSource.getX() + 5 || newTarget.getY() > newSource.getY() + 5;
            }
        });

        // Now we can release the selection
        actions = new Actions(getDriver().getWrappedDriver());
        actions.release();
        actions.perform();
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
