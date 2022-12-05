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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the {@code xpart.vm} template.
 *
 * @version $Id$
 * @since 14.3RC1
 * @since 13.10.5
 */
@ComponentList({
    TemplateScriptService.class
})
class XPartPageTest extends PageTest
{
    private static final String X_PART_VM = "xpart.vm";

    private static final String VM_PARAMETER = "vm";

    private TemplateManager templateManager;

    @BeforeEach
    void setUp() throws Exception
    {
        this.templateManager = this.componentManager.getInstance(TemplateManager.class);
        this.request.put("xpage", "xpart");
    }

    @Test
    void renderRegisterTemplate() throws Exception
    {
        this.request.put(VM_PARAMETER, "register.vm");
        String result = this.templateManager.render(X_PART_VM);
        assertTrue(result.contains("<form id=\"register\""));
    }

    @Test
    void renderForbiddenTemplate() throws Exception
    {
        this.request.put(VM_PARAMETER, "distribution/firstadminuser.wiki");
        String result = this.templateManager.render(X_PART_VM);
        assertEquals("Invalid template.", result.trim());
    }
}
