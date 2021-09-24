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

import java.util.List;

import org.apache.velocity.tools.generic.NumberTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.query.script.QueryManagerScriptService;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.internal.XWikiDateTool;
import org.xwiki.velocity.tools.EscapeTool;
import org.xwiki.velocity.tools.JSONTool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test the {@code getdocuments.vm} template. Assert that the hql queries are well-formed.
 *
 * @version $Id$
 * @since 13.9RC1
 */
@ComponentList(XWikiDateTool.class)
class GetdocumentsTest extends PageTest
{
    private static final String GETDOCUMENTS = "getdocuments.vm";

    private TemplateManager templateManager;

    private VelocityManager velocityManager;

    private QueryManagerScriptService queryManagerScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.oldcore.getMocker().getInstance(TemplateManager.class);
        this.queryManagerScriptService = mock(QueryManagerScriptService.class);
        this.oldcore.getMocker().registerComponent(ScriptService.class, "query", this.queryManagerScriptService);
        registerVelocityTool("jsontool", new JSONTool());

        this.velocityManager = this.oldcore.getMocker().getInstance(VelocityManager.class);
        this.velocityManager.getVelocityContext().put("escapetool", new EscapeTool());
        this.velocityManager.getVelocityContext()
            .put("datetool", this.componentManager.getInstance(XWikiDateTool.class));
        this.velocityManager.getVelocityContext().put("numbertool", new NumberTool());
    }

    /**
     * Request the {@code doc.date} field, filtered by a date range using ISO 8601 time intervals.
     *
     * @throws Exception case of error during the test execution
     */
    @Test
    void dateFilterBetweenISO8601() throws Exception
    {
        this.request.put("offset", "1");
        this.request.put("limit", "15");
        this.request.put("collist", "doc.date");
        this.request.put("doc.date_match", "between");
        this.request.put("doc.date/join_mode", "OR");
        this.request.put("childrenOf", "Sandbox");
        this.request.put("doc.date", "2021-09-22T00:00:00+02:00/2021-09-22T23:59:59+02:00");
        this.templateManager.render(GETDOCUMENTS);
        verify(this.queryManagerScriptService).hql(
            "WHERE 1=1 AND doc.fullName LIKE ?1 AND doc.fullName <> ?2 and doc.date between ?3 and ?4 ");
        List<Object> queryParams = (List<Object>) this.velocityManager.getVelocityContext().get("queryParams");
        assertNull(queryParams.get(0));
        assertEquals("Sandbox.WebHome", queryParams.get(1));
        assertEquals("Wed Sep 22 00:00:00 CEST 2021", queryParams.get(2).toString());
        assertEquals("Wed Sep 22 23:59:59 CEST 2021", queryParams.get(3).toString());
    }

    @Test
    void dateFilterBetweenTimestamp() throws Exception
    {
        this.request.put("outputSyntax", "plain");
        this.request.put("transprefix", "platform.index.");
        this.request.put("classname", "");
        this.request.put("collist", "doc.title,doc.location,doc.date,doc.author,_likes");
        this.request.put("queryFilters", "currentlanguage,hidden");
        this.request.put("offset", "1");
        this.request.put("limit", "15");
        this.request.put("reqNo", "3");
        this.request.put("doc.date", "1632348000000-1632434399999");
        this.request.put("sort", "doc.date");
        this.request.put("dir", "asc");
        this.templateManager.render(GETDOCUMENTS);
        verify(this.queryManagerScriptService).hql(
            "WHERE 1=1 and doc.date between ?1 and ?2 order by doc.date asc");
        List<Object> queryParams = (List<Object>) this.velocityManager.getVelocityContext().get("queryParams");
        assertEquals("Thu Sep 23 00:00:00 CEST 2021", queryParams.get(0).toString());
        assertEquals("Thu Sep 23 23:59:59 CEST 2021", queryParams.get(1).toString());
    }
}
