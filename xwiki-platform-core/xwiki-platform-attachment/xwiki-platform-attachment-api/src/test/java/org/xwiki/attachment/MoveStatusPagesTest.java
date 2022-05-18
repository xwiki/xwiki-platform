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
package org.xwiki.attachment;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobProgress;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.script.JobScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.xml.internal.html.filter.ControlCharactersFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of the macros provided by {@code moveStatus.vm}.
 *
 * @version $Id$
 * @since 14.4RC1
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    ControlCharactersFilter.class,
    TemplateScriptService.class
})
class MoveStatusPagesTest extends PageTest
{
    private static final String MOVE_STATUS_TEMPLATE = "attachment/moveStatus.vm";

    private TemplateManager templateManager;

    @Mock
    private JobScriptService jobScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.componentManager.registerComponent(ScriptService.class, "job", this.jobScriptService);
    }

    @Test
    void renderScriptStatusUnknown() throws Exception
    {
        this.request.put("moveId", "42");
        Document render = Jsoup.parse(this.templateManager.render(MOVE_STATUS_TEMPLATE));
        assertEquals("attachment.move.status.notFound", render.select(".errormessage").text());
        verify(this.jobScriptService).getJobStatus(List.of("refactoring", "moveAttachment", "42"));
    }

    @Test
    void renderScriptActionGet() throws Exception
    {
        JobStatus jobStatus = mock(JobStatus.class);
        Request request = mock(Request.class);
        JobProgress jobProgress = mock(JobProgress.class);

        when(request.getId()).thenReturn(List.of("rq", "id"));
        when(jobProgress.getOffset()).thenReturn(10d);
        when(jobStatus.getRequest()).thenReturn(request);
        when(jobStatus.getState()).thenReturn(JobStatus.State.RUNNING);
        when(jobStatus.getProgress()).thenReturn(jobProgress);
        when(this.jobScriptService.getJobStatus(List.of("refactoring", "moveAttachment", "42")))
            .thenReturn(jobStatus);
        this.request.put("moveId", "42");
        this.context.setAction("get");
        assertEquals(
            "{\"id\":[\"rq\",\"id\"],\"state\":\"RUNNING\",\"progress\":{\"offset\":10.0},"
                + "\"log\":{\"offset\":0,\"items\":[]},\"message\":\"        \",\"questionTimeLeft\":0}",
            this.templateManager.render(MOVE_STATUS_TEMPLATE).trim());
    }
}
