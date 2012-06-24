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
package org.xwiki.gwt.wysiwyg.client.plugin.history;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.ClickCommand;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ShortcutKey;
import org.xwiki.gwt.user.client.ShortcutKeyCommand;
import org.xwiki.gwt.user.client.ShortcutKeyManager;
import org.xwiki.gwt.user.client.ShortcutKey.ModifierKey;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.history.exec.RedoExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.history.exec.UndoExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.history.internal.DefaultHistory;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;

/**
 * Plug-in for undoing and redoing the past actions taken on the rich text area. It installs two push buttons on the
 * tool bar and updates their status depending on the current cursor position.
 * 
 * @version $Id$
 */
public class HistoryPlugin extends AbstractPlugin implements ClickHandler
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
     * @see AbstractPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // Register custom executables.
        int historySize = Integer.parseInt(config.getParameter("historySize", "10"));
        History history = new DefaultHistory(textArea, Math.max(historySize, 1));
        getTextArea().getCommandManager().registerCommand(Command.REDO, new RedoExecutable(history));
        getTextArea().getCommandManager().registerCommand(Command.UNDO, new UndoExecutable(history));

        addFeature("undo", Command.UNDO, Images.INSTANCE.undo(), Strings.INSTANCE.undo(), 'Z');
        addFeature("redo", Command.REDO, Images.INSTANCE.redo(), Strings.INSTANCE.redo(), 'Y');

        if (toolBarExtension.getFeatures().length > 0) {
            saveRegistrations(shortcutKeyManager.addHandlers(getTextArea()));
            getUIExtensionList().add(toolBarExtension);
        }
    }

    /**
     * Creates a tool bar feature and adds it to the tool bar.
     * 
     * @param name the feature name
     * @param command the rich text area command that is executed by this feature
     * @param imageResource the image displayed on the tool bar
     * @param title the tool tip used on the tool bar button
     * @param keyCode the shortcut key to be used
     */
    private void addFeature(String name, Command command, ImageResource imageResource, String title, char keyCode)
    {
        if (getTextArea().getCommandManager().isSupported(command)) {
            PushButton button = new PushButton(new Image(imageResource));
            saveRegistration(button.addClickHandler(this));
            button.setTitle(title);
            toolBarExtension.addFeature(name, button);
            buttons.put(button, command);

            ShortcutKeyCommand shortcutKeyCommand = new ShortcutKeyCommand(new ClickCommand(button), true);
            shortcutKeyManager.put(new ShortcutKey(keyCode, EnumSet.of(ModifierKey.CTRL)), shortcutKeyCommand);
            shortcutKeyManager.put(new ShortcutKey(keyCode, EnumSet.of(ModifierKey.META)), shortcutKeyCommand);
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
        }
        buttons.clear();

        if (toolBarExtension.getFeatures().length > 0) {
            shortcutKeyManager.clear();
            toolBarExtension.clearFeatures();
        }

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickHandler#onClick(ClickEvent)
     */
    public void onClick(ClickEvent event)
    {
        Command command = buttons.get(event.getSource());
        // We have to test if the text area is attached because this method can be called after the event was consumed.
        if (command != null && getTextArea().isAttached() && ((FocusWidget) event.getSource()).isEnabled()) {
            getTextArea().setFocus(true);
            getTextArea().getCommandManager().execute(command);
        }
    }
}
