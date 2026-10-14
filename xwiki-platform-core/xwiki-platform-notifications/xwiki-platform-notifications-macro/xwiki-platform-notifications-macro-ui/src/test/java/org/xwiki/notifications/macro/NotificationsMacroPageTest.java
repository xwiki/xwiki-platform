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
package org.xwiki.notifications.macro;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.model.internal.reference.converter.WikiReferenceConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.notifiers.script.NotificationNotifiersScriptService;
import org.xwiki.notifications.script.NotificationScriptService;
import org.xwiki.notifications.sources.script.NotificationSourcesScriptService;
import org.xwiki.properties.internal.converter.ListConverter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wikimacro.internal.WikiMacroFactoryComponentClass;
import org.xwiki.script.service.ScriptService;
import org.xwiki.tag.internal.TagConverter;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.user.DefaultUserComponentList;
import org.xwiki.user.internal.converter.UserReferenceConverter;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.DocumentReferenceConverter;
import com.xpn.xwiki.internal.model.reference.SpaceReferenceConverter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.test.page.WikiMacroSetup.loadWikiMacro;

/**
 * Page test of the {@code XWiki.Notifications.Code.Macro.NotificationsMacro} wiki macro.
 *
 * @version $Id$
 */
@ComponentList({
    // The converters of the typed macro parameters.
    ListConverter.class,
    WikiReferenceConverter.class,
    SpaceReferenceConverter.class,
    DocumentReferenceConverter.class,
    UserReferenceConverter.class,
    TagConverter.class
})
@DefaultUserComponentList
@XWikiSyntax21ComponentList
@HTML50ComponentList
@WikiMacroFactoryComponentClass
class NotificationsMacroPageTest extends PageTest
{
    private final NotificationSourcesScriptService sourcesScriptService =
        mock(NotificationSourcesScriptService.class);

    @BeforeEach
    void setUp() throws Exception
    {
        NotificationScriptService notificationScriptService = this.componentManager.registerMockComponent(
            ScriptService.class, NotificationScriptService.ROLE_HINT, NotificationScriptService.class, false);
        when(notificationScriptService.get("sources")).thenReturn(this.sourcesScriptService);
        NotificationNotifiersScriptService notifiersScriptService = mock(NotificationNotifiersScriptService.class);
        when(notificationScriptService.get("notifiers")).thenReturn(notifiersScriptService);
        when(notifiersScriptService.getNotifications(any(), any(Boolean.class))).thenReturn("");

        loadWikiMacro(this, this.componentManager,
            new DocumentReference("xwiki", List.of("XWiki", "Notifications", "Code", "Macro"), "NotificationsMacro"));
    }

    @Test
    void locationParametersArePassedAsGiven() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);
        document.setSyntax(Syntax.XWIKI_2_1);
        document.setContent("{{notifications wikis=\"xwiki,subwiki\" spaces=\"Space1,Space1.Nested\" "
            + "pages=\"Space1.Page1,Space2.Page2\" users=\"XWiki.User1,XWiki.User2\" tags=\"tag1,tag2\"/}}");
        this.xwiki.saveDocument(document, this.context);

        renderHTMLPage(document);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> parameters = ArgumentCaptor.forClass(Map.class);
        verify(this.sourcesScriptService).getNotificationParameters(parameters.capture());
        assertEquals("xwiki,subwiki", parameters.getValue().get("wikis"));
        assertEquals("Space1,Space1.Nested", parameters.getValue().get("spaces"));
        assertEquals("Space1.Page1,Space2.Page2", parameters.getValue().get("pages"));
        assertEquals("XWiki.User1,XWiki.User2", parameters.getValue().get("users"));
        assertEquals("tag1,tag2", parameters.getValue().get("tags"));
    }
}
