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

import java.util.List;
import java.util.Set;

import javax.jcr.ValueFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.query.QueryParser;
import org.apache.jackrabbit.core.query.QueryRootNode;
import org.apache.jackrabbit.value.ValueFactoryImpl;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;

/** Plugin for Query API */
public class QueryPlugin extends XWikiDefaultPlugin implements IQueryFactory {
    private static final Log log = LogFactory.getLog(QueryPlugin.class);
    XWikiContext context;
    public QueryPlugin(String name, String className, XWikiContext context) throws XWikiException {
        super(name, className, context);
        this.context = context;
    }

    @Override
    public String getName() { return "query"; }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new QueryPluginApi((QueryPlugin)plugin, context);
    }

    /**
     * @deprecated This version si buggy since it use the initial context of the plugin and
     * not the current context
     */
    @Deprecated
    public XWikiContext getContext() {
        return context;
    }

    /**
     * @deprecated This version si buggy since it use the initial context of the plugin and
     * not the current context. For the store itself this is not major as it is a singleton
     */
    @Deprecated
    public XWikiStoreInterface getStore() {
        return getStore(this);
    }

    public XWikiStoreInterface getStore(IQueryFactory qf) {
        return qf.getContext().getWiki().getStore();
    }

    ValueFactory valueFactory = null;
    public ValueFactory getValueFactory() {
        if (valueFactory==null) {
            valueFactory = ValueFactoryImpl.getInstance();
        }
        return valueFactory;
    }

    /**
     * @deprecated This version si buggy since it use the initial context of the plugin and
     * not the current context
     */
    @Deprecated
    boolean isHibernate() {
        return isHibernate(this);
    }

    boolean isHibernate(IQueryFactory qf) {
        return qf.getContext().getWiki().getHibernateStore() != null;
    }

    /**
     * @deprecated This version si buggy since it use the initial context of the plugin and
     * not the current context
     */
    @Deprecated
    boolean isJcr() {
        return isJcr(this);
    }

    boolean isJcr(IQueryFactory qf) {
        return qf.getContext().getWiki().getNotCacheStore() instanceof XWikiJcrStore;
    }

    /** Translate query string to query tree */
    protected QueryRootNode parse(String query, String language) throws InvalidQueryException {
        if (query==null) return null;
        final QueryRootNode qn = QueryParser.parse(query, language, XWikiNamespaceResolver.getInstance());
        return qn;
    }

    /** create xpath query
     * @throws XWikiException
     * @deprecated This version si buggy since it use the initial context of the plugin and
     * not the current context
     */
    @Deprecated
    public IQuery xpath(String q) throws XWikiException {
        return xpath(q, this);
    }

    public IQuery xpath(String q, IQueryFactory qf) throws XWikiException {
        if (log.isDebugEnabled())
            log.debug("create xpath query: "+q);
        if (isHibernate(qf))
            try {
                return new HibernateQuery( parse(q, Query.XPATH), qf);
            } catch (InvalidQueryException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, "Invalid xpath query: " + q, e);
            }
        if (isJcr(qf))
            return new JcrQuery( q, Query.XPATH, qf );
        return null;
    }

    /** create JCRSQL query
     * unsupported for now
     * @throws XWikiException
     * @deprecated This version si buggy since it use the initial context of the plugin and
     * not the current context
     */
    @Deprecated
    public IQuery ql(String q) throws XWikiException {
        return ql(q, this);
    }

    public IQuery ql(String q, IQueryFactory qf) throws XWikiException {
        if (log.isDebugEnabled())
            log.debug("create JCRSQL query: "+q);
        if (isHibernate(qf))
            try {
                return new HibernateQuery( parse(q, Query.SQL), qf);
            } catch (InvalidQueryException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, "Invalid jcrsql query: " + q);
            }
        if (isJcr(qf))
            return new JcrQuery( q, Query.SQL, qf );
        return null;
    }

    /** create query for docs
     * @param web, docname - document.space & .name. it may consist xpath []-selection. if any - *
     * @param prop - return property, start with @, if null - return document
     * @param order - properties for sort, separated by ','; order: ascending/descending after prop. name, or +/- before. if null - not sort
     * @throws XWikiException
     * @deprecated This version si buggy since it use the initial context of the plugin and
     * not the current context
     */
    @Deprecated
    public IQuery getDocs(String docname, String prop, String order) throws XWikiException {
        return getDocs(docname, prop, order, this);
    }
    
    public IQuery getDocs(String docname, String prop, String order, IQueryFactory qf) throws XWikiException {
    	//if (prop==null) prop = "@fullName";
        return xpath("/"+getXPathName(docname) + getPropertyXPath(prop) + getOrderXPath(order), qf);
    }

    /** create query for child documents
     * @param web,docname must be without templates & [] select
     * @throws XWikiException
     * @see getDocs
     * @deprecated This version si buggy since it use the initial context of the plugin and
     * not the current context
     */
    @Deprecated
    public IQuery getChildDocs(String docname, String prop, String order) throws XWikiException {
        return getChildDocs(docname, prop, order, this);
    }

    public IQuery getChildDocs(String docname, String prop, String order, IQueryFactory qf) throws XWikiException {
        return xpath("/*/*[@parent='"+getXWikiName(docname)+"']"+ getPropertyXPath(prop) + getOrderXPath(order), qf);
    }

    /** create query for attachments
     * @param attachname - name of attachment, may be *, *[]
     * @throws XWikiException
     * @see getDocs
     * @deprecated This version si buggy since it use the initial context of the plugin and
     * not the current context
     */
    @Deprecated
    public IQuery getAttachment(String docname, String attachname, String order) throws XWikiException {
        return getAttachment(docname, attachname, order, this);
    }

    public IQuery getAttachment(String docname, String attachname, String order, IQueryFactory qf) throws XWikiException {
        return xpath("/"+getXPathName(docname)+"/attach/" + attachname + getOrderXPath(order), qf);
    }

    /** create query for objects
     * @param oweb, oclass - object web & class. if any - *
     * @param prop. for flex-attributes use f:flexname
     * @throws XWikiException
     * @see getDocs
     * @deprecated This version si buggy since it use the initial context of the plugin and
     * not the current context
     */
    @Deprecated
    public IQuery getObjects(String docname, String oclass, String prop, String order) throws XWikiException {
        return getObjects(docname, oclass, prop, order, this);
    }

    public IQuery getObjects(String docname, String oclass, String prop, String order, IQueryFactory qf) throws XWikiException {
        return xpath("/"+getXPathName(docname)+"/obj/"+getXPathName(oclass) + getPropertyXPath(prop) + getOrderXPath(order), qf);
    }

    protected String getXWikiName(String name) {
        int is = name.indexOf('['),
        ip = name.indexOf('/');
        if (is<0 || ip < is)
            return name.replaceAll("/", ".");
        return name;
    }

    protected String getXPathName(String name) {
        int is = name.indexOf('['),
            ip = name.indexOf('.');
        if (ip>0 && (is<0 || ip < is)) {
            StringBuffer sb = new StringBuffer(name);
            sb.setCharAt(ip, '/');
            return sb.toString();
        } else
            return name;
    }

    protected String getOrderXPath(String order) {
        if ("".equals(n2e(order))) return "";
        final String[] props = StringUtils.split(order,',');
        StringBuffer res = new StringBuffer();
        res.append(" order by ");
        String comma = "";
        for (int i=0; i<props.length; i++) {
            res.append(comma);
            String prop = props[i].trim();
            char c = prop.charAt(0);
            boolean descending = (c == '-');
            if (c=='-' || c=='+')
                prop = prop.substring(1);
            if (prop.indexOf("@")<0)
                res.append("@");
            res.append(prop);
            if (descending)
                res.append(" descending");
            comma = ",";
        }
        return res.toString();
    }

    protected String getPropertyXPath(String prop) {
        if ("".equals(n2e(prop))) return "";
        prop = prop.trim();
        if (prop.charAt(0)=='(') return "/"+prop;

        final StringBuffer sb = new StringBuffer();
        final String[] props = StringUtils.split(prop, " ,");
        sb.append("/");
        if (props.length>1)
            sb.append("(");
        String comma = "";
        for (int i=0; i<props.length; i++) {
            final String p = props[i];
            sb.append(comma);
            if (p.indexOf("@")<0)
                sb.append("@");
            sb.append(p);
            comma = ",";
        }
        if (props.length>1)
            sb.append(")");

        return sb.toString();
    }

    private final String n2e(String s) {
        return s==null?"":s;
    }

    public String makeQuery(XWikiQuery query) throws XWikiException {
        StringBuffer xwqlfrom = new StringBuffer();
        StringBuffer xwqlwhere = new StringBuffer();
        StringBuffer xwqlorder = new StringBuffer();
        
        Set classes = query.getClasses();
        if (classes.size()>1)
         throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_SEARCH_NOTIMPL, "Search with more than one class is not implemented");
        if (classes.size()==0)
         return "";

        String className = (String) classes.toArray()[0];
        BaseClass bclass = context.getWiki().getClass(className, context);
        xwqlfrom.append("doc.object(" + className + ") as obj");
        String where = bclass.makeQuery(query);
        if (!where.equals("")) {
            xwqlwhere.append(where);
        }

        List oProps = query.getOrderProperties();
        if ((oProps!=null)&&(oProps.size()>0)) {
            for (int i=0;i<oProps.size();i++) {
                OrderClause clause = (OrderClause) oProps.get(i);
                String propPath = clause.getProperty();
                int i1 = propPath.indexOf("_");
                if (i1!=-1) {
                    String propClassName = propPath.substring(0, i1);
                    if (propClassName.equals(className)) {
                        String propName = propPath.substring(i1+1);
                        if (xwqlorder.length()!=0) {
                        	xwqlorder.append(",");
                        }
                        xwqlorder.append("obj." + propName);                      
                        if (clause.getOrder()==OrderClause.DESC)
                        	xwqlorder.append(" desc");
                    }
                }
            }
        }
        
        String xwql = "from " + xwqlfrom.toString();
        if (xwqlwhere.length()!=0)
          xwql += " where " + xwqlwhere.toString();
        if (xwqlorder.length()!=0) {
           xwql += " order by " + xwqlorder.toString();
        }

        return xwql;
    }

    /*
     * @deprecated This version si buggy since it use the initial context of the plugin and
     * not the current context
     */
    @Deprecated
    public List search(XWikiQuery query) throws XWikiException {
        return search(query, this);
    }

    public List search(XWikiQuery query, IQueryFactory qf) throws XWikiException {
        List doclist =  xpath(makeQuery(query), qf).list();
        return doclist;
        /*
        if (doclist==null)
         return null;
        List list = new ArrayList();
        for (int i=0;i<doclist.size();i++) {
            list.add(((XWikiDocument)doclist.get(i)).getFullName());
        }
        return list;
        */
    }
}
