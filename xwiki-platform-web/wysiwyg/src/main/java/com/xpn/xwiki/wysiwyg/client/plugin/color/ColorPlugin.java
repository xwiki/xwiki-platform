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
package com.xpn.xwiki.wysiwyg.client.plugin.color;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.color.exec.BackColorExecutable;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.AbstractPlugin;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Plug-in for controlling the text color and the background color. It installs two push buttons on the tool bar, each
 * opening a color picker dialog, which is synchronized with the text area.
 * 
 * @version $Id$
 */
public class ColorPlugin extends AbstractPlugin implements ClickListener, PopupListener
{
    /**
     * The association between tool bar buttons and the commands that are executed when these buttons are clicked.
     */
    private final Map<PushButton, Command> buttons = new HashMap<PushButton, Command>();

    /**
     * User interface extension for the editor tool bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * The color picker.
     */
    private ColorPicker colorPicker;

    /**
     * The command for which the color picker has been shown. It is needed in order to know which command to execute
     * after the color picker is closed.
     */
    private Command currentCommand;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(Wysiwyg, RichTextArea, Config)
     */
    public void init(Wysiwyg wysiwyg, RichTextArea textArea, Config config)
    {
        super.init(wysiwyg, textArea, config);

        // Register custom executables.
        getTextArea().getCommandManager().registerCommand(Command.BACK_COLOR,
            (Executable) GWT.create(BackColorExecutable.class));

        addFeature("forecolor", Command.FORE_COLOR, Images.INSTANCE.foreColor().createImage(), Strings.INSTANCE
            .foreColor());
        addFeature("backcolor", Command.BACK_COLOR, Images.INSTANCE.backColor().createImage(), Strings.INSTANCE
            .backColor());

        if (toolBarExtension.getFeatures().length > 0) {
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
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        if (colorPicker != null) {
            colorPicker.hide();
            colorPicker.removeFromParent();
            colorPicker.removePopupListener(this);
            colorPicker = null;
        }

        for (PushButton button : buttons.keySet()) {
            button.removeFromParent();
            button.removeClickListener(this);
        }
        buttons.clear();

        currentCommand = null;

        toolBarExtension.clearFeatures();

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
        if (command != null) {
            currentCommand = command;

            String color = getTextArea().getCommandManager().getStringValue(command);
            getColorPicker().setColor(color);

            int left = sender.getAbsoluteLeft();
            int top = sender.getAbsoluteTop() + sender.getOffsetHeight();
            getColorPicker().setPopupPosition(left, top);

            getColorPicker().show();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupListener#onPopupClosed(PopupPanel, boolean)
     */
    public void onPopupClosed(PopupPanel sender, boolean autoHide)
    {
        if (sender == getColorPicker() && !autoHide) {
            String color = getColorPicker().getColor();
            getTextArea().setFocus(true);
            if (color != null) {
                getTextArea().getCommandManager().execute(currentCommand, color);
            }
        }
    }

    /**
     * Lazy loads the color picker.
     * 
     * @return the color picker
     */
    protected ColorPicker getColorPicker()
    {
        if (colorPicker == null) {
            colorPicker = new ColorPicker();
            colorPicker.addPopupListener(this);
        }
        return colorPicker;
    }
}
