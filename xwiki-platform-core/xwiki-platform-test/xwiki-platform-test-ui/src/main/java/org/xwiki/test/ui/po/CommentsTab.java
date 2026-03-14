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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.XWikiWebDriver;

/**
 * Page Object for Comments Tab (or pane).
 *
 * @version $Id$
 * @since 3.2M3
 */
public class CommentsTab extends BaseElement
{
    private static final String COMMENT_FORM_ID = "openCommentForm";

    @FindBy(css = "fieldset#commentform > label > span")
    private WebElement commentAuthor;

    @FindBy(id = "XWiki.XWikiComments_author")
    private WebElement anonymousCommentAuthor;

    private ConfirmationModal confirmDelete;

    private List<WebElement> commentsList;

    public boolean isOpened()
    {
        return getDriver().findElementWithoutWaiting(By.id("commentscontent")).isDisplayed();
    }

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

    /**
     * Opens the comment form by clicking on the comment button.
     * If the comment button has already been clicked, does nothing.
     */
    public void openCommentForm()
    {
        String commentFormId = "AddComment";
        XWikiWebDriver driver = getDriver();
        // if the comments has not already been toggled (ie, the comment button is not displayed).
        // we click on the button and wait until the form is visible
        if (!driver.findElementWithoutWaiting(By.id(commentFormId)).isDisplayed()) {
            driver.findElementWithoutWaiting(By.id(COMMENT_FORM_ID)).click();
            driver.waitUntilElementIsVisible(By.id(commentFormId));
        }
    }

    public void setAnonymousCommentAuthor(String author)
    {
        this.anonymousCommentAuthor.clear();
        this.anonymousCommentAuthor.sendKeys(author);
    }

    public int getCommentID(String content)
    {
        this.commentsList = getDriver().findElementsWithoutWaiting(By.className("xwikicomment"));

        for (WebElement comment : this.commentsList) {
            if (comment.findElement(By.className("commentcontent")).getText().equals(content)) {
                return Integer.parseInt(comment.getAttribute("id").substring("xwikicomment_".length()));
            }
        }
        throw new NotFoundException(String.format("Comment with content [%s] cannot be found.", content));
    }

    /**
     * @return the form used to add a new comment
     */
    public CommentForm getAddCommentForm()
    {
        openCommentForm();
        return new CommentForm(By.id("AddComment"));
    }

    public int postComment(String content, boolean wait)
    {
        CommentForm addCommentForm = getAddCommentForm();
        addCommentForm.addToContentField(content);
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
     * Toggle the comment thread for a comment if available
     *
     * @param id the root comment id
     */
    public void toggleCommentThread(int id)
    {
        getDriver().findElement(
            By.xpath(String.format("//div[@id='xwikicomment_%d']//button[contains(@class, 'thread-toggle')]", id)))
            .click();
        getDriver().waitUntilElementIsVisible(By.xpath(String.format(
            "//div[@id='xwikicomment_%d']//following-sibling::div[contains(@class, 'commentthread')]", id)));
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
        // Comments are handled async so it makes sense to wait for the reply button to be ready if another comment
        // has just been posted for example. That's why we don't use findElementWithoutWaiting here.
        clickOnReplyToCommentByID(id);
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
        replyCommentForm.addToContentField(replyContent);
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
        getDriver().waitUntilElementIsVisible(By.className("commenteditor-" + id));
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
        editCommentForm.clearAndSetContentField(content);
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
     * @since 9.11.9
     */
    public boolean hasDeleteButtonForCommentByID(int id)
    {
        return !getDriver().findElementsWithoutWaiting(By.cssSelector("#xwikicomment_" + id + " a.delete")).isEmpty();
    }

    /**
     * Cancel the currently open comment form (new comment, edit or reply) by clicking on the cancel button.
     *
     * @since 14.1RC1
     * @since 13.10.3
     */
    public void cancelCommentForm()
    {
        // Click on the first visible cancel button.
        getDriver()
            .findElements(By.cssSelector("#Commentspane .cancel"))
            .stream()
            .filter(WebElement::isDisplayed)
            .findAny()
            .ifPresent(WebElement::click);
        getDriver().waitUntilElementIsVisible(By.id(COMMENT_FORM_ID));
    }

    /**
     * Click on the reply button of a comment. The comment to reply to is found by its id.
     *
     * @param id the id of the comment to reply to
     * @since 14.1RC1
     * @since 13.10.3
     */
    public void clickOnReplyToCommentByID(int id)
    {
        getDriver().findElement(
            By.xpath(String.format("//div[@id='xwikicomment_%d']//a[contains(@class, 'commentreply')]", id))).click();
    }

    /**
     * Click on the annotation of a given id.
     *
     * @param id the id of the comment
     * @since 16.2.0RC1
     * @since 15.10.7
     */
    public void clickOnAnnotationQuote(int id)
    {
        getDriver()
            .findElement(By.id(String.format("xwikicomment_%d", id)))
            .findElement(By.cssSelector("blockquote.annotatedText"))
            .click();
    }

    /**
     * Retrieve the list of comments in their displayed order. Note that in case of thread, the list contains also the
     * comments that belongs to the threads in the order they appear. e.g. Comment 0, first reply of 0, second reply
     * of 0, Comment 1, etc.
     * @return the list of comments in their order of appearance.
     * @since 17.3.0RC1
     * @since 16.10.6
     * @since 16.4.8
     */
    public List<CommentElement> getComments()
    {
        List<CommentElement> result = new ArrayList<CommentElement>();
        for (WebElement xwikicomment : getDriver().findElementsWithoutWaiting(By.className("xwikicomment"))) {
            result.add(new CommentElement(xwikicomment));
        }
        return result;
    }
}
