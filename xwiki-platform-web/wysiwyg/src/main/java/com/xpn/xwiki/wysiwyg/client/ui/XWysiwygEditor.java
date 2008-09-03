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
package com.xpn.xwiki.wysiwyg.client.ui;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.Wysiwyg;
import com.xpn.xwiki.wysiwyg.client.plugin.Config;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginFactoryManager;
import com.xpn.xwiki.wysiwyg.client.plugin.PluginManager;
import com.xpn.xwiki.wysiwyg.client.plugin.UIExtension;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.DefaultPluginManager;
import com.xpn.xwiki.wysiwyg.client.syntax.SyntaxValidator;
import com.xpn.xwiki.wysiwyg.client.syntax.SyntaxValidatorManager;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandListener;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandManager;

public class XWysiwygEditor implements ClickListener, KeyboardListener, CommandListener
{
    private static final String DEFAULT_SYNTAX = "xwiki/2.0";

    private static final String DEFAULT_PLUGINS =
        "separator sync text valign justify list indent undo format font color";

    private static final String DEFAULT_TOOLBAR =
        "bold italic underline strikethrough | subscript superscript | justifyleft justifycenter justifyright justifyfull | unorderedlist orderedlist | outdent indent | undo redo | format | fontname fontsize | forecolor backcolor | sync";

    private final XRichTextEditor ui;

    private final PluginManager pm;

    private final SyntaxValidator sv;

    private final Set<String> toolBarFeatures;

    private boolean loaded = false;

    public XWysiwygEditor(Wysiwyg wysiwyg, Config config, SyntaxValidatorManager svm, PluginFactoryManager pfm)
    {
        ui = new XRichTextEditor();
        ui.getTextArea().addClickListener(this);
        ui.getTextArea().addKeyboardListener(this);
        ui.getTextArea().getCommandManager().addCommandListener(this);

        String syntax = config.getParameter("syntax", DEFAULT_SYNTAX);
        sv = svm.getSyntaxValidator(syntax);

        pm = new DefaultPluginManager(wysiwyg, ui.getTextArea(), config);
        pm.setPluginFactoryManager(pfm);

        String[] pluginNames = config.getParameter("plugins", DEFAULT_PLUGINS).split("\\s+");
        for (int i = 0; i < pluginNames.length; i++) {
            pm.load(pluginNames[i]);
        }

        final String[] toolBarFeatures = config.getParameter("toolbar", DEFAULT_TOOLBAR).split("\\s+");
        this.toolBarFeatures = new HashSet<String>();
        for (int i = 0; i < toolBarFeatures.length; i++) {
            UIExtension uie = pm.getUIExtension("toolbar", toolBarFeatures[i]);
            if (uie != null) {
                ui.getToolbar().add((Widget) uie.getUIObject(toolBarFeatures[i]));
                this.toolBarFeatures.add(toolBarFeatures[i]);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == ui.getTextArea()) {
            onUpdate();
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
            onUpdate();
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
            onUpdate();
        }
    }

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

        for (String feature : toolBarFeatures) {
            UIExtension uie = pm.getUIExtension("toolbar", feature);
            uie.setEnabled(feature, sv.isValid(feature, ui.getTextArea()));
        }
    }

    public XRichTextEditor getUI()
    {
        return ui;
    }

    public String getSyntax()
    {
        return sv.getSyntax();
    }
}
