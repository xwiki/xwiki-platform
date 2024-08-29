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
package org.xwiki.officeimporter.test.po;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.test.ui.po.ConfirmationModal;
import org.xwiki.test.ui.po.ViewPage;

/**
 * @since 7.3M1
 * @version $Id$
 */
public class OfficeImporterPage extends ViewPage
{
    @FindBy(name = "filePath")
    private WebElement filePathInput;

    @FindBy(name = "overwriteContent")
    private WebElement overwriteContentCheckbox;

    @FindBy(name = "filterStyles")
    private WebElement filterStyleCheckbox;

    @FindBy(name = "splitDocument")
    private WebElement splitDocumentCheckbox;

    @FindBy(name = "headingLevelsToSplit")
    private WebElement headingLevelsToSplitSelect;

    @FindBy(name = "childPagesNamingMethod")
    private WebElement childPagesNamingMethodSelect;

    @FindBy(name = "terminalChildPages")
    private WebElement terminalChildPagesCheckbox;

    @FindBy(id = "submit")
    private WebElement importButton;

    @FindBy(css = "#officeImportForm a.secondary.button")
    private WebElement cancelButton;

    public OfficeImporterPage()
    {
        // Wait until the office import form is initialized by the JavaScript code.
        getDriver().waitUntilCondition(ExpectedConditions.attributeToBe(this.importButton, "data-ready", "true"));
    }

    public void setFile(File file)
    {
        this.filePathInput.sendKeys(file.getAbsolutePath());
    }

    public String getFilePath()
    {
        return this.filePathInput.getText();
    }

    public void setOverwriteContent(boolean value)
    {
        if (this.overwriteContentCheckbox.isSelected() != value) {
            this.overwriteContentCheckbox.click();
        }
    }

    public boolean isOverwriteContent()
    {
        return this.overwriteContentCheckbox.isSelected();
    }

    public boolean isOverwriteContentDisplayed()
    {
        return this.overwriteContentCheckbox.isDisplayed();
    }

    public void setFilterStyle(boolean value)
    {
        if (this.filterStyleCheckbox.isSelected() != value) {
            this.filterStyleCheckbox.click();
        }
    }

    public boolean isFilterStyles()
    {
        return this.filterStyleCheckbox.isSelected();
    }

    public void setSplitDocument(boolean value)
    {
        if (this.splitDocumentCheckbox.isSelected() != value) {
            this.splitDocumentCheckbox.click();
        }
    }

    public boolean isSplitDocument()
    {
        return this.splitDocumentCheckbox.isSelected();
    }

    public void setHeadingLevelsToSplit(Integer... headingLevels)
    {
        Select select = new Select(this.headingLevelsToSplitSelect);
        select.deselectAll();
        Stream.of(headingLevels).map(Object::toString).forEach(select::selectByValue);
    }

    public List<Integer> getHeadingLevelsToSplit()
    {
        return new Select(this.headingLevelsToSplitSelect).getAllSelectedOptions().stream()
            .map(option -> Integer.parseInt(option.getAttribute("value"))).collect(Collectors.toList());
    }

    public void setChildPagesNamingMethod(String method)
    {
        new Select(this.childPagesNamingMethodSelect).selectByVisibleText(method);
    }

    public String getChildPagesNamingMethod()
    {
        return new Select(this.childPagesNamingMethodSelect).getFirstSelectedOption().getText();
    }

    public boolean isChildPagesNamingMethodDisplayed()
    {
        return this.childPagesNamingMethodSelect.isDisplayed();
    }

    public void setTerminalChildPages(boolean value)
    {
        if (this.terminalChildPagesCheckbox.isSelected() != value) {
            this.terminalChildPagesCheckbox.click();
        }
    }

    public boolean isTerminalChildPages()
    {
        return this.terminalChildPagesCheckbox.isSelected();
    }

    public boolean isTerminalChildPagesDisplayed()
    {
        return this.terminalChildPagesCheckbox.isDisplayed();
    }

    public ViewPage submit()
    {
        return submit(null);
    }

    public ViewPage submit(Boolean confirmOverwriteContent)
    {
        return submit(confirmOverwriteContent, true);
    }

    public ViewPage submit(Boolean confirmOverwriteContent, boolean wait)
    {
        // The JavaScript code will redirect the user to the imported page after the import.
        getDriver().addPageNotYetReloadedMarker();

        ConfirmationModal confirmationModal = null;
        if (confirmOverwriteContent != null) {
            // Create the modal page object before the modal is shown in order to disable the fade effect.
            confirmationModal = new ConfirmationModal(By.className("confirmation-overwriteContent"));
        }

        this.importButton.click();

        if (Boolean.TRUE.equals(confirmOverwriteContent)) {
            confirmationModal.clickOk();
        } else if (Boolean.FALSE.equals(confirmOverwriteContent)) {
            confirmationModal.clickCancel();
            return null;
        }

        if (wait) {
            getDriver().waitUntilPageIsReloaded();
            return new ViewPage();
        } else {
            return null;
        }
    }

    public ViewPage cancel()
    {
        this.cancelButton.click();
        return new ViewPage();
    }
}
