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
package com.xpn.xwiki.wysiwyg.client;

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.DeferredUpdater;
import org.xwiki.gwt.user.client.HandlerRegistrationCollection;
import org.xwiki.gwt.user.client.Updatable;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;

import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginFactoryManager;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginManager;
import com.xpn.xwiki.wysiwyg.client.plugin.UIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.DefaultPluginManager;
import com.xpn.xwiki.wysiwyg.client.syntax.SyntaxValidator;

/**
 * A {@link RichTextEditor} controller.
 * 
 * @version $Id$
 */
public class RichTextEditorController implements Updatable, MouseUpHandler, KeyUpHandler, CommandListener, LoadHandler
{
    /**
     * The string used to identify the menu bar extension point.
     */
    private static final String MENU_ROLE = "menu";

    /**
     * The regular expression used to express the separator for tool bar and menu bar feature names in configuration.
     */
    private static final String WHITE_SPACE_SEPARATOR = "\\s+";

    /**
     * The command used to store the value of the rich text area before submitting the including form.
     */
    private static final Command SUBMIT = new Command("submit");

    /**
     * The object used to configure the editor and its plugins.
     */
    private final Config config;

    /**
     * A reference to the rich text editor.
     */
    private final RichTextEditor richTextEditor;

    /**
     * The object used to manage the tool bar.
     */
    private final ToolBarController toolBarController;

    /**
     * The object used to load and unload plugins.
     */
    private final PluginManager pluginManager;

    /**
     * The object used to assert if a feature must be enabled or disabled in some context.
     */
    private final SyntaxValidator syntaxValidator;

    /**
     * Schedules updates and executes only the most recent one.
     */
    private final DeferredUpdater updater = new DeferredUpdater(this);

    /**
     * The collection of handler registrations used by this editor.
     */
    private final HandlerRegistrationCollection registrations = new HandlerRegistrationCollection();

    /**
     * Flag indicating if the editor has been loaded. It is needed in order to prevent reloading the UI when the edited
     * document is reloaded.
     */
    private boolean loaded;

    /**
     * Creates a new editor.
     * 
     * @param richTextEditor the rich text editor to manage
     * @param config the configuration object
     * @param pfm the plugin factory manager used to instantiate plugins
     * @param syntaxValidator the object used to assert if a feature must be enabled or disabled in some context
     */
    public RichTextEditorController(RichTextEditor richTextEditor, Config config, PluginFactoryManager pfm,
        SyntaxValidator syntaxValidator)
    {
        this.config = config;

        this.richTextEditor = richTextEditor;
        registrations.add(richTextEditor.getTextArea().addLoadHandler(this));
        registrations.add(richTextEditor.getTextArea().addMouseUpHandler(this));
        registrations.add(richTextEditor.getTextArea().addKeyUpHandler(this));
        richTextEditor.getTextArea().getCommandManager().addCommandListener(this);

        toolBarController = new ToolBarController(richTextEditor.getToolbar());

        this.syntaxValidator = syntaxValidator;

        pluginManager = new DefaultPluginManager(richTextEditor.getTextArea(), config);
        pluginManager.setPluginFactoryManager(pfm);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseUpHandler#onMouseUp(MouseUpEvent)
     */
    public void onMouseUp(MouseUpEvent event)
    {
        // We listen to mouse up events instead of clicks because if the user selects text and the end points of the
        // selection are in different DOM nodes the click events are not triggered.
        if (event.getSource() == richTextEditor.getTextArea()) {
            updater.deferUpdate();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyUpHandler#onKeyUp(KeyUpEvent)
     */
    public void onKeyUp(KeyUpEvent event)
    {
        if (event.getSource() == richTextEditor.getTextArea()) {
            updater.deferUpdate();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onBeforeCommand(CommandManager, Command, String)
     */
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        // ignore
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onCommand(CommandManager, Command, String)
     */
    public void onCommand(CommandManager sender, Command command, String param)
    {
        if (sender == richTextEditor.getTextArea().getCommandManager()) {
            updater.deferUpdate();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadHandler#onLoad(LoadEvent)
     */
    public void onLoad(LoadEvent event)
    {
        if (event.getSource() == richTextEditor.getTextArea() && !loaded) {
            loaded = true;

            loadPlugins();
            extendRootUI();
            fillMenu();
            toolBarController.fill(config, pluginManager);
            initTextArea();
        }
    }

    /**
     * Loads the plugins specified in the configuration.
     */
    protected void loadPlugins()
    {
        String[] pluginNames = config.getParameter("plugins", "").split(WHITE_SPACE_SEPARATOR);
        for (int i = 0; i < pluginNames.length; i++) {
            pluginManager.load(pluginNames[i]);
        }
    }

    /**
     * Loads the root user interface extensions.
     */
    protected void extendRootUI()
    {
        String[] rootExtensionNames = config.getParameter("rootUI", SUBMIT.toString()).split(WHITE_SPACE_SEPARATOR);
        for (int i = 0; i < rootExtensionNames.length; i++) {
            UIExtension rootExtension = pluginManager.getUIExtension("root", rootExtensionNames[i]);
            if (rootExtension != null) {
                richTextEditor.getContainer().add((Widget) rootExtension.getUIObject(rootExtensionNames[i]));
            }
        }
    }

    /**
     * Fills the menu of the editor.
     */
    protected void fillMenu()
    {
        // By default we don't show the menu (the list of menu entries is empty if not specified in the configuration).
        String[] entries = config.getParameter(MENU_ROLE, "").split(WHITE_SPACE_SEPARATOR);
        for (int i = 0; i < entries.length; i++) {
            UIExtension uie = pluginManager.getUIExtension(MENU_ROLE, entries[i]);
            if (uie != null) {
                richTextEditor.getMenu().addItem((MenuItem) uie.getUIObject(entries[i]));
            }
        }
    }

    /**
     * Initializes the rich text area.
     */
    protected void initTextArea()
    {
        // Focus the rich text area before executing the commands to ensure it has a proper selection.
        richTextEditor.getTextArea().setFocus(true);
        if (!richTextEditor.getTextArea().isEnabled()) {
            // Enable the rich text area in order to be able to submit its content, and notify the plug-ins.
            richTextEditor.getTextArea().getCommandManager().execute(new Command("enable"), true);
        }
        // Store the initial content of the rich text area.
        richTextEditor.getTextArea().getCommandManager().execute(SUBMIT);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Updatable#update()
     */
    public void update()
    {
        toolBarController.update(richTextEditor.getTextArea(), syntaxValidator);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Updatable#canUpdate()
     */
    public boolean canUpdate()
    {
        return richTextEditor.getTextArea().isAttached() && richTextEditor.getTextArea().isEnabled();
    }

    /**
     * Get the rich text editor. Creates it if it does not exist.
     * 
     * @return the rich text editor
     */
    public RichTextEditor getRichTextEditor()
    {
        return richTextEditor;
    }

    /**
     * @return this editor's configuration object
     */
    public Config getConfig()
    {
        return config;
    }

    /**
     * Saves a handler registration in order for the handler to be automatically removed when the plug-in is destroyed.
     * 
     * @param registration the handler registration to be saved
     */
    protected void saveRegistration(HandlerRegistration registration)
    {
        registrations.add(registration);
    }

    /**
     * Destroys this editor, unregistering all the listeners and releasing the used memory.
     */
    public void destroy()
    {
        toolBarController.destroy();
        // Unload all the plug-ins.
        pluginManager.unloadAll();
        // Remove all listeners and handlers.
        registrations.removeHandlers();
        richTextEditor.getTextArea().getCommandManager().removeCommandListener(this);
        // Detach the user interface.
        richTextEditor.removeFromParent();
    }
}
