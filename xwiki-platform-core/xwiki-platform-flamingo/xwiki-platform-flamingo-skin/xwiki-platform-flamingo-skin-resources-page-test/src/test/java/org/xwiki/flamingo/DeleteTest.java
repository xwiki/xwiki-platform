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
package org.xwiki.flamingo;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.script.JobScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.page.PageTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of the {@code delete.vm} Velocity template.
 *
 * @version $Id$
 * @since 13.9RC1
 */
class DeleteTest extends PageTest
{

    @Mock
    private JobScriptService jobScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        this.componentManager.registerComponent(ScriptService.class, "job", this.jobScriptService);
    }

    @Test
    void test1() throws Exception
    {
        TemplateManager templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        
        this.request.put("jobId", "refactoring/delete/1632730049718-532");

        when(this.jobScriptService.getJobStatus(Arrays.asList("refactoring", "delete", "1632730049718-532")))
            .thenReturn(mock(JobStatus.class));
        
        String render = templateManager.render("flamingo/delete.vm");
        assertEquals("a", render);
    }
}
