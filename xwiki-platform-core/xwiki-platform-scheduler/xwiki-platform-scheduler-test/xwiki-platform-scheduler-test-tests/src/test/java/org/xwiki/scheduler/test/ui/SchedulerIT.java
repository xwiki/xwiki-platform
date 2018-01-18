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
package org.xwiki.scheduler.test.ui;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.scheduler.test.po.SchedulerHomePage;
import org.xwiki.scheduler.test.po.SchedulerPage;
import org.xwiki.scheduler.test.po.editor.SchedulerEditPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;

/**
 * Tests Scheduler application features.
 * 
 * @version $Id$
 */
public class SchedulerIT extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testScheduler()
    {
        // Make sure the job doesn't exist. Note that we don't delete the job after the test is executed (@After)
        // because we want to remain on the same page in case of a test failure so that our TestDebugger rule can
        // collect accurate information about the failure. It's not a problem if the job remains scheduled because it
        // does nothing. Other tests should not rely on the number of scheduler jobs though.
        getUtil().deletePage("Scheduler", "SchedulerTestJob");

        // Create Job
        SchedulerHomePage schedulerHomePage = SchedulerHomePage.gotoPage();
        schedulerHomePage.setJobName("SchedulerTestJob");
        SchedulerEditPage schedulerEdit = schedulerHomePage.clickAdd();

        String jobName = "Tester problem";
        schedulerEdit.setJobName(jobName);
        schedulerEdit.setJobDescription(jobName);
        schedulerEdit.setCron("0 15 10 ? * MON-FRI");
        SchedulerPage schedulerPage = schedulerEdit.clickSaveAndView();
        schedulerHomePage = schedulerPage.backToHome();

        // View Job
        schedulerPage = schedulerHomePage.clickJobActionView(jobName);

        // Tests that a scheduler job page's default edit mode is Form
        // Note: This line below will fail if the page is not edited in Form mode!
        schedulerPage.edit();
        new SchedulerEditPage().setJobDescription("test");
        schedulerEdit.clickCancel();
        schedulerHomePage = schedulerPage.backToHome();

        // Edit Job
        schedulerEdit = schedulerHomePage.clickJobActionEdit(jobName);
        schedulerEdit.setJobDescription("Tester problem2");
        schedulerEdit.setCron("0 0/5 14 * * ?");
        schedulerPage = schedulerEdit.clickSaveAndView();
        schedulerHomePage = schedulerPage.backToHome();

        // Delete and Restore Job
        schedulerHomePage.clickJobActionDelete(jobName).clickYes();
        schedulerHomePage = SchedulerHomePage.gotoPage();
        Assert.assertFalse(getDriver().hasElementWithoutWaiting(By.linkText(jobName)));
        // Note: since the page doesn't exist, we need to disable the space redirect feature so that we end up on the
        // terminal page that was removed.
        getUtil().gotoPage("Scheduler", "SchedulerTestJob", "view", "spaceRedirect=false");
        getDriver().findElement(By.linkText("Restore")).click();
        schedulerPage = new SchedulerPage();
        schedulerPage.backToHome();

        // Schedule Job
        schedulerHomePage.clickJobActionSchedule(jobName);
        if (schedulerHomePage.hasError()) {
            Assert.fail("Failed to schedule job. Error [" + schedulerHomePage.getErrorMessage() + "]");
        }

        // Trigger Job (a Job can only be triggered after it's been scheduled)
        schedulerHomePage.clickJobActionTrigger(jobName);
        if (schedulerHomePage.hasError()) {
            Assert.fail("Failed to trigger job. Error [" + schedulerHomePage.getErrorMessage() + "]");
        }

        // Pause Job
        schedulerHomePage.clickJobActionPause(jobName);
        if (schedulerHomePage.hasError()) {
            Assert.fail("Failed to pause job. Error [" + schedulerHomePage.getErrorMessage() + "]");
        }

        // Resume Job
        schedulerHomePage.clickJobActionResume(jobName);
        if (schedulerHomePage.hasError()) {
            Assert.fail("Failed to resume job. Error [" + schedulerHomePage.getErrorMessage() + "]");
        }

        // Unschedule Job
        schedulerHomePage.clickJobActionUnschedule(jobName);
        if (schedulerHomePage.hasError()) {
            Assert.fail("Failed to unschedule job.  Error [" + schedulerHomePage.getErrorMessage() + "]");
        }
    }
}
