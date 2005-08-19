/**
 * ===================================================================
 *
 * Copyright (c) 2005 Artem Melentev, All rights reserved.
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
 * User: Artem Melentev
 * Date: 16.08.2005
 * Time: 04:12
 */
package com.xpn.xwiki.plugin.query;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/** Plugin for Query API */
public class QueryPlugin extends XWikiDefaultPlugin {
	XWikiCache cache;
	public QueryPlugin(String name, String className, XWikiContext context) {
		super(name, className, context);
		cache = context.getWiki().getCacheService().newCache();
	}
	public String getName() { return "query"; }
	
	public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {		
		return new QueryFactory(context, cache);
	}
	
	protected XWikiCache getCache() { return cache; }
}
