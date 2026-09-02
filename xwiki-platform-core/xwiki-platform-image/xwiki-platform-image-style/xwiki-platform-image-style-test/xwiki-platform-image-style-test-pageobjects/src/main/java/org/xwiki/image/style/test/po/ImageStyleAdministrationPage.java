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

import java.net.URI;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.ui.TestUtils;
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
     * Waits for the section to be displayed and checks that it carries the fields of the configuration object.
     * <p>
     * The wait matters because this page object is also built right after clicking a link, i.e. possibly before the
     * browser left the previous page. The check turns a section rendered without those fields into an explicit
     * failure, instead of letting a later interaction report an opaque "element not found" on one of them. The section
     * renders that way when {@code Image.Style.Code.ConfigurationClass} is not visible to the server, which leaves the
     * property labels as unevaluated Velocity and both fields empty.
     *
     * @since 18.8.0RC1
     */
    public ImageStyleAdministrationPage()
    {
        XWikiWebDriver driver = getDriver();
        driver.waitUntilElementIsVisible(By.id(DEFAULT_IMAGE_STYLE_FORM_ID));
        if (!driver.hasElementWithoutWaiting(By.name(DEFAULT_IMAGE_STYLE_FIELD_NAME))) {
            throw new AssertionError("The image style administration section was rendered without the fields of the "
                + "[Image.Style.Code.ConfigurationClass] object, which means that object or its class was not visible "
                + "to the server while rendering the section.\n"
                + "Section rendered by the server:\n" + getDefaultImageStyleFormMarkup() + '\n'
                + "Configuration document as served by REST:\n" + getConfigurationDocumentFromRest());
        }
    }

    /**
     * @return the markup of the default image style form, as the server rendered it, so that a failure shows which of
     *     the fields and labels of the configuration object are missing
     */
    private String getDefaultImageStyleFormMarkup()
    {
        try {
            return getDriver().findElementWithoutWaiting(By.id(DEFAULT_IMAGE_STYLE_FORM_ID))
                .getAttribute("outerHTML");
        } catch (Exception e) {
            return String.format("could not be read: %s", ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * @return the REST representation of the configuration document, objects included. REST does not read the document
     *     through the same code path as the rendering of the section, so comparing the two tells whether the object is
     *     missing from the database or only invisible to the request that rendered the section
     */
    private String getConfigurationDocumentFromRest()
    {
        TestUtils testUtils = getUtil();
        String uri = String.format("%s/wikis/%s/spaces/Image/spaces/Style/spaces/Code/pages/Configuration/objects",
            testUtils.rest().getBaseURL(), testUtils.getCurrentWiki());
        try {
            return testUtils.rest().executeGet(URI.create(uri)).getResponseBodyAsString();
        } catch (Exception e) {
            return String.format("[%s] could not be read: %s", uri, ExceptionUtils.getRootCauseMessage(e));
        }
    }

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
