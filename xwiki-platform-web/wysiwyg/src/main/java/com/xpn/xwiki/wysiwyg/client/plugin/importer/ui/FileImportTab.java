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

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;

/**
 * Office Importer UI tab for importing office documents.
 * 
 * @version $Id$
 */
public class FileImportTab extends Composite
{
    /**
     * Container panel.
     */
    private VerticalPanel verticalPanel;

    /**
     * Information label.
     */
    private Label infoLabel;

    /**
     * A {@link FormPanel} to hold the fileUpload widget.
     */
    private FormPanel formPanel;    
    
    /**
     * The {@link FileUpload} widget.
     */
    private FileUpload fileUpload;
    
    /**
     * Default constructor.
     * 
     * @param uploadUrl the url to be set for 'action' attribute of the internal form.
     * @param formHandler the {@link FormHandler} for handling form events.
     */
    public FileImportTab(String uploadUrl, FormHandler formHandler)
    {
        // Main container panel.
        verticalPanel = new VerticalPanel();
        verticalPanel.setSpacing(5);

        // Info label.
        infoLabel = new Label(Strings.INSTANCE.importerFileTabInfoLabel());
        infoLabel.addStyleName("xImporterInfoLabel");
        verticalPanel.add(infoLabel);
        
        // Form panel.
        formPanel = new FormPanel();
        formPanel.setAction(uploadUrl);
        formPanel.setEncoding(FormPanel.ENCODING_MULTIPART);
        formPanel.setMethod(FormPanel.METHOD_POST);
        fileUpload = new FileUpload();
        fileUpload.setName("filepath");
        formPanel.add(fileUpload);  
        formPanel.addFormHandler(formHandler);
        verticalPanel.add(formPanel);

        // Finalize.
        initWidget(verticalPanel);
    }
    
    /**
     * Submits the internal form.
     */
    public void sumbit()
    {
        formPanel.submit();
    }
    
    /**
     * @return the file name entered into {@link FileUpload} widget.
     */
    public String getFileName() 
    {
        return fileUpload.getFilename();
    }        
}
