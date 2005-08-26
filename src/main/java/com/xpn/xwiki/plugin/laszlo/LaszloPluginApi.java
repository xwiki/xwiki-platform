/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 23 avr. 2005
 * Time: 00:57:33
 */
package com.xpn.xwiki.plugin.laszlo;

import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.PluginApi;

public class LaszloPluginApi extends PluginApi {

    public LaszloPluginApi(LaszloPlugin plugin, XWikiContext context) {
            super(plugin, context);
        }

    public LaszloPlugin getLaszloPlugin() {
        return (LaszloPlugin) getPlugin();
    }

    public String getFileName(String name, String laszlocode) {
        return getLaszloPlugin().getFileName(name, laszlocode);
    }

    public String getLaszloURL(String name, String laszlocode) throws IOException, XWikiException {
        return getLaszloPlugin().getLaszloURL(name, laszlocode);
    }

    public String getLaszloFlash(String name, String width, String height, String laszlocode) throws IOException, XWikiException {
        return getLaszloPlugin().getLaszloFlash(name, width, height, laszlocode, context);
    }

    public void flushCache() {
        getLaszloPlugin().flushCache();
    }
}
