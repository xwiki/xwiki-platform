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
public class CommentsTab extends BaseElement
{
    @FindBy(css = "fieldset#commentform > label > span")
    private WebElement commentAuthor;

    @FindBy(id = "XWiki.XWikiComments_author")
    private WebElement anonymousCommentAuthor;

    private ConfirmationModal confirmDelete;

    private List<WebElement> commentsList;

    public String getCurrentAuthor()
    {
        return this.commentAuthor.getAttribute("value");
    }

    public boolean isCommentFormShown()
    {
        WebElement commentForm =
            getDriver().findElement(By.xpath("//form[@id='AddComment']/fieldset[@id='commentform']"));
        return commentForm.isDisplayed();
    }

    public void setAnonymousCommentAuthor(String author)
    {
        this.anonymousCommentAuthor.clear();
        this.anonymousCommentAuthor.sendKeys(author);
    }

    public int getCommentID(String content)
    {
        this.commentsList = getDriver().findElementsWithoutWaiting(By.className("xwikicomment"));

        for (int i = 0; i < this.commentsList.size(); i++) {
            if (this.commentsList.get(i).findElement(By.className("commentcontent")).getText().equals(content)) {
                return Integer
                    .parseInt(this.commentsList.get(i).getAttribute("id").substring("xwikicomment_".length()));
            }
        }
        return -1;
    }

    /**
     * @return the form used to add a new comment
     */
    public CommentForm getAddCommentForm()
    {
        return new CommentForm(By.id("AddComment"));
    }

    public int postComment(String content, boolean wait)
    {
        CommentForm addCommentForm = getAddCommentForm();
        addCommentForm.getContentField().sendKeys(content);
        addCommentForm.clickSubmit(wait);
        return this.getCommentID(content);
    }

    public int postCommentAsGuest(String content, String author, boolean wait)
    {
        CommentForm addCommentForm = getAddCommentForm();
        addCommentForm.getContentField().sendKeys(content);
        this.setAnonymousCommentAuthor(author);
        addCommentForm.clickSubmit(wait);
        return this.getCommentID(content);
    }

    /**
     * Deletes a comment.
     *
     * @param id the comment id
     */
    public void deleteCommentByID(int id)
    {
        // We initialize before so we can remove the animation before the modal is shown
        this.confirmDelete = new ConfirmationModal(By.id("deleteModal"));
        getDriver().findElement(By.xpath("//div[@id='xwikicomment_" + id + "']//a[contains(@class, 'delete')]"))
            .click();
        this.confirmDelete.clickOk();
        waitForNotificationSuccessMessage("Comment deleted");
    }

    /**
     * Clicks on the reply icon near the specified comment.
     *
     * @param id identifies the comment to reply to
     * @return the form used to reply
     */
    public CommentForm replyToCommentByID(int id)
    {
        getDriver().findElementWithoutWaiting(
            By.xpath("//div[@id='xwikicomment_" + id + "']//a[contains(@class, 'commentreply')]")).click();
        return getAddCommentForm();
    }

    /**
     * Replies to a comment with the specified content.
     *
     * @param id the comment id
     * @param replyContent the comment content of the reply
     */
    public void replyToCommentByID(int id, String replyContent)
    {
        CommentForm replyCommentForm = replyToCommentByID(id);
        replyCommentForm.getContentField().sendKeys(replyContent);
        replyCommentForm.clickSubmit();
    }

    /**
     * Clicks on the edit icon near the specified comment.
     *
     * @param id identifies the comment to be edited
     * @return the form used to edit the comment
     */
    public CommentForm editCommentByID(int id)
    {
        getDriver()
            .findElementWithoutWaiting(By.xpath("//div[@id='xwikicomment_" + id + "']//a[contains(@class, 'edit')]"))
            .click();
        getDriver().waitUntilElementIsVisible(By.id("XWiki.XWikiComments_" + id + "_comment"));
        return new CommentForm(By.className("edit-xcomment"));
    }

    /**
     * Edits a comment with the specified content.
     *
     * @param id the comment id
     * @param content the new comment content
     */
    public void editCommentByID(int id, String content)
    {
        CommentForm editCommentForm = editCommentByID(id);
        editCommentForm.getContentField().clear();
        editCommentForm.getContentField().sendKeys(content);
        editCommentForm.clickSubmit();
    }

    /**
     * @param id the comment id
     * @return the comment author
     */
    public String getCommentAuthorByID(int id)
    {
        return getDriver().findElementWithoutWaiting(By.cssSelector("#xwikicomment_" + id + " span.commentauthor"))
            .getText();
    }

    /**
     * @param id the comment id
     * @return the comment content
     */
    public String getCommentContentByID(int id)
    {
        return getDriver().findElementWithoutWaiting(By.cssSelector("#xwikicomment_" + id + " .commentcontent"))
            .getText();
    }

    /**
     * @param id the comment id
     * @return true if the comment has the edit button
     * @since 3.2M3
     */
    public boolean hasEditButtonForCommentByID(int id)
    {
        return !getDriver().findElementsWithoutWaiting(By.cssSelector("#xwikicomment_" + id + " a.edit")).isEmpty();
    }

    /**
     * @param id the comment id
     * @return true if the comment has the delete button
     * @since 10.6RC1
     */
    public boolean hasDeleteButtonForCommentByID(int id)
    {
        return !getDriver().findElementsWithoutWaiting(By.cssSelector("#xwikicomment_" + id + " a.delete")).isEmpty();
    }
}
