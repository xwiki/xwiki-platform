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
package com.xpn.xwiki.plugin;

import java.text.MessageFormat;

import com.xpn.xwiki.XWikiException;

/**
 * XWiki-specific exceptions thrown by plugins.
 * 
 * @version $Id$
 * @deprecated the plugin technology is deprecated, and XWikiException was a bad idea from the start
 */
@Deprecated
public class PluginException extends XWikiException
{
    /** Custom message format to use for plugin exceptions. */
    private static final MessageFormat CUSTOM_MESSAGE = new MessageFormat("Exception in plugin [{0}]: {1}");

    /** The name of the plugin that triggered the exception. */
    private final String pluginName;

    /**
     * Exception constructor.
     * 
     * @param pluginName the name of the plugin that triggered the exception
     * @param code exception code
     * @param message the exception message
     * @param e a nested exception that caused this exception in the first place
     * @param args extra information about the exception
     */
    public PluginException(String pluginName, int code, String message, Throwable e, Object[] args)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, code, CUSTOM_MESSAGE.format(new Object[] {pluginName, message}), e,
            args);
        this.pluginName = pluginName;
    }

    /**
     * Exception constructor.
     * 
     * @param pluginName the name of the plugin that triggered the exception
     * @param code exception code
     * @param message the exception message
     * @param e a nested exception that caused this exception in the first place
     */
    public PluginException(String pluginName, int code, String message, Throwable e)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, code, CUSTOM_MESSAGE.format(new Object[] {pluginName, message}), e);
        this.pluginName = pluginName;
    }

    /**
     * Exception constructor.
     * 
     * @param pluginName the name of the plugin that triggered the exception
     * @param code exception code
     * @param message the exception message
     */
    public PluginException(String pluginName, int code, String message)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, code, CUSTOM_MESSAGE.format(new Object[] {pluginName, message}));
        this.pluginName = pluginName;
    }

    /**
     * Exception constructor.
     * 
     * @param plugin the type of plugin that triggered the exception
     * @param code exception code
     * @param message the exception message
     * @param e a nested exception that caused this exception in the first place
     * @param args extra information about the exception
     */
    public PluginException(java.lang.Class< ? extends XWikiPluginInterface> plugin, int code, String message,
        Throwable e, Object[] args)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, code,
            CUSTOM_MESSAGE.format(new Object[] {plugin.getName(), message}), e, args);
        this.pluginName = plugin.getName();
    }

    /**
     * Exception constructor.
     * 
     * @param plugin the type of plugin that triggered the exception
     * @param code exception code
     * @param message the exception message
     * @param e a nested exception that caused this exception in the first place
     */
    public PluginException(java.lang.Class< ? extends XWikiPluginInterface> plugin, int code, String message,
        Throwable e)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, code,
            CUSTOM_MESSAGE.format(new Object[] {plugin.getName(), message}), e);
        this.pluginName = plugin.getName();
    }

    /**
     * Exception constructor.
     * 
     * @param plugin the type of plugin that triggered the exception
     * @param code exception code
     * @param message the exception message
     */
    public PluginException(java.lang.Class< ? extends XWikiPluginInterface> plugin, int code, String message)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, code,
            CUSTOM_MESSAGE.format(new Object[] {plugin.getName(), message}));
        this.pluginName = plugin.getName();
    }

    /** Empty constructor, with no information provided. */
    public PluginException()
    {
        super();
        this.pluginName = "unknown";
    }

    /**
     * Get the name of the plugin that triggered the exception.
     * 
     * @return the plugin name, see {@link XWikiPluginInterface#getName()}
     */
    public String getPluginName()
    {
        return this.pluginName;
    }
}
