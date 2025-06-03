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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.ui.po.DocumentPicker;

/**
 * Represents the actions possible on the Templates Administration Page.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class TemplatesAdministrationSectionPage extends AdministrationSectionPage
{
    public static final String ADMINISTRATION_SECTION_ID = "Templates";

    /**
     * The element that contains the document picker used to select the target document.
     */
    @FindBy(className = "location-picker")
    private WebElement documentPickerElement;

    @FindBy(id = "createTemplateProvider")
    private WebElement createButton;

    /**
     * @since 4.2M1
     */
    public static TemplatesAdministrationSectionPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(ADMINISTRATION_SECTION_ID);
        return new TemplatesAdministrationSectionPage();
    }

    public TemplatesAdministrationSectionPage()
    {
        super(ADMINISTRATION_SECTION_ID);
    }

    /**
     * @return the document picker used to select the target document
     */
    public DocumentPicker getDocumentPicker()
    {
        return new DocumentPicker(this.documentPickerElement);
    }

    public TemplateProviderInlinePage createTemplateProvider(String space, String page)
    {
        DocumentPicker documentPicker = getDocumentPicker();
        documentPicker.toggleLocationAdvancedEdit();
        documentPicker.setParent(space);
        documentPicker.setName(page);
        // Wait until the location has been updated such that no unexpected scrolling happens while the submit button
        // is pressed.
        List<String> path = new ArrayList<>();
        path.add("");
        path.addAll(List.of(StringUtils.split(space, ".")));
        path.add(page);
        documentPicker.waitForLocation(path);

        this.clickOnCreateButton();

        return new TemplateProviderInlinePage();
    }

    private void clickOnCreateButton()
    {
        // Livevalidation blocks the form submission until the validated fields are valid. Until the form is validated,
        // Livevalidation prevents the submission button to be clicked.
        // On some rare cases, the click of the button is done too early and the form submission is prevented.
        // To be sure that the form is validated before clicking, wait for the two fields validation messages to be
        // displayed before clicking.
        getDriver().waitUntilCondition(input ->
            getDriver().findElementsWithoutWaiting(By.cssSelector("form .LV_validation_message.LV_valid"))
                .size() == 1 &&
            getDriver().findElementsWithoutWaiting(By.cssSelector("form .LV_validation_message.LV_invalid")).isEmpty());

        // FIXME: workaround for https://github.com/mozilla/geckodriver/issues/1026
        getDriver().addPageNotYetReloadedMarker();
        this.createButton.click();
        getDriver().waitUntilPageIsReloaded();
    }

    /**
     * Takes the user to the form used to create the template provider.
     * 
     * @param templateProviderReference the reference of the template provider page to create
     * @return the page to edit the template provider
     * @since 12.9RC1
     */
    public TemplateProviderInlinePage createTemplateProvider(LocalDocumentReference templateProviderReference)
    {
        return createTemplateProvider(getUtil().serializeReference(templateProviderReference.getParent()),
            templateProviderReference.getName());
    }

    public List<WebElement> getExistingTemplatesLinks()
    {
        // A bit unreliable here, but it's the best I can do. Don't wait as these links are part of the static HTML.
        return getDriver().findElementsWithoutWaiting(
            By.xpath("//ul[preceding-sibling::*[. = 'Available Template Providers']]//a"));
    }
}
