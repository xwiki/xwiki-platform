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
package org.xwiki.gwt.wysiwyg.client.plugin.macro.exec;

import org.xwiki.gwt.dom.client.DocumentFragment;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.InsertBlockHTMLExecutable;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.InsertHTMLExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroCall;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroDescriptor;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroSelector;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.MacroServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Inserts a new macro in the edited document or replaces an existing one.
 * 
 * @version $Id$
 */
public class InsertExecutable extends InsertHTMLExecutable
{
    /**
     * Used to query the currently selected macros.
     */
    private final MacroSelector selector;

    /**
     * The service used to determine if a macro supports in-line mode.
     * <p>
     * Note that we can't determine if the macro output contains block-level elements before the macro is rendered,
     * which happens asynchronously on the server.
     */
    private final MacroServiceAsync macroService;

    /**
     * The executable used to insert stand-alone macros, i.e. macros that always generate block-level content which
     * can't be inserted in-line.
     */
    private final InsertBlockHTMLExecutable insertBlockHTMLExecutable;

    /**
     * The configuration object used to get the syntax of the edited content.
     */
    private final Config config;

    /**
     * Creates a new executable.
     * 
     * @param selector {@link #selector}
     * @param macroService {@link #macroService}
     * @param config {@link #config}
     */
    public InsertExecutable(MacroSelector selector, MacroServiceAsync macroService, Config config)
    {
        super(selector.getDisplayer().getTextArea());
        this.selector = selector;
        this.macroService = macroService;
        this.config = config;
        insertBlockHTMLExecutable = new InsertBlockHTMLExecutable(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#execute(String)
     */
    public boolean execute(String param)
    {
        // Prepare the macro markers.
        final DocumentFragment markers = rta.getDocument().createDocumentFragment();
        markers.appendChild(rta.getDocument().createComment(param));
        // Separate the comment nodes (the macro markers) with an empty element node to prevent them from being
        // normalized, i.e. merged into a single comment node. Firefox 10.0.2 normalizes the DOM tree for instance if we
        // insert a document fragment when the BODY element contains only a line break BR.
        Element separator = rta.getDocument().createSpanElement().cast();
        // We have to make sure the separator doesn't appear in rich text area's submitted HTML.
        separator.setAttribute(Element.META_DATA_ATTR, "");
        markers.appendChild(separator);
        markers.appendChild(rta.getDocument().createComment("stopmacro"));
        // Note: We refresh the rich text area after inserting the macro without going through the command manager
        // because we don't want to trigger the history mechanism.
        final Executable refresh = rta.getCommandManager().getExecutable(MacroPlugin.REFRESH);
        if (selector.getMacroCount() > 0) {
            // Edit selected macro.
            Element selectedMacro = selector.getMacro(0);
            selectedMacro.getParentNode().replaceChild(markers, selectedMacro);
            return refresh.execute(null);
        } else {
            // Insert a new macro.
            // Determine if the macro supports in-line mode. Even if the service call appears asynchronous it will
            // return immediately because the macro descriptor was cached before this code is executed (the insert macro
            // wizard requires the macro descriptor).
            final boolean[] success = new boolean[] {true};
            macroService.getMacroDescriptor(new MacroCall(param).getName(), config.getParameter("syntax"),
                new AsyncCallback<MacroDescriptor>()
                {
                    @Override
                    public void onFailure(Throwable caught)
                    {
                        // Fall back on in-line insert.
                        success[0] = InsertExecutable.super.execute(markers) && refresh.execute(null);
                    }

                    @Override
                    public void onSuccess(MacroDescriptor result)
                    {
                        success[0] =
                            (result.isSupportingInlineMode() ? InsertExecutable.super.execute(markers)
                                : insertBlockHTMLExecutable.execute(markers)) && refresh.execute(null);
                    }
                });
            return success[0];
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#getParameter()
     */
    public String getParameter()
    {
        if (selector.getMacroCount() > 0) {
            return selector.getDisplayer().getSerializedMacroCall(selector.getMacro(0));
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#isExecuted()
     */
    @Override
    public boolean isExecuted()
    {
        return selector.getMacroCount() > 0;
    }
}
