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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.administration.test.po.AdministrationPage;
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

    @FindBy(id = "saveLayout")
    private WebElement saveLink;

    public static PanelsAdministrationPage gotoPage()
    {
        AdministrationPage.gotoPage().clickSection("Look & Feel", "Panels");
        return new PanelsAdministrationPage();
    }

    public PageLayoutTabContent selectPageLayout()
    {
        this.pageLayoutTab.click();
        return new PageLayoutTabContent();
    }

    public PanelsAdministrationPage clickSave()
    {
        this.saveLink.click();
        waitForNotificationSuccessMessage("The layout has been saved properly.");
        return new PanelsAdministrationPage();
    }
}
