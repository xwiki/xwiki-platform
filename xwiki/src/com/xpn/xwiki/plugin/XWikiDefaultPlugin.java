/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 * Date: 21 janv. 2004
 * Time: 10:46:46
 */
package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiContext;

public class XWikiDefaultPlugin implements XWikiPluginInterface {
    private String name;

    public XWikiDefaultPlugin(String name, XWikiContext context) {
        setName(name);
    }

    public void init(XWikiContext context) {
    }

    public String commonTagsHandler(String line, XWikiContext context) {
        return line;
    }

    public String startRenderingHandler(String line, XWikiContext context) {
        return line;
    }

    public String outsidePREHandler(String line, XWikiContext context) {
        return line;
    }

    public String insidePREHandler(String line, XWikiContext context) {
        return line;
    }

    public String endRenderingHandler(String line, XWikiContext context) {
        return line;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
