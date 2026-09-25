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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.scheduler.test.po.SchedulerHomePage;
import org.xwiki.scheduler.test.po.SchedulerJobOutputPage;
import org.xwiki.scheduler.test.po.SchedulerPage;
import org.xwiki.scheduler.test.po.editor.SchedulerEditPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests Scheduler application features.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // The scheduler UI need programming right, and so does the job page holding the Groovy script executed by
        // verifyJobScriptModificationIsUsed()
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern="
            + "xwiki:Scheduler\\.(WebHome|SchedulerScriptTestJob)",

        // Override in order to add the Scheduler Plugin
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.LinkExtensionPlugin,"
            + "com.xpn.xwiki.plugin.scheduler.SchedulerPlugin"
    },
    resolveExtraJARs = true,
    extraJARs = {
        // The Scheduler plugin needs to be in WEB-INF/lib since it's defined in xwiki.cfg and plugins are loaded
        // by XWiki at startup, i.e. before extensions are provisioned for the tests
        "org.xwiki.platform:xwiki-platform-scheduler-api",
        // Because of https://jira.xwiki.org/browse/XWIKI-17972 we need to install the jython jar manually in
        // WEB-INF/lib.
        "org.python:jython-slim"
})
class SchedulerIT
{
    private static final String OUTPUT_PAGE = "SchedulerScriptTestOutput";

    private static final LocalDocumentReference OUTPUT_REFERENCE =
        new LocalDocumentReference("Scheduler", OUTPUT_PAGE);

    @Test
    @Order(1)
    void verifyScheduler(TestUtils setup)
    {
        setup.loginAsSuperAdmin();

        // Make sure the job doesn't exist. Note that we don't delete the job after the test is executed (@After)
        // because we want to remain on the same page in case of a test failure so that our TestDebugger rule can
        // collect accurate information about the failure. It's not a problem if the job remains scheduled because it
        // does nothing. Other tests should not rely on the number of scheduler jobs though.
        setup.deletePage("Scheduler", "Scheduler]]TestJob");

        // Create Job
        SchedulerHomePage schedulerHomePage = SchedulerHomePage.gotoPage();
        schedulerHomePage.setJobName("Scheduler]]TestJob");
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
        assertFalse(setup.getDriver().hasElementWithoutWaiting(By.linkText(jobName)));
        // Note: since the page doesn't exist, we need to disable the space redirect feature so that we end up on the
        // terminal page that was removed.
        setup.gotoPage("Scheduler", "Scheduler]]TestJob", "view", "spaceRedirect=false");
        setup.getDriver().findElement(By.linkText("Restore")).click();
        schedulerPage = new SchedulerPage();
        schedulerPage.backToHome();

        // Schedule Job
        schedulerHomePage.clickJobActionSchedule(jobName);
        if (schedulerHomePage.hasError()) {
            fail("Failed to schedule job. Error [" + schedulerHomePage.getErrorMessage() + "]");
        }

        // Trigger Job (a Job can only be triggered after it's been scheduled)
        schedulerHomePage.clickJobActionTrigger(jobName);
        if (schedulerHomePage.hasError()) {
            fail("Failed to trigger job. Error [" + schedulerHomePage.getErrorMessage() + "]");
        }

        // Pause Job
        schedulerHomePage.clickJobActionPause(jobName);
        if (schedulerHomePage.hasError()) {
            fail("Failed to pause job. Error [" + schedulerHomePage.getErrorMessage() + "]");
        }

        // Resume Job
        schedulerHomePage.clickJobActionResume(jobName);
        if (schedulerHomePage.hasError()) {
            fail("Failed to resume job. Error [" + schedulerHomePage.getErrorMessage() + "]");
        }

        // Unschedule Job
        schedulerHomePage.clickJobActionUnschedule(jobName);
        if (schedulerHomePage.hasError()) {
            fail("Failed to unschedule job.  Error [" + schedulerHomePage.getErrorMessage() + "]");
        }
    }

    @Test
    @Order(2)
    void verifyEscaping(TestUtils setup)
    {
        setup.loginAsSuperAdmin();

        setup.deletePage("Scheduler", "SchedulerTestJob");

        // Create Job
        SchedulerHomePage schedulerHomePage = SchedulerHomePage.gotoPage();
        schedulerHomePage.setJobName("SchedulerTestJob");
        SchedulerEditPage schedulerEdit = schedulerHomePage.clickAdd();

        schedulerEdit.setScript("{{/code}}");
        SchedulerPage schedulerPage = schedulerEdit.clickSaveAndView();

        assertEquals("{{/code}}", schedulerPage.getScript());
    }

    @Test
    @Order(3)
    void verifyJobScriptModificationIsUsed(TestUtils setup)
    {
        setup.loginAsSuperAdmin();

        setup.deletePage("Scheduler", "SchedulerScriptTestJob");
        // Create the output page up front, so that waiting for the content written by a job is waiting for that
        // content to replace a known one, rather than for the page itself to come into existence.
        setup.createPage(OUTPUT_REFERENCE, "no job executed yet");

        // Create a job that writes a known content in the output page. Its cron expression is set far in the future
        // so that the job only runs when it's explicitly triggered.
        SchedulerHomePage schedulerHomePage = SchedulerHomePage.gotoPage();
        schedulerHomePage.setJobName("SchedulerScriptTestJob");
        SchedulerEditPage schedulerEdit = schedulerHomePage.clickAdd();
        String jobName = "Script modification job";
        schedulerEdit.setJobName(jobName);
        schedulerEdit.setJobDescription(jobName);
        schedulerEdit.setCron("0 0 0 1 1 ? 2100");
        schedulerEdit.setScript(getOutputScript("first"));
        schedulerHomePage = schedulerEdit.clickSaveAndView().backToHome();

        schedulerHomePage.clickJobActionSchedule(jobName);
        if (schedulerHomePage.hasError()) {
            fail("Failed to schedule job. Error [" + schedulerHomePage.getErrorMessage() + "]");
        }

        schedulerHomePage.clickJobActionTrigger(jobName);
        if (schedulerHomePage.hasError()) {
            fail("Failed to trigger job. Error [" + schedulerHomePage.getErrorMessage() + "]");
        }
        SchedulerJobOutputPage.gotoPage(OUTPUT_REFERENCE).waitUntilContentIs("first");

        // Modify the script of the scheduled job. The new script must be used without having to unschedule and
        // schedule the job again.
        schedulerHomePage = SchedulerHomePage.gotoPage();
        schedulerEdit = schedulerHomePage.clickJobActionEdit(jobName);
        schedulerEdit.setScript(getOutputScript("second"));
        schedulerHomePage = schedulerEdit.clickSaveAndView().backToHome();

        schedulerHomePage.clickJobActionTrigger(jobName);
        if (schedulerHomePage.hasError()) {
            fail("Failed to trigger job. Error [" + schedulerHomePage.getErrorMessage() + "]");
        }
        SchedulerJobOutputPage.gotoPage(OUTPUT_REFERENCE).waitUntilContentIs("second");

        SchedulerHomePage.gotoPage().clickJobActionUnschedule(jobName);
    }

    private String getOutputScript(String content)
    {
        return String.format("def output = xwiki.getDocument('Scheduler.%s')\noutput.setContent('%s')\n"
            + "output.save()", OUTPUT_PAGE, content);
    }
}
