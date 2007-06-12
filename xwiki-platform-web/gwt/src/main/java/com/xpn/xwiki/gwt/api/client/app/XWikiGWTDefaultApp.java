package com.xpn.xwiki.gwt.api.client.app;

import com.xpn.xwiki.gwt.api.client.XWikiService;
import com.xpn.xwiki.gwt.api.client.XWikiServiceAsync;
import com.xpn.xwiki.gwt.api.client.XWikiGWTException;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Window;
import com.google.gwt.core.client.GWT;

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

public class XWikiGWTDefaultApp  implements com.xpn.xwiki.gwt.api.client.app.XWikiGWTApp {
    protected Translator translator;
    protected LoadingDialog loadingDialog;
    protected XWikiServiceAsync serviceInstance;


    public XWikiGWTDefaultApp() {
        loadingDialog = new com.xpn.xwiki.gwt.api.client.app.LoadingDialog(this);
    }


    public void onModuleLoad() {
    }

    /**
     * Native method in JavaScript to access gwt:property
     */
    public static native String getProperty(String name) /*-{
	 return $wnd.__gwt_getMetaProperty(name);
     }-*/;

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
    public String getTranslation(String key) {
        if (translator!=null) {
            return translator.getTranslation("watch." + key);
        } else {
            return key;
        }
    }

    /**
     * Provides a translated string
     * @param key
     * @return
     */
    public String getTranslation(String key, String[] args) {
        if (translator!=null) {
            return translator.getTranslation("watch." + key, args);
        } else {
            return key;
        }
    }

    /**
     * Creates an instance of an XWiki Service
     * @return
     */
    public XWikiServiceAsync getXWikiServiceInstance() {
        if (serviceInstance == null) {
            serviceInstance = (XWikiServiceAsync) GWT.create(XWikiService.class);
            String defaultXWikiService;
            if (GWT.isScript())
                defaultXWikiService = XWikiGWTAppConstants.XWIKI_DEFAULT_BASE_URL + XWikiGWTAppConstants.XWIKI_DEFAULT_SERVICE;
            else
                defaultXWikiService = GWT.getModuleBaseURL() + XWikiGWTAppConstants.XWIKI_DEFAULT_SERVICE;
            ((ServiceDefTarget) serviceInstance).setServiceEntryPoint(getParam("xwikiservice" , defaultXWikiService));
        }
        return serviceInstance;
    }


    /**
     * Constructs a skin file URL
     * @param file
     * @return
     */
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
    public void startLoading() {
        getLoadingDialog().startLoading();
    }

    /**
     * Closes the loading box if the number of calls to finish are equal to the number of call to startLoading
     */
    public void finishLoading() {
        getLoadingDialog().finishLoading();
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
    public void showDialog(String title, String message) {
        new ModalMessageDialogBox(this, title, message);
    }

    /**
     *
     * @param message
     */
    public void showDialog(String message) {
        new ModalMessageDialogBox(this, getTranslation("appname"), message);
    }


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

    public void showError(String text) {
        showError("", text);
    }

    public void showError(String code, String text) {
        String[] args = new String[1];
        args[0] = code;
        String message = getTranslation("errorwithcode", args) + "\r\n\r\n" + text;
        showDialog(message);
    }

    public String getCSSPrefix() {
        return getParam("cssprefix", XWikiGWTAppConstants.XWIKI_DEFAULT_CSS_PREFIX);
    }

    public static native String getUserAgent() /*-{
        return navigator.userAgent.toString();
    }-*/;

    public static boolean isMSIE() {
        return (getUserAgent().indexOf("MSIE")!=-1);
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


    public String getLocale() {
        return getParam("locale", XWikiGWTAppConstants.XWIKI_DEFAULT_LOCALE);
    }
}
