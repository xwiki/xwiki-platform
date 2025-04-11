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
package org.xwiki.web;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.page.PageTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToCompressingWhiteSpace;

/**
 * Page test for {@code job_status_json.vm}.
 *
 * @version $Id$
 */
class JobStatusJSONPageTest extends PageTest
{
    @Inject
    private TemplateManager templateManager;

    @Test
    void nonExistingJob() throws Exception
    {
        this.request.put("jobId", "<test>");
        this.request.put("translationPrefix", "<test>");

        String output = this.templateManager.render("job_status_json.vm");

        assertThat(output, equalToCompressingWhiteSpace("""
            <div class="box errormessage">
            &#60;test&#62;.notFound
            </div>"""));
    }
}
