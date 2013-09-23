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
package org.xwiki.administration.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions possible on the main Administration Page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class AdministrationPage extends ViewPage
{
    @FindBy(xpath = "//a[contains(@href, 'section=Localization')]")
    WebElement localizationLink;

    @FindBy(xpath = "//a[contains(@href, 'section=Import')]")
    WebElement importLink;

    @FindBy(xpath = "//a[contains(@href, 'section=Registration')]")
    WebElement registrationLink;

    @FindBy(xpath = "//a[contains(@href, 'section=Users')]")
    WebElement usersLink;

    @FindBy(xpath = "//a[contains(@href, 'section=Rights')]")
    WebElement rightsLink;

    @FindBy(xpath = "//a[contains(@href, 'section=Annotations')]")
    WebElement annotationsLink;

    @FindBy(xpath = "//a[contains(@href, 'section=Extension Manager')]")
    WebElement extensionsLink;

    @FindBy(xpath = "//a[contains(@href, 'section=WYSIWYG')]")
    WebElement wysiwygLink;

    @FindBy(xpath = "//a[contains(@href, 'section=Elements')]")
    private WebElement pageElementsLink;

    @FindBy(id = "goto-select")
    WebElement spaceAdminSelect;

    public static AdministrationPage gotoPage()
    {
        getUtil().gotoPage("XWiki", "XWikiPreferences", "admin");
        return new AdministrationPage();
    }

    public LocalizationAdministrationSectionPage clickLocalizationSection()
    {
        this.localizationLink.click();
        return new LocalizationAdministrationSectionPage();
    }

    public ImportAdministrationSectionPage clickImportSection()
    {
        this.importLink.click();
        return new ImportAdministrationSectionPage();
    }

    public AdministrationSectionPage clickRegistrationSection()
    {
        this.registrationLink.click();
        return new AdministrationSectionPage("register");
    }

    public UsersAdministrationSectionPage clickUsersSection()
    {
        this.usersLink.click();
        return new UsersAdministrationSectionPage();
    }

    public GlobalRightsAdministrationSectionPage clickGlobalRightsSection()
    {
        this.rightsLink.click();
        return new GlobalRightsAdministrationSectionPage();
    }

    public AnnotationsPage clickAnnotationsSection()
    {
        this.annotationsLink.click();
        return new AnnotationsPage();
    }

    public WYSIWYGEditorAdministrationSectionPage clickWYSIWYGEditorSection()
    {
        this.wysiwygLink.click();
        return new WYSIWYGEditorAdministrationSectionPage();
    }

    /**
     * Opens the "Page Elements" administration section.
     * 
     * @return the "Page Elements" administration section
     */
    public PageElementsAdministrationSectionPage clickPageElementsSection()
    {
        pageElementsLink.click();
        return new PageElementsAdministrationSectionPage();
    }

    public boolean hasSection(String sectionName)
    {
        return getUtil().hasElement(By.xpath("//*[contains(@class, 'admin-menu')]//a[contains(@href, 'section="
            + sectionName + "')]"));
    }

    public boolean hasNotSection(String sectionName)
    {
        return getUtil().findElementsWithoutWaiting(getDriver(),
            By.xpath("//*[contains(@class, 'admin-menu')]//a[contains(@href, 'section="
                + sectionName + "')]")).size() == 0;
    }

    /**
     * Select the space to administer.
     *
     * Note that caller of the API need to wait for the page to be loaded since the selection of the page is done
     * asynchronously with JS.
     */
    public AdministrationPage selectSpaceToAdminister(String spaceName)
    {
        Select select = new Select(this.spaceAdminSelect);
        select.selectByVisibleText(spaceName);
        return new AdministrationPage();
    }
}
