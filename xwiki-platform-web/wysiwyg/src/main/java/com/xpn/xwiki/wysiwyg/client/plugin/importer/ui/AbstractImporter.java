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

package com.xpn.xwiki.wysiwyg.client.plugin.importer.ui;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.WysiwygServiceAsync;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.importer.ImporterListener;
import com.xpn.xwiki.wysiwyg.client.util.Config;

/**
 * Base class for both file / clipboard importers.
 * 
 * @version $Id$
 * @since 2.0.1
 */
public abstract class AbstractImporter extends Composite implements ClickHandler
{
    /**
     * CSS style name for indicating busy waiting.
     */
    protected static final String STYLE_LOADING = "loading";
    
    /**
     * Wysiwyg configuration.
     */
    protected Config config;

    /**
     * The importer listener.
     */
    protected ImporterListener importerListener;

    /**
     * {@link WysiwygServiceAsync} used for invoking remote GWT RPC calls.
     */
    protected WysiwygServiceAsync wysiwygService;

    /**
     * Base panel which sits on top of the composite.
     */
    private Panel basePanel;

    /**
     * The content panel sitting on top of the base panel.
     */
    private Panel contentPanel;

    /**
     * Filter Styles check box.
     */
    private CheckBox filterStylesCheckBox;

    /**
     * Import button.
     */
    private Button importButton;

    /**
     * Creates a new importer.
     * 
     * @param wysiwygConfig wysiwyg configuration.
     * @param importerListener import process listner.
     */
    public AbstractImporter(Config wysiwygConfig, ImporterListener importerListener)
    {
        this.config = wysiwygConfig;
        this.importerListener = importerListener;
        this.wysiwygService = WysiwygService.Singleton.getInstance();

        // Create the content panel.
        this.contentPanel = createContentPanel();

        // Append the button panel.
        contentPanel.add(createButtonPanel());

        // Finally add the content panel to base panel.
        this.basePanel = new FlowPanel();
        basePanel.add(contentPanel);
        initWidget(basePanel);
    }

    /**
     * Creates the button panel.
     * 
     * @return the button panel.
     */
    private Panel createButtonPanel()
    {
        Panel buttonPanel = new FlowPanel();
        filterStylesCheckBox = new CheckBox(Strings.INSTANCE.importerFilterStylesCheckBoxCaption());
        buttonPanel.add(filterStylesCheckBox);
        importButton = new Button(Strings.INSTANCE.importerImportButtonCaption());
        importButton.addStyleName("xImporterImportButton");
        importButton.addClickHandler(this);
        buttonPanel.add(importButton);
        // Add a clear floats panel.
        Panel fakeClearPanel = new FlowPanel();
        fakeClearPanel.addStyleName("clearfloats");
        buttonPanel.add(fakeClearPanel);
        return buttonPanel;
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(ClickEvent event)
    {
        if (event.getSource() == importButton) {
            onImportButtonClick();
        }
    }

    /**
     * Delegate method for creating content panel.
     * 
     * @return content panel.
     */
    protected abstract Panel createContentPanel();

    /**
     * Delegate method for handling the import operation.
     */
    protected abstract void onImportButtonClick();

    /**
     * Prepares the cleaning parameters map.
     * 
     * @return a {@link Map} with cleaning parameters for office importer.
     */
    protected Map<String, String> getHTMLCleaningParams()
    {
        Map<String, String> params = new HashMap<String, String>();
        if (filterStylesCheckBox.getValue()) {
            params.put("filterStyles", "strict");
        }
        // For Office2007: Office2007 generates an xhtml document (when copied) which has attributes and tags of
        // several namespaces. But the document itself doesn't contain the namespace definitions, which causes
        // the HTMLCleaner (the DomSerializer) to fail while performing it's operations. As a workaround we
        // force HTMLCleaner to avoid parsing of namespace information.
        params.put("namespacesAware", Boolean.toString(false));
        return params;
    }

    /**
     * Sets / Unsets this importer UI to 'busy' mode.
     * 
     * @param busy busy or not.
     */
    protected void setBusy(boolean busy)
    {
        contentPanel.setVisible(!busy);
        if (busy) {
            basePanel.addStyleName(STYLE_LOADING);
        } else {
            basePanel.removeStyleName(STYLE_LOADING);
        }
    }
}
