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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.importer.ImporterListener;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.TabPanelSelector;
import com.xpn.xwiki.wysiwyg.client.widget.ComplexDialogBox;

/**
 * Office Importer wysiwyg dialog box.
 * 
 * @version $Id$
 */
public class ImporterDialog extends ComplexDialogBox
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
    private FileImporter fileImporter;

    /**
     * Clipboard (copy-paste) import tab.
     */
    private ClipboardImporter clipboardImporter;

    /**
     * Default constructor.
     * 
     * @param wysiwygConfig the wysiwyg configuration object.
     * @param importerListener the import process listener.
     */
    public ImporterDialog(Config wysiwygConfig, ImporterListener importerListener)
    {
        // Dialog box.
        super(false, true);
        getDialog().setIcon(Images.INSTANCE.importer().createImage());
        getDialog().setCaption(Strings.INSTANCE.importerCaption());
        addStyleName("xImporterDialog");
        getHeader().clear();

        // Main container panel.
        mainPanel = new FlowPanel();
        mainPanel.addStyleName("xImporterPanel");

        // Tab panel.
        tabPanel = new TabPanel();
        TabPanelSelector tabPanelSelector = new TabPanelSelector();
        tabPanel.addBeforeSelectionHandler(tabPanelSelector);
        tabPanel.addSelectionHandler(tabPanelSelector);
        clipboardImporter = new ClipboardImporter(wysiwygConfig, importerListener);
        tabPanel.add(clipboardImporter, Strings.INSTANCE.importerClipboardTabCaption());
        fileImporter = new FileImporter(wysiwygConfig, importerListener);
        tabPanel.add(fileImporter, Strings.INSTANCE.importerFileTabCaption());
        tabPanel.selectTab(0);
        tabPanel.addStyleName("xImporterTabPanel");
        mainPanel.add(tabPanel);

        getBody().add(mainPanel);
    }
}
