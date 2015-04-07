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
package org.xwiki.scheduler.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.scheduler.test.po.editor.SchedulerEditPage;
import org.xwiki.test.ui.po.ConfirmationPage;
import org.xwiki.test.ui.po.ViewPage;

public class SchedulerHomePage extends ViewPage
{
    @FindBy(xpath = "//form[@id='newdoc']//input[@type='submit' and @value='Add']")
    private WebElement addButton;

    @FindBy(id = "page")
    private WebElement nameInput;

    public static SchedulerHomePage gotoPage()
    {
        getUtil().gotoPage("Scheduler", "WebHome", "view");
        return new SchedulerHomePage();
    }

    public String getURL()
    {
        return getUtil().getURL("Scheduler", "WebHome");
    }

    public void setJobName(String jobName)
    {
        this.nameInput.clear();
        this.nameInput.sendKeys(jobName);
    }

    public SchedulerPage clickJobActionView(String jobName)
    {
        clickAction(jobName, "View");
        return new SchedulerPage();
    }

    public SchedulerEditPage clickJobActionEdit(String jobName)
    {
        clickAction(jobName, "Edit");

        // Make sure we wait for the WYSIWYG fields to be loaded since otherwise they'll steal the focus and if we
        // start typing in other fields before they're loaded what we type will end up in the wrong fields...
        SchedulerEditPage sep = new SchedulerEditPage();
        sep.waitForJobEditionToLoad();

        return sep;
    }

    public ConfirmationPage clickJobActionDelete(String jobName)
    {
        clickAction(jobName, "Delete");
        return new ConfirmationPage();
    }

    public void clickJobActionScheduler(String jobName)
    {
        clickAction(jobName, "Schedule");
    }

    public void clickJobActionTrigger(String jobName)
    {
        clickAction(jobName, "Trigger");
    }

    public void clickJobActionPause(String jobName)
    {
        clickAction(jobName, "Pause");
    }

    public void clickJobActionResume(String jobName)
    {
        clickAction(jobName, "Resume");
    }

    public void clickJobActionUnschedule(String jobName)
    {
        clickAction(jobName, "Unschedule");
    }

    /**
     * Click one of the actin that can be performed on a job.
     */
    private void clickAction(String jobName, String actionLinkName)
    {
        getDriver().findElement(
            By.xpath("//tr[td[.='" + jobName + "']]//a[.='" + actionLinkName + "']")).click();
    }

    public SchedulerEditPage clickAdd()
    {
        this.addButton.click();

        return new SchedulerEditPage();
    }

    /**
     * @return true if the scheduler home page contains an error message or false otherwise. An error message appears
     *         when one of the scheduler actions fails to execute properly.
     * @since 4.2M1
     */
    public boolean hasError()
    {
        return getDriver().findElementsWithoutWaiting(
            By.xpath("//div[contains(@class, 'errormessage')]")).size() > 0;
    }

    /**
     * @return the text of the error message (see {@link #hasError()}
     * @since 4.2M1
     */
    public String getErrorMessage()
    {
        return getDriver().findElementWithoutWaiting(
            By.xpath("//div[contains(@class, 'errormessage')]")).getText();
    }
}
