/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author amelentev
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
