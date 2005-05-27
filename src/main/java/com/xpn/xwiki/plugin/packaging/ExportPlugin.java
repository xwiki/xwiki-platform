/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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

 * Created by
 * User: jeremi
 * Date: May 9, 2005
 * Time: 4:33:52 PM
 */
package com.xpn.xwiki.plugin.packaging;

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;

import java.io.IOException;

public class ExportPlugin  extends XWikiDefaultPlugin implements XWikiPluginInterface{



    public ExportPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
    }

    public void init(XWikiContext context)
    {
        super.init(context);
    }

    public String getName() {
        return "export";
    }


    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new ExportPluginApi((ExportPlugin) plugin, context);
    }
}
