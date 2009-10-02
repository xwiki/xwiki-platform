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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.WysiwygServiceAsync;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.TabPanelSelector;
import com.xpn.xwiki.wysiwyg.client.widget.ComplexDialogBox;

/**
 * Office Importer wysiwyg dialog box.
 * 
 * @version $Id$
 */
public class ImporterDialog extends ComplexDialogBox implements AsyncCallback<String>, ClickHandler,
    SubmitCompleteHandler
{
    /**
     * Container panel.
     */
    private FlowPanel mainPanel;

    /**
     * Tab panel.
     */
    private TabPanel tabPanel;

    /**
     * File import tab.
     */
    private FileImportTab fileImportTab;

    /**
     * Clipboard (copy-paste) import tab.
     */
    private ClipboardImportTab clipboardImportTab;

    /**
     * The button panel.
     */
    private ButtonPanel buttonPanel;

    /**
     * The name of the current wiki page.
     */
    private String fullPageName;

    /**
     * Resulting xhtml fragment of the import operation.
     */
    private String result;

    /**
     * Default constructor.
     * 
     * @param wysiwygConfig the wysiwyg configuration object.
     */
    public ImporterDialog(Config wysiwygConfig)
    {
        // Dialog box.
        super(false, true);
        getDialog().setIcon(Images.INSTANCE.importer().createImage());
        getDialog().setCaption(Strings.INSTANCE.importerCaption());
        addStyleName("xImporterDialog");
        getHeader().clear();

        // Read current wysiwyg configuration.
        String currentSpace = wysiwygConfig.getParameter("space", "Main");
        String currentPage = wysiwygConfig.getParameter("page", "WebHome");
        boolean openOfficeServerConnected =
            wysiwygConfig.getParameter("openofficeServerConnected", "false").equals("true");

        this.fullPageName = currentSpace + "." + currentPage;

        // Main container panel.
        mainPanel = new FlowPanel();
        mainPanel.addStyleName("xImporterPanel");

        // Tab panel.
        tabPanel = new TabPanel();
        TabPanelSelector tabPanelSelector = new TabPanelSelector();
        tabPanel.addBeforeSelectionHandler(tabPanelSelector);
        tabPanel.addSelectionHandler(tabPanelSelector);
        clipboardImportTab = new ClipboardImportTab();
        tabPanel.add(clipboardImportTab, Strings.INSTANCE.importerClipboardTabCaption());
        String uploadUrl = "../../upload/" + currentSpace + "/" + currentPage;
        fileImportTab = new FileImportTab(openOfficeServerConnected, uploadUrl, this);
        tabPanel.add(fileImportTab, Strings.INSTANCE.importerFileTabCaption());
        tabPanel.selectTab(0);
        tabPanel.addStyleName("xImporterTabPanel");
        mainPanel.add(tabPanel);

        // Button panel.
        buttonPanel = new ButtonPanel(this);
        getFooter().add(buttonPanel);

        getBody().add(mainPanel);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickHandler#onClick(ClickEvent)
     */
    public void onClick(ClickEvent event)
    {
        if (event.getSource() == buttonPanel.getImportButton()) {
            WysiwygServiceAsync wysiwygService = WysiwygService.Singleton.getInstance();
            String htmlPaste = clipboardImportTab.getHtmlPaste();
            if (clipboardImportTab.isVisible() && !htmlPaste.trim().equals("")) {
                startProgress();
                wysiwygService.cleanOfficeHTML(htmlPaste, "wysiwyg", getCleaningParams(), this);
            } else if (fileImportTab.isVisible() && !fileImportTab.getFileName().trim().equals("")) {
                startProgress();
                fileImportTab.sumbit();
            }
        } else if (event.getSource() == buttonPanel.getCancelButton()) {
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
        if (buttonPanel.getFilterStylesCheckBox().getValue()) {
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
     * Starts progress display.
     */
    private void startProgress()
    {
        tabPanel.setVisible(false);
        enableControls(false);
        this.setLoading(true);
    }

    /**
     * Stops progress display.
     */
    private void stopProgress()
    {
        this.setLoading(false);
        tabPanel.setVisible(true);
        enableControls(true);
    }

    /**
     * Enables / Disables the control buttons.
     * 
     * @param enable if true, all controls will be enabled.
     */
    private void enableControls(boolean enable)
    {
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
     * 
     * @see SubmitCompleteHandler#onSubmitComplete(SubmitCompleteEvent)
     */
    public void onSubmitComplete(SubmitCompleteEvent event)
    {
        WysiwygServiceAsync wysiwygService = WysiwygService.Singleton.getInstance();
        wysiwygService.officeToXHTML(fullPageName, getCleaningParams(), this);
    }

    /**
     * {@inheritDoc}
     */
    public void center()
    {
        clipboardImportTab.clearTextArea();
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
