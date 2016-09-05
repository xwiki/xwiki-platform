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
package com.xpn.xwiki.gwt.api.client.app;

import com.xpn.xwiki.gwt.api.client.Dictionary;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.core.client.GWT;

public class Translator {
    private XWikiGWTApp app;
    private Dictionary dictionary = null;
    private AsyncCallback callback;
    private TextArea missingTranslation = null;

    public Translator(XWikiGWTApp app){
        this.app = app;
    }

    /**
     * Adding the key to the missing translations
     * This will allow to detect all keys that have not been translated
     * @param key
     */
    public void addToMissingTranslation(String key){
        if (missingTranslation == null) {
            missingTranslation = new TextArea();
            missingTranslation.setVisible(false);
            RootPanel.get().add(missingTranslation);
            missingTranslation.addStyleName("xwiki-gwtapi-missingtranslation");
        }
        String txt = missingTranslation.getText();
        if (txt == null){
            txt = "";
        }
        txt = txt + "\n" + key + ":";
        missingTranslation.setText(txt);
    }

    /**
     * Retrieves a translation string
     * @param key
     * @return
     */
    public String getTranslation(String key) {
        if (dictionary == null){
            return key;
        }
        if (dictionary.get(key) == null){
            addToMissingTranslation(key);
        }
        return (dictionary.get(key) != null) ? dictionary.get(key) : key ;
    }

    public String getTranslation(String key, String[] args) {
        String oStr = getTranslation(key);
        String oStr2;

        if (args==null)
            return oStr;
        
        for (int i = 0; i<args.length; i++){
            if (GWT.isScript()) {
                oStr2 = oStr.replaceAll("\\{"+i+"\\}", args[i]);
            } else {
                oStr2 = oStr.replaceAll("\\{"+i+"\\}", args[i]);
            }
            oStr = oStr2;
        }

        return oStr;
    }

    public void init() {
        app.getXWikiServiceInstance().getTranslation(app.getTranslationPage(), app.getLocale(), new XWikiAsyncCallback(app) {
            @Override
            public void onSuccess(Object result) {
                super.onSuccess(result);
                dictionary = (Dictionary) result;
                if (callback != null){
                    callback.onSuccess(result);
                }
            }
        });
    }

    public void init(AsyncCallback callback){
        this.callback = callback;
        init();
    }
}
