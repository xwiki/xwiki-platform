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
package org.xwiki.gwt.wysiwyg.client;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.user.client.BackForwardCache;
import org.xwiki.gwt.user.client.Config;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

/**
 * WYSIWYG Editor configuration. Use this class to access the WYSIWYG editor configuration in a strong-typed manner and
 * to cache configuration values.
 * 
 * @version $Id$
 */
public class WysiwygEditorConfig implements ClosingHandler
{
    /**
     * The default storage syntax.
     */
    public static final String DEFAULT_SYNTAX = "xhtml/1.0";

    /**
     * WYWISYWG tab index in the TabPanel.
     */
    public static final int WYSIWYG_TAB_INDEX = 0;

    /**
     * Source tab index in the TabPanel.
     */
    public static final int SOURCE_TAB_INDEX = 1;

    /**
     * The key used to cache the active text area, i.e. the text area that should be visible when the editor is loaded.
     */
    private static final String CACHE_KEY_ACTIVE_TEXT_AREA = "editor.activeTextArea";

    /**
     * The key used to cache the type of WYSIWYG editor input value: {@code true} if the input value is HTML, {@code
     * false} if the input value is in source syntax (it needs to be converted to HTML).
     */
    private static final String CACHE_KEY_INPUT_CONVERTED = "editor.inputConverted";

    /**
     * The configuration source.
     */
    private final Config config;

    /**
     * The object used to cache configuration options.
     */
    private final BackForwardCache cache;

    /**
     * Creates a new WYSIWYG editor configuration object based on the given configuration source.
     * 
     * @param config the configuration source
     */
    public WysiwygEditorConfig(Config config)
    {
        this.config = config;
        cache = new BackForwardCache(Element.as(DOM.getElementById(config.getParameter("cacheId", ""))));
        Window.addWindowClosingHandler(this);
    }

    /**
     * @return the syntax used to store the edited content
     */
    public String getSyntax()
    {
        return config.getParameter("syntax", DEFAULT_SYNTAX);
    }

    /**
     * @return {@code true} if the WYSIWYG editor input value is HTML (no conversion is required), {@code false}
     *         otherwise (the input value is in source syntax and needs to be converted to HTML)
     */
    public boolean isInputConverted()
    {
        boolean inputConverted = !Boolean.valueOf(config.getParameter("convertInput", String.valueOf(false)));
        return Boolean.valueOf(cache.get(CACHE_KEY_INPUT_CONVERTED, String.valueOf(inputConverted)));
    }

    /**
     * Sets whether the WYSIWYG editor needs to convert its input from source syntax to HTML.
     * 
     * @param inputConverted {@code true} if the WYSIWYG editor doesn't need to convert its input value because it's
     *            HTML, {@code false} otherwise (the editor needs to convert the input value from source syntax to HTML)
     */
    public void setInputConverted(boolean inputConverted)
    {
        cache.put(CACHE_KEY_INPUT_CONVERTED, String.valueOf(inputConverted));
    }

    /**
     * @return the element that is replaced by the WYSIWYG editor
     */
    public Element getHook()
    {
        return Element.as(DOM.getElementById(config.getParameter("hookId")));
    }

    /**
     * @return WYSIWYG editor input value
     */
    public String getInputValue()
    {
        return getHook().getPropertyString("value");
    }

    /**
     * @return {@code true} if the editor is enabled, {@code false} otherwise
     */
    public boolean isEnabled()
    {
        Element hook = getHook();
        return !hook.getPropertyBoolean("disabled") && !hook.getPropertyBoolean("readOnly");
    }

    /**
     * @return {@code true} if the WYSIWYG/Source tabs are displayed, {@code false} otherwise
     */
    public boolean isTabbed()
    {
        return Boolean.valueOf(config.getParameter("displayTabs", String.valueOf(false)));
    }

    /**
     * @return the index of the tab that should be selected when the WYSIWYG editor is loaded
     */
    public int getSelectedTabIndex()
    {
        String defaultEditor = config.getParameter("defaultEditor");
        int defaultTabIndex = "wysiwyg".equals(defaultEditor) ? WYSIWYG_TAB_INDEX : SOURCE_TAB_INDEX;
        return Integer.parseInt(cache.get(CACHE_KEY_ACTIVE_TEXT_AREA, String.valueOf(defaultTabIndex)));
    }

    /**
     * Caches the active text area.
     * 
     * @param selectedTabIndex the index of the selected tab
     */
    public void setSelectedTabIndex(int selectedTabIndex)
    {
        cache.put(CACHE_KEY_ACTIVE_TEXT_AREA, String.valueOf(selectedTabIndex));
    }

    /**
     * @return rich text area's template URL
     */
    public String getTemplateURL()
    {
        return config.getParameter("inputURL");
    }

    /**
     * @return {@code true} if the WYSIWYG editor is in debug mode, {@code false} otherwise
     */
    public boolean isDebugMode()
    {
        return Boolean.valueOf(config.getParameter("debug", String.valueOf(false)));
    }

    /**
     * @return the configuration source
     */
    public Config getConfigurationSource()
    {
        return config;
    }

    @Override
    public void onWindowClosing(ClosingEvent event)
    {
        // Make sure the cache is up to date before the page unloads. We have to do this because the queue of deferred
        // commands is discarded when the page unloads and the cache update command might not get executed.
        // NOTE: This is more of a hack since we shouldn't be aware of the internal cache implementation but there's no
        // easy way to schedule the cache update after all the window closing handlers and before the first window
        // closed handler.
        cache.update();
    }
}
