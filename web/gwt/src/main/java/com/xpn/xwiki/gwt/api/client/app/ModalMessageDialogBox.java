package com.xpn.xwiki.gwt.api.client.app;

import asquare.gwt.tk.client.ui.ModalDialog;
import com.google.gwt.user.client.ui.*;

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


public class ModalMessageDialogBox {
    private XWikiGWTApp app;
    private ModalDialog dialog = new ModalDialog();

    public ModalMessageDialogBox() {
    }

    public ModalMessageDialogBox(XWikiGWTApp app, String title, String msg){
        this(app, title, msg, null);
    }

    public ModalMessageDialogBox(XWikiGWTApp app, String title, String msg, String styleName){
       this.app = app;
        dialog.setCaption(title, false);
        if (styleName!=null) {
            ScrollPanel scroll = new ScrollPanel();
            scroll.add(new Label(msg));
            scroll.addStyleName(styleName);
            dialog.add(scroll);
        } else {
            dialog.add(new Label(msg));
        }
        dialog.add(new CloseButton(dialog, app.getTranslation("Ok")));
        dialog.show();
    }

    class CloseListener implements ClickListener {
        private final ModalDialog m_dialog;

        public CloseListener(ModalDialog dialog)
        {
            m_dialog = dialog;
        }

        public void onClick(Widget sender)
        {
            m_dialog.hide();
            m_dialog.removeFromParent();
        }
    }

    class CloseButton extends Button {
        public CloseButton(ModalDialog dialog, String msg)
        {
            super(msg);
            addClickListener(new CloseListener(dialog));
        }
    }

}