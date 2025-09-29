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

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.netflux.EntityChannelStore;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.sheet.SheetManager;
import org.xwiki.wysiwyg.script.WysiwygEditorScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Determines the currently selected editor based on XWiki and Script contexts. Determines wether a realtime session is
 * active for a given editor using the Netflux Channel Store.
 * 
 * @version $Id$
 * @since 16.10.6
 * @since 17.3.0RC1
 */
@Component
@Singleton
public class DefaultRealtimeSessionManager implements RealtimeSessionManager
{
    private static final String EDITOR_KEY = "editor";

    private static final String WYSIWYG = "wysiwyg";

    private static final String WIKI = "wiki";

    private static final String INLINE = "inline";

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private EntityChannelStore entityChannelStore;

    @Inject
    private SheetManager sheetManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    @Named(WYSIWYG)
    private ScriptService wysiwygEditorScriptService;

    @Inject
    private Container container;

    @Override
    public boolean canJoinSession(DocumentReference documentReference, Locale locale)
    {
        String editMode = getEditMode().toLowerCase();

        if (List.of(WYSIWYG, "inplace", WIKI).contains(editMode)) {
            // For the standalone WYSIWYG, inplace WYSIWYG and Wiki edit modes we check if there is an active realtime
            // editing session where the same editor is used to edit the document content.
            String contentEditor = WIKI.equals(editMode) ? WIKI : WYSIWYG;
            // We use Locale.toString() instead of Locale.toLanguageTag() in order to match the output of the Page REST
            // API (see ModelFactory#toRestPage()), which is used by the JavaScript code to determine the locale of the
            // edited document and create the associated Netflux channel.
            List<String> contentChannelPath =
                List.of("translations", locale.toString(), "fields", "content", "editors", contentEditor);
            return this.entityChannelStore.getChannel(documentReference, contentChannelPath)
                .map(contentChannel -> contentChannel.getUserCount() > 0).orElse(false);
        } else if (INLINE.equals(editMode)) {
            // The Inline Form edit mode doesn't support realtime editing yet. When this is implemented, we'll have to
            // return true here (always join) if there is an active session and force the preferred editor for the
            // edited fields to the editor already used in the realtime session (e.g. if a user starts a realtime
            // session and their preferred editor for the text area fields is WYSIWYG then the next user joining the
            // session shold be forced to use also the WYSIWYG editor for the text area fields).
        }

        return false;
    }

    String getEditMode()
    {
        // Check if the edit mode is specified as a request parameter.
        String requestEditor = (String) this.container.getRequest().getProperty(EDITOR_KEY);
        if (!StringUtils.isEmpty(requestEditor)) {
            return requestEditor;
        }

        // The Inplace edit mode comes with a custom InplaceEditing sheet that handles the locking confirmation.
        // To handle this special case, and potential future others, we add the possibility to set the selected
        // editor through the editor variable in the ScriptContext.
        String scontextEditor = (String) this.scriptContextManager.getCurrentScriptContext().getAttribute(EDITOR_KEY);
        if (!StringUtils.isEmpty(scontextEditor)) {
            return scontextEditor;
        }

        // Otherwise, we fallback to the default editor. This part is taken from the getDefaultDocumentEditor macro
        // defined in macros.vm from xwiki-platform-web-templates.

        // If a sheet matches the edit action for this document and no specific editor was specified,
        // the Inline Form edit mode will be used.
        XWikiContext context = this.xwikiContextProvider.get();
        XWikiDocument document = (XWikiDocument) context.get("tdoc");
        if (!this.sheetManager.getSheets(document, context.getAction()).isEmpty()) {
            return INLINE;
        }

        // If the default editor is set to WYSIWYG, it will be used if possible.
        String xwikiEditorPreference = context.getWiki().getEditorPreference(context);
        if (WYSIWYG.equals(xwikiEditorPreference) && ((WysiwygEditorScriptService) wysiwygEditorScriptService)
            .isSyntaxSupported(document.getSyntax().toIdString())) {
            return xwikiEditorPreference;
        }

        return WIKI;
    }
}
