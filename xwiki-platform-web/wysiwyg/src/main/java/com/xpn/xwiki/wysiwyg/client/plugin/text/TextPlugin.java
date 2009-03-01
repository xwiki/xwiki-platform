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
package com.xpn.xwiki.wysiwyg.client.plugin.text;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.ShortcutKey;
import com.xpn.xwiki.wysiwyg.client.util.ShortcutKeyFactory;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Plug-in for making text bold, italic, underline or strike through. It installs four toggle buttons on the tool bar
 * and updates their status depending on the current cursor position and the direction of the navigation using the arrow
 * keys. For instance, if you navigate from a bold region to an italic one and you type a character it will be bold.
 * <p>
 * <b>Known issues:</b> When you navigate backwards, from right to left, using the arrow keys, the status of the toggle
 * buttons is not synchronized with the text area. The text area behaves properly though.
 * 
 * @version $Id$
 */
public class TextPlugin extends AbstractStatefulPlugin implements ClickListener
{
    /**
     * The tool bar button that toggles the bold style.
     */
    private ToggleButton bold;

    /**
     * The shortcut key that toggles the bold style.
     */
    private ShortcutKey boldKey;

    /**
     * The tool bar button that toggles the italic style.
     */
    private ToggleButton italic;

    /**
     * The shortcut key that toggles the italic style.
     */
    private ShortcutKey italicKey;

    /**
     * The tool bar button that toggles the underline style.
     */
    private ToggleButton underline;

    /**
     * The shortcut key that toggles the underline style.
     */
    private ShortcutKey underlineKey;

    /**
     * The tool bar button that toggles the strike through style.
     */
    private ToggleButton strikeThrough;

    /**
     * The tool bar button that toggles the teletype style.
     */
    private ToggleButton teletype;

    /**
     * User interface extension for the editor tool bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        if (getTextArea().getCommandManager().isSupported(Command.BOLD)) {
            bold = new ToggleButton(Images.INSTANCE.bold().createImage(), this);
            bold.setTitle(Strings.INSTANCE.bold());
            boldKey = ShortcutKeyFactory.createCtrlShortcutKey('B');
            getTextArea().addShortcutKey(boldKey);
            toolBarExtension.addFeature("bold", bold);
        }

        if (getTextArea().getCommandManager().isSupported(Command.ITALIC)) {
            italic = new ToggleButton(Images.INSTANCE.italic().createImage(), this);
            italic.setTitle(Strings.INSTANCE.italic());
            italicKey = ShortcutKeyFactory.createCtrlShortcutKey('I');
            getTextArea().addShortcutKey(italicKey);
            toolBarExtension.addFeature("italic", italic);
        }

        if (getTextArea().getCommandManager().isSupported(Command.UNDERLINE)) {
            underline = new ToggleButton(Images.INSTANCE.underline().createImage(), this);
            underline.setTitle(Strings.INSTANCE.underline());
            underlineKey = ShortcutKeyFactory.createCtrlShortcutKey('U');
            getTextArea().addShortcutKey(underlineKey);
            toolBarExtension.addFeature("underline", underline);
        }

        if (getTextArea().getCommandManager().isSupported(Command.STRIKE_THROUGH)) {
            strikeThrough = new ToggleButton(Images.INSTANCE.strikeThrough().createImage(), this);
            strikeThrough.setTitle(Strings.INSTANCE.strikeThrough());
            toolBarExtension.addFeature("strikethrough", strikeThrough);
        }

        if (getTextArea().getCommandManager().isSupported(Command.TELETYPE)) {
            teletype = new ToggleButton(Images.INSTANCE.teletype().createImage(), this);
            teletype.setTitle(Strings.INSTANCE.teletype());
            toolBarExtension.addFeature("teletype", teletype);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addKeyboardListener(this);
            getTextArea().addMouseListener(this);
            getTextArea().getCommandManager().addCommandListener(this);
            getUIExtensionList().add(toolBarExtension);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#destroy()
     */
    public void destroy()
    {
        if (bold != null) {
            bold.removeFromParent();
            bold.removeClickListener(this);
            bold = null;
            getTextArea().removeShortcutKey(boldKey);
        }

        if (italic != null) {
            italic.removeFromParent();
            italic.removeClickListener(this);
            italic = null;
            getTextArea().removeShortcutKey(italicKey);
        }

        if (underline != null) {
            underline.removeFromParent();
            underline.removeClickListener(this);
            underline = null;
            getTextArea().removeShortcutKey(underlineKey);
        }

        if (strikeThrough != null) {
            strikeThrough.removeFromParent();
            strikeThrough.removeClickListener(this);
            strikeThrough = null;
        }

        if (teletype != null) {
            teletype.removeFromParent();
            teletype.removeClickListener(this);
            teletype = null;
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().removeMouseListener(this);
            getTextArea().removeKeyboardListener(this);
            getTextArea().getCommandManager().removeCommandListener(this);
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
        if (sender == bold) {
            onBold();
        } else if (sender == italic) {
            onItalic();
        } else if (sender == underline) {
            onUnderline();
        } else if (sender == strikeThrough) {
            onStrikeThrough();
        } else if (sender == teletype) {
            onTeletype();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyUp(Widget, char, int)
     */
    public void onKeyUp(Widget sender, char keyCode, int modifiers)
    {
        if (sender == getTextArea()) {
            if ((modifiers & KeyboardListener.MODIFIER_CTRL) != 0) {
                if (keyCode == boldKey.getKeyCode()) {
                    onBold();
                } else if (keyCode == italicKey.getKeyCode()) {
                    onItalic();
                } else if (keyCode == underlineKey.getKeyCode()) {
                    onUnderline();
                } else {
                    super.onKeyUp(sender, keyCode, modifiers);
                }
            } else {
                super.onKeyUp(sender, keyCode, modifiers);
            }
        }
    }

    /**
     * Toggles bold style.
     */
    public void onBold()
    {
        if (bold.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.BOLD);
        }
    }

    /**
     * Toggles italic style.
     */
    public void onItalic()
    {
        if (italic.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.ITALIC);
        }
    }

    /**
     * Toggles underline style.
     */
    public void onUnderline()
    {
        if (underline.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.UNDERLINE);
        }
    }

    /**
     * Toggles strike through style.
     */
    public void onStrikeThrough()
    {
        if (strikeThrough.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.STRIKE_THROUGH);
        }
    }

    /**
     * Toggles teletype style.
     */
    public void onTeletype()
    {
        if (teletype.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.TELETYPE);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#update()
     */
    public void update()
    {
        if (bold != null) {
            bold.setDown(getTextArea().getCommandManager().isExecuted(Command.BOLD));
        }
        if (italic != null) {
            italic.setDown(getTextArea().getCommandManager().isExecuted(Command.ITALIC));
        }
        if (underline != null) {
            underline.setDown(getTextArea().getCommandManager().isExecuted(Command.UNDERLINE));
        }
        if (strikeThrough != null) {
            strikeThrough.setDown(getTextArea().getCommandManager().isExecuted(Command.STRIKE_THROUGH));
        }
        if (teletype != null) {
            teletype.setDown(getTextArea().getCommandManager().isExecuted(Command.TELETYPE));
        }
    }
}
