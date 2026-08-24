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
package org.xwiki.image.style.test.po;

import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page object of the image style administration.
 *
 * @version $Id$
 * @since 14.3RC1
 */
public class ImageStyleAdministrationPage extends ViewPage
{
    private static final String DEFAULT_IMAGE_STYLE_FORM_ID = "defaultImageStyleForm";

    private static final String DEFAULT_IMAGE_STYLE_FIELD_NAME = "Image.Style.Code.ConfigurationClass_0_defaultStyle";

    /**
     * Timeout, in seconds, granted to the identifier validation round-trip that enables the creation button. It is
     * larger than the default timeout because that round-trip is a debounced asynchronous request, and the first one
     * hitting the validation page is slow on a loaded machine.
     */
    private static final int VALIDATION_TIMEOUT_SECONDS = 30;

    /**
     * @param wikiReference the reference of the wiki containing the admin to access
     * @return the page object for the administration of the image styles
     */
    public static ImageStyleAdministrationPage getToAdminPage(WikiReference wikiReference)
    {
        getUtil().gotoPage(new DocumentReference(wikiReference.getName(), "XWiki", "XWikiPreferences"), "admin", Map.of(
            "editor", "globaladmin",
            "section", "image.style"
        ));
        return new ImageStyleAdministrationPage();
    }

    /**
     * Create a configuration form from an identifier.
     *
     * @param identifier the identifier (e.g., "frameless")
     * @return the page object of the image style configuration form
     */
    public ImageStyleConfigurationForm submitNewImageStyleForm(String identifier)
    {
        XWikiWebDriver driver = getDriver();
        driver.findElement(By.id("targetTitle")).sendKeys(identifier);
        WebElement submitButton = driver.findElement(By.cssSelector("#newImageStyleForm input[type='submit']"));
        // The button is rendered disabled and is only enabled once the identifier has been normalized by a debounced
        // request to the entity name validation page, so wait for that request to complete before clicking.
        int currentTimeout = driver.getTimeout();
        try {
            driver.setTimeout(VALIDATION_TIMEOUT_SECONDS);
            driver.waitUntilElementIsEnabled(submitButton);
        } finally {
            driver.setTimeout(currentTimeout);
        }
        submitButton.click();
        return new ImageStyleConfigurationForm();
    }

    /**
     * Select the identifier of the default image style and save it.
     *
     * @param identifier the identifier of the default image style
     */
    public void submitDefaultStyleForm(String identifier)
    {
        new FormContainerElement(By.id(DEFAULT_IMAGE_STYLE_FORM_ID))
            .setFieldValue(By.name(DEFAULT_IMAGE_STYLE_FIELD_NAME), identifier);
        getDriver().findElement(By.id(DEFAULT_IMAGE_STYLE_FORM_ID)).findElement(By.cssSelector("input[type='submit']"))
            .click();
    }

    /**
     * @return the value of the current default style
     */
    public String getDefaultStyle()
    {
        return new FormContainerElement(By.id(DEFAULT_IMAGE_STYLE_FORM_ID)).getFieldValue(By.name(
            DEFAULT_IMAGE_STYLE_FIELD_NAME));
    }
}
