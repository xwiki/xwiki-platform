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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
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

    @FindBy(xpath = "//a[contains(@href, 'section=Presentation')]")
    private WebElement presentationLink;

    @FindBy(id = "goto-select")
    WebElement spaceAdminSelect;

    public static AdministrationPage gotoPage()
    {
        getUtil().gotoPage(getSpace(), getPage(), "admin");
        return new AdministrationPage();
    }

    /**
     * Redirects to the administration page of a specified space.
     *
     * @param spaceReference the space reference
     * @return the administration page of the specified space
     * @since 7.2M3
     */
    public static AdministrationPage gotoSpaceAdministrationPage(SpaceReference spaceReference)
    {
        DocumentReference documentReference = new DocumentReference("WebPreferences", spaceReference);
        getUtil().gotoPage(documentReference, "admin");

        return new AdministrationPage();
    }

    /**
     * Redirects to the administration page of a specified space.
     *
     * @param spaceReferenceString the string serialized space reference
     * @return the administration page of the specified space
     * @since 7.2M3
     */
    public static AdministrationPage gotoSpaceAdministrationPage(String spaceReferenceString)
    {
        SpaceReference spaceReference = new SpaceReference(getUtil().resolveSpaceReference(spaceReferenceString));
        return gotoSpaceAdministrationPage(spaceReference);
    }

    public static String getURL()
    {
        return getUtil().getURL(getSpace(), getPage());
    }

    public static String getSpace()
    {
        return "XWiki";
    }

    public static String getPage()
    {
        return "XWikiPreferences";
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
     * @since 6.3M1
     */
    public PresentationAdministrationSectionPage clickPresentationSection()
    {
        this.presentationLink.click();
        return new PresentationAdministrationSectionPage();
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

    /**
     * @since 6.4M2
     */
    public ViewPage clickSection(String categoryName, String sectionName)
    {
        getDriver().findElement(By.xpath(
            "//div[contains(@class, 'admin-menu')]"
            + "/ul/li/span/a[text() = '" + categoryName + "']"
            + "/../../ul/li/span/a[text() = '" + sectionName + "']")).click();
        return new ViewPage();
    }

    /**
     * @since 6.4M2
     */
    public boolean hasSection(String categoryName, String sectionName)
    {
        return getDriver().hasElement(By.xpath(
            "//div[contains(@class, 'admin-menu')]"
            + "/ul/li/span/a[text() = '" + categoryName + "']"
            + "/../../ul/li/span/a[text() = '" + sectionName + "']"));
    }

    public boolean hasSection(String sectionName)
    {
        return getDriver().hasElement(By.xpath("//*[contains(@class, 'admin-menu')]//a[contains(@href, 'section="
            + sectionName + "')]"));
    }

    /**
     * @since 6.4M2
     */
    public boolean hasNotSection(String categoryName, String sectionName)
    {
        return getDriver().findElementsWithoutWaiting(By.xpath(
            "//div[contains(@class, 'admin-menu')]"
            + "/ul/li/span/a[text() = '" + categoryName + "']"
            + "/../../ul/li/span/a[text() = '" + sectionName + "']")).size() == 0;
    }

    public boolean hasNotSection(String sectionName)
    {
        return getDriver().findElementsWithoutWaiting(
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
        // FIXME: actually implement this (and maybe change its signature accordingly) once the new page administration
        // UI is implemented in http://jira.xwiki.org/browse/XWIKI-12219
        return gotoSpaceAdministrationPage(spaceName);
    }
}
