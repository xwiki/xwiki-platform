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
package org.xwiki.platform.wiki.creationjob.internal;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
class DefaultWikiCreatorTest
{
    @InjectMockComponents
    private DefaultWikiCreator defaultWikiCreator;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private JobStatusStore jobStatusStore;

    @Test
    void createWiki() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");

        // Mock
        Job job = mock(Job.class);
        when(this.jobExecutor.execute("wikicreationjob", request)).thenReturn(job);

        // Test
        assertEquals(job, this.defaultWikiCreator.createWiki(request));

        // Verify
        assertEquals(List.of("wikicreation", "createandinstall", "wikiId"), request.getId());
    }

    @Test
    void createWikiWithException() throws Exception
    {
        WikiCreationRequest request = new WikiCreationRequest();
        request.setWikiId("wikiId");

        // Mock
        JobException jobException = new JobException("Error in JobException");
        when(this.jobExecutor.execute("wikicreationjob", request)).thenThrow(jobException);

        // Test and verify
        assertThrows(WikiCreationException.class, () -> this.defaultWikiCreator.createWiki(request));
    }

    @Test
    void getJobStatus() throws Exception
    {
        // Mocks
        Job job = mock(Job.class);
        JobStatus jobStatus1 = mock(JobStatus.class);
        when(job.getStatus()).thenReturn(jobStatus1);
        when(this.jobExecutor.getJob(List.of("wikicreation", "createandinstall", "wiki1"))).thenReturn(job);
        JobStatus jobStatus2 = mock(JobStatus.class);
        when(this.jobStatusStore.getJobStatus(List.of("wikicreation", "createandinstall", "wiki2")))
            .thenReturn(jobStatus2);

        // Tests
        assertEquals(jobStatus1, this.defaultWikiCreator.getJobStatus("wiki1"));
        assertEquals(jobStatus2, this.defaultWikiCreator.getJobStatus("wiki2"));
    }
}
