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
package com.xpn.xwiki.wysiwyg.client.plugin.format;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.format.exec.FormatBlockExecutable;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * {@link RichTextArea} plug-in for formatting text. It can be used to format text as heading 1 to 5. It installs a
 * select on the tool bar and updates its status depending on the current cursor position.
 * 
 * @version $Id$
 */
public class FormatPlugin extends AbstractStatefulPlugin implements ChangeListener, ClickListener
{
    /**
     * The list of formatting levels.
     */
    private ListBox levels;

    /**
     * The association between tool bar buttons and the commands that are executed when these buttons are clicked.
     */
    private final Map<PushButton, Command> buttons = new HashMap<PushButton, Command>();

    /**
     * Tool bar extension that includes the list of formatting levels.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefullPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        // Register custom executables.
        getTextArea().getCommandManager().registerCommand(Command.FORMAT_BLOCK, new FormatBlockExecutable());

        addFeature("removeformat", Command.REMOVE_FORMAT, Images.INSTANCE.removeFormat().createImage(),
            Strings.INSTANCE.removeFormat());

        if (getTextArea().getCommandManager().isSupported(Command.FORMAT_BLOCK)) {
            levels = new ListBox(false);
            levels.addChangeListener(this);
            levels.setVisibleItemCount(1);
            levels.setTitle(Strings.INSTANCE.format());

            levels.addItem(Strings.INSTANCE.formatPlainText(), "p");
            levels.addItem(Strings.INSTANCE.formatHeader1(), "h1");
            levels.addItem(Strings.INSTANCE.formatHeader2(), "h2");
            levels.addItem(Strings.INSTANCE.formatHeader3(), "h3");
            levels.addItem(Strings.INSTANCE.formatHeader4(), "h4");
            levels.addItem(Strings.INSTANCE.formatHeader5(), "h5");

            toolBarExtension.addFeature("format", levels);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            getTextArea().addMouseListener(this);
            getTextArea().addKeyboardListener(this);
            getTextArea().getCommandManager().addCommandListener(this);
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
     */
    private void addFeature(String name, Command command, Image image, String title)
    {
        if (getTextArea().getCommandManager().isSupported(command)) {
            PushButton button = new PushButton(image, this);
            button.setTitle(title);
            toolBarExtension.addFeature(name, button);
            buttons.put(button, command);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefullPlugin#destroy()
     */
    public void destroy()
    {
        for (PushButton button : buttons.keySet()) {
            button.removeFromParent();
            button.removeClickListener(this);
        }
        buttons.clear();

        if (levels != null) {
            levels.removeFromParent();
            levels.removeChangeListener(this);
            levels = null;

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
        Command command = buttons.get(sender);
        if (command != null && ((FocusWidget) sender).isEnabled()) {
            getTextArea().setFocus(true);
            getTextArea().getCommandManager().execute(command);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChangeListener#onChange(Widget)
     */
    public void onChange(Widget sender)
    {
        if (sender == levels && levels.isEnabled()) {
            String level = levels.getValue(levels.getSelectedIndex());
            getTextArea().setFocus(true);
            getTextArea().getCommandManager().execute(Command.FORMAT_BLOCK, level);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#update()
     */
    public void update()
    {
        if (levels != null) {
            String level = getTextArea().getCommandManager().getStringValue(Command.FORMAT_BLOCK);
            if (level != null) {
                for (int i = 0; i < levels.getItemCount(); i++) {
                    if (levels.getValue(i).equalsIgnoreCase(level)) {
                        levels.setSelectedIndex(i);
                        return;
                    }
                }
                // Report plain text if there's no block formatting or the block formatting is unknown.
                levels.setSelectedIndex(0);
            } else {
                // Report no block formatting if the selection includes multiple block formats.
                levels.setSelectedIndex(-1);
            }
        }
    }
}
