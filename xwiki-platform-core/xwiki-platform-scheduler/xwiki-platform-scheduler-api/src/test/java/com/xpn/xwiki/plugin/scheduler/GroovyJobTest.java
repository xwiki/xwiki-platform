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

import java.util.function.Consumer;

import javax.inject.Inject;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.xwiki.context.Execution;
import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.context.internal.DefaultExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.ReadOnlyXWikiContextProvider;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.scheduler.internal.SchedulerJobClassDocumentInitializer;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link GroovyJob}.
 *
 * @version $Id$
 */
@OldcoreTest
@ComponentList({
    SchedulerJobClassDocumentInitializer.class,
    DefaultExecutionContextManager.class,
    DefaultExecution.class,
    ReadOnlyXWikiContextProvider.class
})
@ReferenceComponentList
class GroovyJobTest
{
    private static final String WIKI_NAME = "xwiki";

    @Mock
    private Consumer<UserReference> feedbackFunction;

    @Inject
    private Execution execution;

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void programmingRightCheck(boolean allowExecution, MockitoOldcore mockitoOldcore) throws XWikiException,
        JobExecutionException, AccessDeniedException
    {
        GroovyJob job = new GroovyJob();

        // Mock the job context.
        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDetail jobDetail = mock(JobDetail.class);
        when(context.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getKey()).thenReturn(new JobKey("Test Job"));
        JobDataMap jobDataMap = new JobDataMap();
        when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

        // Create a new context to ensure that the context from the job is used.
        XWikiContext testContext = new XWikiContext();
        jobDataMap.put("context", testContext);
        testContext.setWiki(mockitoOldcore.getSpyXWiki());
        testContext.setUserReference(new DocumentReference(WIKI_NAME, "XWiki", "ContextUser"));

        // Create a mock function that we set on the context and that will then be executed by the job.
        testContext.put("feedbackFunction", this.feedbackFunction);

        // Create the job document.
        XWikiDocument jobDocument = new XWikiDocument(new DocumentReference(WIKI_NAME, "Scheduler", "Job"));
        // Set different authors to verify that the correct author is used.
        UserReference contentAuthor = mock(UserReference.class);
        UserReference jobAuthor = mock(UserReference.class);
        jobDocument.getAuthors().setContentAuthor(contentAuthor);
        jobDocument.getAuthors().setOriginalMetadataAuthor(GuestUserReference.INSTANCE);
        jobDocument.getAuthors().setEffectiveMetadataAuthor(jobAuthor);
        BaseObject xObject = jobDocument.newXObject(SchedulerJobClassDocumentInitializer.XWIKI_JOB_CLASSREFERENCE,
            mockitoOldcore.getXWikiContext());
        xObject.setLargeStringValue("script",
            "xcontext.get('feedbackFunction')"
                + ".accept(xcontext.get(com.xpn.xwiki.doc.XWikiDocument.CKEY_SDOC).getAuthors().getContentAuthor())");
        // Don't save the document as this would override the content author with the effective metadata author.
        jobDataMap.put("xjob", xObject);

        doAnswer(invocationOnMock -> {
            XWikiContext xWikiContext =
                (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
            assertEquals(jobDocument, xWikiContext.getDoc());
            XWikiDocument sDocument = (XWikiDocument) xWikiContext.get(XWikiDocument.CKEY_SDOC);
            assertEquals(jobAuthor, sDocument.getAuthors().getContentAuthor());
            assertNotEquals(contentAuthor, sDocument.getAuthors().getContentAuthor());
            assertNotSame(jobDocument, sDocument);

            if (!allowExecution) {
                throw new AccessDeniedException(Right.PROGRAM, new DocumentReference(WIKI_NAME, "XWiki", "JobAuthor"),
                    sDocument.getDocumentReference());
            }
            return null;
        }).when(mockitoOldcore.getMockContextualAuthorizationManager()).checkAccess(Right.PROGRAM);

        if (allowExecution) {
            job.execute(context);
            verify(this.feedbackFunction).accept(jobAuthor);
        } else {
            JobExecutionException jobExecutionException =
                assertThrows(JobExecutionException.class, () -> job.execute(context));
            verify(this.feedbackFunction, never()).accept(any());
            assertEquals("Executing the job [DEFAULT.Test Job] failed due to insufficient rights.",
                jobExecutionException.getMessage());
            assertEquals("Access denied when checking [programming] access to [xwiki:Scheduler.Job] "
                + "for user [xwiki:XWiki.JobAuthor]", jobExecutionException.getCause().getMessage());
            // Make sure the job is not re-executed immediately.
            assertFalse(jobExecutionException.refireImmediately());
        }

        verify(mockitoOldcore.getMockContextualAuthorizationManager()).checkAccess(Right.PROGRAM);
    }
}
