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
package org.xwiki.gwt.wysiwyg.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.gwt.user.client.Cache;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.Console;
import org.xwiki.gwt.user.client.ui.ToolBar;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.wysiwyg.client.plugin.PluginManager;
import org.xwiki.gwt.wysiwyg.client.plugin.UIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.separator.ToolBarSeparator;
import org.xwiki.gwt.wysiwyg.client.syntax.SyntaxValidator;

import com.google.gwt.user.client.ui.Widget;

/**
 * {@link ToolBar} controller.
 * 
 * @version $Id$
 */
public class ToolBarController
{
    /**
     * The string used to identify the tool bar extension point.
     */
    public static final String TOOLBAR_ROLE = "toolbar";

    /**
     * The list of features this controller will attempt to place on the tool bar by default if the configuration
     * doesn't specify the tool bar features.
     */
    public static final String DEFAULT_TOOLBAR_FEATURES =
        "bold italic underline strikethrough teletype | subscript superscript"
            + " | justifyleft justifycenter justifyright justifyfull | unorderedlist orderedlist | outdent indent"
            + " | undo redo | format | fontname fontsize forecolor backcolor | hr removeformat symbol";

    /**
     * The underlying tool bar that is managed by this object.
     */
    private final ToolBar toolBar;

    /**
     * The features that have been placed on the tool bar. The key is the feature name and the value is the widget that
     * has been placed on the tool bar.
     */
    private final Map<String, UIExtension> toolBarFeatures = new HashMap<String, UIExtension>();

    /**
     * Creates a new tool bar controller.
     * 
     * @param toolBar the tool bar to be managed
     */
    public ToolBarController(ToolBar toolBar)
    {
        this.toolBar = toolBar;
    }

    /**
     * Fills the tool bar with the features specified in the configuration.
     * 
     * @param config the configuration object
     * @param pluginManager the object used to access the tool bar {@link UIExtension}s
     */
    public void fill(Config config, PluginManager pluginManager)
    {
        toolBar.clear();
        toolBarFeatures.clear();
        for (String featureName : split(config.getParameter(TOOLBAR_ROLE, DEFAULT_TOOLBAR_FEATURES), pluginManager)) {
            UIExtension extension = pluginManager.getUIExtension(TOOLBAR_ROLE, featureName);
            toolBarFeatures.put(featureName, extension);
            toolBar.add((Widget) extension.getUIObject(featureName));
        }
    }

    /**
     * Updates the tool bar state, i.e. disables/enables tool bar features, based on the given set of rules and the
     * current state of the rich text area.
     * 
     * @param richTextArea the rich text area whose state is used to determine if a feature must be enabled or disabled
     * @param syntaxValidator the object used to assert if a feature must be enabled or disabled in the current state of
     *            the rich text area
     */
    public void update(RichTextArea richTextArea, SyntaxValidator syntaxValidator)
    {
        Cache selectionCache = new Cache(richTextArea.getElement());
        selectionCache.clear(false);
        for (Map.Entry<String, UIExtension> entry : toolBarFeatures.entrySet()) {
            try {
                entry.getValue().setEnabled(entry.getKey(), syntaxValidator.isValid(entry.getKey(), richTextArea));
            } catch (Exception e) {
                Console.getInstance().error(e, "Failed to update tool bar: " + entry.getKey());
            }
        }
        selectionCache.clear(true);
    }

    /**
     * Splits a string representing the tool bar feature list into its components, i.e. feature names, and removes
     * useless separators (e.g. to avoid empty lines or empty groups) and unavailable features.
     * 
     * @param toolBar a string listing the tool bar features separated by {@link ToolBarSeparator#VERTICAL_BAR} or
     *            {@link ToolBarSeparator#LINE_BREAK}
     * @param pluginManager the object used to check which tool bar features are available
     * @return the list of available tool bar features in the order they appear on the tool bar
     */
    public List<String> split(String toolBar, PluginManager pluginManager)
    {
        List<String> features = new ArrayList<String>();
        String[] toolBarFeatureNames = toolBar.split("\\s+");
        boolean pendingLineBreak = false;
        boolean pendingGroupEnd = false;
        for (int i = 0; i < toolBarFeatureNames.length; i++) {
            String featureName = toolBarFeatureNames[i];
            UIExtension uie = pluginManager.getUIExtension(TOOLBAR_ROLE, featureName);
            if (uie == null) {
                continue;
            } else if (ToolBarSeparator.VERTICAL_BAR.equals(featureName)) {
                pendingGroupEnd = features.size() > 0;
            } else if (ToolBarSeparator.LINE_BREAK.equals(featureName)) {
                pendingLineBreak = features.size() > 0;
            } else {
                if (pendingLineBreak) {
                    features.add(ToolBarSeparator.LINE_BREAK);
                } else if (pendingGroupEnd) {
                    features.add(ToolBarSeparator.VERTICAL_BAR);
                }
                pendingLineBreak = false;
                pendingGroupEnd = false;
                features.add(featureName);
            }
        }
        return features;
    }

    /**
     * Destroys this tool bar controller.
     */
    public void destroy()
    {
        toolBarFeatures.clear();
    }
}
