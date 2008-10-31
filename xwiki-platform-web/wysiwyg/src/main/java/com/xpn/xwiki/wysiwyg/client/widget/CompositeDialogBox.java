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
package com.xpn.xwiki.wysiwyg.client.widget;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * The base class for any custom dialog box. It prevents unwanted modifications of the content from outside. It wraps
 * {@link DialogBox} object that you can customize and which is not accessible outside.
 * 
 * @version $Id$
 */
public class CompositeDialogBox extends Composite implements SourcesPopupEvents,
    com.google.gwt.user.client.ui.PopupListener
{
    /**
     * The underlying dialog box.
     */
    private final DialogBox dialog;

    /**
     * The collection of {@link PopupListener} for the wrapped dialog box. We use a custom collection and not the
     * {@link com.google.gwt.user.client.ui.PopupListener} because the default one exposes our dialog box.
     */
    private final PopupListenerCollection popupListeners;

    /**
     * Creates a new composite dialog box.
     * 
     * @param autoHide Whether or not the dialog should auto hide when the user clicks outside of it.
     * @param modal Specifies if the dialog box can loose focus.
     */
    public CompositeDialogBox(boolean autoHide, boolean modal)
    {
        dialog = new DialogBox(autoHide, modal);
        dialog.addPopupListener(this);

        popupListeners = new PopupListenerCollection();
    }

    /**
     * {@inheritDoc}
     */
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
        // PopupPanel#center calls PopupPanel#hide if the dialog was not showing. We call PopupPanel#show before in
        // order to avoid this behavior.
        // @see http://code.google.com/p/google-web-toolkit/issues/detail?id=3007
        dialog.setVisible(false);
        dialog.show();
        dialog.center();
        dialog.setVisible(true);
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

    /**
     * {@inheritDoc}
     * 
     * @see SourcesPopupEvents#addPopupListener(PopupListener)
     */
    public void addPopupListener(PopupListener listener)
    {
        popupListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesPopupEvents#removePopupListener(PopupListener)
     */
    public void removePopupListener(PopupListener listener)
    {
        popupListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.PopupListener#onPopupClosed(PopupPanel, boolean)
     */
    public void onPopupClosed(PopupPanel sender, boolean autoClosed)
    {
        if (sender == dialog) {
            popupListeners.firePopupClosed(this, autoClosed);
        }
    }
}
