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
import java.util.Collection;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

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
    
    public Collection<String> getApplicationsInBar()
    {
        return getApplicationsInPanel(displayedPanels);
    }

    public Collection<String> getApplicationsNotInBar()
    {
        return getApplicationsInPanel(blacklistedPanels);
    }
    
    private Collection<String> getApplicationsInPanel(WebElement panel)
    {
        Collection<String> results = new ArrayList<>();
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
        WebElement app = fromPanel.findElement(By.linkText(appName));
        new Actions(getDriver()).dragAndDrop(app, panel).perform();
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
