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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.job.JobExecutor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ComponentTest
class WikiDeletedListenerTest
{
    @InjectMockComponents
    private WikiDeletedListener wikiDeletedListener;

    @MockComponent
    private JobExecutor jobExecutor;

    @Test
    void onEvent() throws Exception
    {
        doAnswer(invocationOnMock -> {
            EventStreamWikiCleanerJobRequest request = invocationOnMock.getArgument(1);
            assertEquals("someWiki", request.getWikiId());
            assertEquals(List.of("EventStreamWikiCleanerJob", "someWiki"), request.getId());
            return null;
        }).when(this.jobExecutor).execute(eq("EventStreamWikiCleanerJob"), any(EventStreamWikiCleanerJobRequest.class));

        WikiDeletedEvent wikiDeletedEvent = new WikiDeletedEvent("someWiki");
        this.wikiDeletedListener.onEvent(wikiDeletedEvent, null, null);

        EventStreamWikiCleanerJobRequest request = new EventStreamWikiCleanerJobRequest("someWiki");
        request.setId(List.of("EventStreamWikiCleanerJob", "someWiki"));
        verify(this.jobExecutor).execute(eq("EventStreamWikiCleanerJob"), any(EventStreamWikiCleanerJobRequest.class));
    }
}
