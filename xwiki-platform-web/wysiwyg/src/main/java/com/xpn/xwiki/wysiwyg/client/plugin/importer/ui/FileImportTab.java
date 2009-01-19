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
import com.google.gwt.user.client.ui.Composite;
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
     * Default constructor.
     * 
     * @param listener Listener for the import button.
     */
    public FileImportTab(ClickListener listener)
    {
        // Main container panel.
        verticalPanel = new VerticalPanel();
        verticalPanel.setSpacing(5);

        // Info label.
        infoLabel = new Label(Strings.INSTANCE.importerFileInfoLabel());
        infoLabel.addStyleName("xImporterFileInfoLabel");
        verticalPanel.add(infoLabel);

        // Finalize.
        initWidget(verticalPanel);
    }
}
