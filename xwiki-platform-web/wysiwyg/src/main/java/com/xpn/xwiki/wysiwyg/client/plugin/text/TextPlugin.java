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
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.ClickCommand;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.ShortcutKey;
import com.xpn.xwiki.wysiwyg.client.util.ShortcutKeyManager;
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
     * The tool bar button that toggles the italic style.
     */
    private ToggleButton italic;

    /**
     * The tool bar button that toggles the underline style.
     */
    private ToggleButton underline;

    /**
     * The tool bar button that toggles the strike through style.
     */
    private ToggleButton strikeThrough;

    /**
     * The tool bar button that toggles the teletype style.
     */
    private ToggleButton teletype;

    /**
     * Associates commands to shortcut keys.
     */
    private ShortcutKeyManager shortcutKeyManager;

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

        shortcutKeyManager = new ShortcutKeyManager();

        bold = createFeature("bold", Command.BOLD, Images.INSTANCE.bold().createImage(), Strings.INSTANCE.bold(), 'B');
        italic =
            createFeature("italic", Command.ITALIC, Images.INSTANCE.italic().createImage(), Strings.INSTANCE.italic(),
                'I');
        underline =
            createFeature("underline", Command.UNDERLINE, Images.INSTANCE.underline().createImage(), Strings.INSTANCE
                .underline(), 'U');
        strikeThrough =
            createFeature("strikethrough", Command.STRIKE_THROUGH, Images.INSTANCE.strikeThrough().createImage(),
                Strings.INSTANCE.strikeThrough());
        teletype =
            createFeature("teletype", Command.TELETYPE, Images.INSTANCE.teletype().createImage(), Strings.INSTANCE
                .teletype());

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addKeyboardListener(this);
            getTextArea().addMouseListener(this);
            getTextArea().getCommandManager().addCommandListener(this);
            getTextArea().addKeyboardListener(shortcutKeyManager);
            getUIExtensionList().add(toolBarExtension);
        }
    }

    /**
     * Creates a tool bar feature.
     * 
     * @param name the feature name
     * @param command the rich text area command that is executed by this feature
     * @param image the image displayed on the tool bar
     * @param title the tool tip used on the tool bar button
     * @return the tool bar button that exposes this feature
     */
    private ToggleButton createFeature(String name, Command command, Image image, String title)
    {
        ToggleButton button = null;
        if (getTextArea().getCommandManager().isSupported(command)) {
            button = new ToggleButton(image, this);
            button.setTitle(title);
            toolBarExtension.addFeature(name, button);
        }
        return button;
    }

    /**
     * Creates a tool bar feature and assigns a shortcut key.
     * 
     * @param name the feature name
     * @param command the rich text area command that is executed by this feature
     * @param image the image displayed on the tool bar
     * @param title the tool tip used on the tool bar button
     * @param keyCode the shortcut key to be used
     * @return the tool bar button that exposes this feature
     */
    private ToggleButton createFeature(String name, Command command, Image image, String title, char keyCode)
    {
        ToggleButton button = createFeature(name, command, image, title);
        if (button != null) {
            ClickCommand clickCommand = new ClickCommand(this, button);
            shortcutKeyManager.put(new ShortcutKey(keyCode, KeyboardListener.MODIFIER_CTRL), clickCommand);
            shortcutKeyManager.put(new ShortcutKey(keyCode, KeyboardListener.MODIFIER_META), clickCommand);
        }
        return button;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#destroy()
     */
    public void destroy()
    {
        destroy(bold);
        destroy(italic);
        destroy(underline);
        destroy(strikeThrough);
        destroy(teletype);

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().removeMouseListener(this);
            getTextArea().removeKeyboardListener(this);
            getTextArea().getCommandManager().removeCommandListener(this);
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
        if (sender == bold) {
            onClick(bold, Command.BOLD);
        } else if (sender == italic) {
            onClick(italic, Command.ITALIC);
        } else if (sender == underline) {
            onClick(underline, Command.UNDERLINE);
        } else if (sender == strikeThrough) {
            onClick(strikeThrough, Command.STRIKE_THROUGH);
        } else if (sender == teletype) {
            onClick(teletype, Command.TELETYPE);
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

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#update()
     */
    public void update()
    {
        update(bold, Command.BOLD);
        update(italic, Command.ITALIC);
        update(underline, Command.UNDERLINE);
        update(strikeThrough, Command.STRIKE_THROUGH);
        update(teletype, Command.TELETYPE);
    }

    /**
     * Updates the given toggle button based on the state of the specified command.
     * 
     * @param button the button whose up/down state will be updated
     * @param command the command that determines the state of the button
     */
    private void update(ToggleButton button, Command command)
    {
        if (button != null) {
            button.setDown(getTextArea().getCommandManager().isExecuted(command));
        }
    }
}
