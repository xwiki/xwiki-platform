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

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.BackForwardCache;
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.wysiwyg.client.plugin.PluginFactoryManager;
import org.xwiki.gwt.wysiwyg.client.syntax.SyntaxValidatorManager;

import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A what-you-see-is-what-you-get rich text editor.
 * 
 * @version $Id$
 */
public class WysiwygEditor extends RichTextEditorController
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
     * The interface of the WYSIWYG editor. It can be either a {@link RichTextEditor} or a {@link TabPanel} containing
     * the {@link RichTextEditor} and the {@link PlainTextEditor}.
     */
    private Widget ui;

    /**
     * Creates a new WYSIWYG editor.
     * 
     * @param config The configuration object.
     * @param svm The syntax validation manager used for enabling or disabling plugin features.
     * @param pfm The plugin factory manager used to instantiate plugins.
     */
    public WysiwygEditor(Config config, SyntaxValidatorManager svm, PluginFactoryManager pfm)
    {
        super(new RichTextEditor(), config, pfm, svm.getSyntaxValidator(config.getParameter("syntax", DEFAULT_SYNTAX)));

        // Initialize the rich text area.
        IFrameElement.as(getRichTextEditor().getTextArea().getElement()).setSrc(
            config.getParameter("inputURL", "about:blank"));
        getRichTextEditor().getTextArea().setHeight(Math.max(getHook().getOffsetHeight(), 100) + "px");

        if (isTabbed()) {
            createTabPanel();
        } else {
            ui = getRichTextEditor();
        }

        getHook().getStyle().setProperty("display", "none");
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextEditorController#initTextArea()
     */
    protected void initTextArea()
    {
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
            super.initTextArea();
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
        Element cacheableElement = (Element) DOM.getElementById(getConfig().getParameter("cacheId", "")).cast();
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
        String defaultEditor = cache.get(cacheKeyActiveTextArea, getConfig().getParameter("defaultEditor"));
        tabs.selectTab(wysiwygTabName.equals(defaultEditor) ? WYSIWYG_TAB_INDEX : SOURCE_TAB_INDEX);

        // We initially disable the rich text area because it is loaded asynchronously and during this time we can't
        // submit its value. We enable it as soon as it finishes loading, if the WYSIWYG tab is selected. By enabling
        // the plain text area we can switch to the source tab editor before the WYSIWYG tab is fully loaded.
        plainTextEditor.getTextArea().setEnabled(true);
        getRichTextEditor().getTextArea().setEnabled(false);

        // Create the object that will handle the switch between the source editor and the rich text editor.
        WysiwygEditorTabSwitchHandler editorSwitcher = new WysiwygEditorTabSwitchHandler(this);

        saveRegistration(tabs.addBeforeSelectionHandler(editorSwitcher));
        saveRegistration(tabs.addSelectionHandler(new SelectionHandler<Integer>()
        {
            public void onSelection(SelectionEvent<Integer> event)
            {
                // Cache the active text area.
                cache.put(cacheKeyActiveTextArea, event.getSelectedItem() == WYSIWYG_TAB_INDEX ? wysiwygTabName
                    : "source");
            }
        }));
        saveRegistration(tabs.addSelectionHandler(editorSwitcher));

        return tabs;
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
     * Get the plain text editor.
     * 
     * @return The plain text editor, null if it does not exist.
     */
    public PlainTextEditor getPlainTextEditor()
    {
        return isTabbed() ? (PlainTextEditor) ((TabPanel) ui).getWidget(SOURCE_TAB_INDEX) : null;
    }

    /**
     * @return the element replaced by the WYSIWYG editor
     */
    private Element getHook()
    {
        return DOM.getElementById(getConfig().getParameter("hookId")).cast();
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
     * {@inheritDoc}
     * 
     * @see RichTextEditorController#destroy()
     */
    public void destroy()
    {
        super.destroy();

        // Detach the user interface.
        ui.removeFromParent();
    }
}
