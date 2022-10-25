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
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.EntityReference;

/**
 * Represents the actions available when editing the application class. This is also the second step of the App Within
 * Minutes wizard, in which the application structure defined.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class ApplicationClassEditPage extends ApplicationEditPage
{
    /**
     * The message displayed when the canvas is empty.
     * @since 13.3RC1
     * @since 12.10.6
     */
    public static final String EMPTY_CANVAS_HINT = "Drag fields from the palette and drop them in this area.";
    
    @FindBy(id = "palette")
    private WebElement palette;

    @FindBy(id = "fields")
    private WebElement fields;

    @FindBy(id = "canvas")
    private WebElement fieldsCanvas;

    @FindBy(id = "updateClassSheet")
    private WebElement updateClassSheetCheckbox;

    /**
     * Default constructor which waits on xaction_save button.
     */
    public ApplicationClassEditPage()
    {
        super(true, true);
    }

    /**
     * Clicks on the Next Step button.
     *
     * @return the page that represents the next step of the App Within Minutes wizard
     */
    public ApplicationTemplateProviderEditPage clickNextStep()
    {
        nextStepButton.click();
        return new ApplicationTemplateProviderEditPage();
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
        return getDriver().findElementsWithoutWaiting(By.linkText("Previous Step")).size() > 0;
    }

    /**
     * Drags a field of the specified type from the field palette to the field canvas.
     *
     * @param fieldType the type of field to add, as displayed on the field palette
     * @return the newly added field page object
     */
    public ClassFieldEditPane addField(String fieldType)
    {
        String fieldXPath = "//li[@class = 'field' and normalize-space(.) = '%s']";
        WebElement field = palette.findElement(By.xpath(String.format(fieldXPath, fieldType)));
        // NOTE: We scroll to the top of the page because the drag&drop fails sometimes if the dragged field and the 
        // canvas (drop target) are not fully visible. See https://code.google.com/p/selenium/issues/detail?id=3075.
        // The scroll up action was previously performed by sending a home key pressed action. This action is
        // asynchronous on Chrome, consequently the drag and drop action below was performed while the screen was still 
        // moving. This was leading to the wrong type of field being drag and dropped in the edit pane, making the test 
        // fail. scrollToTop is instantaneous and moves the view to the top of the screen before the drag and drop 
        // starts.
        scrollToTop();
        getDriver().dragAndDrop(field, fieldsCanvas);
        final WebElement addedField = fieldsCanvas.findElement(By.xpath("./ul[@id='fields']/li[last()]"));

        getDriver().waitUntilCondition(driver -> {
            try {
                return !addedField.getAttribute("class").contains("loading");
            } catch (NotFoundException e) {
                return false;
            } catch (StaleElementReferenceException e) {
                // The element was removed from DOM in the meantime
                return false;
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
        new ClassFieldEditPane(fieldToMove).dragTo(fieldsCanvas.findElement(By.id("field-" + beforeField)), 0, -5);
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

    /**
     * Opens an AWM class editor at the provided document location.
     *
     * @param reference the location of the document
     * @return the corresponding AWM class editor page object
     * @since 13.2
     * @since 12.10.6
     */
    public static ApplicationClassEditPage goToEditor(EntityReference reference)
    {
        getUtil().gotoPage(reference, "edit",
            "editor", "inline", "template", "AppWithinMinutes.ClassTemplate", "title",
            reference.getName() + " Class");
        return new ApplicationClassEditPage();
    }

    /**
     * Creates a new application with the given name.
     * @param appName the application name
     * @return the corresponding page object
     * @since 13.2
     * @since 12.10.6
     */
    public static ApplicationClassEditPage createNewApplication(String appName)
    {
        ApplicationCreatePage appCreatePage = ApplicationCreatePage.gotoPage();
        appCreatePage.setApplicationName(appName);
        return appCreatePage.clickNextStep();
    }
}
