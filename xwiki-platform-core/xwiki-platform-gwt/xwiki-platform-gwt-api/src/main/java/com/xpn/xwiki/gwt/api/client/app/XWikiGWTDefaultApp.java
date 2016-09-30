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

import com.xpn.xwiki.gwt.api.client.XWikiServiceAsync;
import com.xpn.xwiki.gwt.api.client.XWikiGWTException;
import com.xpn.xwiki.gwt.api.client.XWikiService;
import com.xpn.xwiki.gwt.api.client.dialog.ModalMessageDialog;
import com.xpn.xwiki.gwt.api.client.dialog.LoadingDialog;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Window;
import com.google.gwt.core.client.GWT;

import java.util.Map;
import java.util.HashMap;

public class XWikiGWTDefaultApp  implements XWikiGWTApp {
    protected Translator translator;
    protected LoadingDialog loadingDialog;
    protected XWikiServiceAsync serviceInstance;
    protected String name;
    protected static Map _metaPropertiesMap = null;

    public XWikiGWTDefaultApp() {
        loadingDialog = new LoadingDialog(this);
    }


    public void onModuleLoad() {
    }

    @Override
    public String getName() {
        return (name==null) ? "app" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private static native Map getMetaProperties(Map map) /*-{
      var metas = $wnd.document.getElementsByTagName("meta");
      var n = metas.length;
      for (var i = 0; i < n; ++i) {
         var meta = metas[i];
         var name = meta.getAttribute("name");
         if (name && name == "gwt:property") {
            var content = meta.getAttribute("content");
            if (content) {
                var name = content;
                var value = "";
                var eq = content.indexOf("=");
                if (eq != -1) {
                    name  = content.substring(0, eq);
                    value = content.substring(eq+1);
                }
                map.@java.util.Map::put(Ljava/lang/Object;Ljava/lang/Object;)(name,value);
            } 
         }
      }
      return map;
   }-*/;

    /**
     * Native method in JavaScript to access gwt:property
     */
    public static String getProperty(String name) {
      if (_metaPropertiesMap == null) {
          _metaPropertiesMap = getMetaProperties(new HashMap());
      }
      return (String) _metaPropertiesMap.get(name);
    };

    public String getParam(String key) {
        return getParam(key, "");
    }

    public String getParam(String key, String defaultValue) {
            String param = getProperty(key);
            if ((param==null)||(param.equals("")))
                return defaultValue;
            else
                return param;
    }

    public int getParamAsInt(String key) {
        return getParamAsInt(key, 0);
    }

    public int getParamAsInt(String key, int defaultValue) {
        String param = getParam(key);
        if ((param==null)||(param.equals("")))
            return defaultValue;
        else
            return Integer.parseInt(param);
    }

    /**
     * Allows to access the name of the translations page provided in gwt parameters
     * @return
     */
    @Override
    public String getTranslationPage() {
        return getParam("translations", XWikiGWTAppConstants.XWIKI_DEFAULT_TRANSLATIONS_PAGE);
    }

    /**
     * Allows to access the skin base url provided in gwt params
     * @return
     */
    public String getSkinBaseURL() {
        String skinbaseurl = getParam("skinbaseurl");
        if (skinbaseurl.equals("")) {
            String skin = getSkin();
            return XWikiGWTAppConstants.XWIKI_DEFAULT_BASE_URL + "/" + XWikiGWTAppConstants.XWIKI_DEFAULT_ACTION_PATH + "/skin/" + skin;
        } else {
            return skinbaseurl;
        }
    }

    /**
     * Allows to acces the name of the skin provided in gwt params
     * @return
     */
    public String getSkin() {
        return getParam("skin", XWikiGWTAppConstants.XWIKI_DEFAULT_SKIN);
    }

    /**
     * Provides a translated string
     * @param key
     * @return
     */
    @Override
    public String getTranslation(String key) {
        if (translator!=null) {
            return translator.getTranslation(getName() + "." + key);
        } else {
            return key;
        }
    }

    /**
     * Provides a translated string
     * @param key
     * @return
     */
    @Override
    public String getTranslation(String key, String[] args) {
        if (translator!=null) {
            return translator.getTranslation(getName() + "." + key, args);
        } else {
            return key;
        }
    }

    /**
     * Creates an instance of an XWiki Service
     * @return
     */
    @Override
    public XWikiServiceAsync getXWikiServiceInstance() {
        if (serviceInstance == null) {
            String moduleBaseURL = GWT.getModuleBaseURL();
            String baseURL = moduleBaseURL.substring(0, moduleBaseURL.indexOf(GWT.getModuleName()) - 1);
            String defaultXWikiService = baseURL + XWikiGWTAppConstants.XWIKI_DEFAULT_SERVICE;

            serviceInstance = (XWikiServiceAsync) GWT.create(XWikiService.class);
            ((ServiceDefTarget) serviceInstance).setServiceEntryPoint(getParam("xwikiservice" , defaultXWikiService));
        }
        return serviceInstance;
    }


    /**
     * Constructs a skin file URL
     * @param file
     * @return
     */
    @Override
    public String getSkinFile(String file) {
        return getSkinBaseURL() + "/" + file;
    }

    /**
     * Private function to access the loading dialog
     * @return
     */
    private LoadingDialog getLoadingDialog() {
        return loadingDialog;
    }

    /**
     * Launches the loading box
     */
    @Override
    public void startLoading() {
        getLoadingDialog().startLoading();
    }

    /**
     * Closes the loading box if the number of calls to finish are equal to the number of call to startLoading
     */
    @Override
    public void finishLoading() {
        getLoadingDialog().finishLoading();
    }

    @Override
    public boolean isTranslatorLoaded() {
        return (translator!=null);
    }

    /**
     * Check if translator is loaded. This needs to be called.
     *
     * @param cback Where to call previousStep after the translator is loaded.
     */
    public void checkTranslator(AsyncCallback cback) {
            if (translator==null) {
            // We need to disable the loading box
            // otherwise it shows without translations
            getLoadingDialog().disable();
            translator = new Translator(this);
            translator.init(cback);
            getLoadingDialog().enable();
        } else {
            // We need to make sure call previousStep is sent
            if (cback!=null)
                cback.onSuccess(null);
        }
    }

    /**
     *
     * @param title
     * @param message
     */
    @Override
    public void showDialog(String title, String message) {
        new ModalMessageDialog(this, title, message);
    }

    /**
     *
     * @param message
     */
    @Override
    public void showDialog(String message) {
        new ModalMessageDialog(this, getTranslation("appname"), message);
    }


    @Override
    public void showError(Throwable caught) {
        if (caught instanceof XWikiGWTException) {
            XWikiGWTException exp = ((XWikiGWTException)caught);
            if (exp.getCode()== 9002) {
                // This is a login error
                showDialog(getTranslation("login_first"));
            }
            else if (exp.getCode()== 9001) {
                // This is a right error
                showDialog(getTranslation("missing_rights"));
            } else
                showError("" + exp.getCode(), exp.getFullMessage());
        }
        else {
            if (caught!=null)
             caught.printStackTrace();
            showError("", (caught==null) ? "" : caught.toString());
        }
    }

    @Override
    public void showError(String text) {
        showError("", text);
    }

    @Override
    public void showError(String code, String text) {
        String[] args = new String[1];
        args[0] = code;
        String message = getTranslation("errorwithcode", args) + "\r\n\r\n" + text;
        showDialog(message);
    }

    @Override
    public String getCSSPrefix() {
        return getParam("cssprefix", XWikiGWTAppConstants.XWIKI_DEFAULT_CSS_PREFIX);
    }

    public static native String getUserAgent() /*-{
        return navigator.userAgent.toString();
    }-*/;

    public static boolean isMSIE() {
        return (getUserAgent().indexOf("MSIE")!=-1);
    }

    public static boolean isGecki() {
        return (getUserAgent().indexOf("Gecko")!=-1);
    }

    public static boolean isSafari() {
        return (getUserAgent().indexOf("Safari")!=-1);
    }
    
    public static int getAbsoluteTop(Widget widget) {
        if (isMSIE())
            return widget.getAbsoluteTop();
        else if (widget instanceof ScrollPanel)
            return widget.getAbsoluteTop() + ((ScrollPanel)widget).getScrollPosition();
        else
            return widget.getAbsoluteTop();
    }

    public static void setMaxHeight(Widget widget) {
        int absoluteTop = getAbsoluteTop(widget);
        int newHeight = (Window.getClientHeight() - absoluteTop);
        if (newHeight>0)  {
            try {
                widget.setHeight(newHeight + "px");
            } catch (Exception e) {
                // We need to catch this call since in IE7
                // it seems to be able to break sometime on initial loading
            }
        }

    }


    @Override
    public String getLocale() {
        return getParam("locale", XWikiGWTAppConstants.XWIKI_DEFAULT_LOCALE);
    }
}
