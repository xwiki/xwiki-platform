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
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp;

public class EditPropDialog extends CustomDialog {
    protected String html;
    protected String formname;
    protected String fieldname;

    /**
     * Choice dialog
     * @param app  XWiki GWT App object to access translations and css prefix names
     * @param name dialog name
     * @param buttonModes button modes Dialog.BUTTON_CANCEL|Dialog.BUTTON_NEXT for Cancel / Next
     */
    public EditPropDialog(XWikiGWTApp app, String name, String html, String formname, String fieldname, int buttonModes) {
        super(app, name, new HTMLPanel(html), buttonModes);
        this.formname = formname;
        this.fieldname = fieldname;
    }

    public native String getFormFieldValue(String formname, String fieldname) /*-{
         var form = $doc.forms[formname];

     if (form) {
             var field = form[fieldname];
             if (field) {
                 if (field.length)
                   return field[0].value
                 else
                   return field.value;                        
             }
             else
                return "";
         } else
          return "";
     }-*/;

    @Override
    protected void endDialog() {
        String result = getFormFieldValue(formname, fieldname);
        setCurrentResult(result);
        hide();
        if (nextCallback!=null)
          nextCallback.onSuccess(getCurrentResult());
    }
}
