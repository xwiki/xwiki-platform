package com.xpn.xwiki.plugin.packaging;

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

/**
 * ===================================================================
 * <p/>
 * Copyright (c) 2005 Jérémi Joslin, All rights reserved.
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 * <p/>
 * User: jeremi
 * Date: May 16, 2005
 * Time: 5:48:18 PM
 */
public class ImportPlugin   extends XWikiDefaultPlugin implements XWikiPluginInterface{



    public ImportPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
    }

    public void init(XWikiContext context)
    {
        super.init(context);
    }

    public String getName() {
        return "import";
    }


    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new ImportPluginApi((ImportPlugin) plugin, context);
    }
}