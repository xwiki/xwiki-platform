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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page Object for Comments Tab.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class CommentsTab extends ViewPage
{
    @FindBy(xpath = "//input[@value='Add comment']")
    private WebElement buttonAddComment;

    @FindBy(xpath = "//input[@value='Save comment']")
    private WebElement buttonSaveComment;

    @FindBy(xpath = "//fieldset[@id='commentform']/label/span")
    private WebElement commentAuthor;

    @FindBy(id = "XWiki.XWikiComments_author")
    private WebElement anonymousCommentAuthor;

    @FindBy(id = "XWiki.XWikiComments_comment")
    private WebElement commentTextArea;

    CommentDeleteConfirmationModal confirmDelete;

    List<WebElement> commentsList;

    public void clickAddComment()
    {
        this.buttonAddComment.click();
    }

    public void clickSaveComment()
    {
        this.buttonSaveComment.click();
    }

    public String getCurrentAuthor()
    {
        return this.commentAuthor.getAttribute("value");
    }

    public boolean isCommentFormShown()
    {
        WebElement commentForm = getDriver().findElement(
            By.xpath("//form[@id='AddComment']/fieldset[@id='commentform']"));
        return commentForm.isDisplayed();
    }

    public void setCommentContent(String content)
    {
        this.commentTextArea.sendKeys(content);
    }

    public void setAnonymousCommentAuthor(String author)
    {
        this.anonymousCommentAuthor.clear();
        this.anonymousCommentAuthor.sendKeys(author);
    }

    public int getCommentID(String content)
    {
        this.commentsList = getDriver().findElements(By.className("xwikicomment"));

        for (int i = 0; i < this.commentsList.size(); i++) {
            if (this.commentsList.get(i).findElement(By.className("commentcontent")).getText().equals(content)) {
                return Integer
                    .parseInt(this.commentsList.get(i).getAttribute("id").substring("xwikicomment_".length()));
            }
        }
        return -1;
    }

    public int postComment(String content, boolean validation)
    {
        this.setCommentContent(content);
        this.clickAddComment();

        if (validation) {
            waitUntilElementIsVisible(By
                .xpath("//div[contains(@class,'xnotification-done') and text()='Comment posted']"));
            getDriver().findElement(
                By.xpath("//div[contains(@class,'xnotification-done') and text()='Comment posted']")).click();
            waitUntilElementIsVisible(By.xpath("//div[@class='commentcontent']/p[contains(text(),'" + content + "')]"));
        }
        return this.getCommentID(content);
    }

    public int postCommentAsGuest(String content, String author, boolean validation)
    {
        this.setCommentContent(content);
        this.setAnonymousCommentAuthor(author);
        this.clickAddComment();

        if (validation) {
            waitUntilElementIsVisible(By
                .xpath("//div[contains(@class,'xnotification-done') and text()='Comment posted']"));
            getDriver().findElement(
                By.xpath("//div[contains(@class,'xnotification-done') and text()='Comment posted']")).click();
            waitUntilElementIsVisible(By.xpath("//div[@class='commentcontent']/p[contains(text(),'" + content + "')]"));
        }
        return this.getCommentID(content);
    }

    public void deleteCommentByID(int id)
    {
        getDriver().findElement(By.xpath("//div[@id='xwikicomment_" + id + "']//a[@class='delete']")).click();
        this.confirmDelete = new CommentDeleteConfirmationModal();
        this.confirmDelete.clickOk();
        waitUntilElementIsVisible(By.xpath("//div[contains(@class,'xnotification-done') and text()='Comment deleted']"));
        getDriver().findElement(By.xpath("//div[contains(@class,'xnotification-done') and text()='Comment deleted']"))
            .click();
    }

    public void replyToCommentByID(int id, String replyContent)
    {
        getDriver().findElement(By.xpath("//div[@id='xwikicomment_" + id + "']//a[@class='commentreply']")).click();
        getDriver().findElement(By.id("XWiki.XWikiComments_comment")).sendKeys(replyContent);
        this.clickAddComment();
        waitUntilElementIsVisible(By.xpath("//div[contains(@class,'xnotification-done') and text()='Comment posted']"));
        getDriver().findElement(By.xpath("//div[contains(@class,'xnotification-done') and text()='Comment posted']"))
            .click();
    }

    public void editCommentByID(int id, String content)
    {
        getDriver().findElement(By.xpath("//div[@id='xwikicomment_" + id + "']//a[@class='edit']")).click();
        waitUntilElementIsVisible(By.id("XWiki.XWikiComments_" + id + "_comment"));
        getDriver().findElement(By.id("XWiki.XWikiComments_" + id + "_comment")).clear();
        getDriver().findElement(By.id("XWiki.XWikiComments_" + id + "_comment")).sendKeys(content);
        this.clickSaveComment();
        waitUntilElementIsVisible(By.xpath("//div[contains(@class,'xnotification-done') and text()='Comment posted']"));
        getDriver().findElement(By.xpath("//div[contains(@class,'xnotification-done') and text()='Comment posted']"))
            .click();
        waitUntilElementIsVisible(By.xpath("//div[@class='commentcontent']/p[contains(text(),'" + content + "')]"));
    }

    public String getCommentAuthorByID(int id)
    {
        return getDriver().findElement(By.xpath("//div[@id='xwikicomment_" + id + "']//span[@class='commentauthor']"))
            .getText();
    }

    public String getCommentContentByID(int id)
    {
        return getDriver().findElement(By.xpath("//div[@id='xwikicomment_" + id + "']//div[@class='commentcontent']"))
            .getText();
    }

    /**
     * @since 3.2M3
     */
    public boolean hasEditbuttonForCommentByID(int commentId)
    {
        return getDriver().findElements(
            By.xpath("//div[@id='xwikicomment_" + commentId + "']//a[@class='edit']")).size() > 0;
    }
}
