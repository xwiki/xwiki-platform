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

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.netflux.EntityChannel;
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
 * @since 15.10.11
 * @since 16.4.1
 * @since 16.5.0RC1
 */
@Component
@Singleton
public class DefaultRealtimeEditorManager implements RealtimeEditorManager
{
    private static final String EDITOR_KEY = "editor";

    private static final String WYSIWYG = "wysiwyg";

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
    public String getSelectedEditor()
    {
        // When there is an editor query parameter, it is the selected editor.
        String requestEditor = (String) this.container.getRequest().getProperty(EDITOR_KEY);
        if (!StringUtils.isEmpty(requestEditor)) {
            return requestEditor;
        }

        // The inplace editor comes with a custom InplaceEditing sheet that handles the locking confirmation.
        // To handle this special case, and potential future others, we add the possibility to set the selected
        // editor through the editor variable in the ScriptContext.
        String scontextEditor = (String) this.scriptContextManager.getCurrentScriptContext().getAttribute(EDITOR_KEY);
        if (!StringUtils.isEmpty(scontextEditor)) {
            return scontextEditor;
        }

        // Otherwise, we fallback to the default editor:
        // This part is taken from the getDefaultDocumentEditor macro
        // defined in macros.vm from xwiki-platform-web-templates.

        // If a sheet matches the edit action for this document and no specific editor was specified,
        // the inline form editor will be used.
        XWikiContext context = this.xwikiContextProvider.get();
        XWikiDocument document = (XWikiDocument) context.get("tdoc");
        if (!this.sheetManager.getSheets(document, context.getAction()).isEmpty()) {
            return "inline";
        }

        // If the default editor is set to Wysiwyg, it will be used if possible.
        String xwikiEditorPreference = context.getWiki().getEditorPreference(context);
        if (WYSIWYG.equals(xwikiEditorPreference) && ((WysiwygEditorScriptService) wysiwygEditorScriptService)
            .isSyntaxSupported(document.getSyntax().toIdString())) {
            return xwikiEditorPreference;
        }

        return "wiki";
    }

    @Override
    public boolean sessionIsActive(DocumentReference target, Locale locale, String editor)
    {
        // The inplace and WYSIWYG realtime editors both use the same "wysiwyg" channel.
        String session = editor;
        if (session.equals("inplace")) {
            session = WYSIWYG;
        }

        // We can't directly specify the path because a document might have multiple fields.
        // Instead, we check all the channels matching the given editor.
        List<EntityChannel> channels = this.entityChannelStore.getChannels(target);
        for (EntityChannel channel : channels) {
            List<String> path = channel.getPath();
            if (!path.isEmpty()) {
                String pathSession = path.get(path.size() - 1);
                String pathLocale = path.get(0);
                // When the channel is the one we are looking for, check that it has users.
                if (pathSession.equals(session) && pathLocale.equals(locale.toLanguageTag())
                    && channel.getUserCount() > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
