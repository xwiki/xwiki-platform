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
package org.xwiki.gwt.wysiwyg.client.plugin.color;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.InlineStyleExecutable;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

/**
 * Plug-in for controlling the text color and the background color. It installs two push buttons on the tool bar, each
 * opening a color picker dialog, which is synchronized with the text area.
 * 
 * @version $Id$
 */
public class ColorPlugin extends AbstractPlugin implements ClickHandler, CloseHandler<PopupPanel>
{
    /**
     * The colors available by default on the color palette.
     */
    public static final String DEFAULT_COLORS =
        "#000000,#444444,#666666,#999999,#CCCCCC,#EEEEEE,#F3F3F3,#FFFFFF,"
            + "#FF0000,#FF9900,#FFFF00,#00FF00,#00FFFF,#0000FF,#9900FF,#FF00FF,"
            + "#F4CCCC,#FCE5CD,#FFF2CC,#D9EAD3,#D0E0E3,#CFE2F3,#D9D2E9,#EAD1DC,"
            + "#EA9999,#F9CB9C,#FFE599,#B6D7A8,#A2C4C9,#9FC5E8,#B4A7D6,#D5A6BD,"
            + "#E06666,#F6B26B,#FFD966,#93C47D,#76A5AF,#6FA8DC,#8E7CC3,#C27BA0,"
            + "#CC0000,#E69138,#F1C232,#6AA84F,#45818E,#3D85C6,#674EA7,#A64D79,"
            + "#990000,#B45F06,#BF9000,#38761D,#134F5C,#0B5394,#351C75,#741B47,"
            + "#660000,#783F04,#7F6000,#274E13,#0C343D,#073763,#20124D,#4C1130";

    /**
     * The default number of columns displayed by the color palette.
     */
    public static final String DEFAULT_COLUMN_COUNT = "8";

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
     * @see AbstractPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // Register custom executables.
        getTextArea().getCommandManager().registerCommand(Command.FORE_COLOR,
            new InlineStyleExecutable(textArea, Style.COLOR));
        getTextArea().getCommandManager().registerCommand(Command.BACK_COLOR,
            new InlineStyleExecutable(textArea, Style.BACKGROUND_COLOR));

        addFeature("forecolor", Command.FORE_COLOR, Images.INSTANCE.foreColor(), Strings.INSTANCE.foreColor());
        addFeature("backcolor", Command.BACK_COLOR, Images.INSTANCE.backColor(), Strings.INSTANCE.backColor());

        if (toolBarExtension.getFeatures().length > 0) {
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
     */
    private void addFeature(String name, Command command, ImageResource imageResource, String title)
    {
        if (getTextArea().getCommandManager().isSupported(command)) {
            PushButton button = new PushButton(new Image(imageResource));
            saveRegistration(button.addClickHandler(this));
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
            colorPicker = null;
        }

        for (PushButton button : buttons.keySet()) {
            button.removeFromParent();
        }
        buttons.clear();

        currentCommand = null;

        toolBarExtension.clearFeatures();

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickHandler#onClick(ClickEvent)
     */
    public void onClick(ClickEvent event)
    {
        Widget sender = (Widget) event.getSource();
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
     * @see CloseHandler#onClose(CloseEvent)
     */
    public void onClose(CloseEvent<PopupPanel> event)
    {
        if (event.getSource() == getColorPicker() && !event.isAutoClosed()) {
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
            String[] colors = getConfig().getParameter("colors", DEFAULT_COLORS).split("\\s*,\\s*");
            int columnCount = Integer.parseInt(getConfig().getParameter("colorsPerRow", DEFAULT_COLUMN_COUNT));

            colorPicker = new ColorPicker(new ColorPalette(colors, columnCount));
            saveRegistration(colorPicker.addCloseHandler(this));
        }
        return colorPicker;
    }
}
