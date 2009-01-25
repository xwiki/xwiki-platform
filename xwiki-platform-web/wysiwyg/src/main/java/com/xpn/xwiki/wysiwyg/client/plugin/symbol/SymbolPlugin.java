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
package com.xpn.xwiki.wysiwyg.client.plugin.symbol;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.PopupListener;
import com.xpn.xwiki.wysiwyg.client.widget.SourcesPopupEvents;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.SelectionPreserver;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Allows the user to insert a special symbol chosen with a symbol picker in place of the current selection.
 * 
 * @version $Id$
 */
public class SymbolPlugin extends AbstractPlugin implements ClickListener, PopupListener
{
    /**
     * The insert button to be placed on the tool bar.
     */
    private PushButton insert;

    /**
     * The symbol picker used for choosing the symbol to insert.
     */
    private SymbolPicker picker;

    /**
     * Used to preserve the selection of the rich text area while the dialog is opened. Some browsers like Internet
     * Explorer loose the selection if you click inside a dialog box.
     */
    private SelectionPreserver selectionPreserver;

    /**
     * Tool bar extension.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.INSERT_HTML)) {
            insert = new PushButton(Images.INSTANCE.charmap().createImage(), this);
            insert.setTitle(Strings.INSTANCE.charmap());

            toolBarExtension.addFeature("symbol", insert);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            selectionPreserver = new SelectionPreserver(getTextArea());
            getUIExtensionList().add(toolBarExtension);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        if (insert != null) {
            insert.removeFromParent();
            insert.removeClickListener(this);
            insert = null;

            if (picker != null) {
                picker.hide();
                picker.removeFromParent();
                picker.removePopupListener(this);
                picker = null;
            }
        }

        if (toolBarExtension.getFeatures().length > 0) {
            toolBarExtension.clearFeatures();
        }

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == insert) {
            onSymbols(true);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupListener#onPopupClosed(SourcesPopupEvents, boolean)
     */
    public void onPopupClosed(SourcesPopupEvents sender, boolean autoHide)
    {
        if (sender == getSymbolPicker() && !autoHide) {
            onSymbols(false);
        }
    }

    /**
     * Either shows the symbol picker dialog or inserts the chosen symbol depending in the given flag.
     * 
     * @param show whether to show the symbol picker or insert the chosen symbol.
     */
    public void onSymbols(boolean show)
    {
        if (show) {
            if (insert.isEnabled()) {
                // We save the selection because in some browsers, including Internet Explorer, by clicking on the
                // dialog we loose the selection in the target document.
                selectionPreserver.saveSelection();
                getSymbolPicker().center();
            }
        } else {
            // We restore the selection in the target document before executing the command.
            selectionPreserver.restoreSelection();
            String character = getSymbolPicker().getSymbol();
            if (character != null && getTextArea().getCommandManager().execute(Command.INSERT_HTML, character)) {
                getTextArea().getDocument().getSelection().collapseToEnd();
            } else {
                // We get here if the symbol picker has been closed by clicking the close button.
                // In this case we return the focus to the text area.
                getTextArea().setFocus(true);
            }
        }
    }

    /**
     * We use lazy loading in case of the symbol picker to optimize editor loading time because the symbol palette is an
     * HTML table with many cells and takes a bit of time to be created. In the future we should consider using
     * innerHTML for creating the palette and widget binding.
     * 
     * @return the symbol picker to be used for selecting the symbol.
     */
    private SymbolPicker getSymbolPicker()
    {
        if (picker == null) {
            picker = new SymbolPicker();
            picker.addPopupListener(this);
        }
        return picker;
    }
}
