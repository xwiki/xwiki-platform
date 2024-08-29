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
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents common actions available in all Administration pages.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class AdministrationSectionPage extends ViewPage
{
    /**
     * There's no id to get only the admin page content so getting the main content area (which includes the
     * breadcrumb and title) is the best we can do, until we add some id for the content of admin pages.
     */
    @FindBy(id = "mainContentArea")
    private WebElement content;

    /**
     * The admin-page-content div is being treated as a form since it may contain multiple forms and we want to be able
     * to access elements in them all.
     */
    @FindBy(xpath = "//div[@id='admin-page-content']")
    private WebElement formContainer;

    private String section;

    /**
     * See {@link #AdministrationSectionPage(String, boolean)}.
     */
    private boolean asyncSave;

    public AdministrationSectionPage(String section)
    {
        this(section, false);
    }

    /**
     * @param section the name of the section in the Admin UI vertical menu
     * @param asyncSave whether the save button in the administration section page executes the save async or not. If
     *        you're not using a {@code ConfigurableClass} with a custom {@code codeToExecute} then asyncSave should
     *        be false. This is until we make all admin sections save async.
     * @since 16.6.0RC1
     */
    public AdministrationSectionPage(String section, boolean asyncSave)
    {
        this.section = section;
        this.asyncSave = asyncSave;
    }

    public static AdministrationSectionPage gotoPage(String section)
    {
        getUtil().gotoPage(getURL(section));
        return new AdministrationSectionPage(section);
    }

    /**
     * Go to the administration section of a given space reference.
     * @since 11.3RC1
     */
    public static AdministrationSectionPage gotoSpaceAdministration(SpaceReference spaceReference, String section)
    {
        getUtil().gotoPage(getURL(section, spaceReference));
        return new AdministrationSectionPage(section);
    }

    /**
     * @param section the section ID
     * @return the URL of the administration section corresponding to the current {@link AdministrationSectionPage}
     *         instance
     * @since 6.3M1
     */
    public static String getURL(String section)
    {
        return getURL(section, null);
    }

    /**
     * @param section the section ID
     * @param spaceReference the space where we want to get the admin page
     * @return the URL of the administration section corresponding to the current {@link AdministrationSectionPage}
     *         instance
     * @since 11.3RC1
     */
    public static String getURL(String section, SpaceReference spaceReference)
    {
        String url;
        if (spaceReference == null) {
            url = getUtil().getURL("XWiki", "XWikiPreferences", "admin", String.format("section=%s", section));
        } else {
            DocumentReference documentReference = new DocumentReference("WebPreferences", spaceReference);
            url = getUtil().getURL(documentReference, "admin", String.format("editor=spaceadmin&section=%s", section));
        }

        return url;
    }

    public String getURL()
    {
        return getURL(this.section);
    }

    public void clickSave()
    {
        // Note: There are some administration sections that are submitted asynchronously, but they use a custom
        // success message. For these cases, their PO should override the {@link #clickSave(boolean)} method to perform
        // the wait themselves.
        clickSave(this.asyncSave);
    }

    /**
     * The only reason to call with wait as false is when the admin section is using a custom success message. See
     * {@link #clickSave()}'s implementation.
     */
    public void clickSave(boolean wait)
    {
        WebElement saveButton;
        if (this.asyncSave) {
            saveButton = getDriver().findElement(By.xpath("//input[@type='submit'][@name='action_saveandcontinue']"));
        } else {
            saveButton = getDriver().findElement(By.xpath("//input[@type='submit'][@name='formactionsac']"));
        }
        saveButton.click();

        if (wait) {
            // Wait until the page is really saved.
            waitForNotificationSuccessMessage("Saved");
        }
    }

    /**
     * @return the first form contained in this section.
     * @deprecated this method can be ambiguous since an administration section page might contain several forms.
     *              The method {@link #getFormContainerElement(String)} should be used instead.
     */
    @Deprecated
    public FormContainerElement getFormContainerElement()
    {
        return new FormContainerElement(this.formContainer);
    }

    /**
     * @param formId ID of the form to reach.
     * @return the form identified by its id.
     * @since 11.5RC1
     * @since 11.3.1
     */
    public FormContainerElement getFormContainerElement(String formId)
    {
        return new FormContainerElement(By.id(formId));
    }

    /**
     * @param documentClass the class name for which we want the dedicated form.
     * @return the form dedicated to the given class.
     * @since 11.5RC1
     * @since 11.3.1
     */
    public FormContainerElement getFormContainerElementForClass(String documentClass)
    {
        String formContainerId = String.format("%s_%s", this.section, documentClass);
        return getFormContainerElement(formContainerId);
    }

    public boolean hasLink(String linkName)
    {
        String xPathSelector = String.format("//form/fieldset//a[@href='%s']", linkName);
        return getDriver().hasElementWithoutWaiting(By.xpath(xPathSelector));
    }

    public boolean hasHeading(int level, String headingId)
    {
        String xPath = String.format("//div[@id='admin-page-content']/h%s[@id='%s']/span", level, headingId);
        return getDriver().hasElementWithoutWaiting(By.xpath(xPath));
    }

    /**
     * If save button is present wait the action button js to be loaded.
     * @since 11.6RC1
     */
    public void waitUntilActionButtonIsLoaded()
    {
        if (getDriver().hasElementWithoutWaiting(By.xpath("//input[@type='submit'][@name='formactionsac']"))) {
            getDriver().waitUntilJavascriptCondition(
                "return typeof XWiki !== 'undefined' "
                    + "&& XWiki.actionButtons != undefined "
                    + "&& XWiki.actionButtons.EditActions != undefined "
                    + "&& XWiki.actionButtons.AjaxSaveAndContinue != undefined");
        }
    }

    @Override
    public String getContent()
    {
        return this.content.getText();
    }
}
