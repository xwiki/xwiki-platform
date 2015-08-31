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
package org.xwiki.index.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.BreadcrumbElement;

/**
 * Represents the Document Picker.
 * 
 * @version $Id$
 * @since 7.2M3
 */
public class DocumentPicker extends BaseElement
{
    protected WebElement container;

    public DocumentPicker(WebElement container)
    {
        this.container = container;
    }

    public String getTitle()
    {
        return getTitleInput().getAttribute("value");
    }

    public DocumentPicker setTitle(String title)
    {
        WebElement titleInput = getTitleInput();
        titleInput.clear();
        titleInput.sendKeys(title);
        return this;
    }

    public WebElement getTitleInput()
    {
        return this.container.findElement(By.className("location-title-field"));
    }

    public BreadcrumbElement getLocation()
    {
        return new BreadcrumbElement(this.container.findElement(By.className("breadcrumb")));
    }

    public DocumentPicker toggleLocationAdvancedEdit()
    {
        this.container.findElement(By.className("location-action-edit")).click();
        return this;
    }

    public String getParent()
    {
        return getParentInput().getAttribute("value");
    }

    public DocumentPicker setParent(String parent)
    {
        WebElement parentInput = getParentInput();
        parentInput.clear();
        parentInput.sendKeys(parent);
        return this;
    }

    public WebElement getParentInput()
    {
        return this.container.findElement(By.className("location-parent-field"));
    }

    public String getName()
    {
        return getNameInput().getAttribute("value");
    }

    public DocumentPicker setName(String name)
    {
        WebElement nameInput = getNameInput();
        nameInput.clear();
        nameInput.sendKeys(name);
        return this;
    }

    public WebElement getNameInput()
    {
        return this.container.findElement(By.className("location-name-field"));
    }

    public DocumentPicker selectDocument(String... path)
    {
        browseDocuments().selectDocument(path);
        return this;
    }

    public DocumentPickerModal browseDocuments()
    {
        this.container.findElement(By.className("location-action-pick")).click();
        return new DocumentPickerModal(this.container.findElement(By.cssSelector(".location-tree.modal"))).waitForIt();
    }
}
