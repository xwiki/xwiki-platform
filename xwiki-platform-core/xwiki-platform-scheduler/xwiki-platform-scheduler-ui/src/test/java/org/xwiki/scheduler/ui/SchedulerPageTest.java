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
package org.xwiki.scheduler.ui;

import java.util.List;
import java.util.stream.Stream;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.quartz.Trigger;
import org.xwiki.csrf.script.CSRFTokenScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.internal.ScriptQuery;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.rendering.internal.macro.message.InfoMessageMacro;
import org.xwiki.rendering.internal.macro.message.WarningMessageMacro;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.scheduler.JobState;
import com.xpn.xwiki.plugin.scheduler.SchedulerPluginApi;
import com.xpn.xwiki.plugin.scheduler.internal.SchedulerJobClassDocumentInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Page tests for {@code Scheduler.WebHome}.
 *
 * @version $Id$
 */
@ComponentList({
    InfoMessageMacro.class,
    ErrorMessageMacro.class,
    SchedulerJobClassDocumentInitializer.class,
    TestNoScriptMacro.class,
    WarningMessageMacro.class
})
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
class SchedulerPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final String XWIKI_SPACE = "Scheduler";

    private static final DocumentReference SCHEDULER_WEB_HOME =
        new DocumentReference(WIKI_NAME, XWIKI_SPACE, "WebHome");

    private static final String CSRF_TOKEN = "a0a0a0a0";

    private QueryManagerScriptService queryService;

    private CSRFTokenScriptService tokenService;

    private SchedulerPluginApi schedulerPluginApi;

    @Mock
    private ScriptQuery query;

    private Object testJobObjectApi;

    @BeforeEach
    void setUp() throws Exception
    {
        // Mock the Query Service to return a job.
        this.queryService = this.oldcore.getMocker().registerMockComponent(ScriptService.class, "query",
            QueryManagerScriptService.class,
            true);
        when(this.queryService.xwql(anyString())).thenReturn(this.query);
        when(this.query.execute()).thenReturn(List.of("Scheduler.TestJob"));

        // Mock the Token Service to get a consistent CSRF token throughout the tests.
        this.tokenService = this.oldcore.getMocker().registerMockComponent(ScriptService.class, "csrf",
            CSRFTokenScriptService.class, true);
        when(this.tokenService.getToken()).thenReturn(CSRF_TOKEN);
        when(this.tokenService.isTokenValid(CSRF_TOKEN)).thenReturn(true);

        // Spy the Scheduler Plugin to obtain a mocked API.
        this.schedulerPluginApi = mock(SchedulerPluginApi.class);
        doReturn(this.schedulerPluginApi).when(this.oldcore.getSpyXWiki()).getPluginApi(eq("scheduler"),
            any(XWikiContext.class));

        this.xwiki.initializeMandatoryDocuments(this.context);

        // Create a new job and keep a reference to its API.
        XWikiDocument testJob = new XWikiDocument(new DocumentReference("xwiki", "Scheduler", "TestJob"));
        BaseObject testJobObject = testJob.newXObject(SchedulerJobClassDocumentInitializer.XWIKI_JOB_CLASSREFERENCE,
            this.context);
        this.xwiki.saveDocument(testJob, this.context);
        this.testJobObjectApi = new Object(testJobObject, this.context);

        // Fake programming access level to display the complete page.
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("programming"), anyString(), anyString(),
            any(XWikiContext.class))).thenReturn(true);
    }

    /**
     * Verify that the trigger operation is not called in the Scheduler Plugin API when the CSRF token is invalid, and
     * that the corresponding error message is properly displayed.
     */
    @Test
    void checkInvalidCSRFToken() throws Exception
    {
        String wrongToken = "wrong token";

        this.request.put("do", "trigger");
        this.request.put("which", "Scheduler.TestJob");
        this.request.put("form_token", wrongToken);
        Document result = renderHTMLPage(SCHEDULER_WEB_HOME);

        verify(this.schedulerPluginApi, never()).triggerJob(any(Object.class));
        verify(this.tokenService).isTokenValid(wrongToken);
        assertEquals("xe.scheduler.invalidToken", result.getElementsByClass("errormessage").text());
    }

    /**
     * Verify that the trigger operation is correctly called in the Scheduler Plugin API when the CSRF token is valid,
     * and that no error displays.
     */
    @Test
    void checkValidCSRFToken() throws Exception
    {
        when(this.schedulerPluginApi.triggerJob(this.testJobObjectApi)).thenReturn(true);

        this.request.put("do", "trigger");
        this.request.put("which", "Scheduler.TestJob");
        this.request.put("form_token", CSRF_TOKEN);
        Document result = renderHTMLPage(SCHEDULER_WEB_HOME);

        verify(this.schedulerPluginApi).triggerJob(this.testJobObjectApi);
        verify(this.tokenService).isTokenValid(CSRF_TOKEN);
        assertTrue(result.getElementsByClass("errormessage").isEmpty());
    }

    /**
     * List every possible action that can be applied to a job depending on its current status.
     *
     * @return a {@link Stream} of {@link org.junit.jupiter.params.provider.Arguments} for every combination of job
     *     status and action
     */
    static Stream<Arguments> jobStatusAndActionProvider()
    {
        return Stream.of(
            Arguments.of(new JobState(Trigger.TriggerState.NONE), "trigger"),
            Arguments.of(new JobState(Trigger.TriggerState.NONE), "schedule"),
            Arguments.of(new JobState(Trigger.TriggerState.NORMAL), "pause"),
            Arguments.of(new JobState(Trigger.TriggerState.NORMAL), "unschedule"),
            Arguments.of(new JobState(Trigger.TriggerState.PAUSED), "resume"),
            Arguments.of(new JobState(Trigger.TriggerState.PAUSED), "unschedule"),
            Arguments.of(new JobState(Trigger.TriggerState.NONE), "delete"));
    }

    /**
     * Verify that each action URL on the page contains the right CSRF token.
     *
     * @param status the status of the job
     * @param action the action to verify
     */
    @ParameterizedTest
    @MethodSource("jobStatusAndActionProvider")
    void checkCSRFTokenPresenceInActionURL(JobState status, String action) throws Exception
    {
        // Set the status of the displayed job to control which action URLs will be rendered.
        when(this.schedulerPluginApi.getJobStatus(this.testJobObjectApi)).thenReturn(status);

        Document result = renderHTMLPage(SCHEDULER_WEB_HOME);
        verify(this.schedulerPluginApi).getJobStatus(this.testJobObjectApi);
        Element actionLink = result.selectFirst(String.format("td a:contains(actions.%s)", action));
        assertNotNull(actionLink);

        // Check the presence of the CSRF token for the given action.
        assertEquals(String.format("path:/xwiki/bin/view/Scheduler/?do=%s&which=Scheduler"
            + ".TestJob&form_token=%s", action, CSRF_TOKEN), actionLink.attr("href"));
    }

    /**
     * Verify that the names of jobs are properly escaped in each action URL.
     *
     * @param status the status of the job
     * @param action the action to verify
     */
    @ParameterizedTest
    @MethodSource("jobStatusAndActionProvider")
    void checkEscapingInJobNames(JobState status, String action) throws Exception
    {
        // Use the `noscript` macro to make sure that no code injection occurs.
        String jobName = "\">]]{{/html}}{{noscript /}}";
        String escapedJobName = "%22%3E%5D%5D%7B%7B%2Fhtml%7D%7D%7B%7Bnoscript%20%2F%7D%7D";

        // Create a new job with a name that needs escaping and get a reference to its API.
        XWikiDocument escapedJob = new XWikiDocument(new DocumentReference("xwiki", "Scheduler", jobName));
        BaseObject escapedJobObject =
            escapedJob.newXObject(SchedulerJobClassDocumentInitializer.XWIKI_JOB_CLASSREFERENCE, this.context);
        Object escapedJobObjectApi = new Object(escapedJobObject, this.context);
        this.xwiki.saveDocument(escapedJob, this.context);

        // Return the name of the new job through the Query Service.
        when(this.query.execute()).thenReturn(List.of("Scheduler." + jobName));

        // Set the status of the new job to control which action URLs will be rendered.
        when(this.schedulerPluginApi.getJobStatus(escapedJobObjectApi)).thenReturn(status);

        Document result = renderHTMLPage(SCHEDULER_WEB_HOME);
        Element actionLink = result.selectFirst(String.format("td a:contains(actions.%s)", action));
        assertNotNull(actionLink);

        // Check the proper escaping of the job name for the given action.
        assertEquals(String.format("path:/xwiki/bin/view/Scheduler/?do=%s&which=Scheduler"
            + ".%s&form_token=%s", action, escapedJobName, CSRF_TOKEN), actionLink.attr("href"));
    }
}
