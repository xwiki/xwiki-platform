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
package org.xwiki.flamingo.test.po;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.editor.EditPage;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOf;

public class EditThemePage extends EditPage
{
    @FindBy(id = "autosync")
    private WebElement autoSyncCheckBox;

    @FindBy(id = "refresh")
    private WebElement refreshButton;

    public EditThemePage()
    {
        waitUntilReady();
    }

    public void selectVariableCategory(String category)
    {
        WebElement categoryElem =
            getDriver().findElement(By.xpath("//div[@id='panel-theme-variables']//div[@class='panel-body']"
                + "//li//a[@data-toggle='tab' and text()='" + category + "']"));
        categoryElem.click();
        // Wait until the panel is displayed
        getDriver().waitUntilElementIsVisible(
            By.xpath("//div[@id='bt-variables']//div[contains(@class, 'active')]/h2[text()='" + category + "']"));
    }

    public List<String> getVariableCategories()
    {
        List<String> results = new ArrayList<>();
        List<WebElement> categoryElems = getDriver().findElementsWithoutWaiting(
            By.xpath("//div[@id='panel-theme-variables']//div[@class='panel-body']" + "//li//a[@data-toggle='tab']"));
        for (WebElement elem : categoryElems) {
            results.add(elem.getText());
        }

        return results;
    }

    public void setAutoRefresh(boolean enabled)
    {
        if (this.autoSyncCheckBox.isEnabled() != enabled) {
            this.autoSyncCheckBox.click();
        }
    }

    public void setVariableValue(String variableName, String value)
    {
        WebElement variableField =
            getDriver().findElement(By.xpath("//label[text() = '@" + variableName + "']/..//input"));
        // Remove the previous value
        variableField.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        // Write the new one
        variableField.sendKeys(value);
    }

    /**
     * Set the value of a color variable with its color picker: click the input to display the picker, type the color in
     * the picker's hexadecimal field and submit it.
     *
     * @param variableName the name of the color variable (e.g. {@code brand-primary})
     * @param color the color to set, as a hexadecimal value (e.g. {@code #1a4d80})
     * @since 17.10.13
     * @since 18.4.5
     * @since 18.7.0RC1
     */
    public void pickVariableColor(String variableName, String color)
    {
        WebElement colorPicker = displayColorPicker(variableName);
        WebElement hexField = colorPicker.findElement(By.cssSelector(".colpick_hex_field input"));
        hexField.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        // The picker's field holds the hexadecimal digits alone, and it takes the new color into account when it fires
        // a change event, which Enter triggers.
        hexField.sendKeys(color.startsWith("#") ? color.substring(1) : color, Keys.ENTER);
        colorPicker.findElement(By.cssSelector(".colpick_submit")).click();

        // Submitting the color hides the picker.
        getDriver().waitUntilCondition(invisibilityOf(colorPicker));
    }

    /**
     * @param variableName the name of a color variable (e.g. {@code brand-primary})
     * @return the background color of the preview box displayed next to the variable's input, which is only filled in
     *         by the color picker, and thus tells whether the color picker got initialized
     * @since 17.10.13
     * @since 18.4.5
     * @since 18.7.0RC1
     */
    public String getColorPreview(String variableName)
    {
        return getDriver()
            .findElementWithoutWaiting(By.xpath("//input[@id = 'var-" + variableName
                + "']/following-sibling::span//span[@class = 'color-preview']"))
            .getCssValue("background-color");
    }

    /**
     * @param variableName the name of a color variable (e.g. {@code brand-primary})
     * @return the color picker of the passed variable, once it is displayed
     */
    private WebElement displayColorPicker(String variableName)
    {
        WebElement input = getDriver().findElement(By.id("var-" + variableName));
        input.click();
        return getDriver().waitUntilCondition(driver -> {
            WebElement colorPicker = getDisplayedColorPicker();
            if (colorPicker == null) {
                // The color theme editor hides the picker when the variables are scrolled, and clicking the input can
                // scroll it into view, so the picker may have been closed right after being displayed.
                input.click();
            }
            return colorPicker;
        });
    }

    /**
     * @return the color picker that is currently displayed, {@code null} if there is none; the color pickers of all the
     *         color variables are appended to the body, but only one of them is displayed at a time
     */
    private WebElement getDisplayedColorPicker()
    {
        return getDriver().findElementsWithoutWaiting(By.cssSelector("body > div.colpick")).stream()
            .filter(WebElement::isDisplayed)
            .findFirst()
            .orElse(null);
    }

    /**
     * Set the value of an image variable (e.g. {@code logo}) by uploading a new image with the attachment picker.
     * The uploaded image is only attached to the theme document when the theme is saved.
     *
     * @param variableName the name of the image variable to set
     * @param filePath the absolute path of the image to upload, as seen by the browser
     * @since 17.10.13
     * @since 18.4.5
     * @since 18.7.0RC1
     */
    public void setImageVariableValue(String variableName, String filePath)
    {
        getDriver().findElement(By.cssSelector("#var-" + variableName + " .attachment-picker-start")).click();
        // The picker is loaded in a modal dialog with an AJAX request.
        getDriver().waitUntilElementIsVisible(By.id("uploadAttachment"));
        getDriver().findElement(By.id("attachfile")).sendKeys(filePath);
        getDriver().findElement(By.cssSelector("#uploadAttachment input[type='submit']")).click();
        // The picker closes the dialog once the image has been uploaded and selected.
        getDriver().waitUntilElementDisappears(By.id("uploadAttachment"));
    }

    /**
     * @param variableName the name of the image variable
     * @return the file name of the image currently selected for the passed variable
     * @since 17.10.13
     * @since 18.4.5
     * @since 18.7.0RC1
     */
    public String getImageVariableValue(String variableName)
    {
        return getDriver()
            .findElementWithoutWaiting(By.cssSelector("#var-" + variableName + " input.property-reference"))
            .getAttribute("value");
    }

    /**
     * @since 6.3RC1
     */
    public void setTextareaValue(String variableName, String value)
    {
        WebElement variableField =
            getDriver().findElement(By.xpath("//label[text() = '@" + variableName + "']/..//textarea"));
        // Remove the previous value
        variableField.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE);
        // Write the new one
        variableField.sendKeys(value);
    }

    public void clickOnRefreshPreview()
    {
        waitUntilReady();
        this.refreshButton.click();
    }

    public void refreshPreview()
    {
        clickOnRefreshPreview();
        waitUntilReady();
    }

    public PreviewBox getPreviewBox()
    {
        return new PreviewBox();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ViewThemePage clickSaveAndView()
    {
        super.clickSaveAndView();
        return new ViewThemePage();
    }

    /**
     * Wait until the theme editor is ready for user interaction.
     * 
     * @since 12.9RC1
     */
    protected void waitUntilReady()
    {
        // The refresh button is disabled initially, until the preview is ready, and whenever a refresh is in progress.
        // Note: Putting a large timeout since the Theme preview is slow and can take a lot of time.
        getDriver().waitUntilCondition(elementToBeClickable(this.refreshButton), getDriver().getTimeout() * 10);
    }
}
