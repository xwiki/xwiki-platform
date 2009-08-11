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

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Panel;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;

/**
 * Button panel used in {@link ImporterDialog}.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public class ButtonPanel extends FlowPanel
{
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
     * @param handler {@link ClickHandler} for buttons.
     */
    public ButtonPanel(ClickHandler handler)
    {
        filterStylesCheckBox = new CheckBox(Strings.INSTANCE.importerFilterStylesCheckBoxCaption());
        add(filterStylesCheckBox);
        importButton = new Button(Strings.INSTANCE.importerImportButtonCaption());
        importButton.addStyleName("xImporterImportButton");
        importButton.addClickHandler(handler);
        cancelButton = new Button(Strings.INSTANCE.importerCancelButtonCaption());
        cancelButton.addStyleName("xImporterCancelButton");
        cancelButton.addClickHandler(handler);
        add(cancelButton);
        add(importButton);

        // add a clear floats panel
        Panel fakeClearPanel = new FlowPanel();
        fakeClearPanel.addStyleName("clearfloats");
        add(fakeClearPanel);
    }

    /**
     * @return the filterStylesCheckBox.
     */
    public CheckBox getFilterStylesCheckBox()
    {
        return filterStylesCheckBox;
    }

    /**
     * @return the importButton.
     */
    public Button getImportButton()
    {
        return importButton;
    }

    /**
     * @return the cancelButton.
     */
    public Button getCancelButton()
    {
        return cancelButton;
    }
}
