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

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;

public class CustomDialog extends Dialog {
    protected HTML messagePanel;
    protected Panel addPanel;

    /**
     * Choice dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes Dialog.BUTTON_CANCEL|Dialog.BUTTON_NEXT for Cancel / Next
     */
    public CustomDialog(XWikiGWTApp app, String name, Panel panel, int buttonModes) {
        super(app, name, buttonModes);

        FlowPanel main = new FlowPanel();
        main.addStyleName(getCSSName("main"));

        messagePanel = new HTML(app.getTranslation(getDialogTranslationName() + ".invitation"));
        messagePanel.addStyleName(getCSSName("invitation"));
        main.add(messagePanel);

        addPanel = new SimplePanel();
        main.add(addPanel);
        if (panel!=null) {
            addPanel.add(panel);
        }

        main.add(getActionsPanel());
        add(main);
    }

    @Override
    protected void endDialog() {
        setCurrentResult("");
        super.endDialog();
    }

    public void setMessage(String text, String[] args) {
        String message = app.getTranslation(text, args);
        messagePanel.setHTML(message);
    }

    public void setPanel(Panel panel) {
        addPanel.clear();
        if (panel!=null)
         addPanel.add(panel);
    }

}
