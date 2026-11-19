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

import javax.script.ScriptContext;

import org.junit.jupiter.api.Test;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xwiki.rendering.syntax.Syntax.PLAIN_1_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Page test for the {@code logging_macros.vm} template.
 *
 * @version $Id$
 */
@XWikiSyntax21ComponentList
@ComponentList({
    TemplateMacro.class,
    ModelScriptService.class
})
class LoggingMacrosPageTest extends PageTest
{
    @Test
    void printLogMessageWithWikiNamespace() throws Exception
    {
        LogEvent log = new LogEvent(LogLevel.INFO, "Uninstalled extension on namespace [{}]",
            new Object[] {"wiki:xwiki"}, null);
        this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class)
            .getCurrentScriptContext().setAttribute("log", log, ScriptContext.GLOBAL_SCOPE);

        // Only logging_macros.vm is loaded, like when the log of a job is refreshed through job_status_json.vm.
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Test", "Page"), this.context);
        document.setSyntax(XWIKI_2_1);
        document.setContent("""
            {{template name='logging_macros.vm' output='false'/}}
            {{velocity}}
            #printLogMessage($log)
            {{/velocity}}""");

        assertEquals("Uninstalled extension on namespace [<a href=\"/xwiki/bin/view/Main/\">xwiki</a>]",
            document.getRenderedContent(PLAIN_1_0, this.context).trim());
    }
}
