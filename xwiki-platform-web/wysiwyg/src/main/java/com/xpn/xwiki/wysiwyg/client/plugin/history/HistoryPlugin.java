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
package com.xpn.xwiki.wysiwyg.client.plugin.history;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.history.exec.RedoExecutable;
import com.xpn.xwiki.wysiwyg.client.plugin.history.exec.UndoExecutable;
import com.xpn.xwiki.wysiwyg.client.plugin.history.internal.DefaultHistory;
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
public class HistoryPlugin extends AbstractPlugin implements ClickListener
{
    /**
     * The association between tool bar buttons and the commands that are executed when these buttons are clicked.
     */
    private final Map<PushButton, Command> buttons = new HashMap<PushButton, Command>();

    /**
     * Associates commands to shortcut keys.
     */
    private final ShortcutKeyManager shortcutKeyManager = new ShortcutKeyManager();

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

        // Register custom executables.
        History history = new DefaultHistory(textArea, 10);
        getTextArea().getCommandManager().registerCommand(Command.REDO, new RedoExecutable(history));
        getTextArea().getCommandManager().registerCommand(Command.UNDO, new UndoExecutable(history));

        addFeature("undo", Command.UNDO, Images.INSTANCE.undo().createImage(), Strings.INSTANCE.undo(), 'Z');
        addFeature("redo", Command.REDO, Images.INSTANCE.redo().createImage(), Strings.INSTANCE.redo(), 'Y');

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addKeyboardListener(shortcutKeyManager);
            getUIExtensionList().add(toolBarExtension);
        }
    }

    /**
     * Creates a tool bar feature and adds it to the tool bar.
     * 
     * @param name the feature name
     * @param command the rich text area command that is executed by this feature
     * @param image the image displayed on the tool bar
     * @param title the tool tip used on the tool bar button
     * @param keyCode the shortcut key to be used
     */
    private void addFeature(String name, Command command, Image image, String title, char keyCode)
    {
        if (getTextArea().getCommandManager().isSupported(command)) {
            PushButton button = new PushButton(image, this);
            button.setTitle(title);
            toolBarExtension.addFeature(name, button);
            buttons.put(button, command);

            ClickCommand clickCommand = new ClickCommand(this, button);
            shortcutKeyManager.put(new ShortcutKey(keyCode, KeyboardListener.MODIFIER_CTRL), clickCommand);
            shortcutKeyManager.put(new ShortcutKey(keyCode, KeyboardListener.MODIFIER_META), clickCommand);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        for (PushButton button : buttons.keySet()) {
            button.removeFromParent();
            button.removeClickListener(this);
        }
        buttons.clear();

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().removeKeyboardListener(shortcutKeyManager);
            shortcutKeyManager.clear();
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
        Command command = buttons.get(sender);
        if (command != null && ((FocusWidget) sender).isEnabled()) {
            getTextArea().getCommandManager().execute(command);
        }
    }
}
