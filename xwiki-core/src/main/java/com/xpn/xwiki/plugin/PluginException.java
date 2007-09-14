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
 *
 */
package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiException;


public class PluginException extends XWikiException{
    String pluginName = "";

    public PluginException(String pluginName, int code, String message, Throwable e, Object[] args)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, code, pluginName + ": " + message, e, args);
        setPluginName(pluginName);
    }

    public PluginException(String pluginName, int code, String message, Throwable e){
        super(XWikiException.MODULE_XWIKI_PLUGINS, code, pluginName + ": " + message, e);
        setPluginName(pluginName);
    }

    public PluginException(String pluginName, int code, String message){
        super(XWikiException.MODULE_XWIKI_PLUGINS, code, pluginName + ": " + message);
        setPluginName(pluginName);
    }

    //java.lang.Class aClass
        public PluginException(java.lang.Class plugin, int code, String message, Throwable e, Object[] args)
    {
        super(XWikiException.MODULE_XWIKI_PLUGINS, code, plugin.getName() + ": " + message, e, args);
        setPluginName(plugin.getName());
    }

    public PluginException(java.lang.Class plugin, int code, String message, Throwable e){
        super(XWikiException.MODULE_XWIKI_PLUGINS, code, plugin.getName() + ": " + message, e);
        setPluginName(plugin.getName());
    }

    public PluginException(java.lang.Class plugin, int code, String message){
        super(XWikiException.MODULE_XWIKI_PLUGINS, code, plugin.getName() + ": " + message);
        setPluginName(plugin.getName());
    }

    public PluginException(){
        super();
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }
}
