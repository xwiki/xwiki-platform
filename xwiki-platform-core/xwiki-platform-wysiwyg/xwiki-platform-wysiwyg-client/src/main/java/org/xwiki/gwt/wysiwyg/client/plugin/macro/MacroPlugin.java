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
package org.xwiki.gwt.wysiwyg.client.plugin.macro;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.ShortcutKeyManager;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.AbstractPlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.exec.CollapseExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.exec.InsertExecutable;
import org.xwiki.gwt.wysiwyg.client.plugin.macro.exec.RefreshExecutable;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;

/**
 * WYSIWYG editor plug-in for inserting macros and for editing macro parameters.
 * 
 * @version $Id$
 */
public class MacroPlugin extends AbstractPlugin
{
    /**
     * Rich text area command for refreshing macro output.
     */
    public static final Command REFRESH = new Command("macroRefresh");

    /**
     * Rich text area command for collapsing all the macros.
     */
    public static final Command COLLAPSE = new Command("macroCollapseAll");

    /**
     * Rich text area command for expanding all the macros.
     */
    public static final Command EXPAND = new Command("macroExpandAll");

    /**
     * Rich text area command for inserting a macro in place of the current selection.
     */
    public static final Command INSERT = new Command("macroInsert");

    /**
     * Hides macro meta data and displays macro output in a read only text box.
     */
    private MacroDisplayer displayer;

    /**
     * Controls the currently selected macros.
     */
    private MacroSelector selector;

    /**
     * The wizard used to cast macro spells on the rich text area.
     */
    private MacroWizard wizard;

    /**
     * Provides a user interface extension to allow users to manipulate macros using the top-level menu of the WYSIWYG
     * editor.
     */
    private MacroMenuExtension menuExtension;

    /**
     * The object used to extend the tool bar with buttons for easy macro insertion, skipping the macro selection step.
     */
    private MacroToolBarExtension toolBarExtension;

    /**
     * The macro service used to retrieve macro descriptors.
     */
    private final MacroServiceAsync macroService;

    /**
     * The object that listens to rich text area events and performs actions on the existing macros.
     */
    private final MacroController macroControler;

    /**
     * The object used to register shortcut keys for the macro plug-in features.
     */
    private final ShortcutKeyManager shortcutKeyManager = new ShortcutKeyManager();

    /**
     * Creates a new macro plug-in that uses the specified macro service.
     * 
     * @param macroService the macro service to be used for retrieving the macro descriptors
     */
    public MacroPlugin(MacroServiceAsync macroService)
    {
        this.macroService = macroService;
        this.macroControler = new MacroController(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#init(RichTextArea, Config)
     */
    public void init(RichTextArea textArea, Config config)
    {
        super.init(textArea, config);

        displayer = new MacroDisplayer(getTextArea());
        selector = new MacroSelector(displayer);
        wizard = new MacroWizard(displayer, config, macroService);

        String templateURL = config.getParameter("inputURL");
        if (!StringUtils.isEmpty(templateURL)) {
            getTextArea().getCommandManager().registerCommand(REFRESH, new RefreshExecutable(textArea, templateURL));
        }
        getTextArea().getCommandManager().registerCommand(COLLAPSE, new CollapseExecutable(selector, true));
        getTextArea().getCommandManager().registerCommand(EXPAND, new CollapseExecutable(selector, false));
        getTextArea().getCommandManager().registerCommand(INSERT, new InsertExecutable(selector, macroService, config));

        saveRegistrations(shortcutKeyManager.addHandlers(getTextArea()));
        saveRegistrations(macroControler.addHadlers());

        menuExtension = new MacroMenuExtension(this);
        getUIExtensionList().add(menuExtension);
        // Hack: We can access the menus where each menu item was placed only after the main menu bar is initialized,
        // which happens after all the plugins are loaded.
        Scheduler.get().scheduleDeferred(new ScheduledCommand()
        {
            public void execute()
            {
                menuExtension.registerAttachHandlers();
            }
        });

        toolBarExtension = new MacroToolBarExtension(this);
        if (toolBarExtension.getExtension().getFeatures().length > 0) {
            getUIExtensionList().add(toolBarExtension.getExtension());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractPlugin#destroy()
     */
    public void destroy()
    {
        menuExtension.clearFeatures();
        toolBarExtension.destroy();
        shortcutKeyManager.clear();

        getTextArea().getCommandManager().unregisterCommand(REFRESH);
        getTextArea().getCommandManager().unregisterCommand(COLLAPSE);
        getTextArea().getCommandManager().unregisterCommand(EXPAND);
        getTextArea().getCommandManager().unregisterCommand(INSERT);

        selector.destroy();
        selector = null;

        displayer.destroy();
        displayer = null;

        wizard.destroy();
        wizard = null;

        super.destroy();
    }

    /**
     * @return the macro selector
     */
    public MacroSelector getSelector()
    {
        return selector;
    }

    /**
     * @return the object that can be used to register shortcut keys for macro plug-in features
     */
    ShortcutKeyManager getShortcutKeyManager()
    {
        return shortcutKeyManager;
    }

    /**
     * @return the service used to retrieve the macro descriptors
     */
    public MacroServiceAsync getMacroService()
    {
        return macroService;
    }

    /**
     * Start the edit macro wizard.
     */
    public void edit()
    {
        wizard.edit();
    }

    /**
     * Start the insert macro wizard.
     */
    public void insert()
    {
        wizard.insert();
    }

    /**
     * Skip the select macro wizard step and go directly to the edit macro wizard step for the specified macro.
     * 
     * @param macroId a macro identifier
     */
    public void insert(String macroId)
    {
        wizard.insert(macroId);
    }
}
