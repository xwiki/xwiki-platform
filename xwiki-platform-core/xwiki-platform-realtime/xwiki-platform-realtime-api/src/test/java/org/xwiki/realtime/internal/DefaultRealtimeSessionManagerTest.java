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

package org.xwiki.realtime.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.netflux.EntityChannel;
import org.xwiki.netflux.EntityChannelStore;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.sheet.SheetManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wysiwyg.script.WysiwygEditorScriptService;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

@ComponentTest
class DefaultRealtimeSessionManagerTest
{

    private static final String WIKI = "wiki";

    private static final String WYSIWYG = "wysiwyg";

    private static final String EDITOR = "editor";

    private static final String EDIT = "edit";

    private static final String EN = "en";

    private static final String CONTENT = "content";

    @InjectMockComponents
    private DefaultRealtimeSessionManager realtimeSessionManager;

    @MockComponent
    private Provider<XWikiContext> xwikiContextProvider;

    @MockComponent
    private EntityChannelStore entityChannelStore;

    @MockComponent
    private SheetManager sheetManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private Container container;

    @Mock
    private Request request;

    @Mock
    private ScriptContext scriptContext;

    @Mock
    private XWikiContext xwikiContext;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWiki wiki;

    EntityChannel channel;

    private WysiwygEditorScriptService wysiwygEditorScriptService;

    private List<String> path;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        // The tested code casts the injected ScriptService to a WysiwygEditorScriptService.
        // To allow for this cast, we need to mock the WysiwygEditorScriptService class directly.
        wysiwygEditorScriptService = mock(WysiwygEditorScriptService.class);
        componentManager.registerComponent(ScriptService.class, WYSIWYG, wysiwygEditorScriptService);
    }

    @BeforeEach
    void setup()
    {
        // Common configuration for getEditMode.

        when(container.getRequest()).thenReturn(request);
        when(scriptContextManager.getCurrentScriptContext()).thenReturn(scriptContext);

        // Set the wiki editor preference to a value other than wysiwyg.
        when(wiki.getEditorPreference(xwikiContext)).thenReturn("other");

        // Always set the document syntax to XWiki 2.1.
        // We change the supported syntaxes rather than the document syntax.
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        // The current document is open in edit mode.
        when(xwikiContext.get("tdoc")).thenReturn(document);
        when(xwikiContext.getAction()).thenReturn(EDIT);
        when(xwikiContext.getWiki()).thenReturn(wiki);
        when(xwikiContextProvider.get()).thenReturn(xwikiContext);

        // This case is the default test configuration, but we test both.
        when(wysiwygEditorScriptService.isSyntaxSupported(Syntax.XWIKI_2_1.toIdString())).thenReturn(false);

        // Configuration for canJoinSession.

        // Define a channel with one user.
        path = Arrays.asList("translations", EN, "fields", CONTENT, "editors", WIKI);

        channel = new EntityChannel(null, path, "");
        channel.setUserCount(1);
        when(entityChannelStore.getChannel(null, path)).thenReturn(Optional.of(channel));
    }

    @Test
    void getEditModeFromQueryString()
    {
        when(request.getParameter(EDITOR)).thenReturn("testQuery");
        assertEquals("testQuery", realtimeSessionManager.getEditMode());
    }

    @Test
    void getEditModeFromScriptContext()
    {
        when(scriptContext.getAttribute(EDITOR)).thenReturn("testScript");
        assertEquals("testScript", realtimeSessionManager.getEditMode());
    }

    @Test
    void getEditModeDefaultInline()
    {
        DocumentReference[] sheets = {new DocumentReference("xwiki", "XWiki", "sheet")};
        when(sheetManager.getSheets(document, EDIT)).thenReturn(Arrays.asList(sheets));
        assertEquals("inline", realtimeSessionManager.getEditMode());
    }

    @Test
    void getEditModeDefaultWysiwygSupported()
    {
        when(wysiwygEditorScriptService.isSyntaxSupported(Syntax.XWIKI_2_1.toIdString())).thenReturn(true);
        when(wiki.getEditorPreference(xwikiContext)).thenReturn(WYSIWYG);
        assertEquals(WYSIWYG, realtimeSessionManager.getEditMode());
    }

    @Test
    void getEditModeDefaultWysiwygNotSupported()
    {
        when(wiki.getEditorPreference(xwikiContext)).thenReturn(WYSIWYG);
        assertEquals(WIKI, realtimeSessionManager.getEditMode());
    }

    @Test
    void getEditModeDefaultOther()
    {
        assertEquals(WIKI, realtimeSessionManager.getEditMode());
    }

    @Test
    void sessionIsActiveSameEditor()
    {
        when(request.getParameter(EDITOR)).thenReturn(WIKI);
        assertTrue(realtimeSessionManager.canJoinSession(null, Locale.ENGLISH));
    }

    @Test
    void sessionIsActiveSameEditorNoUser()
    {
        when(scriptContext.getAttribute(EDITOR)).thenReturn(WIKI);
        channel.setUserCount(0);
        assertFalse(realtimeSessionManager.canJoinSession(null, Locale.ENGLISH));
    }

    @Test
    void sessionIsActiveDifferentEditor()
    {
        when(request.getParameter(EDITOR)).thenReturn(WYSIWYG);
        assertFalse(realtimeSessionManager.canJoinSession(null, Locale.ENGLISH));
    }

    @Test
    void sessionIsActiveDifferentLocale()
    {
        when(scriptContext.getAttribute(EDITOR)).thenReturn(WIKI);
        assertFalse(realtimeSessionManager.canJoinSession(null, Locale.GERMAN));
    }

    @Test
    void sessionIsActiveWysiwygAndInplace()
    {
        path = Arrays.asList("translations", EN, "fields", CONTENT, "editors", WYSIWYG);
        channel = new EntityChannel(null, path, "");
        channel.setUserCount(1);
        when(entityChannelStore.getChannel(null, path)).thenReturn(Optional.of(channel));
        when(scriptContext.getAttribute(EDITOR)).thenReturn("inplace");
        assertTrue(realtimeSessionManager.canJoinSession(null, Locale.ENGLISH));
    }

    @Test
    void sessionMissing()
    {
        when(request.getParameter(EDITOR)).thenReturn(WIKI);
        when(entityChannelStore.getChannel(null, path)).thenReturn(Optional.empty());
        assertFalse(realtimeSessionManager.canJoinSession(null, Locale.ENGLISH));
    }

    @Test
    void unsupportedEditMode()
    {
        when(request.getParameter(EDITOR)).thenReturn("inline");
        assertFalse(realtimeSessionManager.canJoinSession(null, Locale.ENGLISH));
    }
}
