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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.sheet.SheetManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Determines the edit mode that is going to be used to edit the current document, based on the request, the XWiki
 * context and the script context.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@Component(roles = EditModeResolver.class)
@Singleton
public class EditModeResolver
{
    /**
     * The edit mode that uses a WYSIWYG editor to edit the document content in a standalone page.
     */
    public static final String WYSIWYG = "wysiwyg";

    /**
     * The edit mode that uses a WYSIWYG editor to edit the document content in place, from the view page.
     */
    public static final String INPLACE = "inplace";

    /**
     * The edit mode that edits the document content as wiki syntax.
     */
    public static final String WIKI = "wiki";

    /**
     * The edit mode that displays a form with the document fields, based on a sheet.
     */
    public static final String INLINE = "inline";

    private static final String EDITOR_KEY = "editor";

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private SheetManager sheetManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private Container container;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    /**
     * @return the edit mode that is going to be used to edit the current document, in lower case; usually one of
     *         {@link #WYSIWYG}, {@link #INPLACE}, {@link #WIKI} or {@link #INLINE}, but it can be any value when the
     *         edit mode is specified explicitly, e.g. through the {@code editor} request parameter (the object editor
     *         is reached with {@code editor=object})
     */
    public String getEditMode()
    {
        // Check if the edit mode is specified as a request parameter.
        String requestEditor = (String) this.container.getRequest().getParameter(EDITOR_KEY);
        if (!StringUtils.isEmpty(requestEditor)) {
            return requestEditor.toLowerCase();
        }

        // The Inplace edit mode comes with a custom InplaceEditing sheet that handles the locking confirmation.
        // To handle this special case, and potential future others, we add the possibility to set the selected
        // editor through the editor variable in the ScriptContext.
        String scontextEditor = (String) this.scriptContextManager.getCurrentScriptContext().getAttribute(EDITOR_KEY);
        if (!StringUtils.isEmpty(scontextEditor)) {
            return scontextEditor.toLowerCase();
        }

        // Otherwise, we fallback to the default editor. This part is taken from the getDefaultDocumentEditor macro
        // defined in macros.vm from xwiki-platform-web-templates.

        XWikiContext context = this.xwikiContextProvider.get();
        // We access the document to get its syntax and to determine if it has a sheet. Technically, each document
        // translation has its own syntax field, but it doesn't make sense for a translation to have a different syntax
        // than the default translation. The sheet is determined using the objects attached to the document. Even though
        // all document translations share the objects, they can be accessed only from the default translation. For this
        // reasons we retrieve only the default document translation.
        XWikiDocument document = (XWikiDocument) context.get("doc");
        // There is no document to edit when the edit mode is resolved outside of a request that targets a document,
        // in which case we can neither look for sheets nor check the document syntax.
        if (document != null) {
            // If a sheet matches the edit action for this document and no specific editor was specified,
            // the Inline Form edit mode will be used.
            if (!this.sheetManager.getSheets(document, context.getAction()).isEmpty()) {
                return INLINE;
            }

            // If the default editor is set to WYSIWYG, it will be used if possible.
            String xwikiEditorPreference = context.getWiki().getEditorPreference(context);
            if (WYSIWYG.equals(xwikiEditorPreference)
                && isSyntaxWYSIWYGEditable(document.getSyntax().toIdString())) {
                return xwikiEditorPreference;
            }
        }

        return WIKI;
    }

    /**
     * Check if content using the specified syntax can be edited with a WYSIWYG editor. This is generally true for
     * syntaxes that provide a parser and a renderer:
     * <ul>
     * <li>the parser is needed to go from content syntax -> XDOM -> WYSIWYG editor syntax</li>
     * <li>the renderer is needed to go from WYSIWYG editor syntax -> XDOM -> content syntax</li>
     * </ul>
     * This method should be called before attempting to load a WYSIWYG editor.
     *
     * @param syntaxId the syntax identifier, like {@code xwiki/2.1}
     * @return {@code true} if content using the specified syntax can be edited with a WYSIWYG editor, {@code false}
     *         otherwise
     */
    public boolean isSyntaxWYSIWYGEditable(String syntaxId)
    {
        // Special handling for XHTML since the XHTML renderer doesn't produce valid XHTML. Thus if, for example, you
        // use the WYSIWYG editor and add 2 paragraphs, it'll generate {@code <p>a</p><p>b</p>} which is invalid XHTML
        // and the page will fail to render.
        if (Syntax.XHTML_1_0.toIdString().equals(syntaxId)) {
            return false;
        }

        ComponentManager componentManager = this.componentManagerProvider.get();
        return componentManager.hasComponent(Parser.class, syntaxId)
            && componentManager.hasComponent(PrintRendererFactory.class, syntaxId);
    }
}
