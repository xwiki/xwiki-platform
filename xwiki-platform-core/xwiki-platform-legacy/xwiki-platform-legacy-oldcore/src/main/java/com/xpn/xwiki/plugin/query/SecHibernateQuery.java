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
 */
package com.xpn.xwiki.plugin.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jackrabbit.core.query.QueryRootNode;
import org.apache.jackrabbit.name.QName;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.plugin.query.HibernateQuery.XWikiHibernateQueryTranslator.ObjPropProperty;
import com.xpn.xwiki.plugin.query.HibernateQuery.XWikiHibernateQueryTranslator.ObjProperty;
import com.xpn.xwiki.user.api.XWikiRightService;

/** Security version of HibernateQuery */
@Deprecated
public class SecHibernateQuery extends HibernateQuery
{
    public SecHibernateQuery(QueryRootNode tree, IQueryFactory qf)
    {
        super(tree, qf);
    }

    private boolean security = false;

    private List _allowdocs = new ArrayList();

    @Override
    public boolean constructWhere(StringBuffer sb)
    {
        boolean r = super.constructWhere(sb);
        if (security) {
            if (r)
                sb.append(" and ");
            else
                sb.append(" where ");
            String docname = translator.getLastNameClass(qn_xwiki_document);
            if (docname == null)
                throw new TranslateException("To be implemented!"); // TODO:do
            sb.append(docname).append(".id in (:secdocids)"); // TODO: secdocids is reserved parametr.
            _addHqlParam("secdocids", _allowdocs);
            return true;
        } else
            return r;
    }

    Set isReturnClasses = new HashSet();

    boolean isQueryRight = false;

    boolean isViewRight = false;

    boolean isAllow = true;

    @Override
    protected void _addSelect(ObjProperty p)
    {
        super._addSelect(p);
        if (ObjProperty.class.equals(p.getClass())) {
            isReturnClasses.add(p.objclass);
        }
        if (ObjPropProperty.class.equals(p.getClass())) {
            ObjPropProperty p1 = (ObjPropProperty) p;
            if (XWikiDocument.class.equals(p1.objclass)) {
                if ("name".equals(p1.propname) || "fullName".equals(p1.propname))
                    isViewRight = true;
                else
                    isQueryRight = true;
            } else if (BaseObject.class.equals(p1.objclass)) {
                if ("name".equals(p1.propname))
                    isViewRight = true;
                else
                    isQueryRight = true;
            }
        } else
            isQueryRight = true;
    }

    @Override
    protected void _addPropClass(Class class1)
    {
        super._addPropClass(class1);
        if (PasswordClass.class.equals(class1))
            isAllow = false;
    }

    @Override
    public List list() throws XWikiException
    {
        if (translator == null)
            translator = new XWikiHibernateQueryTranslator(getQueryTree());
        if (!isAllowed())
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied for query");
        final XWikiRightService rightserv = getContext().getWiki().getRightService();
        final List rights = getNeededRight();
        final String user = getContext().getUser();

        final SepStringBuffer _real_select = _select;
        try {
            _allowdocs.clear();
            _hqlparams.clear();
            String docname = translator.getLastNameClass(qn_xwiki_document);
            if (docname == null) {
                QName lclass = translator.getLastQNClass();
                if (qn_xwiki_object.equals(lclass)) {
                    final String objname = translator.getLastNameClass(lclass);
                    docname = translator.newXWikiObj(qn_xwiki_document);
                    _where.appendWithSep(docname).append(".fullName=").append(objname).append(".name");
                } else if (qn_xwiki_attachment.equals(lclass)) {
                    final String attname = translator.getLastNameClass(lclass);
                    docname = translator.newXWikiObj(qn_xwiki_document);
                    _where.appendWithSep(docname).append(".id=").append(attname).append(".docId");
                } else
                    throw new TranslateException("Class not exist");
            }
            _select = new SepStringBuffer(docname + ".id," + docname + ".fullName", null);

            security = false;
            int fr = _firstResult;
            _firstResult = -1;
            int fs = _fetchSize;
            _fetchSize = -1;
            final List doclst = super.list();
            _firstResult = fr;
            _fetchSize = fs;
            for (Iterator iter = doclst.iterator(); iter.hasNext();) {
                Object[] element = (Object[]) iter.next();
                // XWikiDocument element = (XWikiDocument) iter.next();
                try {
                    boolean r = true;
                    for (int i = 0; i < rights.size(); i++) {
                        r &= rightserv.hasAccessLevel((String) rights.get(i), user, (String) element[1], getContext());
                        if (!r)
                            break;
                    }
                    if (r)
                        _allowdocs.add(element[0]);
                } catch (XWikiException e) {
                }
            }
        } finally {
            _select = _real_select;
        }
        if (_allowdocs.isEmpty())
            return new ArrayList();
        security = true;

        final List lst = super.list();
        if (!isReturnClasses.contains(jcl_xwiki_classes.get(qn_xwiki_object))
            && !isReturnClasses.contains(jcl_xwiki_classes.get(qn_xwiki_document)))
            return lst;
        // wrapping
        final List result = new ArrayList();
        for (Iterator iter = lst.iterator(); iter.hasNext();) {
            final Object element = iter.next();
            if (element instanceof XWikiDocument) {
                XWikiDocument doc = (XWikiDocument) element;
                doc = getStore().loadXWikiDoc(doc, getContext());
                result.add(doc.newDocument(getContext()));
            } else if (element instanceof BaseObject) {

                // TODO Fix use of deprecated call.
                getHibernateStore().loadXWikiCollection((BaseObject) element, getContext(), true);

                result.add(new com.xpn.xwiki.api.Object((BaseObject) element, getContext()));
            } else if (element instanceof XWikiAttachment) { // TODO: get document. Impossible for now
                XWikiAttachment attach = (XWikiAttachment) element;
                result.add(new com.xpn.xwiki.api.Attachment(null, attach, getContext()));
            } else
                result.add(element);
        }
        return result;
    }

    public List getNeededRight()
    {
        final List r = new ArrayList(2);
        if (isViewRight)
            r.add("view");
        if (isQueryRight)
            r.add("query");
        return r;
    }

    public boolean isAllowed()
    {
        return isAllow;
    }
}
