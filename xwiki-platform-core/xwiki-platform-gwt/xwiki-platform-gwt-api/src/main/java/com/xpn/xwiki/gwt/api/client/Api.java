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
package com.xpn.xwiki.gwt.api.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTDefaultApp;
import com.xpn.xwiki.gwt.api.client.dialog.EditPropDialog;
import com.xpn.xwiki.gwt.api.client.dialog.Dialog;


public class Api implements EntryPoint {
     XWikiGWTDefaultApp app;

    public void onModuleLoad() {
        app = new  XWikiGWTDefaultApp();
        app.setName("gwtapi");
        loadJSApi(this);
    }

    /**
     * Creates the Javascript API interface that can be used by web pages.
     *
     * @param x  An instance of "this" for the callbacks to be made to.
     */
    public native void loadJSApi(Api x) /*-{
       
        $wnd.editProperty = function(fullname, className, propname, cb) {
             x.@com.xpn.xwiki.gwt.api.client.Api::editProperty(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(fullname, className, propname, cb);
        };
        $wnd.xwikiGWTLoaded = true;
    }-*/;


    public native void jscallback(JavaScriptObject callback, Object result) /*-{
        callback(result);
    }-*/;

    public void editProperty(final String fullname, final String className, final String propname, final JavaScriptObject callback) {
         if (!app.isTranslatorLoaded()) {
            app.checkTranslator(new XWikiAsyncCallback(app) {
                public void onFailure(Throwable caught) {
                    super.onFailure(caught);
                    editProperty(fullname, className, propname, callback);
                }

                public void onSuccess(Object result) {
                    super.onSuccess(result);
                    editProperty(fullname, className, propname, callback);
                }
            });
            return;
        }

        XWikiService.App.getInstance().getDocument(fullname, true, true, true, false, new XWikiAsyncCallback(app) {
            public void onFailure(Throwable caught) {
                Window.alert(app.getTranslation("error.failuregetdocument"));
                super.onFailure(caught);
            }

            public void onSuccess(Object result) {
                super.onSuccess(result);
                Document doc = (Document) result;
                XObject obj = doc.getObject(className);
                if (obj == null)
                    Window.alert(app.getTranslation("error.errorgettingfield"));
                else {
                    final String fieldname = obj.getClassName() + "_" + obj.getNumber() + "_" + propname;
                    final String formname = "editpropform";

                    String field = obj.getEditProperty(propname);
                    String form = "<form name='" + formname + "' id='" + formname + "'>" + field + "</form>";
                    // let's ask for editing
                    EditPropDialog dialog = new EditPropDialog(app, "editfield", form, formname, fieldname, Dialog.BUTTON_CANCEL | Dialog.BUTTON_NEXT);
                    dialog.setTitle(app.getTranslation("editfield"));
                    dialog.setMessage("chooseyourvalue", null);
                    dialog.setNextCallback(new AsyncCallback() {
                        public void onFailure(Throwable throwable) {
                        }

                        public void onSuccess(Object object) {
                            final String value = (String) object;
                            if ((value!=null)&&(value!="undefined")) {
                                // let's update the property
                                XWikiService.App.getInstance().updateProperty(fullname, className, propname, value, new XWikiAsyncCallback(app) {
                                    public void onFailure(Throwable caught) {
                                        super.onFailure(caught);
                                        Window.alert(app.getTranslation("error.failedupdatingdata"));
                                    }

                                    public void onSuccess(Object result) {
                                        super.onSuccess(result);
                                        // let's retrieve the new display value now
                                        XWikiService.App.getInstance().getDocument(fullname, true, true, false, false, new XWikiAsyncCallback(app) {
                                                 public void onFailure(Throwable caught) {
                                                     Window.alert(app.getTranslation("error.failuregetdocument"));
                                                     super.onFailure(caught);
                                                     jscallback(callback, value);
                                                 }

                                                 public void onSuccess(Object result) {
                                                     super.onSuccess(result);
                                                     String newValue = value;
                                                     Document doc = (Document) result;
                                                     XObject obj = doc.getObject(className);
                                                     if (obj != null)
                                                                newValue = obj.getViewProperty(propname);
                                                     jscallback(callback, newValue);
                                                 }
                                        });
                                    }
                                });
                            } else {
                                Window.alert(app.getTranslation("error.errorretrievingdata"));
                            }
                        }
                    });
                    dialog.show();
                }
            }
        });
    }


  
}
