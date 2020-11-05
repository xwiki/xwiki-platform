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
        getDriver().waitUntilCondition(elementToBeClickable(this.refreshButton));
    }
}
