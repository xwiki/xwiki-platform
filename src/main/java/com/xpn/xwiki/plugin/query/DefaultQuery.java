/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */

package com.xpn.xwiki.plugin.query;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiStoreInterface;
import org.apache.jackrabbit.core.query.QueryRootNode;

import java.util.List;

public class DefaultQuery implements IQuery {
	protected IQueryFactory _queryFactory;
	protected QueryRootNode _querytree;
	
	public DefaultQuery(QueryRootNode tree, IQueryFactory qf) {
		_querytree = tree;
		_queryFactory = qf;
	}
	
	public List list() throws XWikiException {
		return null;
	}

    public String getNativeQuery() {
        return null;
    }

    protected int _fetchSize=-1;
	protected int _firstResult=-1;
	protected boolean _isdistinct=false;
	public IQuery setMaxResults(int fs)		{ _fetchSize = fs; return this; }
	public IQuery setFirstResult(int fr)	{ _firstResult = fr; return this; }	
	public IQuery setDistinct(boolean d)	{ _isdistinct = d; return this; }
	
	protected XWikiContext getContext() {
		return _queryFactory.getContext();
	}
	protected XWikiStoreInterface getStore() {
		return _queryFactory.getStore();
	}
	protected QueryRootNode getQueryTree() { return _querytree; }
	
	public String toString() {
		try {
			return list().toString();
		} catch (XWikiException e) {}
		return "";
	}
}
