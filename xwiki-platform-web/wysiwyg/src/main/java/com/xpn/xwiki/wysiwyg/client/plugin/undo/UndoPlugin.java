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
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.ClickCommand;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.ShortcutKey;
import com.xpn.xwiki.wysiwyg.client.util.ShortcutKeyManager;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Plug-in for undoing and redoing the past actions taken on the rich text area. It installs two push buttons on the
 * tool bar and updates their status depending on the current cursor position.
 * 
 * @version $Id$
 */
public class UndoPlugin extends AbstractPlugin implements ClickListener
{
    /**
     * The tool bar button used for undoing the last action taken on the rich text area.
     */
    private PushButton undo;

    /**
     * The tool bar button used for redoing the last action taken on the rich text area.
     */
    private PushButton redo;

    /**
     * Associates commands to shortcut keys.
     */
    private ShortcutKeyManager shortcutKeyManager;

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

        shortcutKeyManager = new ShortcutKeyManager();
        if (getTextArea().getCommandManager().isSupported(Command.UNDO)) {
            undo = new PushButton(Images.INSTANCE.undo().createImage(), this);
            undo.setTitle(Strings.INSTANCE.undo());
            ClickCommand undoCommand = new ClickCommand(this, undo);
            shortcutKeyManager.put(new ShortcutKey('Z', KeyboardListener.MODIFIER_CTRL), undoCommand);
            shortcutKeyManager.put(new ShortcutKey('Z', KeyboardListener.MODIFIER_META), undoCommand);
            toolBarExtension.addFeature("undo", undo);
        }

        if (getTextArea().getCommandManager().isSupported(Command.REDO)) {
            redo = new PushButton(Images.INSTANCE.redo().createImage(), this);
            redo.setTitle(Strings.INSTANCE.redo());
            ClickCommand redoCommand = new ClickCommand(this, redo);
            shortcutKeyManager.put(new ShortcutKey('Y', KeyboardListener.MODIFIER_CTRL), redoCommand);
            shortcutKeyManager.put(new ShortcutKey('Y', KeyboardListener.MODIFIER_META), redoCommand);
            toolBarExtension.addFeature("redo", redo);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addKeyboardListener(shortcutKeyManager);
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
        destroy(undo);
        destroy(redo);

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().removeKeyboardListener(shortcutKeyManager);
            shortcutKeyManager.clear();
            toolBarExtension.clearFeatures();
        }

        super.destroy();
    }

    /**
     * Releases the given focus widget.
     * 
     * @param widget the widget to be destroyed
     */
    private void destroy(FocusWidget widget)
    {
        if (widget != null) {
            widget.removeFromParent();
            widget.removeClickListener(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == undo) {
            onClick(undo, Command.UNDO);
        } else if (sender == redo) {
            onClick(redo, Command.REDO);
        }
    }

    /**
     * Toggles the specifies command if the given focus widget is enabled.
     * 
     * @param sender the widget who sent the click event
     * @param command the command to be toggled
     */
    private void onClick(FocusWidget sender, Command command)
    {
        if (sender.isEnabled()) {
            getTextArea().getCommandManager().execute(command);
        }
    }
}
