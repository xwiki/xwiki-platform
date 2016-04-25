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
package org.xwiki.gwt.wysiwyg.client.plugin.text;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.ClickCommand;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ShortcutKey;
import org.xwiki.gwt.user.client.ShortcutKeyCommand;
import org.xwiki.gwt.user.client.ShortcutKeyManager;
import org.xwiki.gwt.user.client.ShortcutKey.ModifierKey;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.ToggleInlineStyleExecutable;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.text.exec.BoldExecutable;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ToggleButton;

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
public class TextPlugin extends AbstractStatefulPlugin implements ClickHandler
{
    /**
     * The association between tool bar buttons and the commands that are executed when these buttons are clicked.
     */
    private final Map<ToggleButton, Command> buttons = new HashMap<ToggleButton, Command>();

    /**
     * User interface extension for the editor tool bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * Associates commands to shortcut keys.
     */
    private final ShortcutKeyManager shortcutKeyManager = new ShortcutKeyManager();

    @Override
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // Register custom executables.
        getTextArea().getCommandManager().registerCommand(Command.BOLD, new BoldExecutable(textArea));
        getTextArea().getCommandManager().registerCommand(Command.ITALIC,
            new ToggleInlineStyleExecutable(textArea, Style.FONT_STYLE, Style.FontStyle.ITALIC, "em"));
        getTextArea().getCommandManager().registerCommand(Command.UNDERLINE,
            new ToggleInlineStyleExecutable(textArea, Style.TEXT_DECORATION, Style.TextDecoration.UNDERLINE, "ins"));
        getTextArea().getCommandManager().registerCommand(Command.STRIKE_THROUGH,
            new ToggleInlineStyleExecutable(textArea, Style.TEXT_DECORATION, Style.TextDecoration.LINE_THROUGH, "del"));
        getTextArea().getCommandManager().registerCommand(Command.TELETYPE,
            new ToggleInlineStyleExecutable(textArea, Style.FONT_FAMILY, "monospace", "tt"));

        addFeature("bold", Command.BOLD, Images.INSTANCE.bold(), Strings.INSTANCE.bold(), 'B');
        addFeature("italic", Command.ITALIC, Images.INSTANCE.italic(), Strings.INSTANCE.italic(), 'I');
        addFeature("underline", Command.UNDERLINE, Images.INSTANCE.underline(), Strings.INSTANCE.underline(), 'U');
        addFeature("strikethrough", Command.STRIKE_THROUGH, Images.INSTANCE.strikeThrough(), Strings.INSTANCE
            .strikeThrough());
        addFeature("teletype", Command.TELETYPE, Images.INSTANCE.teletype(), Strings.INSTANCE.teletype());

        if (toolBarExtension.getFeatures().length > 0) {
            registerTextAreaHandlers();
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
     * @return the tool bar button that exposes this feature
     */
    private ToggleButton addFeature(String name, Command command, ImageResource imageResource, String title)
    {
        ToggleButton button = null;
        if (getTextArea().getCommandManager().isSupported(command)) {
            button = new ToggleButton(new Image(imageResource));
            saveRegistration(button.addClickHandler(this));
            button.setTitle(title);
            toolBarExtension.addFeature(name, button);
            buttons.put(button, command);
        }
        return button;
    }

    /**
     * Creates a tool bar feature and assigns a shortcut key.
     * 
     * @param name the feature name
     * @param command the rich text area command that is executed by this feature
     * @param imageResource the image displayed on the tool bar
     * @param title the tool tip used on the tool bar button
     * @param keyCode the shortcut key to be used
     * @return the tool bar button that exposes this feature
     */
    private ToggleButton addFeature(String name, Command command, ImageResource imageResource, String title,
        char keyCode)
    {
        ToggleButton button = addFeature(name, command, imageResource, title);
        if (button != null) {
            ShortcutKeyCommand shortcutKeyCommand = new ShortcutKeyCommand(new ClickCommand(button));
            shortcutKeyManager.put(new ShortcutKey(keyCode, EnumSet.of(ModifierKey.CTRL)), shortcutKeyCommand);
            shortcutKeyManager.put(new ShortcutKey(keyCode, EnumSet.of(ModifierKey.META)), shortcutKeyCommand);
        }
        return button;
    }

    @Override
    public void destroy()
    {
        for (ToggleButton button : buttons.keySet()) {
            button.removeFromParent();
        }
        buttons.clear();

        if (toolBarExtension.getFeatures().length > 0) {
            shortcutKeyManager.clear();
            toolBarExtension.clearFeatures();
        }

        super.destroy();
    }

    @Override
    public void onClick(ClickEvent event)
    {
        Command command = buttons.get(event.getSource());
        // We have to test if the text area is attached because this method can be called after the event was consumed.
        if (command != null && getTextArea().isAttached() && ((FocusWidget) event.getSource()).isEnabled()) {
            getTextArea().setFocus(true);
            getTextArea().getCommandManager().execute(command);
        }
    }

    @Override
    public void update()
    {
        for (Map.Entry<ToggleButton, Command> entry : buttons.entrySet()) {
            if (entry.getKey().isEnabled()) {
                entry.getKey().setDown(getTextArea().getCommandManager().isExecuted(entry.getValue()));
            }
        }
    }
}
