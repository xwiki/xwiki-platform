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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.macro.MacroDescriptor;
import com.xpn.xwiki.wysiwyg.client.util.CancelableAsyncCallback;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.widget.ComplexDialogBox;
import com.xpn.xwiki.wysiwyg.client.widget.ListBox;
import com.xpn.xwiki.wysiwyg.client.widget.ListItem;

/**
 * Dialog for selecting one of the available macros.
 * 
 * @version $Id$
 */
public class SelectMacroDialog extends ComplexDialogBox implements ClickListener, ChangeListener
{
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
     * Used to be notified when macro descriptors are retrieved form the server. This objects represent pending requests
     * for macro descriptors.
     */
    private final List<CancelableAsyncCallback<MacroDescriptor>> macroDescriptorCallbackList =
        new ArrayList<CancelableAsyncCallback<MacroDescriptor>>();

    /**
     * Used to be notified when the list of available macros is retrieved from the server. This object represents the
     * current pending request for the list of available macros.
     */
    private CancelableAsyncCallback<List<String>> macroListCallback;

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
        macroList.addChangeListener(this);

        select = new Button(Strings.INSTANCE.select());
        select.addClickListener(this);
        getFooter().add(select);
    }

    /**
     * @return the string identifier for the storage syntax
     */
    private String getSyntax()
    {
        return config.getParameter("syntax");
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
        macroListCallback = new CancelableAsyncCallback<List<String>>(new AsyncCallback<List<String>>()
        {
            public void onFailure(Throwable caught)
            {
                showError(caught);
            }

            public void onSuccess(List<String> result)
            {
                fill(result);
            }
        });
        WysiwygService.Singleton.getInstance().getMacros(getSyntax(), macroListCallback);

        super.center();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComplexDialogBox#hide()
     */
    public void hide()
    {
        cancelPendingRequests();
        super.hide();
    }

    /**
     * Fills the dialog with the controls needed to select one of the available macros.
     * 
     * @param macros the list of available macro names
     */
    private void fill(List<String> macros)
    {
        macroList.clear();
        for (String macro : macros) {
            Label name = new Label(macro);
            name.addStyleName("xMacroLabel");

            final ListItem item = new ListItem();
            item.addStyleName("xMacro");
            item.add(name);

            macroList.addItem(item);

            // Request the macro descriptor to fill the macro description.
            AsyncCallback<MacroDescriptor> callback = new AsyncCallback<MacroDescriptor>()
            {
                public void onFailure(Throwable caught)
                {
                    // ignore
                }

                public void onSuccess(MacroDescriptor result)
                {
                    Label description = new Label(result.getDescription());
                    description.addStyleName("xMacroDescription");
                    item.add(description);
                }
            };
            CancelableAsyncCallback<MacroDescriptor> cancelableCallback =
                new CancelableAsyncCallback<MacroDescriptor>(callback)
                {
                    public void onFailure(Throwable caught)
                    {
                        super.onFailure(caught);
                        if (!isCanceled()) {
                            mayFinishLoading(this);
                        }
                    }

                    public void onSuccess(MacroDescriptor result)
                    {
                        super.onSuccess(result);
                        if (!isCanceled()) {
                            mayFinishLoading(this);
                        }
                    }
                };
            macroDescriptorCallbackList.add(cancelableCallback);
            WysiwygService.Singleton.getInstance().getMacroDescriptor(macro, getSyntax(), cancelableCallback);
        }
    }

    /**
     * Counts the given callback and finishes the loading process if it's the last one expected.
     * 
     * @param callback a callback for a macro descriptor
     */
    private void mayFinishLoading(CancelableAsyncCallback<MacroDescriptor> callback)
    {
        macroDescriptorCallbackList.remove(callback);
        if (macroDescriptorCallbackList.isEmpty()) {
            setLoading(false);
            getBody().add(macroList);
        }
    }

    /**
     * @return the name of the selected macro
     */
    public String getSelectedMacro()
    {
        return macroList.getSelectedItem() == null ? null : ((Label) macroList.getSelectedItem().getWidget(0))
            .getText();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ChangeListener#onChange(Widget)
     */
    public void onChange(Widget sender)
    {
        if (sender == macroList) {
            select.setEnabled(macroList.getSelectedItem() != null);
        }
    }

    /**
     * Cancels the pending requests.
     */
    protected void cancelPendingRequests()
    {
        macroListCallback.setCanceled(true);
        for (CancelableAsyncCallback<MacroDescriptor> callback : macroDescriptorCallbackList) {
            callback.setCanceled(true);
        }
        macroDescriptorCallbackList.clear();
    }
}
