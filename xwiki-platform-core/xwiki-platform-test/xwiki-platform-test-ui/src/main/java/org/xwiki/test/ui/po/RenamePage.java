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

    @FindBy(id = "targetParentReference")
    private WebElement targetParentReferenceField;

    @FindBy(css = "form#rename .button[value='Rename']")
    private WebElement renameButton;

    @FindBy(name = "terminal")
    private WebElement terminalCheckbox;

    public boolean preserveChildren()
    {
        return this.preserveChildrenCheckbox.isSelected();
    }

    public void preserveChildren(boolean preserveChildren)
    {
        if (preserveChildren != preserveChildren()) {
            this.preserveChildrenCheckbox.click();
        }
    }
    
    public boolean updateLinks()
    {
        return this.updateLinksCheckbox.isSelected();
    }

    public void updateLinks(boolean updateLinks)
    {
        if (updateLinks != updateLinks()) {
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
