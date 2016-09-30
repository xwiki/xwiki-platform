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
package org.xwiki.test.ui.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the common actions possible on all Pages when using the "inline" action.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class InlinePage extends ViewPage
{
    /**
     * The XPath that locates a form field.
     */
    private static final String FIELD_XPATH_FORMAT = "//*[substring(@name, string-length(@name) - %s - 2) = '_0_%s']";

    @FindBy(name = "action_preview")
    private WebElement preview;

    @FindBy(name = "action_saveandcontinue")
    private WebElement saveandcontinue;

    @FindBy(name = "action_save")
    private WebElement save;

    @FindBy(name = "action_cancel")
    private WebElement cancel;

    @FindBy(id = "inline")
    private WebElement form;

    public void clickPreview()
    {
        preview.click();
    }

    public void clickSaveAndContinue()
    {
        clickSaveAndContinue(true);
    }

    /**
     * Clicks on the Save and Continue button. Use this instead of {@link #clickSaveAndContinue()} when you want to wait
     * for a different message (e.g. an error message).
     *
     * @param wait {@code true} to wait for the page to be saved, {@code false} otherwise
     */
    public void clickSaveAndContinue(boolean wait)
    {
        getSaveAndContinueButton().click();

        if (wait) {
            // Wait until the page is really saved.
            waitForNotificationSuccessMessage("Saved");
        }
    }

    /**
     * Use this method instead of {@link #clickSaveAndContinue()} and call {@link WebElement#click()} when you know that
     * the next page is not a standard XWiki {@link InlinePage}.
     *
     * @return the save and continue button used to submit the form.
     */
    public WebElement getSaveAndContinueButton()
    {
        return saveandcontinue;
    }

    public <T extends ViewPage> T clickSaveAndView()
    {
        clickSaveAndView(true);

        return createViewPage();
    }

    /**
     * Useful when the save and view operation could fail on the client side and a reload (the view part) might thus not
     * take place.
     *
     * @param wait {@code true} to wait for the page to be saved and reloaded, {@code false} otherwise
     * @since 7.4M2
     */
    public void clickSaveAndView(boolean wait)
    {
        if (wait) {
            getDriver().addPageNotYetReloadedMarker();
        }

        this.getSaveAndViewButton().click();

        if (wait) {
            // Since we might have a loading step between clicking Save&View and the view page actually loading
            // (specifically when using templates that have child documents associated), we need to wait for the save to
            // finish and for the redirect to occur.
            getDriver().waitUntilPageIsReloaded();
        }
    }

    /**
     * Use this method instead of {@link #clickSaveAndView()} and call {@link WebElement#click()} when you know that the
     * next page is not a standard XWiki {@link InlinePage}.
     *
     * @return the save and view button used to submit the form.
     */
    public WebElement getSaveAndViewButton()
    {
        return save;
    }

    public <T extends ViewPage> T clickCancel()
    {
        cancel.click();
        return createViewPage();
    }

    /**
     * Can be overridden to return extended {@link ViewPage}.
     */
    protected <T extends ViewPage> T createViewPage()
    {
        return (T) new ViewPage();
    }

    @Override
    public String getContent()
    {
        return form.getText();
    }

    /**
     * @return the form element
     */
    public WebElement getForm()
    {
        return form;
    }

    /**
     * Retrieves the value of the specified form field
     *
     * @param fieldName the name of a form field
     * @return the value of the specified form field
     * @since 7.0RC1
     */
    public String getValue(String fieldName)
    {
        String xpath = String.format(FIELD_XPATH_FORMAT, fieldName.length(), fieldName);
        return new FormElement(getForm()).getFieldValue(By.xpath(xpath));
    }

    /**
     * Sets the value of the specified form field
     *
     * @param fieldName the name of a form field
     * @param fieldValue the new value for the specified form field
     * @since 7.0RC1
     */
    public void setValue(String fieldName, String fieldValue)
    {
        String xpath = String.format(FIELD_XPATH_FORMAT, fieldName.length(), fieldName);
        WebElement field = getForm().findElement(By.xpath(xpath));
        if (field.getAttribute("name").equals(field.getAttribute("id"))) {
            new FormElement(getForm()).setFieldValue(field, fieldValue);
        } else {
            xpath = String.format("//*[@name = '%s' and @value = '%s']", field.getAttribute("name"), fieldValue);
            new FormElement(getForm()).setCheckBox(By.xpath(xpath), true);
        }
    }

    /**
     * @since 7.4M2
     */
    @Override
    public void waitUntilPageJSIsLoaded()
    {
        super.waitUntilPageJSIsLoaded();

        // Actionbuttons javascript for saving the page.
        getDriver().waitUntilJavascriptCondition(
            "return XWiki.actionButtons != undefined && " + "XWiki.actionButtons.EditActions != undefined && "
                + "XWiki.actionButtons.AjaxSaveAndContinue != undefined");
    }

}
