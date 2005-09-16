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
 */
package com.xpn.xwiki.plugin.query;

import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.store.XWikiStoreInterface;

/** Api for QueryPlugin */
public class QueryPluginApi extends Api implements IQueryFactory {
	private static final Log log = LogFactory.getLog(QueryPluginApi.class);
	QueryPlugin qp;
	public QueryPluginApi(QueryPlugin qp) {
		super(qp.getContext());
		this.qp = qp;
	}
	public IQuery getDocs(String docname, String prop, String order) throws InvalidQueryException {
		return qp.getDocs(docname, prop, order);
	}
	public IQuery getChildDocs(String docname, String prop, String order) throws InvalidQueryException {
		return qp.getChildDocs(docname, prop, order);
	}
	public IQuery getAttachment(String docname, String attachname, String order) throws InvalidQueryException {		
		return qp.getAttachment(docname, attachname, order);
	}
	public IQuery getObjects(String docname, String oclass, String prop, String order) throws InvalidQueryException {		
		return qp.getObjects(docname, oclass, prop, order);
	}
	public XWikiCache getCache() {		
		return qp.getCache();
	}
	public XWikiContext getContext() {		
		return qp.getContext();
	}
	public XWikiStoreInterface getStore() {
		return qp.getStore();
	}
	
	public IQuery xpath(String q) throws InvalidQueryException {
		if (log.isDebugEnabled())
			log.debug("create sec xpath query: "+q);
		if (qp.isHibernate())
			return new SecHibernateQuery( qp.parse(q, Query.XPATH), this);
		return null;
	}
	
	public IQuery ql(String q) throws InvalidQueryException {
		if (log.isDebugEnabled())
			log.debug("create sec JCRSQL query: "+q);
		if (qp.isHibernate())
			return new SecHibernateQuery( qp.parse(q, Query.SQL), this);
		return null;
	}
}
