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

import java.util.HashMap;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;
import com.xpn.xwiki.gwt.api.client.app.XWikiGWTDefaultApp;
import com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditor;
import com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditorDebugger;
import com.xpn.xwiki.wysiwyg.client.editor.WysiwygEditorFactory;
import com.xpn.xwiki.wysiwyg.client.util.Config;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.util.internal.DefaultConfig;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

/**
 * The class responsible for loading the WYSIWYG editors. It can be also viewed as the application context.
 * 
 * @version $Id$
 */
public class Wysiwyg extends XWikiGWTDefaultApp implements EntryPoint
{
    /**
     * Wysiwyg editors.
     */
    private Map<String, WysiwygEditor> editors = new HashMap<String, WysiwygEditor>();

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
                    init();
                }
            });
        } else {
            init();
        }
    }

    /**
     * Initialization of the WYSIWYG editors.
     */
    private void init()
    {
        createController(this);
        loadConfigurations();
        loadEditors();
    }

    /**
     * Load all the configuration objects stored in Wysiwyg.configurations in the main Window object. This way we are
     * able to load them with Dictionary.get(String).
     */
    private native void loadConfigurations()
    /*-{
        var configurations = $wnd.WysiwygConfigurations;
        configurations.reverse();
        while (configurations.size() > 0) {
            var config = configurations.pop();
            var configId = "wysiwygConfiguration_" + config.hookId;
            $wnd[configId] = config;
            this.@com.xpn.xwiki.wysiwyg.client.Wysiwyg::addEditor(Ljava/lang/String;)(config.hookId);            
        }
    }-*/;

    /**
     * Creates the WYSIWYG controller JavaScript object. Stored in the window object, the controller provides the
     * following methods:
     * <ul>
     * <li>Wysiwyg.getPlainTextArea(String): get the TextArea for the editor with the given ID.</li>
     * </ul>
     * 
     * @param wysiwygEntryPoint We pass the current object to the method. Using 'this' is supposed to work but does not.
     */
    private native void createController(Wysiwyg wysiwygEntryPoint)
    /*-{
        $wnd.Wysiwyg.getPlainTextArea = function(id) {
            return 
              wysiwygEntryPoint.@com.xpn.xwiki.wysiwyg.client.Wysiwyg::getPlainTextAreaElement(Ljava/lang/String;)(id);
        };
        $wnd.Wysiwyg.getRichTextArea = function(id) {
            return 
              wysiwygEntryPoint.@com.xpn.xwiki.wysiwyg.client.Wysiwyg::getRichTextAreaElement(Ljava/lang/String;)(id);
        };
    }-*/;

    /**
     * Add an editor to the list of the editors to load.
     * 
     * @param id Id of the editor.
     */
    private void addEditor(String id)
    {
        editors.put(id, null);
    }

    /**
     * Load the WYSIWYG editor from a configuration object.
     * 
     * @param id Id of the editor.
     * @return The newly created editor.
     */
    private WysiwygEditor loadEditor(String id)
    {
        if (!isRichTextEditingSupported()) {
            return null;
        }

        Config config = getConfig(id);

        TextAreaElement hook = TextAreaElement.as(DOM.getElementById(id));
        if (hook == null) {
            return null;
        }

        // Prepare the DOM
        Element container = DOM.createDiv();
        String containerId = id + "_container";
        container.setId(containerId);
        hook.getParentElement().insertBefore(container, hook);

        // Create the WYSIWYG Editor
        WysiwygEditor wysiwygEditor = WysiwygEditorFactory.getInstance().newEditor(config, this);

        // Insert the WYSIWYG Editor
        if (Boolean.TRUE.toString().equals(config.getParameter("debug", "false"))) {
            RootPanel.get(containerId).add(new WysiwygEditorDebugger(wysiwygEditor));
        } else {
            RootPanel.get(containerId).add(wysiwygEditor.getUI());
        }

        // Add the editor to the editors list.
        editors.put(id, wysiwygEditor);

        return wysiwygEditor;
    }

    /**
     * Load all the editors that have been found in the page.
     */
    public void loadEditors()
    {
        Set<String> ids = editors.keySet();
        for (final String id : ids) {
            loadEditor(id);
        }
    }

    /**
     * Get the WYSIWYG editor with the given ID. The id of an editor is the name of the TextArea that it wraps.
     * 
     * @param id ID of the editor to get.
     * @return The editor corresponding to the given ID.
     */
    public WysiwygEditor getEditor(String id)
    {
        return editors.get(id);
    }

    /**
     * Get the plain textarea element (textarea) for the editor with the given ID.
     * 
     * @param id ID of the editor.
     * @return The plain textarea element for the editor with the given ID.
     */
    public Element getPlainTextAreaElement(String id)
    {
        return getEditor(id).getPlainTextEditor().getTextArea().getElement();
    }

    /**
     * Get the rich textarea element (iframe) for the editor with the given ID.
     * 
     * @param id ID of the editor.
     * @return The rich textarea element for the editor with the given ID.
     */
    public Element getRichTextAreaElement(String id)
    {
        return getEditor(id).getRichTextEditor().getTextArea().getElement();
    }

    /**
     * @return true if the current browser supports rich text editing.
     */
    private boolean isRichTextEditingSupported()
    {
        RichTextArea textArea = new RichTextArea(null);
        return textArea.getBasicFormatter() != null;
    }

    /**
     * Retrieves the configuration object associated with the WYSIWYG editor with the specified id. We can have more
     * than one WYSIWYG editor in a host page and thus each editor is identified by the ID of the textarea is wraps. A
     * configuration object is a JavaScript object that can be loaded with GWT's {@link Dictionary} mechanism.
     * 
     * @param id Name of the editor to load.
     * @return The configuration object for the specified editor.
     */
    private Config getConfig(String id)
    {
        Dictionary dictionary = null;
        try {
            dictionary = Dictionary.getDictionary("wysiwygConfiguration_" + id);
            return new DefaultConfig(dictionary);
        } catch (MissingResourceException e) {
            return null;
        }
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
