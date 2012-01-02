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
package org.xwiki.test.ui.po.editor;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the common actions possible on all Pages when using the "edit" action with "wiki" editor
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class WikiEditPage extends PreviewableEditPage
{
    @FindBy(id = "xwikidoctitleinput")
    private WebElement titleInput;

    @FindBy(name = "parent")
    private WebElement parentInput;

    @FindBy(id = "editParentTrigger")
    private WebElement editParentTrigger;

    @FindBy(id = "content")
    private WebElement contentText;

    @FindBy(name = "minorEdit")
    private WebElement minorEditCheckBox;

    @FindBy(name = "comment")
    private WebElement commentInput;

    /**
     * Go to the passed page in wiki edit mode.
     */
    public static WikiEditPage gotoPage(String space, String page)
    {
        getUtil().gotoPage(space, page, "edit", "editor=wiki");
        return new WikiEditPage();
    }

    /**
     * Get the <code>title</code> of the page.
     */
    public String getTitle()
    {
        return this.titleInput.getAttribute("value");
    }

    /**
     * Set the <code>title</code> of the page.
     */
    public void setTitle(String title)
    {
        this.titleInput.clear();
        this.titleInput.sendKeys(title);
    }

    /**
     * @return the value of the parent field.
     */
    public String getParent()
    {
        return this.parentInput.getAttribute("value");
    }

    /**
     * Set the {@code parent} of the page
     * 
     * @param parent the value for the new parent to set
     */
    public void setParent(String parent)
    {
        // Without this, the following cast fails with a ClassCastException
        this.parentInput = this.getDriver().findElement(By.name("parent"));
        if (!this.parentInput.isDisplayed()) {
            this.editParentTrigger.click();
        }
        this.parentInput.clear();
        this.parentInput.sendKeys(parent);
    }

    /**
     * Get the <code>content</code> of the page.
     */
    public String getContent()
    {
        return this.contentText.getText();
    }

    /**
     * Set the <code>content</code> of the page.
     */
    public void setContent(String content)
    {
        this.contentText.clear();
        this.contentText.sendKeys(content);
    }

    /**
     * Set the minor edit check box value.
     */
    public void setMinorEdit(boolean value)
    {
        if ((this.minorEditCheckBox.isSelected() && !value)
            || (!this.minorEditCheckBox.isSelected() && value))
        {
            this.minorEditCheckBox.click();
        }
    }

    /**
     * Set <code>comment</code> for this change.
     */
    public void setEditComment(String comment)
    {
        this.commentInput.clear();
        this.commentInput.sendKeys(comment);
    }
}
