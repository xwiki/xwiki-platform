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
package org.xwiki.notifications;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.notifiers.script.NotificationNotifiersScriptService;
import org.xwiki.notifications.script.NotificationScriptService;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.internal.code.layout.PlainLayoutHandler;
import org.xwiki.rendering.internal.macro.code.CodeMacro;
import org.xwiki.rendering.internal.macro.code.source.DefaultCodeMacroSourceFactory;
import org.xwiki.rendering.internal.macro.code.source.ScriptCodeMacroSourceFactory;
import org.xwiki.rendering.internal.macro.script.source.ScriptMacroContentWikiSourceFactory;
import org.xwiki.rendering.internal.macro.source.MacroContentSourceReferenceConverter;
import org.xwiki.rendering.parser.HighlightParser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.web.XWikiServletResponseStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test for {@code NotificationRSSService}.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@ComponentList({
    CodeMacro.class,
    DefaultCodeMacroSourceFactory.class,
    ScriptCodeMacroSourceFactory.class,
    ScriptMacroContentWikiSourceFactory.class,
    MacroContentSourceReferenceConverter.class,
    PlainLayoutHandler.class
})
class NotificationRSSServicePageTest extends PageTest
{
    private static final DocumentReference RSS_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", List.of("XWiki", "Notifications", "Code"), "NotificationRSSService");

    private static final String NOTIFIERS_HINT = "notification.notifiers";

    private NotificationNotifiersScriptService notifiersScriptService;

    @MockComponent
    private HighlightParser highlightParser;

    @BeforeEach
    public void setUp(MockitoComponentManager componentManager) throws Exception
    {
        NotificationScriptService notificationScriptService =
            componentManager.registerMockComponent(ScriptService.class, "notification",
                NotificationScriptService.class, false);
        this.notifiersScriptService = componentManager.registerMockComponent(ScriptService.class,
            NOTIFIERS_HINT, NotificationNotifiersScriptService.class, false);
        when(notificationScriptService.get("notifiers")).thenReturn(this.notifiersScriptService);
    }

    @Test
    void rssResult() throws Exception
    {
        loadPage(RSS_DOCUMENT_REFERENCE);
        this.request.put("outputSyntax", "plain");
        this.setOutputSyntax(Syntax.PLAIN_1_0);
        this.context.setAction("get");

        String testContent = "//Test//";
        when(this.notifiersScriptService.getFeed(anyInt())).thenReturn(testContent);

        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);

        this.response = new XWikiServletResponseStub() {
            @Override
            public PrintWriter getWriter() throws IOException
            {
                return writer;
            }
        };
        this.context.setResponse(this.response);

        String result = renderPage(RSS_DOCUMENT_REFERENCE);
        assertTrue(StringUtils.isAllBlank(result));
        assertEquals(testContent, out.toString());
    }

    @Test
    void codeMacroResult() throws Exception
    {
        loadPage(RSS_DOCUMENT_REFERENCE);

        String testContent = "{{/code}}";
        when(this.notifiersScriptService.getFeed(anyInt())).thenReturn(testContent);
        when(this.highlightParser.highlight(eq("xml"), any())).then(invocation ->
        {
            Reader source = invocation.getArgument(1);
            return List.of(new WordBlock(IOUtils.toString(source)));
        });
        String result = renderPage(RSS_DOCUMENT_REFERENCE);
        assertEquals("<div class=\"box\"><div class=\"code\">&#123;&#123;/code}}</div></div>", result);
    }
}
