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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class RenamePage extends ViewPage
{
    @FindBy(name = "deep")
    private WebElement preserveChildrenCheckbox;

    @FindBy(name = "updateLinks")
    private WebElement updateLinksCheckbox;

    @FindBy(name = "autoRedirect")
    private WebElement autoRedirectCheckbox;

    @FindBy(css = "form#rename .button[value='Rename']")
    private WebElement renameButton;

    @FindBy(name = "terminal")
    private WebElement terminalCheckbox;

    @FindBy(className = "location-picker")
    private WebElement documentPickerElement;

    private DocumentPicker documentPicker;

    public boolean isPreserveChildren()
    {
        return this.preserveChildrenCheckbox.isSelected();
    }

    public void setPreserveChildren(boolean preserveChildren)
    {
        if (preserveChildren != isPreserveChildren()) {
            this.preserveChildrenCheckbox.click();
        }
    }

    public boolean isUpdateLinks()
    {
        return this.updateLinksCheckbox.isSelected();
    }

    public void setUpdateLinks(boolean updateLinks)
    {
        if (updateLinks != isUpdateLinks()) {
            this.updateLinksCheckbox.click();
        }
    }

    public boolean isAutoRedirect()
    {
        return this.autoRedirectCheckbox.isSelected();
    }

    public void setAutoRedirect(boolean isAutoRedirect)
    {
        if (isAutoRedirect != isAutoRedirect()) {
            this.autoRedirectCheckbox.click();
        }
    }

    public DocumentPicker getDocumentPicker()
    {
        if (this.documentPicker == null) {
            this.documentPicker = new DocumentPicker(this.documentPickerElement);
        }
        return this.documentPicker;
    }

    public CopyOrRenameOrDeleteStatusPage clickRenameButton()
    {
        // The rename page form is submitted by JavaScript code in case the rename button is clicked before the
        // asynchronous validation ends, and in this case Selenium doesn't wait for the new page to load.
        getDriver().addPageNotYetReloadedMarker();
        this.renameButton.click();
        getDriver().waitUntilPageIsReloaded();
        return new CopyOrRenameOrDeleteStatusPage();
    }

    public boolean isTerminal()
    {
        return this.terminalCheckbox.isSelected();
    }

    public void setTerminal(boolean isTerminal)
    {
        if (isTerminal != isTerminal()) {
            this.terminalCheckbox.click();
        }
    }
}
