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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.core.query.QueryRootNode;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.user.api.XWikiRightService;

/** Security version of HibernateQuery */
public class SecHibernateQuery extends HibernateQuery {
	public SecHibernateQuery(QueryRootNode tree, IQueryFactory qf) {
		super(tree, qf);
	}
	
	private boolean security = false;
	private List _allowdocs	= new ArrayList();
	public boolean constructWhere(StringBuffer sb) {		
		boolean r = super.constructWhere(sb);
		if (security) {
			if (r)
				sb.append(" and ");
			else
				sb.append(" where ");
			sb.append("doc.id in (:secdocids)"); // TODO: secdocids is reserved word of query api.
			_addHqlParam("secdocids", _allowdocs);
			/*String comma = "";
			for (Iterator iter = _allowdocs.iterator(); iter.hasNext();) {
				Long element = (Long) iter.next();
				sb.append(comma).append(element.toString());
				comma = ",";
			}
			sb.append(")");*/
			return true;
		} else return r;		
	}
	
	public List list() throws XWikiException {
		if (translator==null)
			translator = new XWikiHibernateQueryTranslator(getQueryTree());
		if (!isAllowed())
			throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED, "Access denied for query");
		final XWikiRightService rightserv = getContext().getWiki().getRightService();		
		final List rights = getNeededRight();
		final String user = getContext().getUser();
		
		final SepStringBuffer _real_select = _select;
		_allowdocs.clear();
		try {
			_select = new SepStringBuffer("doc", null);
			List doclst = null;
		
			security = false;
			int fr = _firstResult; _firstResult = -1;
			int fs = _fetchSize; _fetchSize = -1;
			doclst = super.list();
			_firstResult = fr;
			_fetchSize = fs;
			for (Iterator iter = doclst.iterator(); iter.hasNext();) {
				XWikiDocument element = (XWikiDocument) iter.next();
				try {
					boolean r = true;
					for (int i=0; i<rights.size(); i++) {
						r &= rightserv.hasAccessLevel((String) rights.get(i), user, element.getFullName(), getContext());
					}
					if (r)
						_allowdocs.add(new Long(element.getId()));
				} catch (XWikiException e) {}
			}
		} finally {
			_select = _real_select;
		}
		if (_allowdocs.isEmpty()) return new ArrayList();
		security = true;
		
		final List lst = super.list();
		String sel = _select.toString();
		if (!sel.equals("doc") && !sel.equals("obj"))
			return lst;
		// wrapping
		final List result = new ArrayList();
		for (Iterator iter = lst.iterator(); iter.hasNext();) {
			final Object element = (Object) iter.next();
			if (element instanceof XWikiDocument) {
				XWikiDocument doc = (XWikiDocument) element;
				doc = getStore().loadXWikiDoc(doc, getContext());
				result.add(new Document(doc, getContext()));
			} else if (element instanceof BaseObject) {
				getHibernateStore().loadXWikiObject((BaseObject) element, getContext(), true);
				result.add(new com.xpn.xwiki.api.Object((BaseObject) element, getContext()));
			} else if (element instanceof XWikiAttachment) { // TODO: get document. Impossible for now
				XWikiAttachment attach = (XWikiAttachment) element;
				result.add(new com.xpn.xwiki.api.Attachment(null, attach, getContext()));
			} else
				result.add(element);
		}
		return result;
	}
	static Set _viewRight = new HashSet();
	static {
		_viewRight.add("doc.name");
		_viewRight.add("doc.fullname");
		_viewRight.add("obj.name");
	}
	public List getNeededRight() {
		if (translator==null)
			translator = new XWikiHibernateQueryTranslator(getQueryTree());
		boolean bview = false;
		boolean bquery = false;
		final String[] sels = StringUtils.split(_select.toString().toLowerCase()," ,");
		for (int i=0; i<sels.length; i++) {
			final String sel = sels[i];
			if (_viewRight.contains(sel))
				bview = true;
			else
				bquery = true;
		}
		final List r = new ArrayList(2);
		if (bview)
			r.add("view");
		if (bquery)
			r.add("query");
		return r;
	}
	public boolean isAllowed() {
		if (translator==null)
			translator = new XWikiHibernateQueryTranslator(getQueryTree());
		return !translator._propclass.contains(PasswordClass.class);
	}
}
