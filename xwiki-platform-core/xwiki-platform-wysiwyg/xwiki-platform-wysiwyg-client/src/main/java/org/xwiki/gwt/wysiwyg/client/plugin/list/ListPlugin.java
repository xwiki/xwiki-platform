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
package org.xwiki.gwt.wysiwyg.client.plugin.list;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractStatefulPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.list.exec.ListExecutable;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.ToggleButton;

/**
 * Plug-in for inserting ordered (numbered) and unordered (bullet) lists. It installs two toggle buttons on the tool bar
 * and updates their status depending on the current cursor position.
 * 
 * @version $Id$
 */
public class ListPlugin extends AbstractStatefulPlugin implements ClickHandler
{
    /**
     * The association between tool bar buttons and the commands that are executed when these buttons are clicked.
     */
    private final Map<ToggleButton, Command> buttons = new HashMap<ToggleButton, Command>();

    /**
     * User interface extension for the editor menu bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension("toolbar");

    /**
     * List behaviour adjuster to manage valid lists correctly.
     */
    private ListBehaviorAdjuster behaviorAdjuster;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractStatefulPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // Register custom executables in order to handle valid HTML lists.
        getTextArea().getCommandManager().registerCommand(Command.INSERT_ORDERED_LIST,
            new ListExecutable(textArea, true));
        getTextArea().getCommandManager().registerCommand(Command.INSERT_UNORDERED_LIST,
            new ListExecutable(textArea, false));

        addFeature("orderedlist", Command.INSERT_ORDERED_LIST, Images.INSTANCE.ol(), Strings.INSTANCE.ol());
        addFeature("unorderedlist", Command.INSERT_UNORDERED_LIST, Images.INSTANCE.ul(), Strings.INSTANCE.ul());

        if (toolBarExtension.getFeatures().length > 0) {
            registerTextAreaHandlers();
            getUIExtensionList().add(toolBarExtension);

            // Initialize the behavior adjuster and set it up with this text area
            behaviorAdjuster = new ListBehaviorAdjuster(textArea);
            // fake a reset command to handle the loaded document
            behaviorAdjuster.onCommand(getTextArea().getCommandManager(), new Command("reset"), null);
            // add key listener to the rta
            saveRegistration(getTextArea().addKeyDownHandler(behaviorAdjuster));
            saveRegistration(getTextArea().addKeyUpHandler(behaviorAdjuster));
            saveRegistration(getTextArea().addKeyPressHandler(behaviorAdjuster));
            getTextArea().getCommandManager().addCommandListener(behaviorAdjuster);
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
            ToggleButton button = new ToggleButton(new Image(imageResource));
            saveRegistration(button.addClickHandler(this));
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
        }
        buttons.clear();

        toolBarExtension.clearFeatures();

        // if a behaviorAdjuster was created and attached, remove it
        if (behaviorAdjuster != null) {
            getTextArea().getCommandManager().removeCommandListener(behaviorAdjuster);
            behaviorAdjuster = null;
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
        if (command != null && ((FocusWidget) event.getSource()).isEnabled()) {
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
            if (entry.getKey().isEnabled()) {
                entry.getKey().setDown(getTextArea().getCommandManager().isExecuted(entry.getValue()));
            }
        }
    }
}
