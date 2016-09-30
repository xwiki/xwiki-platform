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

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTAppConstants;

public class LoadingDialog {    
    private XWikiGWTApp app;
    protected DefaultDialog loadingPanel;
    private int currentRequest = 0;
    private boolean disable = false;

    public LoadingDialog(XWikiGWTApp app) {
        this.app = app;
    }

    public void disable(){
        disable = true;
    }

    public void enable(){
        disable = false;
    } 

    public void startLoading() {
        if (loadingPanel == null && !disable){
            loadingPanel = new DefaultDialog(false, true);
            loadingPanel.addStyleName("dialog-loading");
            loadingPanel.add(new Label(app.getTranslation("loading.loading_msg")));
            String iconspinner = app.getTranslation("loading.loading_icon_spinner");
            if (iconspinner.equals("loading.loading_icon_spinner")) {
                iconspinner = XWikiGWTAppConstants.LOADING_ICON_SPINNER;
            }
            loadingPanel.add(new Image(app.getSkinFile(iconspinner)));
        }

        if (loadingPanel != null){
            loadingPanel.show();
        }
        currentRequest++;
    }

    public void finishLoading() {
        currentRequest--;
        if (currentRequest <= 0 && loadingPanel != null) {
            loadingPanel.hide();
            loadingPanel.removeFromParent();
            loadingPanel = null;
        }
        if (currentRequest < 0){
            currentRequest = 0;
        }
    }
}
