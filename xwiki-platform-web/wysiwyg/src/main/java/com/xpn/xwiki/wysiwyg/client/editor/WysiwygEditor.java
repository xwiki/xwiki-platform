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
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.LoadListener;
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
import com.xpn.xwiki.wysiwyg.client.util.WithDeferredUpdate;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandListener;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.CommandManager;

public class WysiwygEditor implements WithDeferredUpdate, ClickListener, KeyboardListener, CommandListener,
    ChangeListener, LoadListener
{
    private class SyntaxValidationCommand implements IncrementalCommand
    {
        private final Iterator<Map.Entry<String, UIExtension>> iterator;

        public SyntaxValidationCommand()
        {
            iterator = toolBarFeatures.entrySet().iterator();
        }

        public boolean execute()
        {
            if (iterator.hasNext()) {
                Map.Entry<String, UIExtension> entry = iterator.next();
                entry.getValue().setEnabled(entry.getKey(), sv.isValid(entry.getKey(), ui.getTextArea()));
                return true;
            } else {
                return false;
            }
        }
    }

    private static final String DEFAULT_SYNTAX = "xwiki/2.0";

    private static final String DEFAULT_PLUGINS =
        "separator sync text valign justify list indent undo format font color symbol";

    private static final String DEFAULT_TOOLBAR =
        "bold italic underline strikethrough teletype | subscript superscript | justifyleft justifycenter justifyright justifyfull | unorderedlist orderedlist | outdent indent | undo redo | format | fontname fontsize | forecolor backcolor | hr symbol | sync";

    private final RichTextEditor ui;

    private final Config config;

    private final PluginManager pm;

    private final SyntaxValidator sv;

    private Map<String, UIExtension> toolBarFeatures;

    private boolean loaded = false;

    private long updateIndex = -1;

    public WysiwygEditor(Wysiwyg wysiwyg, Config config, SyntaxValidatorManager svm, PluginFactoryManager pfm)
    {
        this.config = config;

        ui = new RichTextEditor();
        ui.addLoadListener(this);
        ui.getConfig().addFlag("wysiwyg");
        ui.getConfig().setParameter("syntax", config.getParameter("syntax", DEFAULT_SYNTAX));
        ui.getTextArea().addClickListener(this);
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
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == ui.getTextArea()) {
            deferUpdate();
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
            deferUpdate();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see CommandListener#onCommand(CommandManager, Command, String)
     */
    public void onCommand(CommandManager sender, Command command, String param)
    {
        if (sender == ui.getTextArea().getCommandManager()) {
            deferUpdate();
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
            loadPlugins();
        }
    }

    private void loadPlugins()
    {
        if (this.toolBarFeatures != null) {
            return;
        }

        String[] pluginNames = config.getParameter("plugins", DEFAULT_PLUGINS).split("\\s+");
        for (int i = 0; i < pluginNames.length; i++) {
            pm.load(pluginNames[i]);
        }

        final String[] toolBarFeatures = config.getParameter("toolbar", DEFAULT_TOOLBAR).split("\\s+");
        boolean emptyGroup = true;
        boolean emptyLine = true;
        boolean uieNotFound = false;
        UIExtension verticalBar = null;
        UIExtension lineBreak = null;
        this.toolBarFeatures = new HashMap<String, UIExtension>();
        for (int i = 0; i < toolBarFeatures.length; i++) {
            UIExtension uie = pm.getUIExtension("toolbar", toolBarFeatures[i]);
            if (uie != null) {
                if (ToolBarSeparator.VERTICAL_BAR.equals(toolBarFeatures[i])) {
                    if (emptyGroup && uieNotFound) {
                        continue;
                    } else {
                        if (verticalBar != null) {
                            ui.getToolbar().add((Widget) verticalBar.getUIObject(ToolBarSeparator.VERTICAL_BAR));
                            this.toolBarFeatures.put(ToolBarSeparator.VERTICAL_BAR, verticalBar);
                        } else if (lineBreak != null) {
                            ui.getToolbar().add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
                            this.toolBarFeatures.put(ToolBarSeparator.LINE_BREAK, lineBreak);
                            lineBreak = null;
                        }
                        verticalBar = uie;
                        emptyGroup = true;
                        uieNotFound = false;
                    }
                } else if (ToolBarSeparator.LINE_BREAK.equals(toolBarFeatures[i])) {
                    if (emptyLine && uieNotFound) {
                        continue;
                    } else {
                        if (lineBreak != null) {
                            ui.getToolbar().add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
                            this.toolBarFeatures.put(ToolBarSeparator.LINE_BREAK, lineBreak);
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
                        this.toolBarFeatures.put(ToolBarSeparator.VERTICAL_BAR, verticalBar);
                        verticalBar = null;
                    } else if (lineBreak != null) {
                        ui.getToolbar().add((Widget) lineBreak.getUIObject(ToolBarSeparator.LINE_BREAK));
                        this.toolBarFeatures.put(ToolBarSeparator.LINE_BREAK, lineBreak);
                        lineBreak = null;
                    }
                    ui.getToolbar().add((Widget) uie.getUIObject(toolBarFeatures[i]));
                    this.toolBarFeatures.put(toolBarFeatures[i], uie);
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
     * @see WithDeferredUpdate#getUpdateIndex()
     */
    public long getUpdateIndex()
    {
        return updateIndex;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WithDeferredUpdate#incUpdateIndex()
     */
    public long incUpdateIndex()
    {
        return ++updateIndex;
    }

    private void deferUpdate()
    {
        DeferredCommand.addCommand(new UpdateCommand(this));
    }

    /**
     * {@inheritDoc}
     * 
     * @see WithDeferredUpdate#onUpdate()
     */
    public void onUpdate()
    {
        if (!loaded) {
            loaded = true;

            // Make sure the editor uses formatting tags instead of CSS.
            // This is a requirement for HTML to wiki conversion.
            getUI().getTextArea().getCommandManager().execute(Command.STYLE_WITH_CSS, false);

            // Make sure pressing return generates a new paragraph.
            getUI().getTextArea().getCommandManager().execute(Command.INSERT_BR_ON_RETURN, false);
        }

        DeferredCommand.addCommand(new SyntaxValidationCommand());
    }

    public RichTextEditor getUI()
    {
        return ui;
    }

    public String getSyntax()
    {
        return ui.getConfig().getParameter("syntax");
    }
}
