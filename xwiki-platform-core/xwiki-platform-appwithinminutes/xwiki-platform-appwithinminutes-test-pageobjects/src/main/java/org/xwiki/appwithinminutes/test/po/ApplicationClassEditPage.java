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
package org.xwiki.appwithinminutes.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the actions available when editing the application class. This is also the second step of the App Within
 * Minutes wizard, in which the application structure defined.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ApplicationClassEditPage extends InlinePage
{
    @FindBy(id = "wizard-next")
    private WebElement nextStepButton;

    @FindBy(linkText = "PREVIOUS STEP")
    private WebElement previousStepButton;

    /**
     * The form used to edit the application class overwrites the save button because it needs to process the submitted
     * data. Otherwise the request is forwarded by the action filter to the save action.
     */
    @FindBy(name = "xaction_save")
    private WebElement saveButton;

    /**
     * @see #saveButton
     */
    @FindBy(name = "xaction_saveandcontinue")
    private WebElement saveAndContinueButton;

    @FindBy(id = "palette")
    private WebElement palette;

    @FindBy(id = "fields")
    private WebElement fields;

    @FindBy(id = "updateClassSheet")
    private WebElement updateClassSheetCheckbox;

    /**
     * Clicks on the Next Step button.
     * 
     * @return the page that represents the next step of the App Within Minutes wizard
     */
    public ApplicationHomeEditPage clickNextStep()
    {
        nextStepButton.click();
        return new ApplicationHomeEditPage();
    }

    /**
     * Clicks on the Previous Step button.
     * 
     * @return the page that represents the previous step of the App Within Minutes wizard
     */
    public ApplicationCreatePage clickPreviousStep()
    {
        previousStepButton.click();
        return new ApplicationCreatePage();
    }

    /**
     * @return {@code true} if the Previous Step button is present, {@code false} otherwise
     */
    public boolean hasPreviousStep()
    {
        return getUtil().findElementsWithoutWaiting(getDriver(), By.linkText("PREVIOUS STEP")).size() > 0;
    }

    @Override
    public <T extends ViewPage> T clickSaveAndView()
    {
        saveButton.click();
        return createViewPage();
    }

    @Override
    public void clickSaveAndContinue()
    {
        saveAndContinueButton.click();
    }

    /**
     * Drags a field of the specified type from the field palette to the field canvas.
     * 
     * @param fieldType the type of field to add, as displayed on the field palette
     */
    public ClassFieldEditPane addField(String fieldType)
    {
        String fieldXPath = "//span[@class = 'field' and normalize-space(.) = '%s']";
        WebElement field = palette.findElement(By.xpath(String.format(fieldXPath, fieldType)));
        int fieldCount = getUtil().findElementsWithoutWaiting(getDriver(), fields, By.xpath("li")).size();
        // NOTE: We scroll the page up because the drag&drop fails sometimes if the dragged field and the canvas (drop
        // target) are not fully visible. See https://code.google.com/p/selenium/issues/detail?id=3075 .
        palette.sendKeys(Keys.HOME);
        new Actions(getDriver()).dragAndDrop(field, fields).perform();
        final WebElement addedField = fields.findElement(By.xpath("li[" + (fieldCount + 1) + "]"));

        getUtil().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                try {
                    return !addedField.getAttribute("class").contains("loading");
                } catch (NotFoundException e) {
                    return false;
                } catch (StaleElementReferenceException e) {
                    // The element was removed from DOM in the meantime
                    return false;
                }
            }
        });

        return new ClassFieldEditPane(addedField.getAttribute("id").substring("field-".length()));
    }

    /**
     * Moves the class field specified by the first parameter before the class field specified by the second parameter
     * 
     * @param fieldToMove the class field to be moved
     * @param beforeField the class field before which to insert the field being moved
     */
    public void moveFieldBefore(String fieldToMove, String beforeField)
    {
        // Drag the field slightly before the target field (vertically). For some reason it doesn't work if we use x=0.
        new ClassFieldEditPane(fieldToMove).dragTo(fields.findElement(By.id("field-" + beforeField)), 1, -3);
    }

    /**
     * Sets whether the class sheet should be updated or not.
     * 
     * @param update {@code true} to update the class sheet, {@code false} otherwise
     */
    public void setUpdateClassSheet(boolean update)
    {
        if (updateClassSheetCheckbox.isSelected() != update) {
            updateClassSheetCheckbox.click();
        }
    }
}
