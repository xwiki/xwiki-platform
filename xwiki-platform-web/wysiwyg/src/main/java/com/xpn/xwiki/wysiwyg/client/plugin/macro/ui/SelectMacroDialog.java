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
package com.xpn.xwiki.wysiwyg.client.plugin.macro.ui;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.ComplexDialogBox;

/**
 * Dialog for selecting one of the available macros.
 * 
 * @version $Id$
 */
public class SelectMacroDialog extends ComplexDialogBox implements ClickListener, ChangeListener
{
    /**
     * The call-back used by the select macro dialog to be notified when the list of available macro names is being
     * received from the server. Only the last request for the list of available macros triggers an update of the select
     * macro dialog.
     */
    private class MacrosAsyncCallback implements AsyncCallback<List<String>>
    {
        /**
         * The index of the request.
         */
        private int index;

        /**
         * Creates a new call-back for the request with the specified index.
         */
        public MacrosAsyncCallback()
        {
            this.index = ++SelectMacroDialog.this.macrosRequestIndex;
        }

        /**
         * {@inheritDoc}
         * 
         * @see AsyncCallback#onFailure(Throwable)
         */
        public void onFailure(Throwable caught)
        {
            showError(caught);
        }

        /**
         * {@inheritDoc}
         * 
         * @see AsyncCallback#onSuccess(Object)
         */
        public void onSuccess(List<String> result)
        {
            if (index == SelectMacroDialog.this.macrosRequestIndex) {
                fill(result);
            }
        }
    }

    /**
     * The button that selects the chosen macro.
     */
    private final Button select;

    /**
     * The list box displaying the available macros.
     */
    private final ListBox macroList;

    /**
     * The object used to configure the dialog.
     */
    private final Config config;

    /**
     * The index of the last request for the list of available macros.
     */
    private int macrosRequestIndex = -1;

    /**
     * Creates a new dialog for selecting one of the available macros. The dialog is modal.
     * 
     * @param config the object used to configure the newly created dialog
     */
    public SelectMacroDialog(Config config)
    {
        super(false, true);

        this.config = config;

        getDialog().setIcon(Images.INSTANCE.macroInsert().createImage());
        getDialog().setCaption(Strings.INSTANCE.macroInsertDialogCaption());

        getHeader().add(new Label(Strings.INSTANCE.macroInsertDialogTitle()));

        macroList = new ListBox();
        macroList.setVisibleItemCount(2);
        macroList.addChangeListener(this);
        macroList.addStyleName("xMacroList");

        select = new Button(Strings.INSTANCE.select());
        select.addClickListener(this);
        getFooter().add(select);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == select) {
            setCanceled(false);
            hide();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComplexDialogBox#center()
     */
    public void center()
    {
        // Clear the dialog.
        getBody().clear();

        // Disable the select button till we receive the list of available macros from the server.
        select.setEnabled(false);

        // Put the dialog in loading state.
        setLoading(true);

        // Request the list of available macros.
        WysiwygService.Singleton.getInstance().getMacros(config.getParameter("syntax"), new MacrosAsyncCallback());

        super.center();
    }

    /**
     * Fills the dialog with the controls needed to select one of the available macros.
     * 
     * @param macros the list of available macro names
     */
    private void fill(List<String> macros)
    {
        // First get the dialog out of the loading state.
        setLoading(false);

        macroList.clear();
        for (String macro : macros) {
            macroList.addItem(macro);
        }
        getBody().add(macroList);
    }

    /**
     * @return the name of the selected macro
     */
    public String getSelectedMacro()
    {
        return macroList.getSelectedIndex() < 0 ? null : macroList.getItemText(macroList.getSelectedIndex());
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChangeListener#onChange(Widget)
     */
    public void onChange(Widget sender)
    {
        if (sender == macroList) {
            select.setEnabled(macroList.getSelectedIndex() >= 0);
        }
    }
}
