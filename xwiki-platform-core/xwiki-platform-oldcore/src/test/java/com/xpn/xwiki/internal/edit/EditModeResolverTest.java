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
package com.xpn.xwiki.internal.edit;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorDescriptor;
import org.xwiki.edit.EditorManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxContent;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.sheet.SheetManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EditModeResolver}.
 *
 * @version $Id$
 */
@ComponentTest
class EditModeResolverTest
{
    private static final String EDITOR = "editor";

    private static final String EDIT = "edit";

    private static final String XWIKI_2_1 = Syntax.XWIKI_2_1.toIdString();

    @InjectMockComponents
    private EditModeResolver editModeResolver;

    @MockComponent
    private Provider<XWikiContext> xwikiContextProvider;

    @MockComponent
    private SheetManager sheetManager;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private Container container;

    @MockComponent
    private EditorManager editorManager;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

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

    private ComponentManager contextComponentManager;

    @BeforeEach
    void setUp()
    {
        when(this.container.getRequest()).thenReturn(this.request);
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(this.scriptContext);

        // Set the wiki editor preference to a value other than wysiwyg.
        when(this.wiki.getEditorPreference(this.xwikiContext)).thenReturn("other");

        // Always set the document syntax to XWiki 2.1. We change the supported syntaxes rather than the document
        // syntax.
        when(this.document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        // The current document is open in edit mode.
        when(this.xwikiContext.get("tdoc")).thenReturn(this.document);
        when(this.xwikiContext.getAction()).thenReturn(EDIT);
        when(this.xwikiContext.getWiki()).thenReturn(this.wiki);
        when(this.xwikiContextProvider.get()).thenReturn(this.xwikiContext);

        this.contextComponentManager = mock(ComponentManager.class);
        when(this.componentManagerProvider.get()).thenReturn(this.contextComponentManager);
    }

    private void wysiwygSyntaxSupported(boolean supported)
    {
        when(this.contextComponentManager.hasComponent(Parser.class, XWIKI_2_1)).thenReturn(supported);
        when(this.contextComponentManager.hasComponent(PrintRendererFactory.class, XWIKI_2_1)).thenReturn(supported);
    }

    private void wysiwygEditor(String editorId)
    {
        Editor<SyntaxContent> editor = mock(Editor.class);
        EditorDescriptor descriptor = mock(EditorDescriptor.class);
        when(editor.getDescriptor()).thenReturn(descriptor);
        when(descriptor.getId()).thenReturn(editorId);
        when(this.editorManager.<SyntaxContent>getDefaultEditor(SyntaxContent.class, EditModeResolver.WYSIWYG))
            .thenReturn(editor);
    }

    @Test
    void getEditModeFromQueryString()
    {
        when(this.request.getParameter(EDITOR)).thenReturn("Inplace");
        assertEquals("inplace", this.editModeResolver.getEditMode());
    }

    @Test
    void getEditModeFromScriptContext()
    {
        when(this.scriptContext.getAttribute(EDITOR)).thenReturn("Inplace");
        assertEquals("inplace", this.editModeResolver.getEditMode());
    }

    @Test
    void getEditModeDefaultInline()
    {
        when(this.sheetManager.getSheets(this.document, EDIT))
            .thenReturn(List.of(new DocumentReference("xwiki", "XWiki", "sheet")));
        assertEquals(EditModeResolver.INLINE, this.editModeResolver.getEditMode());
    }

    @Test
    void getEditModeDefaultWysiwygSupported()
    {
        wysiwygSyntaxSupported(true);
        when(this.wiki.getEditorPreference(this.xwikiContext)).thenReturn(EditModeResolver.WYSIWYG);
        assertEquals(EditModeResolver.WYSIWYG, this.editModeResolver.getEditMode());
    }

    @Test
    void getEditModeDefaultWysiwygNotSupported()
    {
        when(this.wiki.getEditorPreference(this.xwikiContext)).thenReturn(EditModeResolver.WYSIWYG);
        assertEquals(EditModeResolver.WIKI, this.editModeResolver.getEditMode());
    }

    @Test
    void getEditModeDefaultOther()
    {
        assertEquals(EditModeResolver.WIKI, this.editModeResolver.getEditMode());
    }

    @Test
    void isWysiwygSyntaxSupported()
    {
        wysiwygSyntaxSupported(true);
        assertTrue(this.editModeResolver.isSyntaxWYSIWYGEditable(XWIKI_2_1));

        // The XHTML renderer doesn't produce valid XHTML so the WYSIWYG editor can't be used with this syntax.
        String xhtml = Syntax.XHTML_1_0.toIdString();
        when(this.contextComponentManager.hasComponent(Parser.class, xhtml)).thenReturn(true);
        when(this.contextComponentManager.hasComponent(PrintRendererFactory.class, xhtml)).thenReturn(true);
        assertFalse(this.editModeResolver.isSyntaxWYSIWYGEditable(xhtml));

        // A syntax that can be parsed but not rendered can't be edited with a WYSIWYG editor either.
        when(this.contextComponentManager.hasComponent(PrintRendererFactory.class, XWIKI_2_1)).thenReturn(false);
        assertFalse(this.editModeResolver.isSyntaxWYSIWYGEditable(XWIKI_2_1));
    }
}
