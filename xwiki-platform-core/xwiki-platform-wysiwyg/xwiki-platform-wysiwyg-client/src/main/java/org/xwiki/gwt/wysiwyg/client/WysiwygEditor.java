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
import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.wysiwyg.client.plugin.PluginFactoryManager;
import org.xwiki.gwt.wysiwyg.client.syntax.SyntaxValidator;
import org.xwiki.gwt.wysiwyg.client.syntax.SyntaxValidatorManager;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
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
     * The interface of the WYSIWYG editor. It can be either a {@link RichTextEditor} or a {@link TabPanel} containing
     * the {@link RichTextEditor} and the {@link PlainTextEditor}.
     */
    private final Widget ui;

    /**
     * The configuration object.
     */
    private final WysiwygEditorConfig config;

    /**
     * The object that will handle the switch between the source editor and the rich text editor.
     */
    private final WysiwygEditorTabSwitchHandler switcher;

    /**
     * Flag indicating if the rich text area was loaded.
     */
    private boolean richTextAreaLoaded;

    /**
     * Flag indicating if the rich text editor was initialized. The rich text editor must be initialized after the rich
     * text area is loaded (after it is attached to the document).
     */
    private boolean richTextEditorInitialized;

    /**
     * Creates a new WYSIWYG editor.
     * 
     * @param config the configuration source
     * @param svm the syntax validation manager used for enabling or disabling plugin features
     * @param pfm the plugin factory manager used to instantiate plugins
     */
    public WysiwygEditor(Config config, SyntaxValidatorManager svm, PluginFactoryManager pfm)
    {
        this(new WysiwygEditorConfig(config), svm, pfm);
    }

    /**
     * Creates a new WYSIWYG editor.
     * 
     * @param config the configuration object
     * @param svm the syntax validation manager used for enabling or disabling plugin features
     * @param pfm the plugin factory manager used to instantiate plugins
     */
    public WysiwygEditor(WysiwygEditorConfig config, SyntaxValidatorManager svm, PluginFactoryManager pfm)
    {
        super(new RichTextEditor(), config.getConfigurationSource(), pfm, getSyntaxValidator(svm, config));

        this.config = config;

        // Initialize the WYSIWYG/Source tab switcher. Even if the editor tabs are disabled, this object is still useful
        // to convert the source text to HTML (when initializing the rich text area).
        switcher = new WysiwygEditorTabSwitchHandler(this);

        // Initialize the user interface.
        if (config.isTabbed()) {
            ui = createTabPanel();
            initializePlainTextArea();
        } else {
            ui = getRichTextEditor();
        }

        // Resize the rich text area and hide the hook.
        Element hook = config.getHook();
        getRichTextEditor().getTextArea().setHeight(Math.max(hook.getOffsetHeight(), 100) + "px");
        hook.getStyle().setDisplay(Display.NONE);
    }

    /**
     * @param svm the object used to retrieve the syntax validator
     * @param config the configuration object
     * @return a validator for the configured syntax, if it exists, otherwise a validator for the default syntax
     */
    private static SyntaxValidator getSyntaxValidator(SyntaxValidatorManager svm, WysiwygEditorConfig config)
    {
        SyntaxValidator sv = svm.getSyntaxValidator(config.getSyntax());
        if (sv == null) {
            sv = svm.getSyntaxValidator(WysiwygEditorConfig.DEFAULT_SYNTAX);
        }
        return sv;
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextEditorController#onLoad(LoadEvent)
     */
    @Override
    public void onLoad(LoadEvent event)
    {
        if (event.getSource() == getRichTextEditor().getTextArea() && !richTextAreaLoaded) {
            richTextAreaLoaded = true;
            boolean inputConverted = config.isInputConverted();
            if (inputConverted && StringUtils.isEmpty(config.getTemplateURL())) {
                // We don't have to convert the input value nor to load a rich text area template.
                getRichTextEditor().getTextArea().setHTML(config.getInputValue());
                super.onLoad(event);
            } else {
                // If input value is already converted then we just load the rich text area template.
                switcher.convertToHTML(inputConverted ? "" : config.getInputValue());
            }
        }
    }

    /**
     * Initializes the rich text editor if it wasn't already initialized.
     */
    protected void maybeInitializeRichTextEditor()
    {
        if (!richTextEditorInitialized) {
            richTextEditorInitialized = true;
            if (config.isInputConverted()) {
                // The rich text area template was loaded. We can now set the inner HTML.
                getRichTextEditor().getTextArea().setHTML(config.getInputValue());
            }
            maybeInitialize();
        }
    }

    /**
     * Initializes the plain text area.
     */
    private void initializePlainTextArea()
    {
        if (config.isInputConverted()) {
            switcher.convertFromHTML(config.getInputValue());
        } else {
            getPlainTextEditor().getTextArea().setText(config.getInputValue());
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
        PlainTextEditor plainTextEditor = new PlainTextEditor(config.getHook());

        TabPanel tabs = new TabPanel();
        tabs.add(getRichTextEditor(), Strings.INSTANCE.wysiwyg());
        tabs.add(plainTextEditor, Strings.INSTANCE.source());
        tabs.setStyleName("xRichTextEditorTabPanel");
        tabs.setAnimationEnabled(false);
        tabs.selectTab(config.getSelectedTabIndex());

        // Enable the appropriate text area based on the type of input (source or HTML).
        boolean richTextAreaEnabled = config.isInputConverted();
        plainTextEditor.getTextArea().setEnabled(!richTextAreaEnabled);
        getRichTextEditor().getTextArea().setEnabled(richTextAreaEnabled);

        saveRegistration(tabs.addBeforeSelectionHandler(switcher));
        saveRegistration(tabs.addSelectionHandler(new SelectionHandler<Integer>()
        {
            public void onSelection(SelectionEvent<Integer> event)
            {
                // Cache the active text area.
                config.setSelectedTabIndex(event.getSelectedItem());
            }
        }));
        saveRegistration(tabs.addSelectionHandler(switcher));

        return tabs;
    }

    /**
     * In a Model-View-Controller architecture the UI represents the View component, while this class represents the
     * Controller. The model could be considered the DOM document edited.
     * 
     * @return the user interface of the editor, i.e. the rich text editor if tabs are disabled, the tab panel otherwise
     */
    public Widget getUI()
    {
        return ui;
    }

    /**
     * Get the plain text editor.
     * 
     * @return the plain text editor, {@code null} if it does not exist
     */
    public PlainTextEditor getPlainTextEditor()
    {
        return config.isTabbed() ? (PlainTextEditor) ((TabPanel) ui).getWidget(WysiwygEditorConfig.SOURCE_TAB_INDEX)
            : null;
    }

    /**
     * @return the index of the currently selected tab
     */
    public int getSelectedTab()
    {
        return ui instanceof TabPanel ? ((TabPanel) ui).getTabBar().getSelectedTab()
            : WysiwygEditorConfig.WYSIWYG_TAB_INDEX;
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
     * @return the configuration object
     */
    public WysiwygEditorConfig getConfig()
    {
        return config;
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
