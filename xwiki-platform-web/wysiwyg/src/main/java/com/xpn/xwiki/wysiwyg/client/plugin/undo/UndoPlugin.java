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
package com.xpn.xwiki.wysiwyg.client.plugin.undo;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.ShortcutKey;
import com.xpn.xwiki.wysiwyg.client.util.ShortcutKeyFactory;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Plug-in for undoing and redoing the past actions taken on the rich text area. It installs two push buttons on the
 * tool bar and updates their status depending on the current cursor position.
 * 
 * @version $Id$
 */
public class UndoPlugin extends AbstractPlugin implements ClickListener, KeyboardListener
{
    /**
     * The tool bar button used for undoing the last action taken on the rich text area.
     */
    private PushButton undo;

    /**
     * The shortcut key that triggers the undo action.
     */
    private ShortcutKey undoKey;

    /**
     * The tool bar button used for redoing the last action taken on the rich text area.
     */
    private PushButton redo;

    /**
     * The shortcut key that triggers the redo action.
     */
    private ShortcutKey redoKey;

    /**
     * Tool bar extension that includes the undo and redo buttons.
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

        if (getTextArea().getCommandManager().isSupported(Command.UNDO)) {
            undo = new PushButton(Images.INSTANCE.undo().createImage(), this);
            undo.setTitle(Strings.INSTANCE.undo());
            undoKey = ShortcutKeyFactory.createCtrlShortcutKey('Z');
            textArea.addShortcutKey(undoKey);
            toolBarExtension.addFeature("undo", undo);
        }

        if (getTextArea().getCommandManager().isSupported(Command.REDO)) {
            redo = new PushButton(Images.INSTANCE.redo().createImage(), this);
            redo.setTitle(Strings.INSTANCE.redo());
            redoKey = ShortcutKeyFactory.createCtrlShortcutKey('Y');
            textArea.addShortcutKey(redoKey);
            toolBarExtension.addFeature("redo", redo);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addKeyboardListener(this);
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
        if (undo != null) {
            undo.removeFromParent();
            undo.removeClickListener(this);
            undo = null;
            getTextArea().removeShortcutKey(undoKey);
        }

        if (redo != null) {
            redo.removeFromParent();
            redo.removeClickListener(this);
            redo = null;
            getTextArea().removeShortcutKey(redoKey);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().removeKeyboardListener(this);
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
        if (sender == undo) {
            onUndo();
        } else if (sender == redo) {
            onRedo();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyDown(Widget, char, int)
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyUp(Widget, char, int)
     */
    public void onKeyUp(Widget sender, char keyCode, int modifiers)
    {
        if (sender == getTextArea() && (modifiers & KeyboardListener.MODIFIER_CTRL) != 0) {
            if (keyCode == undoKey.getKeyCode()) {
                onUndo();
            } else if (keyCode == redoKey.getKeyCode()) {
                onRedo();
            }
        }
    }

    /**
     * Loads the previous (older) history state of the rich text area.
     */
    public void onUndo()
    {
        if (undo.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.UNDO);
        }
    }

    /**
     * Loads the next (newer) history state of the rich text area.
     */
    public void onRedo()
    {
        if (redo.isEnabled()) {
            getTextArea().getCommandManager().execute(Command.REDO);
        }
    }
}
