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
package com.xpn.xwiki.wysiwyg.client.plugin.valign;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Plug-in for making text superscript or subscript. It installs two toggle buttons on the tool bar and updates their
 * status depending on the current cursor position and the direction of the navigation using the arrow keys. For
 * instance, if you navigate from a subscript region to a superscript one and you type a character it will be subscript.
 * <p>
 * <b>Known issues:</b> When you navigate backwards, from right to left, using the arrow keys, the status of the toggle
 * buttons is not synchronized with the text area. The text area behaves properly though.
 * 
 * @version $Id$
 */
public class VerticalAlignPlugin extends AbstractStatefulPlugin implements ClickListener
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
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        addFeature("superscript", Command.SUPER_SCRIPT, Images.INSTANCE.superscript().createImage(), Strings.INSTANCE
            .superscript());
        addFeature("subscript", Command.SUB_SCRIPT, Images.INSTANCE.subscript().createImage(), Strings.INSTANCE
            .subscript());

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
            ToggleButton button = new ToggleButton(image, this);
            button.setTitle(title);
            toolBarExtension.addFeature(name, button);
            buttons.put(button, command);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#destroy()
     */
    public void destroy()
    {
        for (ToggleButton button : buttons.keySet()) {
            button.removeFromParent();
            button.removeClickListener(this);
        }
        buttons.clear();

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
        Command command = buttons.get(sender);
        if (command != null && ((FocusWidget) sender).isEnabled()) {
            getTextArea().setFocus(true);
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
        for (Map.Entry<ToggleButton, Command> entry : buttons.entrySet()) {
            entry.getKey().setDown(getTextArea().getCommandManager().isExecuted(entry.getValue()));
        }
    }
}
