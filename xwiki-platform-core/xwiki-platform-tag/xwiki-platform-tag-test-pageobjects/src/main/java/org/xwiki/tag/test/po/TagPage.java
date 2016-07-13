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
package org.xwiki.tag.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Models a view page of a Tag (when clicking on a tag).
 *
 */
public class TagPage extends ViewPage
{
    @FindBy(xpath = "//a[@class='button rename']")
    private WebElement buttonRenameTag;

    @FindBy(xpath = "//a[@class='button delete']")
    private WebElement buttonDeleteTag;
    
    @FindBy(xpath = "//input[@class='button' and contains(@value,'Delete')]")
    private WebElement buttonConfirmDeleteTag;

    @FindBy(xpath = "//a[@class='secondary button']")
    private WebElement buttonCancelDeleteTag;

    @FindBy(xpath = "//input[@name='renameTo']")
    private WebElement renameTagInput;
    
    @FindBy(xpath = "//input[@value='Rename']")
    private WebElement buttonConfirmRename;

    public void clickRenameButton()
    {
      this.buttonRenameTag.click();
    }
    
    public void setNewTagName(String newTagName)
    {
        this.renameTagInput.sendKeys(newTagName);
    }

    public void clickCancelRenameButton()
    {
        this.buttonDeleteTag.click();
    }
    public void clickConfirmRenameTagButton()
    {
        this.buttonConfirmRename.click();
    }
    
    public void clickDeleteButton()
    {
        this.buttonDeleteTag.click();
    }
    public void clickConfirmDeleteTag()
    {
        this.buttonConfirmDeleteTag.click();
    }

    public boolean hasTagHighlight(String tagName)
    {   
        return getDriver().findElementsWithoutWaiting(
            By.xpath("//span[@class='highlight tag' and contains(text()[1], '" + tagName + "')]")).size() == 1;
    }
    public boolean hasConfirmationMessage(String tagName)
    {
        return getDriver().findElementsWithoutWaiting(
            By.xpath("//div[@class='box infomessage' and contains(. ,'has been successfully deleted')]"))
            .size() == 1;
    }

}
