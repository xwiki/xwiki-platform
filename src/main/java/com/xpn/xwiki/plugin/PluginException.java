package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiException;

/**
 * ===================================================================
 *
 * Copyright (c) 2005 XpertNet, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 */
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
