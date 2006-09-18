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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.query.QueryParser;
import org.apache.jackrabbit.core.query.QueryRootNode;
import org.apache.jackrabbit.value.ValueFactoryImpl;

import javax.jcr.ValueFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import java.util.List;
import java.util.Set;

/** Plugin for Query API */
public class QueryPlugin extends XWikiDefaultPlugin implements IQueryFactory {
    private static final Log log = LogFactory.getLog(QueryPlugin.class);
    XWikiContext	context;
    public QueryPlugin(String name, String className, XWikiContext context) throws XWikiException {
        super(name, className, context);
        this.context = context;
    }

    public String getName() { return "query"; }

    QueryPluginApi queryApi;
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        if (queryApi == null)
            queryApi = new QueryPluginApi(this);
        return queryApi;
    }

    public XWikiContext getContext() {
        return context;
    }
    public XWikiStoreInterface getStore() {
        return getContext().getWiki().getStore();
    }

    ValueFactory valueFactory = null;;
    public ValueFactory getValueFactory() {
        if (valueFactory==null) {
            valueFactory = new ValueFactoryImpl();
        }
        return valueFactory;
    }

    boolean isHibernate() {
        return getContext().getWiki().getHibernateStore() != null;
    }
    boolean isJcr() {
        return getContext().getWiki().getNotCacheStore() instanceof XWikiJcrStore;
    }

    /** Translate query string to query tree */
    protected QueryRootNode parse(String query, String language) throws InvalidQueryException {
        if (query==null) return null;
        final QueryRootNode qn = QueryParser.parse(query, language, XWikiNamespaceResolver.getInstance());
        return qn;
    }
    /** create xpath query
     * @throws XWikiException */
    public IQuery xpath(String q) throws XWikiException {
        if (log.isDebugEnabled())
            log.debug("create xpath query: "+q);
        if (isHibernate())
            try {
                return new HibernateQuery( parse(q, Query.XPATH), this);
            } catch (InvalidQueryException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, "Invalid xpath query: " + q);
            }
        if (isJcr())
            return new JcrQuery( q, Query.XPATH, this );
        return null;
    }
    /** create JCRSQL query
     * unsupported for now
     * @throws XWikiException */
    public IQuery ql(String q) throws XWikiException {
        if (log.isDebugEnabled())
            log.debug("create JCRSQL query: "+q);
        if (isHibernate())
            try {
                return new HibernateQuery( parse(q, Query.SQL), this);
            } catch (InvalidQueryException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN, "Invalid jcrsql query: " + q);
            }
        if (isJcr())
            return new JcrQuery( q, Query.SQL, this );
        return null;
    }
    /** create query for docs
     * @param web, docname - document.web & .name. it may consist xpath []-selection. if any - *
     * @param prop - return property, start with @, if null - return document
     * @param order - properties for sort, separated by ','; order: ascending/descending after prop. name, or +/- before. if null - not sort
     * @throws XWikiException
     * */
    public IQuery getDocs(String docname, String prop, String order) throws XWikiException {
        return xpath("/"+getXPathName(docname) + getPropertyXPath(prop) + getOrderXPath(order));
    }
    /** create query for child documents
     * @param web,docname must be without templates & [] select
     * @throws XWikiException
     * @see getDocs */
    public IQuery getChildDocs(String docname, String prop, String order) throws XWikiException {
        return xpath("/*/*[@parent='"+getXWikiName(docname)+"']"+ getPropertyXPath(prop) + getOrderXPath(order));
    }
    /** create query for attachments
     * @param attachname - name of attachment, may be *, *[]
     * @throws XWikiException
     * @see getDocs
     */
    public IQuery getAttachment(String docname, String attachname, String order) throws XWikiException {
        return xpath("/"+getXPathName(docname)+"/attach/" + attachname + getOrderXPath(order));
    }
    /** create query for objects
     * @param oweb, oclass - object web & class. if any - *
     * @param prop. for flex-attributes use f:flexname
     * @throws XWikiException
     * @see getDocs
     */
    public IQuery getObjects(String docname, String oclass, String prop, String order) throws XWikiException {
        return xpath("/"+getXPathName(docname)+"/obj/"+getXPathName(oclass) + getPropertyXPath(prop) + getOrderXPath(order));
    }

    protected String getXWikiName(String name) {
        int is = name.indexOf('['),
        ip = name.indexOf('/');
        if (is<0 || ip < is)
            return name.replace('/', '.');
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
        StringBuffer xpath = new StringBuffer();
        Set classes = query.getClasses();
        if (classes.size()>1)
         throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_SEARCH_NOTIMPL, "Search with more than one class is not implemented");
        if (classes.size()==0)
         return "//*/*";

        String className = (String) classes.toArray()[0];
        BaseClass bclass = context.getWiki().getClass(className, context);
        xpath.append("//*/*/obj/");
        xpath.append(className.replace('.','/'));
        String where= bclass.makeQuery(query);
        if (where.equals(""))
            xpath.append("/*");
        else
        {
            xpath.append("[");
            xpath.append(where);
            xpath.append("]");
        }
        xpath.append("/@name");
        return xpath.toString();
    }

    public List search(XWikiQuery query) throws XWikiException {
        List doclist =  xpath(makeQuery(query)).list();
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
