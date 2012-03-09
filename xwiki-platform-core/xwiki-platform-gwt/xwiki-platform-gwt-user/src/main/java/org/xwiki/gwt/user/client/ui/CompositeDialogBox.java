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

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The base class for any custom dialog box. It prevents unwanted modifications of the content from outside. It wraps
 * {@link DialogBox} object that you can customize and which is not accessible outside.
 * 
 * @version $Id$
 */
public class CompositeDialogBox extends Composite implements HasCloseHandlers<CompositeDialogBox>,
    CloseHandler<PopupPanel>
{
    /**
     * The underlying dialog box.
     */
    private final DialogBox dialog;

    /**
     * Creates a new composite dialog box.
     * 
     * @param autoHide Whether or not the dialog should auto hide when the user clicks outside of it.
     * @param modal Specifies if the dialog box can loose focus.
     */
    public CompositeDialogBox(boolean autoHide, boolean modal)
    {
        dialog = new DialogBox(autoHide, modal);
        dialog.addCloseHandler(this);
    }

    @Override
    protected void initWidget(Widget widget)
    {
        super.initWidget(widget);
        dialog.setWidget(this);
    }

    /**
     * Protected access to the underlying dialog box.
     * 
     * @return the wrapped dialog box.
     */
    protected DialogBox getDialog()
    {
        return dialog;
    }

    /**
     * Centers this dialog on the screen.
     * 
     * @see DialogBox#center()
     */
    public void center()
    {
        dialog.center();
    }

    /**
     * Hides this dialog box.
     * 
     * @see DialogBox#hide()
     */
    public void hide()
    {
        dialog.hide();
    }

    @Override
    public HandlerRegistration addCloseHandler(CloseHandler<CompositeDialogBox> handler)
    {
        return addHandler(handler, CloseEvent.getType());
    }

    @Override
    public void onClose(CloseEvent<PopupPanel> event)
    {
        if (event.getSource() == dialog) {
            CloseEvent.fire(this, this, event.isAutoClosed());
        }
    }

    /**
     * @return {@code true} if this dialog is currently shown, {@code false} otherwise
     * 
     * @see DialogBox#isShowing()
     */
    public boolean isShowing()
    {
        return dialog.isShowing();
    }
}
