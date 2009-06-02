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
package com.xpn.xwiki.wysiwyg.client;

import java.util.MissingResourceException;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTDefaultApp;
import com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditorApi;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

/**
 * The class responsible for loading the WYSIWYG editors. It can be also viewed as the application context.
 * 
 * @version $Id$
 */
public class Wysiwyg extends XWikiGWTDefaultApp implements EntryPoint
{
    /**
     * A static reference to this GWT module.
     */
    private static Wysiwyg instance;

    /**
     * {@inheritDoc}
     * 
     * @see EntryPoint#onModuleLoad()
     */
    public void onModuleLoad()
    {
        // This method should be called only once.
        instance = this;
        setName("Wysiwyg");
        // Test to see if we're running in hosted mode or web mode.
        if (!GWT.isScript()) {
            // We're running in hosted mode so we need to login first.
            getXWikiServiceInstance().login("Admin", "admin", true, new XWikiAsyncCallback(this)
            {
                public void onFailure(Throwable caught)
                {
                    super.onFailure(caught);
                }

                public void onSuccess(Object result)
                {
                    super.onSuccess(result);
                    WysiwygEditorApi.publish();
                }
            });
        } else {
            WysiwygEditorApi.publish();
        }
    }

    /**
     * @return a reference to this GWT module
     */
    public static Wysiwyg getInstance()
    {
        return instance;
    }

    /**
     * {@inheritDoc}<br/>
     * NOTE: We overwrite this method in order to be able to control the URL of the XWikiService.
     * 
     * @see XWikiGWTDefaultApp#getParam(String, String)
     */
    public String getParam(String key, String defaultValue)
    {
        // First look for meta gwt:property.
        String value = getProperty(key);
        if (!StringUtils.isEmpty(value)) {
            return value;
        }
        // Then look in the global configuration object.
        try {
            return Dictionary.getDictionary(getName()).get(key);
        } catch (MissingResourceException e) {
            return defaultValue;
        }
    }
}
