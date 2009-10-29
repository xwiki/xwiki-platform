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
package com.xpn.xwiki.wysiwyg.client.editor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.Cache;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.Console;
import org.xwiki.gwt.user.client.DeferredUpdater;
import org.xwiki.gwt.user.client.HandlerRegistrationCollection;
import org.xwiki.gwt.user.client.Updatable;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandListener;
import org.xwiki.gwt.user.client.ui.rta.cmd.CommandManager;

import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginFactoryManager;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginManager;
import com.xpn.xwiki.wysiwyg.client.plugin.UIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.DefaultPluginManager;
import com.xpn.xwiki.wysiwyg.client.plugin.separator.ToolBarSeparator;
import com.xpn.xwiki.wysiwyg.client.syntax.SyntaxValidator;
import com.xpn.xwiki.wysiwyg.client.syntax.SyntaxValidatorManager;

/**
 * The controller part of the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class WysiwygEditor implements Updatable, MouseUpHandler, KeyUpHandler, CommandListener, LoadHandler
{
    /**
     * Iterates through the features placed on the tool bar and enables or disables them by following the syntax
     * validation rules.
     */
    private class SyntaxValidationCommand implements IncrementalCommand
    {
        /**
         * Iterator for the features placed on the tool bar. For each entry the key is the feature name and the value is
         * the widget present on the tool bar.
         */
        private final Iterator<Map.Entry<String, UIExtension>> iterator;

        /**
         * Default constructor. Initializes the iterator for the tool bar features.
         */
        public SyntaxValidationCommand()
        {
            iterator = toolBarFeatures.entrySet().iterator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see IncrementalCommand#execute()
         */
        public boolean execute()
        {
            try {
                if (canUpdate() && iterator.hasNext()) {
                    Map.Entry<String, UIExtension> entry = iterator.next();
                    entry.getValue().setEnabled(entry.getKey(),
                        sv.isValid(entry.getKey(), richTextEditor.getTextArea()));
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                Console.getInstance().error(e, WysiwygEditor.class.getName(), SyntaxValidationCommand.class.getName());
                return false;
            }
        }
    }

    /**
     * The default storage syntax.
     */
    public static final String DEFAULT_SYNTAX = "xhtml/1.0";

    /**
     * WYWISYWG tab index in the TabPanel.
     */
    protected static final int WYSIWYG_TAB_INDEX = 0;

    /**
     * Source tab index in the TabPanel.
     */
    protected static final int SOURCE_TAB_INDEX = 1;

    /**
     * The string used to identify the tool bar extension point.
     */
    private static final String TOOLBAR_ROLE = "toolbar";

    /**
     * The string used to identify the menu bar extension point.
     */
    private static final String MENU_ROLE = "menu";

    /**
     * The list of plug-ins that can be loaded by default. Can be overwritten from the configuration.
     * <p>
     * NOTE: By default we load only the plug-ins that can work offline, without the need of a service.
     */
    private static final String DEFAULT_PLUGINS =
        "submit line separator text valign justify list indent history format font color symbol table";

    /**
     * The list of features that can be placed on the tool bar by default. Can be overwritten from the configuration.
     */
    private static final String DEFAULT_TOOLBAR =
        "bold italic underline strikethrough teletype | subscript superscript"
            + " | justifyleft justifycenter justifyright justifyfull | unorderedlist orderedlist | outdent indent"
            + " | undo redo | format | fontname fontsize forecolor backcolor | hr removeformat symbol";

    /**
     * The regular expression used to express the separator for tool bar and menu bar feature names in configuration.
     */
    private static final String WHITE_SPACE_SEPARATOR = "\\s+";

    /**
     * The command used to store the value of the rich text area before submitting the including form.
     */
    private static final Command SUBMIT = new Command("submit");

    /**
     * The interface of the WYSIWYG editor. It can be either a {@link RichTextEditor} or a {@link TabPanel} containing
     * the {@link RichTextEditor} and the {@link PlainTextEditor}.
     */
    private final Widget ui;

    /**
     * The configuration object.
     */
    private final Config config;

    /**
     * The plug-in factory manager.
     */
    private final PluginFactoryManager pfm;

    /**
     * The syntax validator manager.
     */
    private final SyntaxValidatorManager svm;

    /**
     * Schedules updates and executes only the most recent one.
     */
    private final DeferredUpdater updater = new DeferredUpdater(this);

    /**
     * Listen to events and takes the appropriate actions.
     */
    private final WysiwygEditorListener listener = new WysiwygEditorListener(this);

    /**
     * The collection of handler registrations used by this editor.
     */
    private final HandlerRegistrationCollection registrations = new HandlerRegistrationCollection();

    /**
     * The features that have been placed on the tool bar. The key is the feature name and the value is the widget that
     * has been placed on the tool bar.
     */
    private final Map<String, UIExtension> toolBarFeatures = new HashMap<String, UIExtension>();

    /**
     * The plug-in manager.
     */
    private PluginManager pm;

    /**
     * The syntax validator.
     */
    private SyntaxValidator sv;

    /**
     * A reference to the rich text editor.
     */
    private RichTextEditor richTextEditor;

    /**
     * Flag indicating if the WYSIWYG editor has been loaded. It is needed in order to prevent reloading the UI when the
     * edited document is reloaded.
     */
    private boolean loaded;

    /**
     * Creates a new WYSIWYG editor.
     * 
     * @param config The configuration object.
     * @param svm The syntax validation manager used for enabling or disabling plugin features.
     * @param pfm The plugin factory manager used to instantiate plugins.
     */
    public WysiwygEditor(Config config, SyntaxValidatorManager svm, PluginFactoryManager pfm)
    {
        this.config = config;
        this.svm = svm;
        this.pfm = pfm;

        if (isTabbed()) {
            ui = createTabPanel();
        } else {
            ui = getRichTextEditor();
        }

        // Hide the hook.
        getHook().getStyle().setProperty("display", "none");
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
            initEditor();
            fillMenu();
            fillToolBar();
            // Store the initial content of the rich text area.
            richTextEditor.getTextArea().getCommandManager().execute(SUBMIT);
            // Focus the rich text area to be sure it has reached design mode.
            richTextEditor.getTextArea().setFocus(true);
        }
    }

    /**
     * Loads the plugins specified in the configuration (or the default list of plugins if the configuration doesn't
     * specify the <em>plugins</em> parameter).
     */
    private void loadPlugins()
    {
        String[] pluginNames = config.getParameter("plugins", DEFAULT_PLUGINS).split(WHITE_SPACE_SEPARATOR);
        for (int i = 0; i < pluginNames.length; i++) {
            pm.load(pluginNames[i]);
        }
    }

    /**
     * Loads the root user interface extensions.
     */
    private void initEditor()
    {
        // TODO: Transform the tool bar and the menu bar in root UI extensions.
        String[] rootExtensionNames = config.getParameter("rootUI", SUBMIT.toString()).split(WHITE_SPACE_SEPARATOR);
        for (int i = 0; i < rootExtensionNames.length; i++) {
            UIExtension rootExtension = pm.getUIExtension("root", rootExtensionNames[i]);
            if (rootExtension != null) {
                getRichTextEditor().getContainer().add((Widget) rootExtension.getUIObject(rootExtensionNames[i]));
            }
        }
    }

    /**
     * Build the editor tab panel. This panel contains two tabs, one for the WYSIWYG editor and one for the source
     * editor.
     * 
     * @return the newly created tab panel
     */
    private TabPanel createTabPanel()
    {
        final Cache cache = new Cache((Element) DOM.getElementById(config.getParameter("cacheId", "")).cast());
        PlainTextEditor plainTextEditor = new PlainTextEditor(getHook(), cache);

        TabPanel tabs = new TabPanel();
        tabs.add(getRichTextEditor(), Strings.INSTANCE.wysiwyg());
        tabs.add(plainTextEditor, Strings.INSTANCE.source());
        tabs.setStyleName("xRichTextEditorTabPanel");
        tabs.setAnimationEnabled(false);

        final String wysiwygTabName = "wysiwyg";
        final String cacheKeyActiveTextArea = "editor.activeTextArea";
        if (wysiwygTabName.equals(cache.get(cacheKeyActiveTextArea, config.getParameter("defaultEditor")))) {
            plainTextEditor.getTextArea().setEnabled(false);
            tabs.selectTab(WYSIWYG_TAB_INDEX);
        } else {
            getRichTextEditor().getTextArea().setEnabled(false);
            tabs.selectTab(SOURCE_TAB_INDEX);
        }

        registrations.add(tabs.addBeforeSelectionHandler(listener));
        registrations.add(tabs.addSelectionHandler(new SelectionHandler<Integer>()
        {
            public void onSelection(SelectionEvent<Integer> event)
            {
                // Cache the active text area.
                cache.put(cacheKeyActiveTextArea, event.getSelectedItem() == WYSIWYG_TAB_INDEX ? wysiwygTabName
                    : "source");
            }
        }));
        registrations.add(tabs.addSelectionHandler(listener));

        return tabs;
    }

    /**
     * Fills the menu of the editor.
     */
    private void fillMenu()
    {
        // By default we don't show the menu (the list of menu entries is empty if not specified in the configuration).
        String[] entries = config.getParameter(MENU_ROLE, "").split(WHITE_SPACE_SEPARATOR);
        for (int i = 0; i < entries.length; i++) {
            UIExtension uie = pm.getUIExtension(MENU_ROLE, entries[i]);
            if (uie != null) {
                getRichTextEditor().getMenu().addItem((MenuItem) uie.getUIObject(entries[i]));
            }
        }
    }

    /**
     * Fills the tool bar of the editor with the features specified in the configuration.
     */
    private void fillToolBar()
    {
        String[] toolBarFeatureNames = config.getParameter(TOOLBAR_ROLE, DEFAULT_TOOLBAR).split(WHITE_SPACE_SEPARATOR);
        boolean emptyGroup = true;
        boolean emptyLine = true;
        boolean uieNotFound = false;
        UIExtension verticalBar = null;
        UIExtension lineBreak = null;
        for (int i = 0; i < toolBarFeatureNames.length; i++) {
            UIExtension uie = pm.getUIExtension(TOOLBAR_ROLE, toolBarFeatureNames[i]);
            if (uie != null) {
                if (ToolBarSeparator.VERTICAL_BAR.equals(toolBarFeatureNames[i])) {
                    if (emptyGroup && uieNotFound) {
                        continue;
                    } else {
                        if (verticalBar != null) {
                            getRichTextEditor().getToolbar().add(
                                (Widget) verticalBar.getUIObject(ToolBarSeparator.VERTICAL_BAR));
                            toolBarFeatures.put(ToolBarSeparator.VERTICAL_BAR, verticalBar);
                        } else if (lineBreak != null) {
                            getRichTextEditor().getToolbar().add(
                                (Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
                            toolBarFeatures.put(ToolBarSeparator.LINE_BREAK, lineBreak);
                            lineBreak = null;
                        }
                        verticalBar = uie;
                        emptyGroup = true;
                        uieNotFound = false;
                    }
                } else if (ToolBarSeparator.LINE_BREAK.equals(toolBarFeatureNames[i])) {
                    if (emptyLine && uieNotFound) {
                        continue;
                    } else {
                        if (lineBreak != null) {
                            getRichTextEditor().getToolbar().add(
                                (Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
                            toolBarFeatures.put(ToolBarSeparator.LINE_BREAK, lineBreak);
                        }
                        lineBreak = uie;
                        verticalBar = null;
                        emptyLine = true;
                        emptyGroup = true;
                        uieNotFound = false;
                    }
                } else {
                    if (verticalBar != null) {
                        getRichTextEditor().getToolbar().add(
                            (Widget) verticalBar.getUIObject(ToolBarSeparator.VERTICAL_BAR));
                        toolBarFeatures.put(ToolBarSeparator.VERTICAL_BAR, verticalBar);
                        verticalBar = null;
                    } else if (lineBreak != null) {
                        getRichTextEditor().getToolbar().add(
                            (Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
                        toolBarFeatures.put(ToolBarSeparator.LINE_BREAK, lineBreak);
                        lineBreak = null;
                    }
                    getRichTextEditor().getToolbar().add((Widget) uie.getUIObject(toolBarFeatureNames[i]));
                    toolBarFeatures.put(toolBarFeatureNames[i], uie);
                    emptyGroup = false;
                    emptyLine = false;
                }
            } else {
                uieNotFound = true;
            }
        }
    }

    /**
     * Puts the editor in loading state or get it out of it.
     * 
     * @param loading {@code true} to put the editor in loading state, {@code false} to get it out of it
     */
    protected void setLoading(boolean loading)
    {
        if (isTabbed()) {
            getRichTextEditor().setLoading(loading);
            getPlainTextEditor().setLoading(loading);
        } else {
            getRichTextEditor().setLoading(loading);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Updatable#update()
     */
    public void update()
    {
        DeferredCommand.addCommand(new SyntaxValidationCommand());
    }

    /**
     * {@inheritDoc}
     * 
     * @see Updatable#canUpdate()
     */
    public boolean canUpdate()
    {
        // NOTE: Currently only the rich text area triggers updates.
        return getRichTextEditor().getTextArea().isAttached() && getRichTextEditor().getTextArea().isEnabled();
    }

    /**
     * In a Model-View-Controller architecture the UI represents the View component, while this class represents the
     * Controller. The model could be considered the DOM document edited.
     * 
     * @return The editor User Interface main panel.
     */
    public Widget getUI()
    {
        return ui;
    }

    /**
     * Get the rich text editor. Creates it if it does not exist.
     * 
     * @return The rich text editor.
     */
    public RichTextEditor getRichTextEditor()
    {
        if (richTextEditor == null) {
            richTextEditor = new RichTextEditor();
            registrations.add(richTextEditor.getTextArea().addLoadHandler(this));
            registrations.add(richTextEditor.getTextArea().addMouseUpHandler(this));
            registrations.add(richTextEditor.getTextArea().addKeyUpHandler(this));
            richTextEditor.getTextArea().getCommandManager().addCommandListener(this);
            IFrameElement.as(richTextEditor.getTextArea().getElement()).setSrc(
                config.getParameter("inputURL", "about:blank"));
            richTextEditor.getTextArea().setHeight(Math.max(getHook().getOffsetHeight(), 100) + "px");

            sv = svm.getSyntaxValidator(getConfig().getParameter("syntax", DEFAULT_SYNTAX));

            pm = new DefaultPluginManager(richTextEditor.getTextArea(), config);
            pm.setPluginFactoryManager(pfm);
        }
        return richTextEditor;
    }

    /**
     * Get the plain text editor.
     * 
     * @return The plain text editor, null if it does not exist.
     */
    public PlainTextEditor getPlainTextEditor()
    {
        return isTabbed() ? (PlainTextEditor) ((TabPanel) ui).getWidget(SOURCE_TAB_INDEX) : null;
    }

    /**
     * @return this editor's configuration object
     */
    public Config getConfig()
    {
        return config;
    }

    /**
     * @return the element replaced by the WYSIWYG editor
     */
    private Element getHook()
    {
        return DOM.getElementById(config.getParameter("hookId")).cast();
    }

    /**
     * @return {@code true} if the WYSIWYG/Source tabs are displayed, {@code false} otherwise
     */
    private boolean isTabbed()
    {
        return Boolean.valueOf(getConfig().getParameter("displayTabs", "false"));
    }

    /**
     * Destroys this WYSIWYG editor, unregistering all the listeners and releasing the used memory.
     */
    public void destroy()
    {
        // Unload all the plug-ins.
        toolBarFeatures.clear();
        pm.unloadAll();
        // Remove all listeners and handlers.
        registrations.removeHandlers();
        richTextEditor.getTextArea().getCommandManager().removeCommandListener(this);
        // Detach the user interface.
        ui.removeFromParent();
        // Drop references.
        pm = null;
        sv = null;
        richTextEditor = null;
    }
}
