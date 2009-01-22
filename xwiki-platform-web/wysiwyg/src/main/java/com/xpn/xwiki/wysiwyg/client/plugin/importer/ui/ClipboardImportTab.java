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

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;

/**
 * Office Importer UI tab for importing clipboard content.
 * 
 * @version $Id$
 */
public class ClipboardImportTab extends Composite
{
    /**
     * Container panel.
     */
    private VerticalPanel mainPanel;

    /**
     * Information label.
     */
    private Label infoLabel;

    /**
     * Editor area (where the user can paste his content).
     */
    private RichTextArea editor;

    /**
     * Filter Styles check box.
     */
    private CheckBox filterStylesCheckBox;
    
    /**
     * Import button.
     */
    private Button importButton;
    
    /**
     * Cancel button.
     */
    private Button cancelButton;

    /**
     * Default constructor.
     * 
     * @param listener Listener for the import button.
     */
    public ClipboardImportTab(ClickListener listener)
    {
        // Main container panel.
        mainPanel = new VerticalPanel();
        mainPanel.setSpacing(5);

        // Info label.
        infoLabel = new Label(Strings.INSTANCE.importerClipboardInfoLabel());
        infoLabel.addStyleName("xImporterClipboardInfoLabel");
        mainPanel.add(infoLabel);

        // Editor panel.
        editor = new RichTextArea();
        editor.addStyleName("xImporterClipboardEditor");
        mainPanel.add(editor);

        // Button panel.
        HorizontalPanel buttonPanel = new HorizontalPanel();
        filterStylesCheckBox = new CheckBox(Strings.INSTANCE.importerClipboardFilterStylesCheckBox());
        filterStylesCheckBox.addStyleName("xImporterClipboardFilterStylesCheckBox");
        filterStylesCheckBox.setChecked(true);
        buttonPanel.add(filterStylesCheckBox);
        importButton = new Button(Strings.INSTANCE.importerClipboardImportButton());
        importButton.addStyleName("xImporterClipboardImportButton");
        importButton.addClickListener(listener);
        buttonPanel.add(importButton);
        cancelButton = new Button(Strings.INSTANCE.importerClipboardCancelImportButton());
        cancelButton.addStyleName("xImporterClipboardCancelImportButton");
        cancelButton.addClickListener(listener);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel);

        // Finalize.
        initWidget(mainPanel);
    }

    /**
     * Clears the content of the editor rta.
     */
    public void resetEditor()
    {
        editor.setHTML("");
    }
    
    /**
     * @return the editor.
     */
    public RichTextArea getEditor()
    {
        return editor;
    }

    /**
     * @return The import button.
     */
    public Button getImportButton()
    {
        return importButton;
    }

    /**
     * @return the filter styles check box.
     */
    public CheckBox getFilterStylesCheckBox()
    {
        return filterStylesCheckBox;
    }    
}
