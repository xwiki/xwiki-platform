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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.importer.ImporterListener;
import com.xpn.xwiki.wysiwyg.client.util.Config;

/**
 * Office Importer for importing office documents.
 * 
 * @version $Id$
 */
public class FileImporter extends AbstractImporter implements SubmitCompleteHandler, AsyncCallback<String>
{
    /**
     * Style identifier for help message labels.
     */
    private static final String HELP_LABEL_STYLE = "xHelpLabel";

    /**
     * A {@link FormPanel} to hold the fileUpload widget.
     */
    private FormPanel formPanel;

    /**
     * The {@link FileUpload} widget.
     */
    private FileUpload fileUpload;  
    
    /**
     * The name of the current wiki page.
     */
    private String fullPageName;
    
    /**
     * A flag indicating if openoffice server is available or not.
     */
    private boolean isOpenOfficeServerConnected;
        
    /**
     * Default constructor.
     * 
     * @param config wysiwyg configuration.
     * @param importerListener importer listener.
     */
    public FileImporter(Config config, ImporterListener importerListener)
    {
        super(config, importerListener);                        
    }
            
    /**
     * {@inheritDoc}
     */
    protected Panel createContentPanel()
    {
        // Read current wysiwyg configuration.
        String currentSpace = config.getParameter("space", "Main");
        String currentPage = config.getParameter("page", "WebHome");
        this.fullPageName = currentSpace + "." + currentPage;
        this.isOpenOfficeServerConnected = config.getParameter("openofficeServerConnected", "false").equals("true");
        String uploadURL = "../../upload/" + currentSpace + "/" + currentPage;

        return isOpenOfficeServerConnected ? createMainUI(uploadURL) : createFeatureNotAvailableUI();
    }

    /**
     * {@inheritDoc}
     */
    protected void onImportButtonClick()
    {
        if (isOpenOfficeServerConnected) {
            String fileName = fileUpload.getFilename().trim();
            if (!fileName.equals("")) {
                setBusy(true);
                formPanel.submit();
            }
        }
    }

    /**
     * Creates a panel that displays a message indicating that this feature is not available.
     * 
     * @return feature not available message panel.
     */
    private Panel createFeatureNotAvailableUI()
    {
        Panel container = new FlowPanel();
        Label featureNotAvailableLabel = new Label(Strings.INSTANCE.importerFileTabNotAvailableLabel());
        featureNotAvailableLabel.setStyleName(HELP_LABEL_STYLE);
        container.add(featureNotAvailableLabel);
        return container;
    }
    
    /**
     * Creates the main UI panel.
     * 
     * @param uploadURL file upload url.
     * @return main ui panel.
     */
    private Panel createMainUI(String uploadURL)
    {
        Panel contentPanel = new FlowPanel();
        // Info label.
        Panel infoLabel = new FlowPanel();
        infoLabel.setStyleName("xInfoLabel");
        infoLabel.add(new InlineLabel(Strings.INSTANCE.importerFileTabInfoLabel()));
        InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
        mandatoryLabel.addStyleName("xMandatory");
        infoLabel.add(mandatoryLabel);
        contentPanel.add(infoLabel);

        // Help label.
        Label helpLabel = new Label(Strings.INSTANCE.importerFileTabHelpLabel());
        helpLabel.setStyleName(HELP_LABEL_STYLE);
        contentPanel.add(helpLabel);

        // Form panel.
        formPanel = new FormPanel();
        formPanel.setAction(uploadURL);
        formPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
        formPanel.setMethod(FormPanel.METHOD_POST);
        fileUpload = new FileUpload();
        fileUpload.setName("filepath");
        formPanel.add(fileUpload);
        formPanel.addSubmitCompleteHandler(this);
        contentPanel.add(formPanel);
        
        return contentPanel;
    }   

    /**
     * {@inheritDoc}
     */
    public void onSubmitComplete(SubmitCompleteEvent event)
    {
        wysiwygService.officeToXHTML(fullPageName, getHTMLCleaningParams(), this);
    }

    /**
     * {@inheritDoc}
     */
    public void onFailure(Throwable thrown)
    {
        this.importerListener.onFailure(thrown.getMessage());
        setBusy(false);
    }

    /**
     * {@inheritDoc}
     */
    public void onSuccess(String result)
    {
        this.importerListener.onSuccess(result);
        setBusy(false);
    }    
}
