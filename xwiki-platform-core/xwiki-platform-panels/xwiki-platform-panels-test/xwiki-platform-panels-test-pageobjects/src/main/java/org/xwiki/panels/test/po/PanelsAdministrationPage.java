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

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the Panels administration section.
 *
 * @version $Id$
 * @since 9.2RC1
 */
public class PanelsAdministrationPage extends ViewPage
{
    @FindBy(xpath = "//a[@role = 'tab' and normalize-space() = 'Page Layout']")
    private WebElement pageLayoutTab;

    @FindBy(xpath = "//a[@role = 'tab' and normalize-space() = 'Panel List']")
    private WebElement panelListTab;

    @FindBy(id = "saveLayout")
    private WebElement saveLink;

    @FindBy(id = "revertLayout")
    private WebElement resetLink;

    @FindBy(xpath = "//a[normalize-space() = 'Go to Panels']")
    private WebElement gotoPanelsLink;

    /**
     * Navigates to the panels administration page using the administration menu.
     *
     * @return an panels administration page object
     */
    public static PanelsAdministrationPage navigate()
    {
        AdministrationPage.gotoPage().clickSection("Look & Feel", "Panels");
        return new PanelsAdministrationPage();
    }

    /**
     * Goes to the panels administration page, with the {@code force} and {@code forceEdit} url parameters 
     * set to {@code true}.
     *
     * @return a panels administration page object
     */
    public static PanelsAdministrationPage gotoPage()
    {
        Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put("editor", "globaladmin");
        queryParameters.put("section", "Panels.PanelWizard");
        // Forces the access to the administration when XWikiPreferences is locked (admin.vm)
        queryParameters.put("force", true);
        // Forces the access to the administration when XWiki preferences is locked (ConfigurableClass)
        queryParameters.put("forceEdit", true);
        getUtil()
            .gotoPage(new DocumentReference("xwiki", "XWiki", "XWikiPreferences"), "admin", queryParameters);
        return new PanelsAdministrationPage();
    }

    public PageLayoutTabContent selectPageLayout()
    {
        this.pageLayoutTab.click();
        return new PageLayoutTabContent();
    }

    public boolean hasPageLayoutTab()
    {
        return this.pageLayoutTab.isDisplayed();
    }

    public boolean hasPanelListTab()
    {
        return this.panelListTab.isDisplayed();
    }

    public boolean hasGotoPanelsLink()
    {
        return this.gotoPanelsLink.isDisplayed();
    }

    public PanelsHomePage clickGotoPanels()
    {
        this.gotoPanelsLink.click();
        return new PanelsHomePage();
    }

    public boolean hasResetLink()
    {
        return this.resetLink.isDisplayed();
    }

    public void clickResetLink()
    {
        this.resetLink.click();
    }

    public PanelsAdministrationPage clickSave()
    {
        this.saveLink.click();
        waitForNotificationSuccessMessage("The layout has been saved properly.");
        return new PanelsAdministrationPage();
    }
}
