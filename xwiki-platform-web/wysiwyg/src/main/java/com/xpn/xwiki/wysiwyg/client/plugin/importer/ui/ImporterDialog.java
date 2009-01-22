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

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.widget.CompositeDialogBox;

/**
 * Office Importer wysiwyg dialog box.
 * 
 * @version $Id$
 */
public class ImporterDialog extends CompositeDialogBox implements ClickListener
{
    /**
     * Tab panel.
     */
    private TabPanel tabPanel = new TabPanel();

    /**
     * File import tab.
     */
    private FileImportTab fileImportTab = new FileImportTab(this);

    /**
     * Clipboard (copy-paste) import tab.
     */
    private ClipboardImportTab clipboardImportTab = new ClipboardImportTab(this);

    /**
     * Indicates if the import type is clipboard or not.
     */
    private boolean clipBoardImport;

    /**
     * Default constructor.
     */
    public ImporterDialog()
    {
        // Dialog box.
        super(false, true);
        getDialog().setText(Strings.INSTANCE.importerCaption());

        // Tab panel.
        tabPanel.add(clipboardImportTab, Strings.INSTANCE.importerClipboardTab());
        tabPanel.add(fileImportTab, Strings.INSTANCE.importerFileTab());
        tabPanel.selectTab(0);
        clipBoardImport = false;

        // Finalize.
        initWidget(tabPanel);
        tabPanel.addStyleName("xImporterTabPanel");
    }

    /**
     * {@inheritDoc}
     */
    public void onClick(Widget sender)
    {
        clipBoardImport = (sender == clipboardImportTab.getImportButton()) && !getHtmlPaste().trim().equals("");
        hide();
    }

    /**
     * {@inheritDoc}
     */
    public void center()
    {
        clipboardImportTab.resetEditor();
        super.center();
    }

    /**
     * @return Whether the import is from clipboard or not.
     */
    public boolean isClipBoardImport()
    {
        return clipBoardImport;
    }
    
    /**
     * @return The html content of the clipboard import tab.
     */
    public String getHtmlPaste()
    {
        return clipboardImportTab.getEditor().getHTML();
    }

    /**
     * @return if the styles present in the html paste should be filtered or not.
     */
    public boolean isFilterStyles()
    {
        return clipboardImportTab.getFilterStylesCheckBox().isChecked();
    }
}
