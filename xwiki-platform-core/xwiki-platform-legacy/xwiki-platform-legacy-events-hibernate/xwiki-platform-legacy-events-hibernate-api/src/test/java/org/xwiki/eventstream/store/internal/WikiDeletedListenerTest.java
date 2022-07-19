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
package org.xwiki.eventstream.store.internal;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.job.JobExecutor;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link WikiDeletedListener}.
 * 
 * @version $Id$
 */
public class WikiDeletedListenerTest
{
    @Rule
    public MockitoComponentMockingRule<WikiDeletedListener> mocker =
        new MockitoComponentMockingRule<>(WikiDeletedListener.class);

    private JobExecutor jobExecutor;

    @Before
    public void setUp() throws Exception
    {
        jobExecutor = mocker.getInstance(JobExecutor.class);
    }

    @Test
    public void onEvent() throws Exception
    {
        doAnswer(invocationOnMock -> {
            EventStreamWikiCleanerJobRequest request = invocationOnMock.getArgument(1);
            assertEquals("someWiki", request.getWikiId());
            assertEquals(Arrays.asList("EventStreamWikiCleanerJob", "someWiki"), request.getId());
            return null;
        }).when(jobExecutor).execute(eq("EventStreamWikiCleanerJob"), any(EventStreamWikiCleanerJobRequest.class));

        WikiDeletedEvent wikiDeletedEvent = new WikiDeletedEvent("someWiki");
        mocker.getComponentUnderTest().onEvent(wikiDeletedEvent, null, null);

        // Verify
        EventStreamWikiCleanerJobRequest request = new EventStreamWikiCleanerJobRequest("someWiki");
        request.setId(Arrays.asList("EventStreamWikiCleanerJob", "someWiki"));
        verify(jobExecutor).execute(eq("EventStreamWikiCleanerJob"), any(EventStreamWikiCleanerJobRequest.class));
    }
}
