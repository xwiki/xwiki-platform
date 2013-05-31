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
package com.xpn.xwiki.gwt.api.client.dialog;

import com.google.gwt.user.client.ui.*;

/**
 * Default web gwt dialog, created for show and hide customizations
 * and dialog content management.
 */
public class DefaultDialog extends DialogBox {
    protected Panel contentPanel = new FlowPanel();

    public DefaultDialog() {
        super();
        this.setDefaultSettings();
    }

    public DefaultDialog(boolean autoHide) {
        super(autoHide);
        this.setDefaultSettings();
    }

    public DefaultDialog(boolean autoHide, boolean modal) {
        super(autoHide, modal);
        this.setDefaultSettings();
    }

    private void setDefaultSettings() {
        this.setWidget(this.contentPanel);
        this.contentPanel.setStyleName("gwt-ModalDialog-Content");
    }

    /**
     * Override to give the possibility of adding multiple widgets to the dialog.
     *
     * @param w widget to add to the dialog.
     */
    @Override
    public void add(Widget w) {
        this.contentPanel.add(w);
    }

    /**
     * Override show to also center the dialog.
     */
    @Override
    public void show() {
        super.show();
        super.center();
        //set a body class to mark the dialog show
        RootPanel.get().addStyleName("gwt-ModalDialog-show");
    }

    @Override
    public void hide() {
        //remove the body class
        RootPanel.get().removeStyleName("gwt-ModalDialog-show");        
        super.hide();
    }
}
