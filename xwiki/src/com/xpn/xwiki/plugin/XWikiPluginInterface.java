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
 * Time: 10:46:28
 */
package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiContext;

public interface XWikiPluginInterface {
    String getName();
    void setName(String name);

    void init(XWikiContext context);

    String commonTagsHandler(String line, XWikiContext context);
    String startRenderingHandler(String line, XWikiContext context);
    String outsidePREHandler(String line, XWikiContext context);
    String insidePREHandler(String line, XWikiContext context);
    String endRenderingHandler(String line, XWikiContext context);

}
