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

import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOf;

/**
 * The page object used to edit a document or object property in-place.
 * 
 * @version $Id$
 * @since 12.6.3
 * @since 12.9RC1
 */
public class EditablePropertyPane extends BaseElement
{
    protected final WebElement element;

    protected final WebElement viewer;

    protected final WebElement editor;

    protected final WebElement editButton;

    protected final WebElement cancelButton;

    protected final WebElement saveButton;

    public EditablePropertyPane(String property)
    {
        this.element = getDriver().findElementByCssSelector("dt.editableProperty[data-property=\"" + property + "\"]");
        this.viewer =
            this.element.findElement(By.xpath("./following-sibling::dd[contains(@class, 'editableProperty-viewer')]"));
        this.editor =
            this.viewer.findElement(By.xpath("./following-sibling::dd[contains(@class, 'editableProperty-editor')]"));
        this.editButton = this.element.findElement(By.className("editableProperty-edit"));
        this.cancelButton = this.element.findElement(By.className("editableProperty-cancel"));
        this.saveButton = this.element.findElement(By.className("editableProperty-save"));
    }

    public EditablePropertyPane clickEdit()
    {
        this.editButton.click();
        getDriver().waitUntilCondition(visibilityOf(this.editor));
        return this;
    }

    public EditablePropertyPane clickCancel()
    {
        this.cancelButton.click();
        getDriver().waitUntilCondition(visibilityOf(this.editButton));
        return this;
    }

    public EditablePropertyPane clickSave()
    {
        this.saveButton.click();
        getDriver().waitUntilCondition(visibilityOf(this.editButton));
        return this;
    }

    public String getLabel()
    {
        return this.element.findElement(By.tagName("label")).getText();
    }

    public String getDisplayValue()
    {
        return this.viewer.getText();
    }
}
