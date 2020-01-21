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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
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

    private DocumentPicker documentPicker;

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
        this.createButton.click();

        return new TemplateProviderInlinePage();
    }

    public List<WebElement> getExistingTemplatesLinks()
    {
        // A bit unreliable here, but it's the best I can do.
        return getDriver().findElements(By.xpath("//ul[preceding-sibling::*[. = 'Available Template Providers']]//a"));
    }
}
