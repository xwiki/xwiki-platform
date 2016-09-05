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

public interface XWikiGWTApp {

    /**
     * Returns the name of the app
     */
    public String getName();

    /**
     * Returns the css prefix of the app
     * @return
     */
    public String getCSSPrefix();

    /**
     * Returns the locale of the app
     * @return
     */
    public String getLocale();

    /**
     * Returns the translation page to load
     * @return
     */
    public String getTranslationPage();

    /**
     * Provides a translation of a text string
     * @param key
     * @return  translated string
     */
    public String getTranslation(String key);

    /**
     * Provides a translation of a text string
     * @param key
     * @param args
     * @return  translated string
     */
    public String getTranslation(String key, String[] args);

    /**
     * Retrieves an instance of an XWiki Remove Service
     * @return
     */
    public XWikiServiceAsync getXWikiServiceInstance();

    /**
     * Retrieves a url path from the current skin
     * @param file
     * @return A loadable url pointing to the file in the current skin
     */
    public String getSkinFile(String file);

    /**
     * Launch the loading dialog. Each call to startLoading will add 1 to counter
     * The dialog will be closed only if the same amount of calls to finishLoading is made
     */
    public void startLoading();

    /**
     * Close the loading dialog when the same amount of call to finishLoading are
     * made than call to startLoading
     */
    public void finishLoading();

    /**
     *
     * @param title
     * @param message
     */
    public void showDialog(String title, String message);

    /**
     *
     * @param message
     */
    public void showDialog(String message);


    public void showError(Throwable caught);

    public void showError(String text);

    public void showError(String code, String text);

    public boolean isTranslatorLoaded();

}
