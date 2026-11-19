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

import javax.inject.Named;
import javax.script.ScriptContext;

import org.junit.jupiter.api.Test;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.script.WikiManagerScriptService;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.syntax.Syntax.PLAIN_1_0;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Page test for the {@code logging_macros.vm} template.
 *
 * @version $Id$
 */
@XWikiSyntax21ComponentList
@ComponentList(TemplateMacro.class)
class LoggingMacrosPageTest extends PageTest
{
    @MockComponent(classToMock = WikiManagerScriptService.class)
    @Named(WikiManagerScriptService.ROLEHINT)
    private ScriptService wikiScriptService;

    @Test
    void printLogMessageWithWikiNamespaces() throws Exception
    {
        WikiDescriptor wikiDescriptor = new WikiDescriptor("xwiki", "localhost");
        wikiDescriptor.setPrettyName("My Wiki");
        wikiDescriptor.setMainPageReference(new DocumentReference("xwiki", "Home", "WebHome"));
        when(((WikiManagerScriptService) this.wikiScriptService).getById("xwiki")).thenReturn(wikiDescriptor);

        LogEvent log = new LogEvent(LogLevel.INFO, "Uninstalled extension on namespaces [{}] and [{}]",
            new Object[] {"wiki:xwiki", "wiki:deletedwiki"}, null);
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

        // The link targets the home page configured for the wiki, and a wiki which does not exist is not linked.
        assertEquals("Uninstalled extension on namespaces [<a href=\"/xwiki/bin/view/Home/\">My Wiki</a>] and "
            + "[deletedwiki]", document.getRenderedContent(PLAIN_1_0, this.context).trim());
    }
}
