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

import java.util.List;

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
    private final String tag;

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

    public TagPage(String tag)
    {
        this.tag = tag;
    }

    public TagPage renameTag(String newTagName)
    {
        this.buttonRenameTag.click();
        this.renameTagInput.sendKeys(newTagName);
        this.buttonConfirmRename.click();
        return new TagPage(newTagName);
    }
    
    public void clickDeleteButton()
    {
        this.buttonDeleteTag.click();
    }
    public void clickConfirmDeleteTag()
    {
        this.buttonConfirmDeleteTag.click();
    }

    public boolean hasConfirmationMessage()
    {
        return getDriver().findElementsWithoutWaiting(
            By.xpath("//div[@class='box infomessage' and contains(. ,'has been successfully deleted')]"))
            .size() == 1;
    }

    public List<String> getTaggedPages()
    {
        String titleId = "HAllpagestaggedwith" + tag;
        return getDriver().findElementsWithoutWaiting(By.cssSelector(String.format("h3[id='%s'] ~ ul > li", titleId)))
            .stream().map(WebElement::getText)
            .toList();
    }

}
