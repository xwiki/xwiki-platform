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
package org.xwiki.gwt.wysiwyg.client.plugin.format;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.format.exec.FormatBlockExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.format.exec.RemoveFormatExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;

/**
 * {@link RichTextArea} plug-in for formatting text. It can be used to format text as heading 1 to 5. It installs a
 * select on the tool bar and updates its status depending on the current cursor position.
 * 
 * @version $Id$
 */
public class FormatPlugin extends AbstractStatefulPlugin implements ChangeHandler, ClickHandler
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

    @Override
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // Register custom executables.
        getTextArea().getCommandManager().registerCommand(Command.FORMAT_BLOCK, new FormatBlockExecutable(textArea));
        getTextArea().getCommandManager().registerCommand(Command.REMOVE_FORMAT, new RemoveFormatExecutable(textArea));

        addFeature("removeformat", Command.REMOVE_FORMAT, Images.INSTANCE.removeFormat(), Strings.INSTANCE
            .removeFormat());

        if (getTextArea().getCommandManager().isSupported(Command.FORMAT_BLOCK)) {
            levels = new ListBox(false);
            saveRegistration(levels.addChangeHandler(this));
            levels.setVisibleItemCount(1);
            levels.setTitle(Strings.INSTANCE.format());

            levels.addItem(Strings.INSTANCE.formatPlainText(), "p");
            levels.addItem(Strings.INSTANCE.formatHeader1(), "h1");
            levels.addItem(Strings.INSTANCE.formatHeader2(), "h2");
            levels.addItem(Strings.INSTANCE.formatHeader3(), "h3");
            levels.addItem(Strings.INSTANCE.formatHeader4(), "h4");
            levels.addItem(Strings.INSTANCE.formatHeader5(), "h5");
            levels.addItem(Strings.INSTANCE.formatHeader6(), "h6");

            toolBarExtension.addFeature("format", levels);
        }

        if (toolBarExtension.getFeatures().length > 0) {
            registerTextAreaHandlers();
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

    @Override
    public void destroy()
    {
        for (PushButton button : buttons.keySet()) {
            button.removeFromParent();
        }
        buttons.clear();

        if (levels != null) {
            levels.removeFromParent();
            levels = null;

            toolBarExtension.clearFeatures();
        }

        super.destroy();
    }

    @Override
    public void onClick(ClickEvent event)
    {
        Command command = buttons.get(event.getSource());
        if (command != null && ((FocusWidget) event.getSource()).isEnabled()) {
            getTextArea().setFocus(true);
            getTextArea().getCommandManager().execute(command);
        }
    }

    @Override
    public void onChange(ChangeEvent event)
    {
        if (event.getSource() == levels && levels.isEnabled()) {
            String level = levels.getValue(levels.getSelectedIndex());
            getTextArea().setFocus(true);
            getTextArea().getCommandManager().execute(Command.FORMAT_BLOCK, level);
        }
    }

    @Override
    public void update()
    {
        if (levels != null && levels.isEnabled()) {
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
