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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * @since 7.3M1
 * @version $Id$
 */
public class OfficeImporterPage extends ViewPage
{
    @FindBy(id = "filepath")
    private WebElement fileInput;

    @FindBy(id = "filterStylesInputId")
    private WebElement filterStyleInput;
    
    @FindBy(id = "splitDocumentInputId")
    private WebElement splitDocumentInput;

    @FindBy(id = "headingLevelsToSplitInputId")
    private WebElement headingLevelsToSplitInput;

    @FindBy(id = "childPagesNamingMethodInputId")
    private WebElement childPagesNamingMethodInput;
    
    @FindBy(id = "submit")
    private WebElement importButton;
    
    public OfficeImporterPage()
    {
        super();
        getDriver().waitUntilCondition(webDriver -> webDriver.findElement(By.id("officeImportForm")) != null);
    }
    
    public void setFile(File file)
    {
        fileInput.sendKeys(file.getAbsolutePath());
    }
    
    public void setFilterStyle(boolean value)
    {
        if (filterStyleInput.isSelected() != value) {
            filterStyleInput.click();
        }
    }

    public void setSplitDocument(boolean value)
    {
        if (splitDocumentInput.isSelected() != value) {
            splitDocumentInput.click();
        }
    }
    
    public OfficeImporterResultPage clickImport()
    {
        importButton.click();
        return new OfficeImporterResultPage();
    }
    
    public boolean isChildPagesNamingMethodDisplayed()
    {
        return getDriver().hasElementWithoutWaiting(By.id("childPagesNamingMethodInputId"));
    }
}
