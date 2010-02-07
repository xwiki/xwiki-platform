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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.gwt.user.client.ui.ToolBar;
import org.xwiki.gwt.wysiwyg.client.plugin.PluginManager;
import org.xwiki.gwt.wysiwyg.client.plugin.UIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.MockPluginManager;
import org.xwiki.gwt.wysiwyg.client.plugin.internal.MockUIExtension;
import org.xwiki.gwt.wysiwyg.client.plugin.separator.ToolBarSeparator;


/**
 * Unit tests for {@link ToolBarController}.
 * 
 * @version $Id$
 */
public class ToolBarControllerTest extends WysiwygTestCase
{
    /**
     * The tool bar controller being tested.
     */
    private ToolBarController toolBarController;

    /**
     * The plugin manager use to look up {@link UIExtension}s.
     */
    private PluginManager pluginManager;

    /**
     * {@inheritDoc}
     * 
     * @see WysiwygTestCase#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        Map<String, UIExtension> toolBarExtensions = new HashMap<String, UIExtension>();
        toolBarExtensions.put(ToolBarSeparator.VERTICAL_BAR, new MockUIExtension(ToolBarController.TOOLBAR_ROLE, ""));
        toolBarExtensions.put(ToolBarSeparator.LINE_BREAK, new MockUIExtension(ToolBarController.TOOLBAR_ROLE, ""));
        toolBarExtensions.put("bold", new MockUIExtension(ToolBarController.TOOLBAR_ROLE, ""));
        toolBarExtensions.put("italic", new MockUIExtension(ToolBarController.TOOLBAR_ROLE, ""));
        toolBarExtensions.put("underline", new MockUIExtension(ToolBarController.TOOLBAR_ROLE, ""));
        toolBarExtensions.put("strike", new MockUIExtension(ToolBarController.TOOLBAR_ROLE, ""));

        Map<String, Map<String, UIExtension>> uiExtensions = new HashMap<String, Map<String, UIExtension>>();
        uiExtensions.put(ToolBarController.TOOLBAR_ROLE, toolBarExtensions);

        pluginManager = new MockPluginManager(uiExtensions);

        toolBarController = new ToolBarController(new ToolBar());
    }

    /**
     * Unit test for {@link ToolBarController#split(String, PluginManager)}.
     */
    public void testSplit()
    {
        String toolBar = "bold | underline strike / italic";
        assertEquals(toolBar, join(toolBarController.split(toolBar, pluginManager)));
    }

    /**
     * Tests that {@link ToolBarController#split(String, PluginManager)} ignores white spaces.
     */
    public void testSplitIgnoresWhiteSpaces()
    {
        assertEquals("bold strike", join(toolBarController.split(" \tbold   strike\n", pluginManager)));
    }

    /**
     * Tests that {@link ToolBarController#split(String, PluginManager)} ignores unavailable features.
     */
    public void testSplitIgnoresUnavailableFeatures()
    {
        assertEquals("underline bold", join(toolBarController.split("underline foo bold", pluginManager)));
    }

    /**
     * Tests that {@link ToolBarController#split(String, PluginManager)} ignores useless separators.
     */
    public void testSplitIgnoresUselessSeparators()
    {
        assertEquals("italic / strike | bold", join(toolBarController.split("| | / italic / / | strike | | bold |",
            pluginManager)));
    }

    /**
     * Tests that {@link ToolBarController#split(String, PluginManager)} ignores empty groups.
     */
    public void testSplitIgnoresEmptyGroups()
    {
        assertEquals("italic | bold", join(toolBarController.split("italic | bar | | bold /", pluginManager)));
    }

    /**
     * Joins the given list of features.
     * 
     * @param featureNames the list of feature names
     * @return a string that can be used to fill the tool bar
     */
    private String join(List<String> featureNames)
    {
        StringBuffer result = new StringBuffer();
        for (String featureName : featureNames) {
            result.append(result.length() > 0 ? " " : "");
            result.append(featureName);
        }
        return result.toString();
    }
}
