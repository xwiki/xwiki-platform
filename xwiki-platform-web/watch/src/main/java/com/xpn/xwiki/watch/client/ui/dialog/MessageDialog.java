package com.xpn.xwiki.watch.client.ui.dialog;

import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.client.Window;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;

/**
 * Copyright 2006,XpertNet SARL,and individual contributors as indicated
 * by the contributors.txt.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 *
 * @author ldubost
 */

public class MessageDialog extends WatchDialog {
    protected HTML messagePanel;
    /**
     * Choice dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes WatchDialog.BUTTON_CANCEL|WatchDialog.BUTTON_NEXT for Cancel / Next
     */
    public MessageDialog(XWikiGWTApp app, String name, int buttonModes) {
        super(app, name, buttonModes);

        FlowPanel main = new FlowPanel();
        main.addStyleName(getCSSName("main"));

        messagePanel = new HTML(app.getTranslation(getDialogTranslationName() + ".invitation"));
        messagePanel.addStyleName(getCssPrefix() + "-invitation");
        main.add(messagePanel);
        main.add(getActionsPanel());
        add(main);
    }

    protected void endDialog() {
        setCurrentResult(null);
        super.endDialog();
    }

    public void setMessage(String text, String[] args) {
        String message = app.getTranslation(text, args);
        messagePanel.setHTML(message);
    }

}
