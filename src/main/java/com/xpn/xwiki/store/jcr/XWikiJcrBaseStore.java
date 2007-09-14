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
package com.xpn.xwiki.store.jcr;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.portals.graffito.jcr.query.Filter;

import javax.jcr.*;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.util.TraversingItemVisitor;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class XWikiJcrBaseStore {
	IJcrProvider jcr;
	
	public XWikiJcrBaseStore(XWiki xwiki, XWikiContext context) throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		super();
		if (xwiki.getStore() instanceof XWikiJcrBaseStore) {
			XWikiJcrBaseStore jcrstore = (XWikiJcrBaseStore) xwiki.getStore();
			this.jcr = jcrstore.getJcrProvider();
		} else {
			String sprovider = xwiki.Param("xwiki.store.jcr.provider");
			Constructor cn = Class.forName(sprovider).getConstructor(new Class[]{XWikiConfig.class, XWikiContext.class});
			IJcrProvider jcr = (IJcrProvider) cn.newInstance(new Object[]{context.getWiki().getConfig(), context});
			this.jcr = jcr;
		}
	}
	
	protected Node getDocNodeById(XWikiJcrSession session, long docId) throws InvalidQueryException, RepositoryException {
		Filter filter = session.getObjectQueryManager().createFilter(XWikiDocument.class);
		filter = filter.addEqualTo("id", new Long(docId));
		String s = session.getObjectQueryManager().buildJCRExpression(
				session.getObjectQueryManager().createQuery(filter));
		Query q = session.getQueryManager().createQuery(s, Query.XPATH);
		QueryResult qr = q.execute();
		NodeIterator ni = qr.getNodes();
		if (ni.getSize()==1)
			return ni.nextNode();
		return null;
	}
	protected String getDocPathById(XWikiContext context, final long docId) throws Exception {
		return (String) executeRead(context, new JcrCallBack() {
			public Object doInJcr(XWikiJcrSession ses) throws Exception {
				Node docNode = getDocNodeById(ses, docId);
				if (docNode==null)
					return null;
				return docNode.getPath();
			}
		});
	}

	/** Delete all data in workspace! For junit testing. 
	 * @throws RepositoryException 
	 * @throws LoginException 
	 * @throws XWikiException */
	public void cleanupWiki(String workspace) throws XWikiException {
		try {
			XWikiJcrSession ses = jcr.getSession(workspace);
			try {
				ses.getStoreNode().remove();
				ses.save();
			} finally {
				ses.logout();
			}
		} catch (Exception e) {
			throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Exception while cleanupWiki("+workspace+")", e);
		}
	}
	public void shutdown(XWikiContext context) {
		XWikiJcrSession ses = (XWikiJcrSession) context.get("jcrsession");
		if (ses!=null)
			ses.getJcrSession().logout();
		jcr.shutdown();
	}

	public IJcrProvider getJcrProvider() {
		return jcr;
	}
	
	/** for testing */
	public String dumpData(XWikiContext context) {
		StringWriter sw = new StringWriter();
		final PrintWriter out = new PrintWriter(sw);
		try {
			executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					session.getStoreNode().accept(new TraversingItemVisitor.Default() {
						protected void entering(Node node, int level) throws RepositoryException {
							while (level-->0)
								out.print(' ');
							out.println(node.getPath() + " - " + node.getPrimaryNodeType().getName());
						}
						protected void entering(Property property, int level) throws RepositoryException {
							while (level-->0)
								out.print(' ');
							out.print("@ "+property.getName() + " = ");
							if (property.getDefinition().isMultiple())
								out.print("'"+property.getValues()+"'");
							else {
								String s = property.getValue().getString();
								if (s.length() > 50)
									out.print("...");
								else
									out.print("'"+s+"'");
							}
							out.println(" - " + property.getType());
						}
					});
					return null;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sw.toString();
	}

	protected void clearNodeChildrens(Node node) throws RepositoryException {
		NodeIterator ni = node.getNodes();
		while (ni.hasNext())
			ni.nextNode().remove();
	}
	
	/** springmodules-jcr like Callback interface for working in jcr */
	public interface JcrCallBack {
		Object doInJcr(XWikiJcrSession session) throws Exception;
	}
	private XWikiJcrSession getReadSession(XWikiContext context) throws RepositoryException {
		if (context.get("jcrsession")==null) {
			context.put("jcrsession", jcr.getSession(context.getDatabase()));
		}
		return (XWikiJcrSession) context.get("jcrsession");
	}
	private XWikiJcrSession getWriteSession(XWikiContext context) throws LoginException, RepositoryException {
		return jcr.getSession(context.getDatabase());
	}	
	/** springmodules-jcr like execute method for read-only operations in jcr. Logout is not needed  
	 * @throws Exception 
	 * @throws Exception */
	public Object executeRead(XWikiContext context, JcrCallBack cb) throws Exception {
		XWikiJcrSession session = getReadSession(context);
		Object res = cb.doInJcr(session);
		session.save(); // XXX: hack
		return res;
	}
	/** springmodules-jcr like execute method for write operations in jcr. Logout is not needed 
	 * @throws Exception */
	protected Object executeWrite(XWikiContext context, JcrCallBack cb) throws Exception {
		//XWikiJcrSession session = getWriteSession(context);
		XWikiJcrSession session = getReadSession(context);
		try {
			Object o = cb.doInJcr(session);
			session.save();
			return o;
		} finally {
			//session.logout();
		}
	}
	protected String getBaseDocPath(XWikiDocument doc) {
		return "/store/"+ encode(doc.getSpace()) + '/' + encode( ( doc.getName() ) );
	}
	protected String getDocPath(XWikiDocument doc) {
		String s = getBaseDocPath(doc);
		String lang = doc.getLanguage();
		if (lang!=null && !"".equals(lang))
			s += "."+lang;
		return s;
	}
	
	protected String encode(String s) {
		return ISO9075.encode(s);
	}
	protected String decode(String s) {
		return ISO9075.decode(s);
	}
	
	public static final String	ntXWikiStore			= "xwiki:store",
								ntXWikiSpace			= "xwiki:space",
								ntXWikiDocument			= "xwiki:document",
								ntXWikiAttachments		= "xwiki:attachments",
								ntXWikiAttachment		= "xwiki:attachment",
								ntXWikiAttachmentContent= "xwiki:attachmentContent",
								ntXWikiAttachmentArchive= "xwiki:attachmentArchive",
								ntXWikiClass			= "xwiki:class",
								ntXWikiObjects			= "xwiki:objects",
								ntXWikiSpaceObject		= "xwiki:spaceobject",
								ntXWikiObject			= "xwiki:object",
								ntXWikiProperty			= "xwiki:property",
								ntXWikiLinks			= "xwiki:links",
								ntXWikiLock				= "xwiki:lock";
}
