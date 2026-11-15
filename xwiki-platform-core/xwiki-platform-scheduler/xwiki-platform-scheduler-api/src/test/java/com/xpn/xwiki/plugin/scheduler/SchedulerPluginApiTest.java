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
package com.xpn.xwiki.plugin.scheduler;

import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link SchedulerPluginApi}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class SchedulerPluginApiTest
{
    private static final DocumentReference JOB_DOCUMENT = new DocumentReference("xwiki", "Scheduler", "Job");

    private static final String ERROR = "error";

    private static final String JOB_ERROR = "Error number 90010 in 5: Exception in plugin "
        + "[com.xpn.xwiki.plugin.scheduler.SchedulerPlugin]: Job with reference "
        + "[Object xwiki:Scheduler.Job^XWiki.SchedulerJobClass[0]] could not be ";

    private static final String NOT_FOUND_ERROR = JOB_ERROR + "found.";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private XWikiContext context;

    private SchedulerPlugin plugin;

    private SchedulerPluginApi api;

    @BeforeEach
    void beforeEach()
    {
        this.context = this.oldcore.getXWikiContext();
        this.plugin = mock(SchedulerPlugin.class);
        this.api = new SchedulerPluginApi(this.plugin, this.context);
    }

    @Test
    void scheduleJob() throws Exception
    {
        Object job = createJob();

        assertTrue(this.api.scheduleJob(job));

        // The job is retrieved from the stored document, without requiring programming right.
        verify(this.plugin).scheduleJob(
            eq(this.oldcore.getSpyXWiki().getDocument(JOB_DOCUMENT, this.context)
                .getXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE)),
            same(this.context));
        assertNull(this.context.get(ERROR));
    }

    @Test
    void scheduleJobWhenTheJobDocumentCannotBeLoaded() throws Exception
    {
        Object job = createJob();
        doThrow(new XWikiException(0, 0, "Store failure")).when(this.oldcore.getSpyXWiki())
            .getDocument(any(EntityReference.class), any(XWikiContext.class));

        assertFalse(this.api.scheduleJob(job));

        assertEquals(JOB_ERROR + "retrieved.", this.context.get(ERROR));
        verifyNoInteractions(this.plugin);
    }

    static Stream<Arguments> jobActions()
    {
        return Stream.of(
            jobAction("scheduleJob", SchedulerPluginApi::scheduleJob, false),
            jobAction("pauseJob", SchedulerPluginApi::pauseJob, false),
            jobAction("resumeJob", SchedulerPluginApi::resumeJob, false),
            jobAction("unscheduleJob", SchedulerPluginApi::unscheduleJob, false),
            jobAction("triggerJob", SchedulerPluginApi::triggerJob, false),
            jobAction("getNextFireTime", SchedulerPluginApi::getNextFireTime, null),
            jobAction("getPreviousFireTime", SchedulerPluginApi::getPreviousFireTime, null));
    }

    private static Arguments jobAction(String name, BiFunction<SchedulerPluginApi, Object, java.lang.Object> action,
        java.lang.Object failureResult)
    {
        return Arguments.of(Named.of(name, action), failureResult);
    }

    @ParameterizedTest
    @MethodSource("jobActions")
    void jobActionWhenTheJobObjectNoLongerExists(BiFunction<SchedulerPluginApi, Object, java.lang.Object> action,
        java.lang.Object failureResult) throws Exception
    {
        Object job = createJob();
        // The job object has been removed from the job document since it was loaded.
        this.oldcore.getSpyXWiki().saveDocument(new XWikiDocument(JOB_DOCUMENT), this.context);

        assertEquals(failureResult, action.apply(this.api, job));

        assertEquals(NOT_FOUND_ERROR, this.context.get(ERROR));
        verifyNoInteractions(this.plugin);
    }

    @Test
    void getJobStatusWhenTheJobObjectNoLongerExists() throws Exception
    {
        Object job = createJob();
        this.oldcore.getSpyXWiki().saveDocument(new XWikiDocument(JOB_DOCUMENT), this.context);

        SchedulerPluginException exception =
            assertThrows(SchedulerPluginException.class, () -> this.api.getJobStatus(job));

        assertEquals(NOT_FOUND_ERROR, exception.getMessage());
        assertEquals(NOT_FOUND_ERROR, this.context.get(ERROR));
    }

    private Object createJob() throws XWikiException
    {
        XWikiDocument document = new XWikiDocument(JOB_DOCUMENT);
        BaseObject job = document.newXObject(SchedulerPlugin.XWIKI_JOB_CLASSREFERENCE, this.context);
        job.setStringValue("jobName", "Test Job");
        this.oldcore.getSpyXWiki().saveDocument(document, this.context);
        return new Object(job, this.context);
    }
}
