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

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import com.xpn.xwiki.gwt.api.client.XWikiService;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTDefaultApp;
import com.xpn.xwiki.wysiwyg.client.plugin.Config;
import com.xpn.xwiki.wysiwyg.client.plugin.internal.DefaultConfig;
import com.xpn.xwiki.wysiwyg.client.ui.XWysiwygEditor;
import com.xpn.xwiki.wysiwyg.client.ui.XWysiwygEditorDebugger;
import com.xpn.xwiki.wysiwyg.client.ui.XWysiwygEditorFactory;

public class Wysiwyg extends XWikiGWTDefaultApp implements EntryPoint
{
    /**
     * {@inheritDoc}
     * 
     * @see EntryPoint#onModuleLoad()
     */
    public void onModuleLoad()
    {
        if (!GWT.isScript()) {
            XWikiService.App.getInstance().login("Admin", "admin", true, new XWikiAsyncCallback(this)
            {
                public void onFailure(Throwable caught)
                {
                    super.onFailure(caught);
                }

                public void onSuccess(Object result)
                {
                    super.onSuccess(result);
                    loadUI();
                }
            });
        } else {
            loadUI();
        }
    }

    private void loadUI()
    {
        setName("Wysiwyg");
        for (final Config config : getConfigs()) {
            String hookId = config.getParameter("hookId");
            if (hookId == null) {
                continue;
            }

            Element hook = DOM.getElementById(hookId);
            if (hook == null) {
                continue;
            }

            // Extract info from DOM
            String name = hook.getAttribute("name");
            String value = hook.getPropertyString("value");
            if (value == null) {
                value = hook.getInnerHTML();
            }
            String height = String.valueOf(Math.max(hook.getOffsetHeight(), 100)) + "px";

            // Prepare the DOM
            Element container = DOM.createDiv();
            final String containerId = hookId + "_container";
            container.setId(containerId);
            hook.getParentElement().replaceChild(container, hook);

            // Create the WYSIWYG Editor
            final XWysiwygEditor editor = XWysiwygEditorFactory.getInstance().newEditor(config, this);
            editor.getUI().getTextArea().setHTML(value);
            editor.getUI().getTextArea().setHeight(height);
            if (name != null) {
                editor.getUI().getTextArea().setName(name);
            }

            // Insert the WYSIWYG Editor
            if ("true".equals(config.getParameter("debug", "false"))) {
                RootPanel.get(containerId).add(new XWysiwygEditorDebugger(editor));
            } else {
                RootPanel.get(containerId).add(editor.getUI());
            }
        }
    }

    private List<Config> getConfigs()
    {
        List<Config> configs = new ArrayList<Config>();
        int i = 0;
        Config config = getConfig(i++);
        while (config != null) {
            configs.add(config);
            config = getConfig(i++);
        }
        return configs;
    }

    private Config getConfig(int index)
    {
        Dictionary dictionary = null;
        try {
            dictionary = Dictionary.getDictionary("Wysiwyg" + String.valueOf(index));
            return new DefaultConfig(dictionary);
        } catch (MissingResourceException e) {
            return null;
        }
    }
}
