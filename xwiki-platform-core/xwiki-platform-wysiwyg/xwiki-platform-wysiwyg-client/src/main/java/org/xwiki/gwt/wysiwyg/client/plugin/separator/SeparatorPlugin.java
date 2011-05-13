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
package org.xwiki.gwt.wysiwyg.client.plugin.separator;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Images;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.CompositeUIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.separator.exec.InsertHRExecutable;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;

/**
 * Utility plug-in for separating tool bar entries, menu entries and so on.
 * 
 * @version $Id$
 */
public class SeparatorPlugin extends AbstractPlugin implements ClickHandler
{
    /**
     * The tool bar button used for inserting a new horizontal rule.
     */
    private PushButton hr;

    /**
     * Tool bar extension that includes the horizontal rule button.
     */
    private final FocusWidgetUIExtension toolBarFocusWidgets = new FocusWidgetUIExtension("toolbar");

    /**
     * The menu extension that provides the menu separators.
     */
    private final MenuBarSeparator menuExtension = new MenuBarSeparator();

    /**
     * Tool bar extension that includes {@link #toolBarFocusWidgets} and tool bar specific separators like the vertical
     * bar and the line break.
     */
    private final CompositeUIExtension toolBarExtension = new CompositeUIExtension(toolBarFocusWidgets.getRole());

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        // Register custom executables.
        getTextArea().getCommandManager().registerCommand(Command.INSERT_HORIZONTAL_RULE,
            new InsertHRExecutable(textArea));

        // User interface extension that provides ways of separating tool bar entries.
        toolBarExtension.addUIExtension(new ToolBarSeparator());
        // User interface extension for separator widgets that can be focused.
        toolBarExtension.addUIExtension(toolBarFocusWidgets);

        if (getTextArea().getCommandManager().isSupported(Command.INSERT_HORIZONTAL_RULE)) {
            hr = new PushButton(new Image(Images.INSTANCE.hr()));
            saveRegistration(hr.addClickHandler(this));
            hr.setTitle(Strings.INSTANCE.hr());
            toolBarFocusWidgets.addFeature("hr", hr);
        }

        getUIExtensionList().add(menuExtension);
        getUIExtensionList().add(toolBarExtension);

        // Hack: We can access the menus where each menu item separator was placed only after the main menu bar is
        // initialized, which happens after all the plugins are loaded.
        Scheduler.get().scheduleDeferred(new ScheduledCommand()
        {
            public void execute()
            {
                menuExtension.registerAttachHandlers();
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        if (hr != null) {
            hr.removeFromParent();
            hr = null;
        }

        toolBarFocusWidgets.clearFeatures();
        menuExtension.destroy();

        super.destroy();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickHandler#onClick(ClickEvent)
     */
    public void onClick(ClickEvent event)
    {
        if (event.getSource() == hr && hr.isEnabled()) {
            getTextArea().setFocus(true);
            getTextArea().getCommandManager().execute(Command.INSERT_HORIZONTAL_RULE);
        }
    }
}
