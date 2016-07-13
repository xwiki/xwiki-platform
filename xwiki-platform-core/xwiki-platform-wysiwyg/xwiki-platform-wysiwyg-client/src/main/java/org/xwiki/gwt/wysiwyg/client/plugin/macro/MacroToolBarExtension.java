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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.gwt.user.client.HandlerRegistrationCollection;
import org.xwiki.gwt.wysiwyg.client.Messages;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.FocusWidgetUIExtension;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

/**
 * Extends the tool bar with buttons for easy macro insertion, skipping the macro selection step.
 * 
 * @version $Id$
 */
public class MacroToolBarExtension implements ClickHandler
{
    /**
     * The string used to identify the tool bar extension point.
     */
    private static final String TOOLBAR_ROLE = "toolbar";

    /**
     * The string used prefix macro names on the tool bar.
     */
    private static final String MACRO_NAMESPACE = "macro:";

    /**
     * The macro plug-in associated with this tool bar extension.
     */
    private final MacroPlugin plugin;

    /**
     * The association between tool bar buttons and macro IDs.
     */
    private final Map<PushButton, String> macroIds = new HashMap<PushButton, String>();

    /**
     * The collection of handler registrations used by this extension.
     */
    private final HandlerRegistrationCollection registrations = new HandlerRegistrationCollection();

    /**
     * User interface extension for the editor tool bar.
     */
    private final FocusWidgetUIExtension toolBarExtension = new FocusWidgetUIExtension(TOOLBAR_ROLE);

    /**
     * Creates a new tool bar extension for the given macro plug-in.
     * 
     * @param plugin the macro plug-in that extends the tool bar
     */
    public MacroToolBarExtension(MacroPlugin plugin)
    {
        this.plugin = plugin;

        // Discover macro names between tool bar features and add buttons to insert those macros.
        List<String> toolBarFeatureNames =
            Arrays.asList(plugin.getConfig().getParameter(TOOLBAR_ROLE, "").split("\\s+"));
        for (String featureName : toolBarFeatureNames) {
            if (featureName.startsWith(MACRO_NAMESPACE)) {
                addToolBarButtonToInsertMacro(featureName.substring(MACRO_NAMESPACE.length()));
            }
        }
    }

    /**
     * Adds a button on the tool bar for inserting the specified macro.
     * 
     * @param macroId a macro identifier
     */
    private void addToolBarButtonToInsertMacro(String macroId)
    {
        if (plugin.getTextArea().getCommandManager().isSupported(MacroPlugin.INSERT)) {
            PushButton button = new PushButton();
            registrations.add(button.addClickHandler(this));
            button.setTitle(Messages.INSTANCE.macroInsertTooltip(macroId));
            button.addStyleName("xMacroButton");
            button.addStyleName("macro-" + macroId);
            toolBarExtension.addFeature(MACRO_NAMESPACE + macroId, button);
            macroIds.put(button, macroId);
        }
    }

    /**
     * Destroy this extension.
     */
    public void destroy()
    {
        registrations.removeHandlers();
        for (PushButton button : macroIds.keySet()) {
            button.removeFromParent();
        }
        macroIds.clear();
        toolBarExtension.clearFeatures();
    }

    /**
     * @return the tool bar extension
     */
    public FocusWidgetUIExtension getExtension()
    {
        return toolBarExtension;
    }

    @Override
    public void onClick(ClickEvent event)
    {
        Widget sender = (Widget) event.getSource();
        String macroId = macroIds.get(sender);
        if (macroId != null) {
            plugin.insert(macroId);
        }
    }
}
