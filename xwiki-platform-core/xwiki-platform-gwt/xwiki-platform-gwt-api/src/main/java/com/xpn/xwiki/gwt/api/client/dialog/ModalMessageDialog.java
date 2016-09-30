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
import com.xpn.xwiki.gwt.api.client.dialog.DefaultDialog;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;

public class ModalMessageDialog extends DefaultDialog
{
    private XWikiGWTApp app;

    public ModalMessageDialog() {
    }

    public ModalMessageDialog(XWikiGWTApp app, String title, String msg){
        this(app, title, msg, null);
    }

    public ModalMessageDialog(XWikiGWTApp app, String title, String msg, String styleName){
        super(false, true);
        this.app = app;
        this.addStyleName("dialog-message");
        this.setText(title);
        if (styleName!=null) {
            ScrollPanel scroll = new ScrollPanel();
            scroll.add(new Label(msg));
            scroll.addStyleName(styleName);
            this.add(scroll);
        } else {
            this.add(new Label(msg));
        }
        Button closeButton = new Button(this.app.getTranslation("Ok"));
        closeButton.addClickListener(new ClickListener(){
           @Override
        public void onClick(Widget arg0)
           {
               ModalMessageDialog.this.hide();
           }
        });
        this.add(closeButton);
        this.show();
    }
}
