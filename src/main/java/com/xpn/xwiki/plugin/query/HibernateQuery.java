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

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.query.AndQueryNode;
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
import org.apache.jackrabbit.core.query.QueryNodeVisitor;
import org.apache.jackrabbit.core.query.QueryRootNode;
import org.apache.jackrabbit.core.query.RelationQueryNode;
import org.apache.jackrabbit.core.query.TextsearchQueryNode;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.util.ISO8601;
import org.hibernate.Query;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.plugin.query.HibernateQuery.XWikiHibernateQueryTranslator.ObjProperty;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.util.Util;

/** Query implementation for Hibernate */
public class HibernateQuery extends DefaultQuery {
	private static final Log log = LogFactory.getLog(HibernateQuery.class);
	
	protected XWikiHibernateStore			hbstore;
	protected XWikiHibernateQueryTranslator translator;
	public HibernateQuery(QueryRootNode tree, IQueryFactory qf) {
		super(tree, qf);
		this.hbstore = getContext().getWiki().getHibernateStore();
		//translator = new XWikiHibernateQueryTranslator( getQueryTree() );
	}
	
	/** @return true, if something added */
	protected boolean constructWhere(StringBuffer sb) {
		if (_where.length()==0 && _userwhere.length()==0) return false;
		if (_where.length()>0) {
			sb.append(" where ").append(_where);			
		}
		if (_where.length()>0 && _userwhere.length()>0) {
			sb.append(" and ").append(_userwhere);
		} else if (_userwhere.length()>0) {
			sb.append(" where ").append(_userwhere);
		}
		return true;
	}
	public List list() throws XWikiException {
		if (translator==null)
			translator = new XWikiHibernateQueryTranslator( getQueryTree() );
		StringBuffer _result = new StringBuffer();
		if (_select.length()>0)
			_result.append("select ").append(_select);
		_result.append(" from ").append(_from);
		
		constructWhere(_result);
		
		if (_order.length()>0)
			_result.append(" order by ").append(_order);
		
		String hql = _result.toString();
		
		if (log.isDebugEnabled())
			log.debug("hql: "+hql);
		
		return hqlexec(hql, null, _fetchSize, _firstResult);
	}
	
	protected SepStringBuffer	_select		= new SepStringBuffer(",");
	protected SepStringBuffer	_from		= new SepStringBuffer(",");
	protected SepStringBuffer	_where		= new SepStringBuffer(" and ");
	protected SepStringBuffer	_userwhere	= new SepStringBuffer(" and ");
	protected SepStringBuffer	_order		= new SepStringBuffer(",");
	protected String		 	_mainobj	= null;
	protected String		 	_classname	= null;
	
	protected class XWikiHibernateQueryTranslator implements QueryNodeVisitor {
		XWikiHibernateQueryTranslator(QueryNode node) {
			super();
			node.accept(this, null);			
		}

		public Object visit(QueryRootNode node, Object data) {
			// path
	        traverse(node.getLocationNode(), data);
	        // order by
	        OrderQueryNode order = node.getOrderNode();
	        if (order != null) {
	            traverse(order, _mainobj);
	        }
	        QName[] select = node.getSelectProperties();
	        if (n2e(_mainobj).equals(""))
				throw new TranslateException("what object?");
			if (select.length>0) {
				if (_isdistinct)
					_select.append("distinct ");
				
				for (int i=0; i<select.length; i++) {
					final QName sel = select[i];
					final String hqlpropname = getProp(sel, _mainobj);
					_select.appendSeparator().append(hqlpropname);
				}
			} else {
				if (_isdistinct)
					_select.append("distinct ");
				_select.append(_mainobj);
			}
	        return data;
		}
		
		private Object NAryVisit(NAryQueryNode node, Object data, String operand) {
			if (data==null) {
				throw new TranslateException("No object for relation");
			}		
			final String obj = (String) data;
			boolean bracket = false;
	        if (node.getParent() instanceof LocationStepQueryNode
	                || node.getParent() instanceof AndQueryNode
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
	            traverse(operands[i], obj);
	            or = operand;
	        }
	        if (bracket) {
	        	_userwhere.append(")");
	        }
	        return obj;
		}

		public Object visit(OrQueryNode node, Object data) {
			return NAryVisit(node, data, " or ");
		}

		public Object visit(AndQueryNode node, Object data) {
			return NAryVisit(node, data, " and ");
		}

		public Object visit(NotQueryNode node, Object data) {
			if (data==null) {
				throw new TranslateException("No object for relation");
			}
			final String obj = (String) data;
			QueryNode[] operands = node.getOperands();
			if (operands.length > 0) {				
	            _userwhere.append(" NOT (");
	            traverse(operands[0], obj);
	            _userwhere.append(")");
	        }
			return obj;
		}

		public Object visit(ExactQueryNode node, Object data) {
			if (data==null) {
				throw new TranslateException("No object for relation");			
			}
			final String obj = (String) data;
			throw new TranslateException("Not implemented");
			/*final StringBuffer sb = (StringBuffer) data;
			sb.append(" ");
			sb.append(node.getPropertyName().toString());
			sb.append("='").append( tosqlstring( node.getValue().toString() ) ).append("'");*/
			//return obj;
		}

		public Object visit(NodeTypeQueryNode node, Object data) {
			throw new TranslateException("Not implemented");			
		}

		public Object visit(TextsearchQueryNode node, Object data) { // jcr:contain
			if (data==null)
				throw new TranslateException("No object for relation");			
			if (node.getPropertyName()==null)
				throw new TranslateException("Full search is not implemented");
			final String obj = (String) data;
			final String hqlprop = getProp(node.getPropertyName(), obj);
			int type = getPropType(node.getPropertyName(), obj);
			if (type==TYPE_DEFAULT)
				_userwhere.append(hqlprop).append(" LIKE ").append("'%").append(node.getQuery()).append("%'");
			else if (type==TYPE_LIST)
				_userwhere.append("'").append(node.getQuery()).append("'").append(" in elements(").append(hqlprop).append(")");
			return obj;
		}
		
		private String _getLocationNodeName(QueryNode p) {
			QName qn = ((LocationStepQueryNode)p).getNameTest();
			if (qn==null) return "*";
			return tosqlstring( qn.getLocalName() );
		}
		public Object visit(PathQueryNode node, Object data) {
			//final StringBuffer sb = (StringBuffer) data;
			final QueryNode[] ps = node.getOperands();
			if (ps.length < 1) {
				throw new TranslateException("path must be");
			}
			final String sSpace = _getLocationNodeName(ps[0]);
			if (ps.length==1) {
				_addfrom("doc");
				_mainobj = "doc.web";
				_isdistinct = true;
				traverse(ps[0], "doc.web");
			} else {
				if (!n2e(sSpace).equals("*")) {
					_addfrom("doc");
					_where.appendSeparator().append("doc.web='").append(sSpace).append("'");				
				}
				traverse(ps[0], "doc.web");
				final String sDoc = _getLocationNodeName(ps[1]);
				_addfrom("doc");
				if (!sDoc.equals("*")) {
					_where.appendSeparator().append("doc.name='").append(sDoc).append("'");
				}
				traverse(ps[1], "doc");
				if (ps.length==2) {
					_mainobj = "doc";
				} else {
					final String sProp = _getLocationNodeName(ps[2]);
					if (sProp.equals("attach")) {
						if (ps.length!=4) {
							throw new TranslateException("node attach should be with ../attach/filename");						
						}
						final String sAttach = _getLocationNodeName(ps[3]);
						_addfrom("attach");
						_where.appendSeparator().append("attach.docId=doc.id");
						if (!sAttach.equals("*")) {
							_where.appendSeparator().append("attach.filename='").append(sAttach).append("'");
						}
						traverse(ps[2], null);
						traverse(ps[3], "attach");
						_mainobj = "attach";
					} else if (sProp.equals("obj")) {
						if (ps.length!=5) {
							throw new TranslateException("node obj should be with .../obj/space/classname. Query of space not implemented");						
						}
						final String sClassSp	= _getLocationNodeName(ps[3]);
						final String sClass		= _getLocationNodeName(ps[4]);
						_addfrom("obj");
						_where.appendSeparator().append("obj.name=doc.fullName");
						boolean bcs = sClassSp.equals("*");
						boolean bcn = sClass.equals("*");
						if (bcs && !bcn) {
							_where.appendSeparator().append("obj.className like '%.").append(sClass).append("\'");
						} else if (!bcs && bcn) {
							_where.appendSeparator().append("obj.className like '").append(sClassSp).append(".%'");
						} else if (!bcs && !bcn) {
							_where.appendSeparator().append("obj.className='").append(sClassSp).append(".").append(sClass).append("\'");
						}
						_classname = sClassSp+"."+sClass;
						_objClassName.put("obj", _classname);
						
						traverse(ps[2], null);
						traverse(ps[3], null);
						_mainobj = "obj";
						traverse(ps[4], "obj");
					} else {
						throw new TranslateException("Undefined node of document: "+sProp);
					}
				}
			}
			return data;
		}

		public Object visit(LocationStepQueryNode node, Object data) {
			traverse(node.getOperands(), data);
			return data;
		}
		
		public Object visit(RelationQueryNode node, Object data) {
			if (data==null) {
				throw new TranslateException("No object for relation");
			}
			final String obj  = (String)data;
			final QName prop = node.getProperty();			
			if (!(node.getParent() instanceof AndQueryNode)
				&& !(node.getParent() instanceof OrQueryNode))
					_userwhere.appendSeparator();
			String sprop = getProp(prop, obj);
			_userwhere.append(sprop);
			int op = node.getOperation();
			final String sop = (String) _mapOphql.get(new Integer(op));
			_userwhere.append(sop);
	        int vt = node.getValueType(); 
	        if (vt == QueryConstants.TYPE_DOUBLE) {
	        	_userwhere.append(node.getDoubleValue());
	        } else if (vt == QueryConstants.TYPE_LONG) {
	        	_userwhere.append(node.getLongValue());
	        } else if (vt == QueryConstants.TYPE_POSITION) {
	        	_userwhere.append(node.getPositionValue()); // XXX: I don`t know that is it
	        } else if (vt == QueryConstants.TYPE_STRING) {
	        	_userwhere.append("'").append(tosqlstring(node.getStringValue())).append("'");
	        } else if (vt == QueryConstants.TYPE_TIMESTAMP || vt == QueryConstants.TYPE_DATE) {
	        	Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
	            cal.setTime(node.getDateValue());
	            _userwhere.append("'").append(ISO8601.format(cal)).append("'");
	        }
	        return data;
		}

		public Object visit(OrderQueryNode node, Object data) {
			final String obj = (String) data;
			final OrderQueryNode.OrderSpec[] specs = node.getOrderSpecs();
	        for (int i = 0; i < specs.length; i++) {
	        	final String sprop = getProp(specs[i].getProperty(), obj);	        	
	        	_order.appendSeparator().append(sprop);
	        	if (!specs[i].isAscending())
	        		_order.append(" DESC");
	        }
			return data;
		}

		public Object visit(DerefQueryNode node, Object data) {
			throw new TranslateException("Not implemented");
		}
		
		private int indent;
		private final void traverse(QueryNode[] node, Object buffer) {
	        indent ++;
	        for (int i = 0; i < node.length; i++) {
	            node[i].accept(this, buffer);
	        }
	        indent --;
	    }
		private final void traverse(QueryNode node, Object buffer) {
			indent ++;
			node.accept(this, buffer);
			indent --;
		}
				
		private Set _isfrom = new HashSet();
		private void _addfrom(String sf) {
			if (!_isfrom.contains(sf)) {
				String scl = (String) _mapObjClass.get(sf);
				if (scl==null)
					throw new TranslateException("addfrom: Undefined object "+sf);
				_from.appendSeparator().append(scl).append(" as ").append(sf);
				_isfrom.add(sf);
			}
		}
		Map ObjNumb = new HashMap();
		Integer getNextObjNumb(String sf) {
			if (ObjNumb.get(sf)==null) {
				ObjNumb.put(sf, new Integer(1));
			}
			final Integer n = (Integer) ObjNumb.get(sf);		
			ObjNumb.put(sf, new Integer(n.intValue()+1));
			return n;
		}
		Set _fromclass = new HashSet();
		//Map _objpropClass = new HashMap();
		private String _addnewfrom(String sf) {
			final String scl = (String) _mapObjClass.get(sf);
			return _addnewfrom(sf, scl);			
		}
		private String _addnewfrom(String sf, String scl) {			
			if (scl==null)
				throw new TranslateException("addfrom: Undefined object "+scl);			
			Integer n = getNextObjNumb(sf);
			final String r = sf+n;
			_from.appendSeparator().append(scl).append(" as ").append(r);
			_fromclass.add(scl);
			return r;
		}
		/*private String _addjoin(String what, String name) {
			Integer n = getNextObjNumb(name);
			final String r = name+n;
			_from.append(" join ").append(what).append(" ").append(r);
			return r;
		}*/
				
		private final String n2e(String s) {
			return s==null?"":s;
		}
		private final String tosqlstring(String s) {
			return s;
			//if (s==null) s=""; XXX: is this needed?
			//return s.replace('\'', '`');
		}
		
		public class TranslateException extends RuntimeException {
			private static final long serialVersionUID = 1L;
			public TranslateException(String string) {
				super(string);
			}
			public TranslateException(String string, Throwable e) {
				super(string, e);
			}
		}
		/** Map obj - BaseClass.name */
		Map _objClassName	= new HashMap();
		///** "obj:flex-prop" - string property for hql */ 
		//Map _props			= new HashMap();
		/** set of used PropertyClass */
		Set _propclass		= new HashSet();
		public String getProp(QName qname, String obj) {
			final String prop = qname.getLocalName();
			ObjProperty oprop = (ObjProperty) _propertyes.get(obj+"|"+prop);
			if (oprop!=null)
				return oprop.hqlname;
			
			if (XWikiNamespaceResolver.NS_DOC_URI.equals(qname.getNamespaceURI())) {
				_addfrom("doc");				
				return "doc"+ (prop.equals("self")?"":"."+prop);
			}
			if (XWikiNamespaceResolver.NS_OBJ_URI.equals(qname.getNamespaceURI())) {
				_addfrom("obj");
				return "obj"+(prop.equals("self")?"":"."+prop);
			}
			if (!XWikiNamespaceResolver.NS_FLEX_URI.equals(qname.getNamespaceURI()))
				return obj+"."+prop;
						
			final String classname = (String) _objClassName.get(obj);
			if (classname==null)
				throw new TranslateException("ClassName for "+obj+" not found");
			XWikiDocument doc = new XWikiDocument();
			doc.setFullName(classname);
			try {
				doc = getStore().loadXWikiDoc(doc, getContext());
			} catch (XWikiException e) {
				throw new TranslateException("Couldn't load BaseClass document", e);
			}
			BaseClass bc = doc.getxWikiClass();
			if (bc==null)
				throw new TranslateException("Couldn't load BaseClass");
			
			PropertyClass pc = (PropertyClass) bc.get(prop);
			if (pc==null)
				throw new TranslateException("Couldn`t find property "+prop+" of class "+classname);
			_propclass.add(pc.getClass());
			final Class propclass = pc.newProperty().getClass();
			String propclassname = propclass.getName();
			
			String result = _addnewfrom(prop, propclassname);
			
			_where.appendSeparator().append("obj.id=").append(result).append(".id.id")
				.appendSeparator().append(result).append(".name='").append(prop).append("'");
			
			String val = (String) _mapPropValue.get(propclassname);
			if (val==null)
				val = "value";
			result += "." + val;
			//if (propclass.equals(DBStringListProperty.class))
			//	result = _addjoin(result, prop+"_"+val);			
			
			oprop = new ObjProperty(obj, prop, result, pc.getClass(), propclass);
			_propertyes.put(obj+"|"+prop, oprop);
			
			return result;
		}
		static final int TYPE_DEFAULT = -1;
		static final int TYPE_LIST = TYPE_DEFAULT-1;
		protected int getPropType(QName prop, String obj) {
			getProp(prop, obj);
			ObjProperty oprop = getObjProperty(prop.getLocalName(), obj);
			if (oprop==null)
				return TYPE_DEFAULT;
			if (oprop.propertyclass.equals(DBStringListProperty.class))
				return TYPE_LIST;
			return TYPE_DEFAULT;
		}
		protected ObjProperty getObjProperty(String prop, String obj) {
			return (ObjProperty) _propertyes.get(obj+"|"+prop);
		}
		/**	 obj:prop - ObjProperty */
		Map _propertyes = new HashMap(); 
		class ObjProperty {			
			String	object,
					propname,
					hqlname;
			Class	basepropclass,
					propertyclass;
			public ObjProperty(String obj, String prop, String hqlname, Class bpclass, Class propclass) {
				this.object = obj;
				this.propname = prop;
				this.hqlname = hqlname;
				this.basepropclass = bpclass;
				this.propertyclass = propclass;
			}
		}
	}
	
	private static Map _mapOphql = new HashMap();
	static {
		_mapOphql.put(new Integer(QueryConstants.OPERATION_EQ_VALUE),	"=");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_EQ_GENERAL),	"=");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_NE_VALUE),	"<>");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_NE_GENERAL),	"<>");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_LT_VALUE),	"<");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_LT_GENERAL),	"<");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_GT_VALUE),	">");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_GT_GENERAL),	">");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_GE_VALUE),	">=");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_GE_GENERAL),	">=");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_LE_VALUE),	"<=");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_LE_GENERAL),	"=");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_LIKE),		" like ");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_BETWEEN),	" between ");		
		_mapOphql.put(new Integer(QueryConstants.OPERATION_IN),			" in ");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_NULL),		" is null ");
		_mapOphql.put(new Integer(QueryConstants.OPERATION_NOT_NULL),	" is not null ");
	}
	private static Map _mapObjClass = new HashMap();
	static {
		_mapObjClass.put("doc",		"XWikiDocument");
		_mapObjClass.put("attach",	"XWikiAttachment");
		_mapObjClass.put("obj",		"BaseObject");
		_mapObjClass.put("prop",	"BaseProperty");
	}
	/** Value name for classes properties */
	private static Map _mapPropValue = new HashMap();
	static {
		_mapPropValue.put("com.xpn.xwiki.objects.StringListProperty", 	"textValue");
		_mapPropValue.put("com.xpn.xwiki.objects.DBStringListProperty", "list");		
	}
	
	protected List hqlexec(String hql, Map params, int fs, int fr) throws XWikiException {
		boolean bTransaction = true;
		final MonitorPlugin monitor  = Util.getMonitorPlugin(getContext());
		List r = null;
		try {
			 // Start monitoring timer
            if (monitor!=null)
                monitor.startTimer("hibernate");
            
            hbstore.checkHibernate(getContext());
            bTransaction = hbstore.beginTransaction(getContext());            
                        
			final Session ses = hbstore.getSession(getContext());
			final Query q = ses.createQuery(hql);			
			if (params!=null && !params.isEmpty()) {
				for (Iterator iter = params.keySet().iterator(); iter.hasNext();) {
					final String element = (String) iter.next();
					final Object val = params.get(element);
					if (val instanceof Collection)
						q.setParameterList(element, (Collection) val);
					else
						q.setParameter(element, val);
				}
			}
			if (fs>0)
				q.setMaxResults(fs);
			if (fr>0)
				q.setFirstResult(fr);
			
			r = q.list();
			
			if (bTransaction)
                hbstore.endTransaction(getContext(), false);
			return r;
		} catch (Throwable e) {
			Object[] args = { hql };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
                    "Exception while searching documents with sql {0}", e, args);			
		} finally {
			try {
				if (bTransaction)
					hbstore.endTransaction(getContext(), false);
			} catch (Exception e) {}
			// End monitoring timer
			if (monitor!=null)
				monitor.endTimer("hibernate");
		}
	}
	public IQuery setDistinct(boolean d) {
		if (d!=_isdistinct)
			translator = null;
		return super.setDistinct(d);
	}
}
