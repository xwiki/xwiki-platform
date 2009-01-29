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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.WysiwygServiceAsync;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.widget.CompositeDialogBox;

/**
 * Office Importer wysiwyg dialog box.
 * 
 * @version $Id$
 */
public class ImporterDialog extends CompositeDialogBox implements AsyncCallback<String>, ClickListener
{
    /**
     * 'loading' style name.
     */
    private static final String STYLE_LOADING = "loading";

    /**
     * CSS style used by tab panel.
     */
    private static final String STYLE_TAB_PANEL = "xImporterTabPanel";

    /**
     * Container panel.
     */
    private VerticalPanel mainPanel;

    /**
     * Hidden panel for displaying progress.
     */
    private HorizontalPanel progressPanel;

    /**
     * Tab panel.
     */
    private TabPanel tabPanel = new TabPanel();

    /**
     * File import tab.
     */
    private FileImportTab fileImportTab = new FileImportTab();

    /**
     * Clipboard (copy-paste) import tab.
     */
    private ClipboardImportTab clipboardImportTab = new ClipboardImportTab();

    /**
     * The button panel.
     */
    private ButtonPanel buttonPanel = new ButtonPanel(this);

    /**
     * Resulting xhtml fragment of the import operation.
     */
    private String result;

    /**
     * Default constructor.
     */
    public ImporterDialog()
    {
        // Dialog box.
        super(false, true);
        getDialog().setText(Strings.INSTANCE.importerCaption());

        // Main container panel.
        mainPanel = new VerticalPanel();
        mainPanel.setSpacing(5);

        // Progress panel
        progressPanel = new HorizontalPanel();
        progressPanel.addStyleName(STYLE_TAB_PANEL);
        progressPanel.addStyleName(STYLE_LOADING);
        progressPanel.setVisible(false);
        mainPanel.add(progressPanel);

        // Tab panel.
        tabPanel.add(clipboardImportTab, Strings.INSTANCE.importerClipboardTabCaption());
        tabPanel.add(fileImportTab, Strings.INSTANCE.importerFileTabCaption());
        tabPanel.selectTab(0);
        tabPanel.addStyleName(STYLE_TAB_PANEL);
        mainPanel.add(tabPanel);

        // Button panel.
        mainPanel.add(buttonPanel);

        // Finalize.
        initWidget(mainPanel);
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(Widget sender)
    {
        if (sender == buttonPanel.getImportButton()) {
            WysiwygServiceAsync wysiwygService = WysiwygService.Singleton.getInstance();
            String htmlPaste = clipboardImportTab.getHtmlPaste();
            if (clipboardImportTab.isVisible() && !htmlPaste.trim().equals("")) {
                startProgress();
                wysiwygService.cleanOfficeHTML(htmlPaste, "wysiwyg", getCleaningParams(), this);
            }
        } else if (sender == buttonPanel.getCancelButton()) {
            hide();
        }
    }

    /**
     * Prepares the cleaning parameters map.
     * 
     * @return a {@link Map} with cleaning parameters for office importer.
     */
    private Map<String, String> getCleaningParams()
    {
        Map<String, String> params = new HashMap<String, String>();
        if (buttonPanel.getFilterStylesCheckBox().isChecked()) {
            params.put("filterStyles", "strict");
        }
        // For Office2007: Office2007 generates an xhtml document (when copied) which has attributes and tags of
        // several namespaces. But the document itself doesn't contain the namespace definitions, which causes
        // the HTMLCleaner (the DomSerializer) to fail while performing it's operations. As a workaround we
        // force HTMLCleaner to avoid parsing of namespace information.
        params.put("namespacesAware", "false");
        return params;
    }

    /**
     * Starts progress display.
     */
    private void startProgress()
    {
        tabPanel.setVisible(false);
        enableControls(false);        
        progressPanel.setVisible(true);
    }

    /**
     * Stops progress display.
     */
    private void stopProgress()
    {
        progressPanel.setVisible(false);        
        tabPanel.setVisible(true);
        enableControls(true);
    }
    
    /**
     * Enables / Disables the control buttons.
     * 
     * @param enable if true, all controls will be enabled.
     */
    private void enableControls(boolean enable) {
        buttonPanel.getFilterStylesCheckBox().setEnabled(enable);
        buttonPanel.getImportButton().setEnabled(enable);
        buttonPanel.getCancelButton().setEnabled(enable);
    }

    /**
     * {@inheritDoc}
     */
    public void onSuccess(String result)
    {
        stopProgress();
        this.result = result;
        hide();
    }

    /**
     * {@inheritDoc}
     */
    public void onFailure(Throwable caught)
    {
        stopProgress();
        Window.alert(caught.getMessage());
        hide();
    }

    /**
     * {@inheritDoc}
     */
    public void center()
    {
        clipboardImportTab.resetEditor();
        enableControls(true);
        super.center();
    }

    /**
     * @return the ultimate xhtml result of the import operation.
     */
    public String getResult()
    {
        return result;
    }
}
