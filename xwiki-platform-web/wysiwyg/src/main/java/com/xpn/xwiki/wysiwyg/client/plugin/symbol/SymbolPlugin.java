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
import com.google.gwt.user.client.ui.PopupListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.Config;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.ui.Images;
import com.xpn.xwiki.wysiwyg.client.ui.Strings;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;

public class SymbolPlugin extends AbstractPlugin implements ClickListener, PopupListener
{
    private PushButton symbolsButton;

    private SymbolPicker symbolPicker;

    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, XRichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, XRichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.INSERT_HTML)) {
            symbolsButton = new PushButton(Images.INSTANCE.charmap().createImage(), this);
            symbolsButton.setTitle(Strings.INSTANCE.charmap());

            toolBarExtension.addFeature("symbol", symbolsButton);

            symbolPicker = new SymbolPicker();
            symbolPicker.addPopupListener(this);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addClickListener(this);
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
        if (symbolsButton != null) {
            symbolsButton.removeFromParent();
            symbolsButton.removeClickListener(this);
            symbolsButton = null;

            symbolPicker.hide();
            symbolPicker.removeFromParent();
            symbolPicker.removePopupListener(this);
            symbolPicker = null;
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().removeClickListener(this);
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
        if (sender == symbolsButton) {
            onSymbols(true);
        } else if (sender == getTextArea()) {
            symbolPicker.hide();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupListener#onPopupClosed(PopupPanel, boolean)
     */
    public void onPopupClosed(PopupPanel sender, boolean autoHide)
    {
        if (sender == symbolPicker && !autoHide) {
            onSymbols(false);
        }
    }

    public void onSymbols(boolean show)
    {
        if (show) {
            symbolPicker.center();
        } else {
            String character = symbolPicker.getSymbol();
            if (character != null) {
                getTextArea().getCommandManager().execute(Command.INSERT_HTML, character);
            }
        }
    }
}
