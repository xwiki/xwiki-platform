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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTDefaultApp;
import com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditor;
import com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditorDebugger;
import com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditorFactory;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.util.internal.DefaultConfig;
import com.xpn.xwiki.wysiwyg.client.widget.rta.DocumentTemplate;

/**
 * The class responsible for loading the WYSIWYG editors. It can be also viewed as the application context.
 * 
 * @version $Id$
 */
public class Wysiwyg extends XWikiGWTDefaultApp implements EntryPoint
{
    /**
     * {@inheritDoc}
     * 
     * @see EntryPoint#onModuleLoad()
     */
    public void onModuleLoad()
    {
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
                    loadUI();
                }
            });
        } else {
            loadUI();
        }
    }

    /**
     * Loads all the WYSIWYG editors on the host page.
     */
    private void loadUI()
    {
        for (final Config config : getConfigs()) {
            String hookId = config.getParameter("hookId");
            if (hookId == null) {
                continue;
            }

            Element hook = DOM.getElementById(hookId);
            if (hook == null) {
                continue;
            }

            Element input = hook;
            String inputId = config.getParameter("inputId");
            if (inputId != null) {
                input = DOM.getElementById(inputId);
                input = input == null ? hook : input;
            }

            // Extract info from DOM
            String name = hook.getAttribute("name");
            String value = input.getPropertyString("value");
            if (value == null) {
                value = input.getInnerHTML();
            }
            String height = String.valueOf(Math.max(hook.getOffsetHeight(), 100)) + "px";

            // Prepare the DOM
            Element container = DOM.createDiv();
            String containerId = hookId + "_container";
            container.setId(containerId);
            hook.getParentElement().replaceChild(container, hook);

            // Create the WYSIWYG Editor
            final WysiwygEditor editor = WysiwygEditorFactory.getInstance().newEditor(config, this);
            initDocumentTemplate(editor.getUI().getTextArea().getDocumentTemplate(), config);
            editor.getUI().getTextArea().setHTML(value);
            editor.getUI().getTextArea().setHeight(height);
            if (name != null) {
                editor.getUI().getTextArea().setName(name);
            }

            // Insert the WYSIWYG Editor
            if ("true".equals(config.getParameter("debug", "false"))) {
                RootPanel.get(containerId).add(new WysiwygEditorDebugger(editor));
            } else {
                RootPanel.get(containerId).add(editor.getUI());
            }
        }
    }

    /**
     * Initializes the document template to use for the rich text area document.
     * 
     * @param template the template to initialize
     * @param config configuration parameters
     */
    private void initDocumentTemplate(DocumentTemplate template, Config config)
    {
        String stylesheet = config.getParameter("stylesheet");
        if (!StringUtils.isEmpty(stylesheet)) {
            template.addStyleSheet(stylesheet);
        }
        template.addStyleSheet(GWT.getModuleBaseURL() + "RichTextArea.css");
        template.setBodyId("body");
        template.addBodyStyleName("main");
        template.setBaseURL(Window.Location.getHref());
    }

    /**
     * @return The list of configuration objects present in the host page.
     */
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

    /**
     * Retrieves the configuration object associated with the WYSIWYG editor with the specified index. We can have more
     * than one WYSIWYG editor in a host page and thus each editor will have an index. The first index is 0. A
     * configuration object is a JavaScript object that can be loaded with GWT's {@link Dictionary} mechanism.
     * 
     * @param index The index of the editor whose configuration object must be retrieved.
     * @return The configuration object for the specified editor.
     */
    private Config getConfig(int index)
    {
        Dictionary dictionary = null;
        try {
            dictionary = Dictionary.getDictionary(getName() + String.valueOf(index));
            return new DefaultConfig(dictionary);
        } catch (MissingResourceException e) {
            return null;
        }
    }
}
