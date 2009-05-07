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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.TabPanel;
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
public class WysiwygEditor implements Updatable, MouseListener, KeyboardListener, CommandListener, LoadListener
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
                    entry.getValue().setEnabled(entry.getKey(), sv.isValid(entry.getKey(), richTextEditor.getTextArea()));
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
     * The CSS class name used to make an element invisible.
     */
    protected static final String STYLE_NAME_INVISIBLE = "invisible";    
    
    /**
     * The CSS class name used to display a spinner in the middle of an element.
     */
    protected static final String STYLE_NAME_LOADING = "loading";
    
    /**
     * WYWISYWG tab index in the TabPanel.
     */
    protected static final int WYSIWYG_TAB_INDEX = 0;

    /**
     * Wiki tab index in the TabPanel.
     */
    protected static final int WIKI_TAB_INDEX = 1;

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
     */
    private static final String DEFAULT_PLUGINS =
        "submit line separator sync text valign justify list indent history format font color symbol link image table";

    /**
     * The list of features that can be placed on the tool bar by default. Can be overwritten from the configuration.
     */
    private static final String DEFAULT_TOOLBAR =
        "bold italic underline strikethrough teletype | subscript superscript"
            + " | justifyleft justifycenter justifyright justifyfull | unorderedlist orderedlist | outdent indent"
            + " | undo redo | format | fontname fontsize forecolor backcolor | hr removeformat symbol | link unlink"
            + " | image | inserttable deletetable | insertrowbefore insertrowafter deleterow"
            + " | insertcolbefore insertcolafter deletecol | sync";

    /**
     * The list of default menu entries. Can be overwritten from the configuration.
     */
    private static final String DEFAULT_MENU = "link image table macro";

    /**
     * The regular expression used to express the separator for tool bar and menu bar feature names in configuration.
     */
    private static final String WHITE_SPACE_SEPARATOR = "\\s+";
    
    /**
     * The command used to store the value of the rich text area before submitting the including form.
     */
    private static final Command SUBMIT = new Command("submit");

    /**
     * Main Container.
     */
    private final FlowPanel ui;
        
    /**
     * The WYSIWYG entry point.
     */
    private final Wysiwyg wysiwyg;

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
     * Flag set to true when the editor has a TabBar.
     */
    private final boolean isTabbed;
    
    /**
     * A reference to the rich text editor panel.
     */
    private final FlowPanel richTextEditorWrapper;
    
    /**
     * Schedules updates and executes only the most recent one.
     */
    private final DeferredUpdater updater = new DeferredUpdater(this);
    
    /**
     * Listen to events and takes the appropriate actions.
     */
    private final WysiwygEditorListener listener = new WysiwygEditorListener(this);
    
    /**
     * Height of the editor.
     */
    private final String height;
    
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
     * A reference to the plain text editor.
     */
    private PlainTextEditor plainTextEditor;

    /**
     * The features that have been placed on the tool bar. The key is the feature name and the value is the widget that
     * has been placed on the tool bar.
     */
    private Map<String, UIExtension> toolBarFeatures;

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
        this.wysiwyg = wysiwyg;
        this.config = config;
        this.svm = svm;
        this.pfm = pfm;               
        
        TextAreaElement originalTextArea = TextAreaElement.as(DOM.getElementById(config.getParameter("hookId")));
        height = Math.max(originalTextArea.getOffsetHeight(), 100) + "px";
        
        ui = new FlowPanel();
        richTextEditorWrapper = new FlowPanel();
        
        if (Boolean.TRUE.toString().equals(config.getParameter("displayTabs"))) {
            isTabbed = true;
            plainTextEditor = new PlainTextEditor(originalTextArea);            
            if ("wysiwyg".equals(config.getParameter("defaultEditor"))) {                
                ui.add(createTabPanel(true));
                // Call the getter to be sure the RichTextEditor is created.
                getRichTextEditor();
            } else {                       
                ui.add(createTabPanel(false));
                plainTextEditor.setFocus(true);                
            }
        } else {
            isTabbed = false;            
            plainTextEditor = null;
            originalTextArea.getStyle().setProperty("display", "none");
            // Call the getter to be sure the RichTextEditor is created.
            getRichTextEditor();
            ui.add(richTextEditorWrapper);
        }
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
        if (sender == richTextEditor.getTextArea()) {
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
        if (sender == richTextEditor.getTextArea()) {
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
        if (sender == richTextEditor) {
            initRichTextArea();
            loadPlugins();
            initEditor();
            fillMenu();
            fillToolBar();
            richTextEditor.getTextArea().getCommandManager().execute(new Command("update"));
        }
    }

    /**
     * Initializes the rich text area.
     */
    private void initRichTextArea()
    {
        // Focus the rich text area to be sure it has reached design mode.
        getRichTextEditor().getTextArea().setFocus(true);

        // Make sure the editor uses formatting tags instead of CSS.
        // This is a requirement for HTML to wiki conversion.
        StyleWithCssExecutable styleWithCss = new StyleWithCssExecutable();
        if (styleWithCss.isSupported(getRichTextEditor().getTextArea())) {
            // If we disable styleWithCss then the indent command will generate blockquote elements even when the caret
            // is inside a list item. Let's keep it enabled until we overwrite the default list support.
            styleWithCss.execute(getRichTextEditor().getTextArea(), String.valueOf(true));
        }

        // Make sure pressing return generates a new paragraph.
        DefaultExecutable insertBrOnReturn = new DefaultExecutable(Command.INSERT_BR_ON_RETURN.toString());
        if (insertBrOnReturn.isSupported(getRichTextEditor().getTextArea())) {
            insertBrOnReturn.execute(getRichTextEditor().getTextArea(), String.valueOf(false));
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
     * Build the editor tab panel.
     * This panel contains two tabs, one for the WYSIWYG editor and one for the wiki editor.
     * 
     * @param defaultIsWysiwyg True if the WYSIWYG editor must be displayed by default.  
     * @return The newly created tab panel.
     */
    public TabPanel createTabPanel(boolean defaultIsWysiwyg)
    {
        TabPanel tabs = new TabPanel();
        
        tabs.add(richTextEditorWrapper, Strings.INSTANCE.wysiwyg());
        tabs.add(plainTextEditor, Strings.INSTANCE.wiki());        
        tabs.setStyleName("xRichTextEditorTabPanel");        
        
        if (defaultIsWysiwyg) {
            tabs.selectTab(WYSIWYG_TAB_INDEX);            
        } else {
            tabs.selectTab(WIKI_TAB_INDEX);
        }
        
        tabs.addTabListener(listener);
                
        return tabs;
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
        toolBarFeatures = new HashMap<String, UIExtension>();
        for (int i = 0; i < toolBarFeatureNames.length; i++) {
            UIExtension uie = pm.getUIExtension(TOOLBAR_ROLE, toolBarFeatureNames[i]);
            if (uie != null) {
                if (ToolBarSeparator.VERTICAL_BAR.equals(toolBarFeatureNames[i])) {
                    if (emptyGroup && uieNotFound) {
                        continue;
                    } else {
                        if (verticalBar != null) {
                            getRichTextEditor().getToolbar().add((Widget) verticalBar.getUIObject(ToolBarSeparator.VERTICAL_BAR));
                            toolBarFeatures.put(ToolBarSeparator.VERTICAL_BAR, verticalBar);
                        } else if (lineBreak != null) {
                            getRichTextEditor().getToolbar().add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
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
                            getRichTextEditor().getToolbar().add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
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
                        getRichTextEditor().getToolbar().add((Widget) verticalBar.getUIObject(ToolBarSeparator.VERTICAL_BAR));
                        toolBarFeatures.put(ToolBarSeparator.VERTICAL_BAR, verticalBar);
                        verticalBar = null;
                    } else if (lineBreak != null) {
                        getRichTextEditor().getToolbar().add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
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
        if (isTabbed) {            
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
     * In a Model-View-Controller architecture the UI represents the View component, while this class
     * represents the Controller. The model could be considered the DOM document edited.
     * 
     * @return The editor User Interface main panel.
     */
    public FlowPanel getUI()
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
            richTextEditor.addLoadListener(this);
            richTextEditor.getTextArea().addMouseListener(this);
            richTextEditor.getTextArea().addKeyboardListener(this);
            richTextEditor.getTextArea().getCommandManager().addCommandListener(this);
            IFrameElement.as(richTextEditor.getTextArea().getElement()).setSrc(config.getParameter("inputURL", "about:blank"));
            richTextEditor.getTextArea().setHeight(height);

            richTextEditorWrapper.add(richTextEditor);
    
            sv = svm.getSyntaxValidator(getConfig().getParameter("syntax"));
    
            pm = new DefaultPluginManager(wysiwyg, richTextEditor.getTextArea(), config);
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
        return plainTextEditor;
    }

    /**
     * @return this editor's configuration object
     */
    public Config getConfig()
    {
        return config;
    }
}
