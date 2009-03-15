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

import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginFactoryManager;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginManager;
import com.xpn.xwiki.wysiwyg.client.plugin.UIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.DefaultPluginManager;
import com.xpn.xwiki.wysiwyg.client.plugin.separator.ToolBarSeparator;
import com.xpn.xwiki.wysiwyg.client.syntax.SyntaxValidator;
import com.xpn.xwiki.wysiwyg.client.syntax.SyntaxValidatorManager;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.Console;
import com.xpn.xwiki.wysiwyg.client.util.DeferredUpdater;
import com.xpn.xwiki.wysiwyg.client.util.Updatable;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandListener;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandManager;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.DefaultExecutable;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal.StyleWithCssExecutable;

/**
 * The controller part of the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class WysiwygEditor implements Updatable, MouseListener, KeyboardListener, CommandListener, ChangeListener,
    LoadListener
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
                if (iterator.hasNext()) {
                    Map.Entry<String, UIExtension> entry = iterator.next();
                    entry.getValue().setEnabled(entry.getKey(), sv.isValid(entry.getKey(), ui.getTextArea()));
                    return true;
                } else {
                    return false;
                }
            } catch (Throwable t) {
                Console.getInstance().error(t, WysiwygEditor.class.getName(), SyntaxValidationCommand.class.getName());
                return false;
            }
        }
    }

    /**
     * The string used to identify the tool bar extension point.
     */
    private static final String TOOLBAR_ROLE = "toolbar";

    /**
     * The string used to identify the menu bar extension point.
     */
    private static final String MENU_ROLE = "menu";

    /**
     * The name of the syntax configuration parameter.
     */
    private static final String SYNTAX = "syntax";

    /**
     * Default syntax. Can be overwritten from the configuration.
     */
    private static final String DEFAULT_SYNTAX = "xwiki/2.0";

    /**
     * The list of plug-ins that can be loaded by default. Can be overwritten from the configuration.
     */
    private static final String DEFAULT_PLUGINS =
        "separator sync text valign justify list indent undo format font color symbol link image table";

    /**
     * The list of features that can be placed on the tool bar by default. Can be overwritten from the configuration.
     */
    private static final String DEFAULT_TOOLBAR =
        "bold italic underline strikethrough teletype | subscript superscript"
            + " | justifyleft justifycenter justifyright justifyfull | unorderedlist orderedlist | outdent indent"
            + " | undo redo | format | fontname fontsize | forecolor backcolor | hr symbol | link unlink | image"
            + " | inserttable deletetable | insertrowbefore insertrowafter deleterow | insertcolbefore insertcolafter"
            + " deletecol | sync";

    /**
     * The list of default menu entries. Can be overwritten from the configuration.
     */
    private static final String DEFAULT_MENU = "link image table macro";

    /**
     * The regular expression used to express the separator for tool bar and menu bar feature names in configuration.
     */
    private static final String WHITE_SPACE_SEPARATOR = "\\s+";

    /**
     * A reference to the user interface.
     */
    private final RichTextEditor ui;

    /**
     * The configuration object.
     */
    private final Config config;

    /**
     * The plug-in manager.
     */
    private final PluginManager pm;

    /**
     * The syntax validator.
     */
    private final SyntaxValidator sv;

    /**
     * The features that have been placed on the tool bar. The key is the feature name and the value is the widget that
     * has been placed on the tool bar.
     */
    private Map<String, UIExtension> toolBarFeatures;

    /**
     * Schedules updates and executes only the most recent one.
     */
    private final DeferredUpdater updater = new DeferredUpdater(this);

    /**
     * Creates a new WYSIWYG editor.
     * 
     * @param wysiwyg The application context.
     * @param config The configuration object.
     * @param svm The syntax validation manager used for enabling or disabling plugin features.
     * @param pfm The plugin factory manager used to instantiate plugins.
     */
    public WysiwygEditor(Wysiwyg wysiwyg, Config config, SyntaxValidatorManager svm, PluginFactoryManager pfm)
    {
        this.config = config;

        ui = new RichTextEditor();
        ui.addLoadListener(this);
        ui.getConfig().addFlag("wysiwyg");
        ui.getConfig().setParameter(SYNTAX, config.getParameter(SYNTAX, DEFAULT_SYNTAX));
        ui.getTextArea().addMouseListener(this);
        ui.getTextArea().addKeyboardListener(this);
        ui.getTextArea().getCommandManager().addCommandListener(this);
        ui.getTextArea().addChangeListener(this);

        sv = svm.getSyntaxValidator(getSyntax());

        pm = new DefaultPluginManager(wysiwyg, ui.getTextArea(), config);
        pm.setPluginFactoryManager(pfm);
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseDown(Widget, int, int)
     */
    public void onMouseDown(Widget sender, int x, int y)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseEnter(Widget)
     */
    public void onMouseEnter(Widget sender)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseLeave(Widget)
     */
    public void onMouseLeave(Widget sender)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseMove(Widget, int, int)
     */
    public void onMouseMove(Widget sender, int x, int y)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseUp(Widget, int, int)
     */
    public void onMouseUp(Widget sender, int x, int y)
    {
        // We listen to mouse up events instead of clicks because if the user selects text and the end points of the
        // selection are in different DOM nodes the click events are not triggered.
        if (sender == ui.getTextArea()) {
            updater.deferUpdate();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyDown(Widget, char, int)
     */
    public void onKeyDown(Widget sender, char keyCode, int modifier)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char keyCode, int modifier)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyUp(Widget, char, int)
     */
    public void onKeyUp(Widget sender, char keyCode, int modifier)
    {
        if (sender == ui.getTextArea()) {
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
        if (sender == ui.getTextArea().getCommandManager()) {
            updater.deferUpdate();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChangeListener#onChange(Widget)
     */
    public void onChange(Widget sender)
    {
        if (sender == ui.getTextArea()) {
            ui.getConfig().setNameSpace(ui.getTextArea().getName());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onError(Widget)
     */
    public void onError(Widget sender)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onLoad(Widget)
     */
    public void onLoad(Widget sender)
    {
        if (sender == ui) {
            initTextArea();
            loadPlugins();
            fillMenu();
            fillToolBar();
            update();
        }
    }

    /**
     * Initializes the rich text area.
     */
    private void initTextArea()
    {
        // Focus the rich text area to be sure it has reached desing mode.
        getUI().getTextArea().setFocus(true);

        // Make sure the editor uses formatting tags instead of CSS.
        // This is a requirement for HTML to wiki conversion.
        StyleWithCssExecutable styleWithCss = new StyleWithCssExecutable();
        if (styleWithCss.isSupported(getUI().getTextArea())) {
            // If we disable styleWithCss then the indent command will generate blockquote elements even when the caret
            // is inside a list item. Let's keep it enabled until we overwrite the default list support.
            styleWithCss.execute(getUI().getTextArea(), String.valueOf(true));
        }

        // Make sure pressing return generates a new paragraph.
        DefaultExecutable insertBrOnReturn = new DefaultExecutable(Command.INSERT_BR_ON_RETURN.toString());
        if (insertBrOnReturn.isSupported(getUI().getTextArea())) {
            insertBrOnReturn.execute(getUI().getTextArea(), String.valueOf(false));
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
     * Fills the menu of the editor.
     */
    private void fillMenu()
    {
        String[] entries = config.getParameter(MENU_ROLE, DEFAULT_MENU).split(WHITE_SPACE_SEPARATOR);
        for (int i = 0; i < entries.length; i++) {
            UIExtension uie = pm.getUIExtension(MENU_ROLE, entries[i]);
            if (uie != null) {
                ui.getMenu().addItem((MenuItem) uie.getUIObject(entries[i]));
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
        toolBarFeatures = new HashMap<String, UIExtension>();
        for (int i = 0; i < toolBarFeatureNames.length; i++) {
            UIExtension uie = pm.getUIExtension(TOOLBAR_ROLE, toolBarFeatureNames[i]);
            if (uie != null) {
                if (ToolBarSeparator.VERTICAL_BAR.equals(toolBarFeatureNames[i])) {
                    if (emptyGroup && uieNotFound) {
                        continue;
                    } else {
                        if (verticalBar != null) {
                            ui.getToolbar().add((Widget) verticalBar.getUIObject(ToolBarSeparator.VERTICAL_BAR));
                            toolBarFeatures.put(ToolBarSeparator.VERTICAL_BAR, verticalBar);
                        } else if (lineBreak != null) {
                            ui.getToolbar().add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
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
                            ui.getToolbar().add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
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
                        ui.getToolbar().add((Widget) verticalBar.getUIObject(ToolBarSeparator.VERTICAL_BAR));
                        toolBarFeatures.put(ToolBarSeparator.VERTICAL_BAR, verticalBar);
                        verticalBar = null;
                    } else if (lineBreak != null) {
                        ui.getToolbar().add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
                        toolBarFeatures.put(ToolBarSeparator.LINE_BREAK, lineBreak);
                        lineBreak = null;
                    }
                    ui.getToolbar().add((Widget) uie.getUIObject(toolBarFeatureNames[i]));
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
        DeferredCommand.addCommand(new SyntaxValidationCommand());
    }

    /**
     * In a Model-View-Controller architecture {@link RichTextEditor} represents the View component, while this class
     * represents the Controller. The model could be considered the DOM document edited.
     * 
     * @return The user interface of this editor.
     */
    public RichTextEditor getUI()
    {
        return ui;
    }

    /**
     * @return The syntax in which the HTML output of this editor will be converted on the server side.
     */
    public String getSyntax()
    {
        return ui.getConfig().getParameter(SYNTAX);
    }
}
