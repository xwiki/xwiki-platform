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
package org.xwiki.logging;

import java.util.HashMap;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.internal.macro.LiveDataMacroComponentList;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.message.ErrorMessageMacro;
import org.xwiki.rendering.internal.macro.message.SuccessMessageMacro;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.plugin.skinx.SkinExtensionPluginApi;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@code XWiki.LoggingAdmin}.
 *
 * @version $Id$
 * @since 13.10.11
 * @since 14.4.7
 * @since 14.10
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@LiveDataMacroComponentList
@LoggingScriptServiceComponentList
@ComponentList({
    TranslationMacro.class,
    ErrorMessageMacro.class,
    SuccessMessageMacro.class,
    EnumConverter.class
})
class LoggingAdminPageTest extends PageTest
{
    private LoggerManager loggerManager;

    @BeforeEach
    void setUp() throws Exception
    {
        // Spy the jsfx plugin used during the macro rendering to return a mock of its API when required. 
        when(this.oldcore.getSpyXWiki().getPluginApi("jsfx", this.context))
            .thenReturn(mock(SkinExtensionPluginApi.class));
        // Return minimal icons metadata since this is not what we want to test here.
        IconManager iconManager = this.componentManager.registerMockComponent(IconManager.class);
        doReturn(new HashMap<>()).when(iconManager).getMetaData(anyString());
        // Setting all rights as allowed by default.
        when(this.xwiki.getRightService().hasAccessLevel(any(), any(), anyString(), eq(this.context)))
            .thenReturn(true);
        when(this.componentManager.<ContextualAuthorizationManager>getInstance(ContextualAuthorizationManager.class)
            .hasAccess(any())).thenReturn(true);
        // Register a mocked LoggerManager as we want to control the logger level. 
        this.loggerManager = this.componentManager.registerMockComponent(LoggerManager.class);
        Logger loggerA = mock(Logger.class);
        when(loggerA.getName()).thenReturn("loggerA");
        when(this.loggerManager.getLoggers()).thenReturn(asList(loggerA));
    }

    @Test
    void unknownLogger() throws Exception
    {
        this.request.put("loggeraction_set", "1");
        this.request.put("logger_name", "{{cache}}{{groovy}}new File(\"/tmp/exploit.txt\").withWriter { out -> "
            + "out.println(\"created from notification filter preferences!\"); "
            + "}{{/groovy}}{{/cache}}<strong>bold</strong>\"'");
        this.request.put("logger_level", "TRACE");
        Document document = renderHTMLPage(new DocumentReference("xwiki", "XWiki", "LoggingAdmin"));
        assertEquals("logging.admin.setLevel.error [{{cache}}{{groovy}}new File(\"/tmp/exploit.txt\")"
            + ".withWriter { out -> out.println(\"created from notification filter preferences!\"); }{{/groovy}}"
            + "{{/cache}}<strong>bold</strong>\"']", document.select(".box.errormessage").text());
        verify(this.loggerManager, never()).setLoggerLevel(any(), any());
    }

    @Test
    void unsetLogger() throws Exception
    {
        this.request.put("loggeraction_set", "1");
        this.request.put("logger_name", "loggerA");
        this.request.put("logger_level", "");
        Document document = renderHTMLPage(new DocumentReference("xwiki", "XWiki", "LoggingAdmin"));
        assertEquals("logging.admin.unsetLevel.success [loggerA]", document.select(".box.successmessage").text());
        verify(this.loggerManager).setLoggerLevel("loggerA", null);
    }

    @Test
    void setLogger() throws Exception
    {
        this.request.put("loggeraction_set", "1");
        this.request.put("logger_name", "loggerA");
        this.request.put("logger_level", "TRACE");
        Document document = renderHTMLPage(new DocumentReference("xwiki", "XWiki", "LoggingAdmin"));
        assertEquals("logging.admin.setLevel.success [loggerA, TRACE]", document.select(".box.successmessage").text());
        verify(this.loggerManager).setLoggerLevel("loggerA", LogLevel.TRACE);
    }
}
