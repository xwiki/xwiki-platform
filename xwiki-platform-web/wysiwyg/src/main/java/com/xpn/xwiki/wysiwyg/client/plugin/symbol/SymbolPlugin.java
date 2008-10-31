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
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

public class SymbolPlugin extends AbstractPlugin implements ClickListener, PopupListener
{
    private PushButton symbolButton;

    private SymbolPicker symbolPicker;

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
            symbolButton = new PushButton(Images.INSTANCE.charmap().createImage(), this);
            symbolButton.setTitle(Strings.INSTANCE.charmap());

            toolBarExtension.addFeature("symbol", symbolButton);
        }

        if (toolBarExtension.getFeatures().length > 0) {
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
        if (symbolButton != null) {
            symbolButton.removeFromParent();
            symbolButton.removeClickListener(this);
            symbolButton = null;

            if (symbolPicker != null) {
                symbolPicker.hide();
                symbolPicker.removeFromParent();
                symbolPicker.removePopupListener(this);
                symbolPicker = null;
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
        if (sender == symbolButton) {
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

    public void onSymbols(boolean show)
    {
        if (show) {
            getSymbolPicker().center();
        } else {
            String character = getSymbolPicker().getSymbol();
            if (character != null) {
                getTextArea().getCommandManager().execute(Command.INSERT_HTML, character);
            } else {
                // We get here if the symbol picker has been closed by clicking the close button.
                // In this case we return the focus to the text area.
                getTextArea().setFocus(true);
            }
        }
    }

    private SymbolPicker getSymbolPicker()
    {
        if (symbolPicker == null) {
            symbolPicker = new SymbolPicker();
            symbolPicker.addPopupListener(this);
        }
        return symbolPicker;
    }
}
