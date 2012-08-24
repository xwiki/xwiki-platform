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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jackrabbit.core.query.AndQueryNode;
import org.apache.jackrabbit.core.query.DefaultQueryNodeVisitor;
import org.apache.jackrabbit.core.query.DerefQueryNode;
import org.apache.jackrabbit.core.query.ExactQueryNode;
import org.apache.jackrabbit.core.query.LocationStepQueryNode;
import org.apache.jackrabbit.core.query.NAryQueryNode;
import org.apache.jackrabbit.core.query.NodeTypeQueryNode;
import org.apache.jackrabbit.core.query.NotQueryNode;
import org.apache.jackrabbit.core.query.OrQueryNode;
import org.apache.jackrabbit.core.query.OrderQueryNode;
import org.apache.jackrabbit.core.query.PathQueryNode;
import org.apache.jackrabbit.core.query.QueryConstants;
import org.apache.jackrabbit.core.query.QueryNode;
import org.apache.jackrabbit.core.query.QueryRootNode;
import org.apache.jackrabbit.core.query.RelationQueryNode;
import org.apache.jackrabbit.core.query.TextsearchQueryNode;
import org.apache.jackrabbit.name.NameFormat;
import org.apache.jackrabbit.name.QName;
import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.plugin.query.HibernateQuery.XWikiHibernateQueryTranslator.ObjProperty;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.util.Util;

/**
 * Query implementation for Hibernate.
 * 
 * @version $Id$
 */
@Deprecated
public class HibernateQuery extends DefaultQuery
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateQuery.class);

    protected XWikiHibernateQueryTranslator translator;

    public HibernateQuery(QueryRootNode tree, IQueryFactory qf)
    {
        super(tree, qf);
    }

    public XWikiHibernateStore getHibernateStore()
    {
        return getContext().getWiki().getHibernateStore();
    }

    /** @return true, if something added */
    protected boolean constructWhere(StringBuffer sb)
    {
        if (_where.length() == 0 && _userwhere.length() == 0)
            return false;
        if (_where.length() > 0) {
            sb.append(" where ").append(_where);
        }
        if (_where.length() > 0 && _userwhere.length() > 0) {
            sb.append(" and ").append(_userwhere);
        } else if (_userwhere.length() > 0) {
            sb.append(" where ").append(_userwhere);
        }
        return true;
    }

    public List list() throws XWikiException
    {
        String hql = getNativeQuery();
        return hqlexec(hql, _hqlparams, _fetchSize, _firstResult);
    }

    public String getNativeQuery()
    {
        if (translator == null)
            translator = new XWikiHibernateQueryTranslator(getQueryTree());
        StringBuffer _result = new StringBuffer();
        if (_select.length() > 0)
            _result.append("select ").append(_select);
        _result.append(" from ").append(_from);

        constructWhere(_result);

        if (_order.length() > 0)
            _result.append(" order by ").append(_order);

        String hql = _result.toString();

        if (LOGGER.isDebugEnabled())
            LOGGER.debug("hql: " + hql);

        return hql;
    }

    protected SepStringBuffer _select = new SepStringBuffer(",");

    protected SepStringBuffer _from = new SepStringBuffer(",");

    protected SepStringBuffer _where = new SepStringBuffer(" and ");

    protected SepStringBuffer _userwhere = new SepStringBuffer(" and ");

    protected SepStringBuffer _order = new SepStringBuffer(",");

    protected void _addSelect(ObjProperty p)
    {
        _select.appendWithSep(p.getHqlName());
    }

    protected void _addPropClass(Class class1)
    {
    } // used in SecHibernateQuery

    static QName fromJCRName(String s)
    {
        try {
            return NameFormat.parse(s, XWikiNamespaceResolver.getInstance());
        } catch (Throwable e) {
            throw new TranslateException("unknown name:" + s, e);
        }
    }

    // jcr class constants
    static final QName qn_xwiki_document = fromJCRName("xwiki:document");

    static final QName qn_xwiki_object = fromJCRName("xwiki:object");

    static final QName qn_xwiki_attachment = fromJCRName("xwiki:attachment");

    static final QName qn_property = fromJCRName("property");

    static final QName qn_xwikiproperty = fromJCRName("xp:property");

    static final QName qn_listproperty = fromJCRName("xp:listproperty");

    /** Abridgement of jcr classes */
    static final Map abr_xwiki_classes = new HashMap();

    /** Mapping of jcr classes to Hibernate classes */
    static final Map hbn_xwiki_classes = new HashMap();

    static final Map jcl_xwiki_classes = new HashMap();
    static {
        abr_xwiki_classes.put(fromJCRName("doc"), qn_xwiki_document);
        abr_xwiki_classes.put(fromJCRName("obj"), qn_xwiki_object);
        abr_xwiki_classes.put(fromJCRName("attach"), qn_xwiki_attachment);
        hbn_xwiki_classes.put(qn_xwiki_document, "XWikiDocument");
        hbn_xwiki_classes.put(qn_xwiki_object, "BaseObject");
        hbn_xwiki_classes.put(qn_xwiki_attachment, "XWikiAttachment");
        jcl_xwiki_classes.put(qn_xwiki_document, XWikiDocument.class);
        jcl_xwiki_classes.put(qn_xwiki_object, BaseObject.class);
        jcl_xwiki_classes.put(qn_xwiki_attachment, XWikiAttachment.class);
    }

    protected class XWikiHibernateQueryTranslator implements org.apache.jackrabbit.core.query.QueryNodeVisitor
    {
        XWikiHibernateQueryTranslator(QueryNode node)
        {
            super();
            node.accept(this, null);
        }

        public Object visit(QueryRootNode node, Object data)
        {
            // path
            traverse(node.getLocationNode(), data);
            // order by
            OrderQueryNode order = node.getOrderNode();
            QName mainclass = getLastQNClass();
            String mainobj = getLastNameClass(mainclass);
            if (order != null) {
                Object[] args = new Object[] {mainobj, mainclass};
                traverse(order, args);
            }
            QName[] select = node.getSelectProperties();
            if (n2e(mainobj).equals(""))
                throw new TranslateException("what object?");
            if (_isdistinct)
                _select.append("distinct ");
            if (select.length > 0) {
                for (int i = 0; i < select.length; i++) {
                    final QName sel = select[i];
                    final ObjProperty prop = getProp(sel, mainobj, mainclass);
                    _addSelect(prop);
                }
            } else {
                _addSelect(new ObjProperty(mainobj, (Class) jcl_xwiki_classes.get(mainclass)));
            }
            return data;
        }

        public Object visit(PathQueryNode node, Object data)
        {
            final QueryNode[] ps = node.getOperands();
            if (ps.length < 1)
                throw new TranslateException("path must be");

            final QName[] nodesclass = new QName[ps.length];
            final boolean[] nodeflag = new boolean[ps.length];
            // get types
            for (int i = 0; i < ps.length; i++) {
                final int ind = i;
                final LocationStepQueryNode lsqn = (LocationStepQueryNode) ps[i];
                lsqn.acceptOperands(new DefaultQueryNodeVisitor()
                {
                    public Object visit(AndQueryNode arg0, Object arg1)
                    {
                        final QueryNode[] qns = arg0.getOperands();
                        for (int i = 0; i < qns.length; i++)
                            qns[i].accept(this, arg1);
                        return null;
                    }

                    public Object visit(NodeTypeQueryNode node, Object data)
                    {
                        nodesclass[ind] = node.getValue();
                        return data;
                    }
                }, null);
            }
            for (int i = 0; i < ps.length; i++) {
                final LocationStepQueryNode lsqn = (LocationStepQueryNode) ps[i];
                QName qn = lsqn.getNameTest();
                QName xwcl = (QName) abr_xwiki_classes.get(qn);
                if (ps.length - i >= 2 && DerefQueryNode.class == ps[i + 1].getClass()) {
                    if (qn_xwiki_attachment.equals(nodesclass[i]) || qn_xwiki_object.equals(nodesclass[i])) {
                        nodeflag[i + 1] = true;
                        nodesclass[i + 1] = qn_xwiki_document;
                    } else
                        throw new TranslateException("jcr:deref can be only after xwiki:object and xwiki:attachment");
                }
                if (ps.length - i >= 2 && qn_xwiki_attachment.equals(xwcl)
                    && !((LocationStepQueryNode) ps[i + 1]).getIncludeDescendants() && nodesclass[i] == null
                    && nodesclass[i + 1] == null) {
                    final NodeTypeQueryNode ntqn = new NodeTypeQueryNode(ps[i + 1], xwcl);
                    nodesclass[i + 1] = ntqn.getValue();
                    ((LocationStepQueryNode) ps[i + 1]).addOperand(ntqn);
                    nodeflag[i] = true;
                    nodeflag[i + 1] = true;
                }
                if (ps.length - i >= 3 && xwcl != null && !((LocationStepQueryNode) ps[i + 1]).getIncludeDescendants()
                    && !((LocationStepQueryNode) ps[i + 2]).getIncludeDescendants() && nodesclass[i] == null
                    && nodesclass[i + 1] == null && nodesclass[i + 2] == null) {
                    final NodeTypeQueryNode ntqn = new NodeTypeQueryNode(ps[i + 2], xwcl);
                    nodesclass[i + 2] = ntqn.getValue();
                    ((LocationStepQueryNode) ps[i + 2]).addOperand(ntqn);
                    nodeflag[i] = true;
                    nodeflag[i + 1] = true;
                    nodeflag[i + 2] = true;
                }
                if (ps.length - i >= 2 && !nodeflag[i] && !nodeflag[i + 1]
                    && !((LocationStepQueryNode) ps[i + 1]).getIncludeDescendants() && nodesclass[i] == null
                    && nodesclass[i + 1] == null) {
                    final NodeTypeQueryNode ntqn = new NodeTypeQueryNode(ps[i + 1], qn_xwiki_document);
                    nodesclass[i + 1] = ntqn.getValue();
                    ((LocationStepQueryNode) ps[i + 1]).addOperand(ntqn);
                    nodeflag[i] = true;
                    nodeflag[i + 1] = true;
                }
            }

            // name[&space] in objects,docs,attachs
            for (int i = 0; i < ps.length; i++) {
                final LocationStepQueryNode lsqn = (LocationStepQueryNode) ps[i];
                final QName qncl = nodesclass[i];
                final QName qname = lsqn.getNameTest(); // get name
                if (qname != null && !"".equals(qname.getNamespaceURI()))
                    continue;
                if (qn_xwiki_attachment.equals(qncl) && qname != null) {
                    lsqn.addPredicate(new RelationQueryNode(lsqn, fromJCRName("filename"), qname.getLocalName(),
                        QueryConstants.OPERATION_EQ_GENERAL));
                }
                if (qncl != null) { // space
                    QName qspace = null;
                    if (i > 0 && nodesclass[i - 1] == null)
                        qspace = ((LocationStepQueryNode) ps[i - 1]).getNameTest(); // get space
                    if (qspace != null && !"".equals(qspace.getNamespaceURI()))
                        continue;
                    if (qn_xwiki_document.equals(qncl)) {
                        if (qspace != null)
                            lsqn.addPredicate(new RelationQueryNode(lsqn, fromJCRName("web"), qspace.getLocalName(),
                                QueryConstants.OPERATION_EQ_GENERAL));
                        if (qname != null)
                            lsqn.addPredicate(new RelationQueryNode(lsqn, fromJCRName("name"), qname.getLocalName(),
                                QueryConstants.OPERATION_EQ_GENERAL));
                    } else if (qn_xwiki_object.equals(qncl)) {
                        final RelationQueryNode rqn = getXWikiQNameRelation(lsqn, "className", qspace, qname);
                        if (rqn != null)
                            lsqn.addPredicate(rqn);
                        if (qspace != null && qname != null)
                            _objClassName.put(lsqn, qspace.getLocalName() + "." + qname.getLocalName());
                    }
                }
                if (qn_xwiki_object.equals(qncl)) {
                    final QName qnpClassName = fromJCRName("className");
                    lsqn.acceptOperands(new DefaultQueryNodeVisitor()
                    {
                        public Object visit(AndQueryNode node, Object data)
                        {
                            final QueryNode[] qns = node.getOperands();
                            for (int i = 0; i < qns.length; i++)
                                qns[i].accept(this, data);
                            return null;
                        }

                        public Object visit(RelationQueryNode arg0, Object arg1)
                        {
                            if (qnpClassName.equals(arg0.getProperty()))
                                _objClassName.put(lsqn, arg0.getStringValue());
                            return null;
                        }
                    }, null);
                }
            }

            String lastobj = null;
            QName lastclass = null;
            for (int i = 0; i < ps.length; i++) {
                final LocationStepQueryNode lsqn = (LocationStepQueryNode) ps[i];
                final QName qncl = nodesclass[i];
                if (qncl == null)
                    continue;
                final Object[] res = (Object[]) traverse(lsqn, new Object[] {"", qncl, lastobj, lastclass});
                lastobj = (String) res[0];
                lastclass = qncl;
            }

            return data;
        }

        /** @param data - Object[]{String curname="", QName curclass, String ParentName, QName ParentClass } */
        public Object visit(LocationStepQueryNode node, Object data)
        {
            final Object[] args = (Object[]) data;
            QName qclass = (QName) args[1];
            String parentname = (String) args[2];
            QName parentclass = (QName) args[3];

            String objname = newXWikiObj(qclass);
            args[0] = objname;
            if (qn_xwiki_object.equals(qclass)) {
                final String s = (String) _objClassName.get(node);
                _objClassName.remove(node);
                _objClassName.put(objname, s);
            }
            if (parentclass != null) {
                if (!qn_xwiki_document.equals(parentclass))
                    throw new TranslateException("Only xwiki:document have childrens");
                if (qn_xwiki_attachment.equals(qclass)) {
                    _where.appendWithSep(objname).append(".docId=").append(parentname).append(".id");
                } else if (qn_xwiki_object.equals(qclass)) {
                    _where.appendWithSep(objname).append(".name=").append(parentname).append(".fullName");
                } else if (qn_xwiki_document.equals(qclass)) {
                    _where.appendWithSep(objname).append(".parent=").append(parentname).append(".fullName");
                }
            }

            traverse(node.getOperands(), data);
            return data;
        }

        Map nameQueue = new HashMap();

        QName _lastClass = null;

        private String newNameClass(QName qn)
        {
            if (hbn_xwiki_classes.get(qn) != null)
                _lastClass = qn;
            Integer n = (Integer) nameQueue.get(qn);
            if (n == null) {
                nameQueue.put(qn, new Integer(0));
                return qn.getLocalName() + "0";
            } else {
                n = new Integer(n.intValue() + 1);
                nameQueue.put(qn, n);
                return qn.getLocalName() + n;
            }
        }

        protected QName getLastQNClass()
        {
            return _lastClass;
        }

        protected String getLastNameClass(QName qn)
        {
            Integer n = (Integer) nameQueue.get(qn);
            if (n == null)
                return null; // class not used yet
            return qn.getLocalName() + n;
        }

        protected String newXWikiObj(QName qclass)
        {
            String hbclass = (String) hbn_xwiki_classes.get(qclass);
            if (hbclass == null)
                throw new TranslateException("Class " + qclass + " is not found");
            return newXWikiObj(qclass, hbclass);
        }

        protected String newXWikiObj(QName qname, String hbclass)
        {
            String newobjname = newNameClass(qname);
            _from.appendWithSep(hbclass).append(" as ").append(newobjname);
            return newobjname;
        }

        private Object NAryVisit(NAryQueryNode node, Object data, String operand)
        {
            if (data == null)
                throw new TranslateException("No object for relation");
            boolean bracket = false;
            if (node.getParent() instanceof LocationStepQueryNode)
                _userwhere.appendSeparator();
            if (node.getParent() instanceof LocationStepQueryNode || node.getParent() instanceof AndQueryNode
                || node.getParent() instanceof NotQueryNode) {
                bracket = true;
            }
            if (bracket) {
                _userwhere.append("(");
            }
            String or = "";
            QueryNode[] operands = node.getOperands();
            for (int i = 0; i < operands.length; i++) {
                _userwhere.append(or);
                traverse(operands[i], data);
                or = operand;
            }
            if (bracket) {
                _userwhere.append(")");
            }
            return data;
        }

        public Object visit(OrQueryNode node, Object data)
        {
            return NAryVisit(node, data, " or ");
        }

        public Object visit(AndQueryNode node, Object data)
        {
            return NAryVisit(node, data, " and ");
        }

        public Object visit(NotQueryNode node, Object data)
        {
            if (data == null)
                throw new TranslateException("No object for relation");
            QueryNode[] operands = node.getOperands();
            if (node.getParent() instanceof LocationStepQueryNode)
                _userwhere.appendSeparator();
            String sep = "";
            for (int i = 0; i < operands.length; i++) {
                _userwhere.append(sep);
                _userwhere.append(" NOT (");
                traverse(operands[i], data);
                _userwhere.append(")");
                sep = "AND";
            }
            return data;
        }

        public Object visit(ExactQueryNode node, Object data)
        {
            if (data == null) {
                throw new TranslateException("No object for relation");
            }
            throw new TranslateException("Not implemented. (That is it?)");
        }

        public Object visit(NodeTypeQueryNode node, Object data)
        {
            // This was handled in visit(PathQueryNode,..)
            if (node.getParent() instanceof AndQueryNode)
                _userwhere.append("(true=true)");
            return data;
        }

        public Object visit(TextsearchQueryNode node, Object data)
        { // jcr:contain
            throw new TranslateException("Text search is not implemented for hibernate");
            /*
             * if (data==null) throw new TranslateException("No object for relation"); if (node.getPropertyName()==null)
             * throw new TranslateException("Full search is not implemented"); final Object[] args = (Object[]) data;
             * final String obj = (String) args[0]; final QName objclass = (QName) args[1]; final ObjProperty prop =
             * getProp(node.getPropertyName(), obj, objclass); int type = prop.getResultType(); if (node.getParent()
             * instanceof LocationStepQueryNode) _userwhere.appendSeparator(); if (type==TYPE_DEFAULT)
             * _userwhere.append(prop.getHqlName()).append(" LIKE ").append("'%").append(node.getQuery()).append("%'");
             * else if (type==TYPE_LIST)
             * _userwhere.append("'").append(node.getQuery()).append("'").append(" in elements("
             * ).append(prop.getHqlName()).append(")"); return data;
             */
        }

        public Object visit(RelationQueryNode node, Object data)
        {
            if (data == null)
                throw new TranslateException("No object for relation");

            final Object[] args = (Object[]) data;
            final String obj = (String) args[0];
            final QName objclass = (QName) args[1];

            final QName prop = node.getProperty();
            if (node.getParent() instanceof LocationStepQueryNode)
                _userwhere.appendSeparator();
            final ObjProperty oprop = getProp(prop, obj, objclass);
            int op = node.getOperation();
            final String sop = XWikiQueryConstants.getHqlOperation(op);
            int vt = node.getValueType();
            boolean isGeneralComp = XWikiQueryConstants.isGeneralComparisonType(op);
            boolean isValueComp = XWikiQueryConstants.isValueComparisonType(op);
            boolean isMultyValue = oprop.isMultiValue();

            StringBuffer svalue = new StringBuffer();
            if (vt == QueryConstants.TYPE_DOUBLE) {
                svalue.append(node.getDoubleValue());
            } else if (vt == QueryConstants.TYPE_LONG) {
                svalue.append(node.getLongValue());
            } else if (vt == QueryConstants.TYPE_POSITION) {
                svalue.append(node.getPositionValue()); // [1]. unuseful.
            } else if (vt == QueryConstants.TYPE_STRING) {
                svalue.append("'").append(tosqlstring(node.getStringValue())).append("'");
            } else if (vt == QueryConstants.TYPE_TIMESTAMP || vt == QueryConstants.TYPE_DATE) {
                String datename = newNameParam("pvd", node.getDateValue());
                svalue.append(":").append(datename);
            }

            if (isMultyValue && isGeneralComp) {
                _userwhere.append(svalue.toString()).append(sop).append(" some elements(").append(oprop.getHqlName())
                    .append(')');
            } else if (isMultyValue && isValueComp) {
                _userwhere.append(svalue.toString()).append(sop).append(" all elements(").append(oprop.getHqlName())
                    .append(')');
            } else {
                _userwhere.append(oprop.getHqlName()).append(sop).append(svalue.toString());
            }

            return data;
        }

        Map nameParamQueue;

        private String newNameParam(String string, Object v)
        {
            if (nameParamQueue == null)
                nameParamQueue = new HashMap();
            final Integer Ir = (Integer) nameParamQueue.get(string);
            int ir;
            if (Ir != null)
                ir = Ir.intValue();
            else
                ir = 0;
            nameParamQueue.put(string, new Integer(ir + 1));
            final String sr = string + ir;
            _addHqlParam(sr, v);
            return sr;
        }

        public Object visit(OrderQueryNode node, Object data)
        {
            if (data == null)
                throw new TranslateException("No object for relation");
            final Object[] args = (Object[]) data;
            final String obj = (String) args[0];
            final QName objclass = (QName) args[1];

            final OrderQueryNode.OrderSpec[] specs = node.getOrderSpecs();
            for (int i = 0; i < specs.length; i++) {
                final ObjProperty oprop = getProp(specs[i].getProperty(), obj, objclass);
                _order.appendSeparator().append(oprop.getHqlName());
                if (!specs[i].isAscending())
                    _order.append(" DESC");
            }
            return data;
        }

        public Object visit(DerefQueryNode node, Object data)
        {
            final Object[] args = (Object[]) data;
            QName parentclass = (QName) args[3];
            String parentname = (String) args[2];

            if (qn_xwiki_object.equals(parentclass) || qn_xwiki_attachment.equals(parentclass)) {
                if ("doc".equals(node.getRefProperty().getLocalName())
                    && "".equals(node.getRefProperty().getNamespaceURI())) {
                    String docobj = getLastNameClass(qn_xwiki_document);
                    _lastClass = qn_xwiki_document;
                    if (docobj == null) {
                        docobj = newXWikiObj(qn_xwiki_document);
                        if (qn_xwiki_attachment.equals(parentclass)) {
                            _where.appendWithSep(parentname).append(".docId=").append(docobj).append(".id");
                        } else { // qn_xwiki_object == parentclass)
                            _where.appendWithSep(parentname).append(".name=").append(docobj).append(".fullName");
                        }
                    }
                    args[0] = docobj;
                    // TODO: is operators, etc is possible in jcr:deref node?
                    return data;
                } else
                    throw new TranslateException("jcr:deref is possible only by jcr:deref(@doc,'*')");
            } else
                throw new TranslateException("jcr:deref is possible only from xwiki:object and xwiki:attachment");
        }

        private int indent;

        private void traverse(QueryNode[] node, Object data)
        {
            indent++;
            for (int i = 0; i < node.length; i++) {
                node[i].accept(this, data);
            }
            indent--;
        }

        private Object traverse(QueryNode node, Object data)
        {
            indent++;
            final Object r = node.accept(this, data);
            indent--;
            return r;
        }

        private RelationQueryNode getXWikiQNameRelation(LocationStepQueryNode par, String prop, QName qspace,
            QName qname)
        {
            if (qspace == null && qname == null) {
                return null;
            } else if (qspace != null && qname != null) {
                return new RelationQueryNode(par, fromJCRName(prop),
                    qspace.getLocalName() + "." + qname.getLocalName(), QueryConstants.OPERATION_EQ_GENERAL);
            } else if (qspace != null && qname == null) {
                return new RelationQueryNode(par, fromJCRName(prop), qspace.getLocalName() + ".%",
                    QueryConstants.OPERATION_LIKE);
            } else if (qspace == null && qname != null) {
                return new RelationQueryNode(par, fromJCRName(prop), "%." + qname.getLocalName(),
                    QueryConstants.OPERATION_LIKE);
            }
            return null;
        }

        private String n2e(String s)
        {
            return s == null ? "" : s;
        }

        private String tosqlstring(String s)
        {
            return s;
        }

        /** Map obj - BaseClass.name */
        Map _objClassName = new HashMap();

        /** set of used PropertyClass */
        // Set _propclass = new HashSet();
        public ObjProperty getProp(QName qname, String obj, QName objclass)
        {
            final String prop = qname.getLocalName();
            ObjProperty oprop = (ObjProperty) _propertyes.get(obj + "|" + prop);
            if (oprop != null)
                return oprop;

            if (XWikiNamespaceResolver.NS_DOC_URI.equals(qname.getNamespaceURI())) {
                final String docname = getLastNameClass(qn_xwiki_document);
                if (prop.equals("self"))
                    return new ObjProperty(docname, XWikiDocument.class);
                else
                    return new ObjPropProperty(docname, XWikiDocument.class, prop);
            }
            if (XWikiNamespaceResolver.NS_OBJ_URI.equals(qname.getNamespaceURI())) {
                final String objname = getLastNameClass(qn_xwiki_object);
                if (prop.equals("self"))
                    return new ObjProperty(objname, BaseObject.class);
                else
                    return new ObjPropProperty(objname, BaseObject.class, prop);
            }
            Class objjclass = (Class) jcl_xwiki_classes.get(getLastQNClass());
            if (!XWikiNamespaceResolver.NS_XWIKI_PROPERTY_URI.equals(qname.getNamespaceURI()))
                return new ObjPropProperty(obj, objjclass, prop);

            if (!qn_xwiki_object.equals(objclass))
                throw new TranslateException("xp: attributes is only for xwiki:object");

            final String classname = (String) _objClassName.get(obj);
            if (classname == null)
                throw new TranslateException("ClassName for " + obj + " not found");
            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(classname);
            try {
                doc = getStore().loadXWikiDoc(doc, getContext());
            } catch (XWikiException e) {
                throw new TranslateException("Couldn't load BaseClass document", e);
            }
            BaseClass bc = doc.getXClass();
            if (bc == null)
                throw new TranslateException("Couldn't load BaseClass");

            PropertyClass pc = (PropertyClass) bc.get(prop);
            if (pc == null)
                throw new TranslateException("Couldn`t find property " + prop + " of class " + classname);

            _addPropClass(pc.getClass());
            final Class propclass = pc.newProperty().getClass();
            String propclassname = propclass.getName();

            String hqls = newXWikiObj(qname, propclassname);

            _where.appendSeparator().append(obj).append(".id=").append(hqls).append(".id.id").appendSeparator()
                .append(hqls).append(".name='").append(prop).append("'");

            String suff = (String) _mapPropValue.get(propclassname);
            if (suff == null)
                suff = "value";
            hqls += "." + suff;
            /*
             * if ("com.xpn.xwiki.objects.DBStringListProperty".equals(propclassname)) { String newhqls =
             * newNameClass(qn_listproperty); _from.append(" join ").append(hqls).append(" as ").append(newhqls); hqls =
             * newhqls; }
             */
            oprop = new ObjFlexProperty(obj, objjclass, pc.getClass(), prop, hqls, propclass);
            _propertyes.put(obj + "|" + prop, oprop);

            return oprop;
        }

        static final int TYPE_DEFAULT = -1;

        static final int TYPE_LIST = TYPE_DEFAULT - 1;

        protected ObjProperty getObjProperty(String prop, String obj)
        {
            return (ObjProperty) _propertyes.get(obj + "|" + prop);
        }

        /** obj:prop - ObjProperty */
        Map _propertyes = new HashMap();

        class ObjProperty
        {
            String obj;

            Class objclass;

            public ObjProperty(String obj, Class objclass)
            {
                this.obj = obj;
                this.objclass = objclass;
            }

            public int getResultType()
            {
                return TYPE_DEFAULT;
            }

            public String getHqlName()
            {
                return obj;
            }

            public boolean isMultiValue()
            {
                return false;
            }
        }

        class ObjPropProperty extends ObjProperty
        {
            String propname;

            public ObjPropProperty(String obj, Class objclass, String prop)
            {
                super(obj, objclass);
                this.propname = prop;
            }

            public String getHqlName()
            {
                return obj + "." + propname;
            }
        }

        class ObjFlexProperty extends ObjPropProperty
        {
            String hqlname;

            Class baseclass, propertyclass;

            public ObjFlexProperty(String obj, Class objclass, Class baseclass, String prop, String hqlname,
                Class propertyclass)
            {
                super(obj, objclass, prop);
                this.hqlname = hqlname;
                this.baseclass = baseclass;
                this.propertyclass = propertyclass;
            }

            public String getHqlName()
            {
                return hqlname;
            }

            public int getResultType()
            {
                if (propertyclass.equals(DBStringListProperty.class))
                    return TYPE_LIST;
                return TYPE_DEFAULT;
            }

            public boolean isMultiValue()
            {
                return getResultType() == TYPE_LIST;
            }
        }
    }

    /** Value name for classes properties */
    private static Map _mapPropValue = new HashMap();
    static {
        _mapPropValue.put("com.xpn.xwiki.objects.StringListProperty", "textValue");
        _mapPropValue.put("com.xpn.xwiki.objects.DBStringListProperty", "list");
    }

    protected List hqlexec(String hql, Map params, int fs, int fr) throws XWikiException
    {
        boolean bTransaction = true;
        final MonitorPlugin monitor = Util.getMonitorPlugin(getContext());
        List r = null;
        try {
            // Start monitoring timer
            if (monitor != null)
                monitor.startTimer("hibernate");

            getHibernateStore().checkHibernate(getContext());
            bTransaction = getHibernateStore().beginTransaction(getContext());

            final Session ses = getHibernateStore().getSession(getContext());
            final Query q = ses.createQuery(hql);
            if (params != null && !params.isEmpty()) {
                for (Iterator iter = params.keySet().iterator(); iter.hasNext();) {
                    final String element = (String) iter.next();
                    final Object val = params.get(element);
                    if (val instanceof Collection)
                        q.setParameterList(element, (Collection) val);
                    else
                        q.setParameter(element, val);
                }
            }
            if (fs > 0)
                q.setMaxResults(fs);
            if (fr > 0)
                q.setFirstResult(fr);

            r = q.list();

            if (bTransaction)
                getHibernateStore().endTransaction(getContext(), false);
            return r;
        } catch (Throwable e) {
            Object[] args = {hql};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH, "Exception while searching documents with sql {0}",
                e, args);
        } finally {
            try {
                if (bTransaction)
                    getHibernateStore().endTransaction(getContext(), false);
            } catch (Exception e) {
            }
            // End monitoring timer
            if (monitor != null)
                monitor.endTimer("hibernate");
        }
    }

    static public class TranslateException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        public TranslateException(String string)
        {
            super(string);
        }

        public TranslateException(String string, Throwable e)
        {
            super(string, e);
        }

        public TranslateException(Throwable e)
        {
            super(e);
        }
    }

    public IQuery setDistinct(boolean d)
    {
        if (d != _isdistinct)
            translator = null;
        return super.setDistinct(d);
    }

    Map _hqlparams = new HashMap();

    protected void _addHqlParam(String pn, Object v)
    {
        _hqlparams.put(pn, v);
    }
}
