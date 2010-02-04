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

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.BackForwardCache;
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
     * The collection of handler registrations used by this editor.
     */
    private final HandlerRegistrationCollection registrations = new HandlerRegistrationCollection();

    /**
     * The features that have been placed on the tool bar. The key is the feature name and the value is the widget that
     * has been placed on the tool bar.
     */
    private final Map<String, UIExtension> toolBarFeatures = new HashMap<String, UIExtension>();

    /**
     * The interface of the WYSIWYG editor. It can be either a {@link RichTextEditor} or a {@link TabPanel} containing
     * the {@link RichTextEditor} and the {@link PlainTextEditor}.
     */
    private Widget ui;

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
     * The object used to cache some of the evaluations made for the current rich text area selection.
     */
    private Cache selectionCache;

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
            createTabPanel();
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

            // Enable the rich text area if needed.
            int selectedTab = getSelectedTab();
            // If the source tab is not selected then either we don't have tabs or the WYSIWYG tab is selected.
            if (selectedTab != SOURCE_TAB_INDEX) {
                if (selectedTab == WYSIWYG_TAB_INDEX) {
                    // Disable the plain text area to prevent submitting its content. The plain text area was enabled
                    // while the the rich text area was loading because the rich text area couldn't have been submitted
                    // during that time.
                    getPlainTextEditor().getTextArea().setEnabled(false);
                }
                // Focus the rich text area before executing the commands to ensure it has a proper selection.
                richTextEditor.getTextArea().setFocus(true);
                // Store the initial content of the rich text area.
                richTextEditor.getTextArea().getCommandManager().execute(SUBMIT);
                if (!richTextEditor.getTextArea().isEnabled()) {
                    // Enable the rich text area in order to be able to submit its content, and notify the plug-ins.
                    richTextEditor.getTextArea().getCommandManager().execute(new Command("enable"), true);
                }
            }
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
                richTextEditor.getContainer().add((Widget) rootExtension.getUIObject(rootExtensionNames[i]));
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
        Element cacheableElement = (Element) DOM.getElementById(config.getParameter("cacheId", "")).cast();
        final BackForwardCache cache = new BackForwardCache(cacheableElement);
        PlainTextEditor plainTextEditor = new PlainTextEditor(getHook(), cache);

        TabPanel tabs = new TabPanel();
        ui = tabs;
        tabs.add(getRichTextEditor(), Strings.INSTANCE.wysiwyg());
        tabs.add(plainTextEditor, Strings.INSTANCE.source());
        tabs.setStyleName("xRichTextEditorTabPanel");
        tabs.setAnimationEnabled(false);

        final String wysiwygTabName = "wysiwyg";
        final String cacheKeyActiveTextArea = "editor.activeTextArea";
        String defaultEditor = cache.get(cacheKeyActiveTextArea, config.getParameter("defaultEditor"));
        tabs.selectTab(wysiwygTabName.equals(defaultEditor) ? WYSIWYG_TAB_INDEX : SOURCE_TAB_INDEX);

        // We initially disable the rich text area because it is loaded asynchronously and during this time we can't
        // submit its value. We enable it as soon as it finishes loading, if the WYSIWYG tab is selected. By enabling
        // the plain text area we can switch to the source tab editor before the WYSIWYG tab is fully loaded.
        plainTextEditor.getTextArea().setEnabled(true);
        getRichTextEditor().getTextArea().setEnabled(false);

        // Create the object that will handle the switch between the source editor and the rich text editor.
        WysiwygEditorListener editorSwitcher = new WysiwygEditorListener(this);

        registrations.add(tabs.addBeforeSelectionHandler(editorSwitcher));
        registrations.add(tabs.addSelectionHandler(new SelectionHandler<Integer>()
        {
            public void onSelection(SelectionEvent<Integer> event)
            {
                // Cache the active text area.
                cache.put(cacheKeyActiveTextArea, event.getSelectedItem() == WYSIWYG_TAB_INDEX ? wysiwygTabName
                    : "source");
            }
        }));
        registrations.add(tabs.addSelectionHandler(editorSwitcher));

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
                richTextEditor.getMenu().addItem((MenuItem) uie.getUIObject(entries[i]));
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
                            richTextEditor.getToolbar().add(
                                (Widget) verticalBar.getUIObject(ToolBarSeparator.VERTICAL_BAR));
                            toolBarFeatures.put(ToolBarSeparator.VERTICAL_BAR, verticalBar);
                        } else if (lineBreak != null) {
                            richTextEditor.getToolbar()
                                .add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
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
                            richTextEditor.getToolbar()
                                .add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
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
                        richTextEditor.getToolbar()
                            .add((Widget) verticalBar.getUIObject(ToolBarSeparator.VERTICAL_BAR));
                        toolBarFeatures.put(ToolBarSeparator.VERTICAL_BAR, verticalBar);
                        verticalBar = null;
                    } else if (lineBreak != null) {
                        richTextEditor.getToolbar().add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
                        toolBarFeatures.put(ToolBarSeparator.LINE_BREAK, lineBreak);
                        lineBreak = null;
                    }
                    richTextEditor.getToolbar().add((Widget) uie.getUIObject(toolBarFeatureNames[i]));
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
     * {@inheritDoc}
     * 
     * @see Updatable#update()
     */
    public void update()
    {
        selectionCache.clear(false);
        for (Map.Entry<String, UIExtension> entry : toolBarFeatures.entrySet()) {
            try {
                entry.getValue().setEnabled(entry.getKey(), sv.isValid(entry.getKey(), richTextEditor.getTextArea()));
            } catch (Exception e) {
                Console.getInstance().error(e, "Failed to update tool bar: " + entry.getKey());
            }
        }
        selectionCache.clear(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Updatable#canUpdate()
     */
    public boolean canUpdate()
    {
        // NOTE: Currently only the rich text area triggers updates.
        return richTextEditor.getTextArea().isAttached() && richTextEditor.getTextArea().isEnabled();
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

            selectionCache = new Cache(richTextEditor.getTextArea().getElement());
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
     * @return the index of the currently selected tab
     */
    public int getSelectedTab()
    {
        return ui instanceof TabPanel ? ((TabPanel) ui).getTabBar().getSelectedTab() : -1;
    }

    /**
     * Sets the selected tab.
     * 
     * @param index the tab index
     */
    public void setSelectedTab(int index)
    {
        if (ui instanceof TabPanel) {
            ((TabPanel) ui).selectTab(index);
        }
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
