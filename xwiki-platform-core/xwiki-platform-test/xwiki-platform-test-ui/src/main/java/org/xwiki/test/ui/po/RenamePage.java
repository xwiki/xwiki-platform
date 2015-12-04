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
    private WebElement deepCheckbox;

    @FindBy(name = "autoRedirect")
    private WebElement autoRedirectCheckbox;

    @FindBy(css = "a.location-action-edit")
    private WebElement locationActionEdit;

    @FindBy(id = "targetParentReference")
    private WebElement targetParentReferenceField;

    @FindBy(css = "form#rename .button[value='Rename']")
    private WebElement renameButton;

    @FindBy(name = "terminal")
    private WebElement terminalCheckbox;

    public boolean isDeep()
    {
        return this.deepCheckbox.isSelected();
    }

    public void setDeep(boolean isDeep)
    {
        if (isDeep != isDeep()) {
            this.deepCheckbox.click();
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

    public void clickLocationActionEditButton()
    {
        this.locationActionEdit.click();
    }

    public void setTargetParentReference(String parent)
    {
        this.targetParentReferenceField.clear();
        this.targetParentReferenceField.sendKeys(parent);
    }

    public void clickRenameButton()
    {
        this.renameButton.click();
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
