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
package org.xwiki.gwt.user.client.ui;

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.Strings;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/**
 * A dialog box that can be part of a wizard and whose content is structured in three parts: the header, the body and
 * the footer. The header usually contains the title of the dialog and optional wizard navigation buttons (back and
 * forth). The body contains the widgets for acquiring user data. The footer usually contains buttons for closing the
 * dialog or ending the wizard.
 * 
 * @version $Id$
 */
public class ComplexDialogBox extends CompositeDialogBox
{
    /**
     * The CSS class name used when the dialog is in loading state.
     */
    private static final String STYLE_NAME_LOADING = "loading";

    /**
     * The dialog header, containing the title of the dialog and optional wizard navigation buttons (back and forth).
     */
    private final FlowPanel header;

    /**
     * The dialog body contains the widgets for acquiring user data.
     */
    private final FlowPanel body;

    /**
     * The dialog footer usually contains buttons for closing the dialog or ending the wizard.
     */
    private final FlowPanel footer;

    /**
     * Flag indicating whether the dialog was canceled or not. This flag is reset each time before the dialog is shown.
     */
    private boolean canceled;

    /**
     * Flag indicating whether the dialog is in loading state or not. Usually when the dialog is in loading state it
     * means it is waiting for the response of a server request.
     */
    private boolean loading;

    /**
     * Creates a new complex dialog box.
     * 
     * @param autoHide Whether or not the dialog should auto hide when the user clicks outside of it.
     * @param modal Specifies if the dialog box can loose focus.
     */
    public ComplexDialogBox(boolean autoHide, boolean modal)
    {
        super(autoHide, modal);

        header = new FlowPanel();
        header.addStyleName("xDialogHeader");

        body = new FlowPanel();
        body.addStyleName("xDialogBody");

        footer = new FlowPanel();
        footer.addStyleName("xDialogFooter");

        FlowPanel wrapper = new FlowPanel();
        wrapper.add(header);
        wrapper.add(body);
        wrapper.add(footer);
        initWidget(wrapper);
    }

    /**
     * @return {@link #header}
     */
    protected FlowPanel getHeader()
    {
        return header;
    }

    /**
     * @return {@link #body}
     */
    protected FlowPanel getBody()
    {
        return body;
    }

    /**
     * @return {@link #footer}
     */
    protected FlowPanel getFooter()
    {
        return footer;
    }

    @Override
    public void center()
    {
        // Reset the canceled state before showing the dialog.
        setCanceled(true);
        super.center();
    }

    /**
     * @return {@code true} if this dialog was canceled, {@code false} otherwise
     */
    public boolean isCanceled()
    {
        return canceled;
    }

    /**
     * Sets the canceled state of this dialog.
     * 
     * @param canceled {@code true} if the dialog was canceled, {@code false} otherwise
     */
    protected void setCanceled(boolean canceled)
    {
        this.canceled = canceled;
    }

    /**
     * @return {@code true} if the dialog is in loading state, {@code false} otherwise
     */
    protected boolean isLoading()
    {
        return loading;
    }

    /**
     * Puts the dialog in loading state or get it out of it.
     * 
     * @param loading {@code true} to put the dialog in loading state, {@code false} to get it out of it
     */
    protected void setLoading(boolean loading)
    {
        if (this.loading != loading) {
            this.loading = loading;
            if (loading) {
                body.addStyleName(STYLE_NAME_LOADING);
            } else {
                body.removeStyleName(STYLE_NAME_LOADING);
            }
        }
    }

    /**
     * If an error occurred on the server while fulfilling the request then this method can be used to display the error
     * message to the user.
     * 
     * @param caught the exception that has been caught
     */
    protected void showError(Throwable caught)
    {
        // First get the dialog out of the loading state.
        setLoading(false);

        String message = caught.getLocalizedMessage();
        if (StringUtils.isEmpty(message)) {
            // Use a default error message.
            message = Strings.INSTANCE.dialogFailedToLoad();
        }

        Label error = new Label(message);
        error.addStyleName("errormessage");

        getBody().add(error);
    }
}
