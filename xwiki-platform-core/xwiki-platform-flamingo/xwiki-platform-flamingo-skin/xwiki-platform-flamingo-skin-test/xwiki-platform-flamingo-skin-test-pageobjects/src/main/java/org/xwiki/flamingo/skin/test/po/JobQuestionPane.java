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
package org.xwiki.flamingo.skin.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.tree.test.po.TreeElement;

/**
 * Represent a job question element to be manipulated in functional tests.
 *
 * @version $Id$
 * @since 11.1RC1
 */
public class JobQuestionPane extends BaseElement
{
    /**
     * Class name of the main question div.
     */
    private static final String UI_QUESTION_CLASSNAME = "ui-question";

    /**
     * The main job question area.
     */
    @FindBy(className = UI_QUESTION_CLASSNAME)
    private WebElement pane;

    /**
     * The container containing buttons.
     */
    @FindBy(className = "buttons-question")
    private WebElement questionPane;

    /**
     * Waits for the elements to be visible before returning the question pane.
     * It shouldn't be used if a blocked job is expected (see {@link #isBlockedJob()}).
     * @return a job question pane loaded.
     */
    public JobQuestionPane waitForQuestionPane()
    {
        getDriver().waitUntilElementIsVisible(By.className(UI_QUESTION_CLASSNAME));
        return new JobQuestionPane();
    }

    /**
     * @return true if the question has been canceled.
     */
    public boolean isCanceled()
    {
        return getDriver().findElement(By.cssSelector("#mainContentArea .box.warningmessage")).getText()
            .equals("Canceled.");
    }

    /**
     * @return true if the question area is empty.
     */
    public boolean isEmpty()
    {
        return this.pane.getText().isEmpty();
    }

    /**
     * @return the title of the current question.
     */
    public String getQuestionTitle()
    {
        return this.pane.findElement(By.className("panel-title")).getText();
    }

    /**
     * @return the tree element corresponding to the current question.
     */
    public TreeElement getQuestionTree()
    {
        return new TreeElement(this.pane.findElement(By.className("panel-body")).findElement(By.className("jstree")));
    }

    /**
     * Cancel the question.
     */
    public void cancelQuestion()
    {
        this.questionPane.findElement(By.className("btAnswerCancel")).click();
        getDriver().waitUntilCondition(driver -> isEmpty());
    }

    /**
     * Confirm the question.
     * @return return a page status to check what happens.
     */
    public CopyOrRenameOrDeleteStatusPage confirmQuestion()
    {
        this.questionPane.findElement(By.className("btAnswerConfirm")).click();
        return new CopyOrRenameOrDeleteStatusPage();
    }

    /**
     * Click on the button with the given name and value.
     *
     * @param name the name of the button
     * @param value the value of the button
     * @return the status page
     */
    public CopyOrRenameOrDeleteStatusPage clickButton(String name, String value)
    {
        this.questionPane.findElement(By.cssSelector("button[name='" + name + "'][value='" + value + "']")).click();
        return new CopyOrRenameOrDeleteStatusPage();
    }

    /**
     * @return true if the current job is blocked by another one.
     */
    public boolean isBlockedJob()
    {
        return this.getDriver().hasElementWithoutWaiting(By.id("state-none-hint"));
    }
}
