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
 */
package com.xpn.xwiki.plugin.query;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.cache.api.XWikiCache;

public class SecQueryFactory extends QueryFactory {	
	public SecQueryFactory(XWikiContext context, XWikiCache cache) {
		super(context, cache);
	}	
	public IQuery xpath(String q) throws InvalidQueryException {
		if (isHibernate())
			return new SecHibernateQuery( parse(q, Query.XPATH), this);
		return null;
	}
	
	public IQuery ql(String q) throws InvalidQueryException {
		if (isHibernate())
			return new SecHibernateQuery( parse(q, Query.SQL), this);
		return null;
	}
}
