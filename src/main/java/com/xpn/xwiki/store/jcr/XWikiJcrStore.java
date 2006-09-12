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
package com.xpn.xwiki.store.jcr;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.stats.impl.XWikiStats;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.util.Util;
import org.apache.portals.graffito.jcr.query.Filter;
import org.apache.portals.graffito.jcr.query.QueryManager;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.VersionException;
import javax.transaction.NotSupportedException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/** XWikiJCRStore - XWiki Store System backend to JCR */
public class XWikiJcrStore extends XWikiJcrBaseStore implements XWikiStoreInterface {
	public XWikiJcrStore(XWiki xwiki, XWikiContext context) throws SecurityException, IllegalArgumentException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, XWikiException {
		super(xwiki, context);
		// create default wiki
		createWiki("xwiki", context);
	}
	
	public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
		saveXWikiDoc(doc, context, true);
	}
	public void saveXWikiDoc(final XWikiDocument doc, final XWikiContext context, boolean bTransaction) throws XWikiException {
		MonitorPlugin monitor  = Util.getMonitorPlugin(context);
		try {
			//    From XWikiHibernateStore:
			// Start monitoring timer
            if (monitor!=null)
                monitor.startTimer("jcr");
            doc.setStore(this);
            // Make sure the database name is stored
            doc.setDatabase(context.getDatabase());
            
            // These informations will allow to not look for attachments and objects on loading
            doc.setElement(XWikiDocument.HAS_ATTACHMENTS, (doc.getAttachmentList().size()!=0));
            doc.setElement(XWikiDocument.HAS_OBJECTS, (doc.getxWikiObjects().size()!=0));

            // Let's update the class XML since this is the new way to store it
            final BaseClass bclass = doc.getxWikiClass();
            if ((bclass!=null)&&(bclass.getFieldList().size()>0))
                doc.setxWikiClassXML(bclass.toXMLString());
            if (bclass!=null)
                bclass.setName(doc.getFullName());
            
            /*not needed: if (doc.hasElement(XWikiDocument.HAS_ATTACHMENTS))
                saveAttachmentList(doc, context, false);*/
            boolean needUpdateArchive = false;
            boolean needSaveArchive = false;
            boolean needGetArchive = false;
            // Handle the latest text file
            if (doc.isContentDirty()||doc.isMetaDataDirty()) {
                Date ndate = new Date();
                doc.setDate(ndate);
                if (doc.isContentDirty()) {
                    doc.setContentUpdateDate(ndate);
                    doc.setContentAuthor(doc.getAuthor());
                }
                doc.incrementVersion();
                needUpdateArchive = true;                
            } else {
            	if (doc.getDocumentArchive()!=null) {
                    // Let's make sure we save the archive if we have one
                    // This is especially needed if we load a document from XML
            		needSaveArchive = true;
                } else {
                    // Make sure the getArchive call has been made once
                    // with a valid context
                   	needGetArchive = true;                        
                }
            }
            //   end
            executeWrite(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession ses) throws RepositoryException, XWikiException {
					Node storeNode = ses.getStoreNode();
					Node spaceNode = JcrUtil.getOrCreateSubNode(storeNode, doc.getWeb(), ntXWikiSpace);
					String docNodeName = doc.getName();
					String docLang = doc.getLanguage();
					if (docLang!=null) {
						docLang = docLang.trim();
						if (!"".equals(docLang))
							docNodeName += "." + docLang;
					}
					Node docNode = JcrUtil.getOrCreateSubNode(spaceNode, docNodeName, ntXWikiDocument);
					Node baseDocNode = JcrUtil.getOrCreateSubNode(spaceNode, doc.getName(), ntXWikiDocument);
					Node attachNode = JcrUtil.getOrCreateSubNode(docNode, "attach", ntXWikiAttachments);
					ses.updateObject(docNode, doc);
					
					NodeIterator ni = attachNode.getNodes();
					while (ni.hasNext()) {
						ni.nextNode().setProperty("doc", docNode);
					}
					
					// Not needed: Remove objects planned for removal
					doc.setObjectsToRemove(new ArrayList());
					if (bclass!=null) {
		                bclass.setName(doc.getFullName());
		                if (bclass.getFieldList().size()>0) {
		                	Node classNode = JcrUtil.getOrCreateSubNode(baseDocNode, "class", ntXWikiClass);
		                	saveXWikiClass(ses, classNode, bclass);
		                }
		            } else {
		            	ses.removeObject(baseDocNode.getPath()+"/class");
		            }
					
					ses.removeObject(baseDocNode.getPath() +"/obj");					
					if (doc.hasElement(XWikiDocument.HAS_OBJECTS)) {
	            		Iterator it = doc.getxWikiObjects().values().iterator();
		                while (it.hasNext()) {
		                    Vector objects = (Vector) it.next();
		                    for (int i=0;i<objects.size();i++) {
		                        BaseCollection obj = (BaseCollection)objects.get(i);
		                        if (obj!=null) {
		                            saveXWikiCollection(ses, baseDocNode, obj);
		                        }
		                    }
		                }
		            }
					if (context.getWiki().hasBacklinks(context)) {
		                saveLinks(doc, context, false);
		            }
					doc.setNew(false);
					ses.save();
		            return null;
				}
            });
            
            if (needUpdateArchive)
            	context.getWiki().getVersioningStore().updateXWikiDocArchive(doc, doc.toXML(context), false, context);
            if (needSaveArchive)
            	context.getWiki().getVersioningStore().saveXWikiDocArchive(doc.getDocumentArchive(),false, context);
            if (needGetArchive) {
            	try {
                	doc.getDocumentArchive(context);
                } catch (XWikiException e) {
                    // this is a non critical error
                }
            }
		} catch (Exception e) {
			Object[] args = { doc.getFullName() };
			throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_SAVING_DOC, "Exception while saving document {0}", e, args);
		} finally {
			// End monitoring timer
            if (monitor!=null)
                monitor.endTimer("jcr");
		}
	}
	
	private void saveXWikiClass(XWikiJcrSession ses, Node classNode, BaseClass bclass) throws RepositoryException {
		clearNodeChildrens(classNode);
		ses.updateObject(classNode, bclass, BaseClass.class);
		Iterator it = bclass.getFieldList().iterator();
		while (it.hasNext()) {
			PropertyClass pc = (PropertyClass) it.next();
			ses.insertObject(classNode, pc.getName(), pc);
		}
	}

	private Node saveXWikiCollection(XWikiJcrSession ses, Node docnode, BaseCollection object) throws XWikiException {
		try {
			// We need a slightly different behavior here
            boolean stats = (object instanceof XWikiStats);
            
			String oclassname = object.getClassName();
			int i = oclassname.lastIndexOf(".");
		    String oweb = oclassname.substring(0, i);
		    String oname = oclassname.substring(i+1);
		    Node wnode = docnode;
			wnode = JcrUtil.getOrCreateSubNode(wnode, "obj", ntXWikiObjects);
			if (!"".equals(oweb)) // if className = internal, then save to .../doc/obj/internal
				wnode = JcrUtil.getOrCreateSubNode(wnode, oweb,	 ntXWikiSpaceObject);
			Node onode;
			if (stats)
				onode = ses.insertObject(wnode, oname, object);
			else
				onode = ses.insertObject(wnode, oname, object, BaseObject.class);
			
			onode.setProperty("doc", docnode);
			
			if (object.getClassName().equals("internal")) return onode;
			
			Iterator it = object.getPropertyList().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                BaseProperty prop = (BaseProperty) object.getField(key);
                if (!prop.getName().equals(key)) {
                    Object[] args = { key, object.getName() };
                    throw new XWikiException(XWikiException.MODULE_XWIKI_CLASSES, XWikiException.ERROR_XWIKI_CLASSES_FIELD_INVALID,
                            "Field {0} in object {1} has an invalid name", null, args);
                }
                String pname = prop.getName();
                if(pname != null && !pname.trim().equals("")) {
                	saveXWikiProperty(ses, onode, prop);
                }
            }
            return onode;
		} catch (RepositoryException e) {
	        Object[] args = { object.getName() };
	        throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER,
	                "Exception while saving object {0}", e, args);
	    }
	}
	private BaseObject loadXWikiCollection(XWikiJcrSession ses, Node nobj, XWikiContext context) throws RepositoryException, XWikiException {
		BaseObject object = (BaseObject) ses.loadObject(nobj.getPath());
		if (!object.getClassName().equals("")) {
			BaseObject newobject = BaseClass.newCustomClassInstance(object.getClassName(), context);
			if (newobject!=null) {
                newobject.setId(object.getId());
                newobject.setClassName(object.getClassName());
                newobject.setName(object.getName());
                newobject.setNumber(object.getNumber());
                object = newobject;
            }
		}
		NodeIterator npi = nobj.getNodes(); // properties
		while (npi.hasNext()) {
			Node nprop = npi.nextNode();
			BaseProperty prop = loadXWikiProperty(ses, nprop);
			prop.setObject(object);
			object.addField(prop.getName(), prop);
		}
		return object;
	}
	
	private void saveXWikiProperty(XWikiJcrSession ses, Node objnode, BaseProperty prop) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
		Node propNode = ses.insertObject(objnode, prop.getName(), prop);
		propNode.setProperty("obj", objnode);
	}
	private BaseProperty loadXWikiProperty(XWikiJcrSession ses, Node nprop) throws RepositoryException {
		BaseProperty prop = (BaseProperty) ses.loadObject(nprop.getPath());
		prop.setName( decode( nprop.getName() ) );
		return prop;
	}
	
	public XWikiDocument loadXWikiDoc(final XWikiDocument doc, final XWikiContext context) throws XWikiException {
		MonitorPlugin monitor = Util.getMonitorPlugin(context);
		try {
			// Start monitoring timer
            if (monitor!=null)
                monitor.startTimer("jcr");
			XWikiDocument retdoc = (XWikiDocument) executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession ses) throws RepositoryException, XWikiException {
					XWikiDocument retdoc = (XWikiDocument) ses.loadObject(getDocPath(doc));
					if (retdoc!=null) {
						retdoc.setStore(context.getWiki().getStore());
						retdoc.setDatabase(context.getDatabase());
						retdoc.setNew(false);
						if (retdoc.getAttachmentList()==null) 
							retdoc.setAttachmentList(new ArrayList());
						Iterator it = retdoc.getAttachmentList().iterator();
						while (it.hasNext()) {
							XWikiAttachment att = (XWikiAttachment) it.next();
							att.setDoc(retdoc);
						}
					} else {
						doc.setNew(true);
						return doc;
					}
					Node baseDocNode = ses.getNode(getBaseDocPath(doc));
					//TODO: handle the case where there are no xWikiClass and xWikiObject in the Database
					BaseClass bclass = new BaseClass();
					String cxml = retdoc.getxWikiClassXML();
		            if (cxml!=null) {
		                bclass.fromXML(cxml);
		                bclass.setName(retdoc.getFullName());
		                retdoc.setxWikiClass(bclass);
		            } else {
		            	try {
		            		Node nclass = baseDocNode.getNode("class");
		            		bclass = loadXWikiClass(ses, nclass);
		            	} catch (PathNotFoundException e) {
		            		bclass = new BaseClass();
		            	}
		            	bclass.setName(retdoc.getFullName());
		            	retdoc.setxWikiClass(bclass);		            	
		            }
		            // Store this XWikiClass in the context so that we can use it in case of recursive usage of classes
		            context.addBaseClass(bclass);
			        
		            if (doc.hasElement(XWikiDocument.HAS_OBJECTS)) {
			            Node nobjs = JcrUtil.getOrCreateSubNode(baseDocNode, "obj", ntXWikiObjects);
						NodeIterator nsi = nobjs.getNodes(); // spaces
						while (nsi.hasNext()) {
							Node nspace = nsi.nextNode();
							// We use the internal class to store the statistics
							if (ntXWikiObject.equals( nspace.getPrimaryNodeType().getName() )) // internal class
								continue;
							NodeIterator noi = nspace.getNodes(); // objects
							while (noi.hasNext()) {
								Node nobj = noi.nextNode();
								BaseObject object = loadXWikiCollection(ses, nobj, context);
								object.setName(doc.getFullName());
								retdoc.setObject(object.getClassName(), object.getNumber(), object);
							}
						}
		            }
		            return retdoc;
				}
			});
			return retdoc;
		} catch (Exception e) {
			Object[] args = { doc.getFullName() };
			throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Exception while loading document {0}", e, args);
		} finally {
			if (monitor!=null)
                monitor.endTimer("jcr");
		}
	}

	private BaseClass loadXWikiClass(XWikiJcrSession ses, Node nclass) throws RepositoryException {
		BaseClass bclass = (BaseClass) ses.loadObject(nclass.getPath());
		if (bclass==null)
			return null;
		NodeIterator ni = nclass.getNodes(); // PropertyClasses
		while (ni.hasNext()) {
			Node npc = ni.nextNode();
			PropertyClass pc = (PropertyClass) ses.loadObject(npc.getPath());
			bclass.addField(pc.getName(), pc);
		}
		return bclass;
	}

	public void deleteXWikiDoc(final XWikiDocument doc, XWikiContext context) throws XWikiException {
		try {
			executeWrite(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws RepositoryException, XWikiException {
					session.removeObject(getDocPath(doc));
					return null;
				}
			});
		} catch (Exception e) {
			Object[] args = { doc.getFullName() };
			throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Exception while deleting document {0}", e, args);
		}
	}
	
	public void cleanUp(XWikiContext context) {
	}

	public void createWiki(String wikiName, XWikiContext context) throws XWikiException {
		try {
			jcr.initWorkspace(wikiName);
		} catch (Exception e) {
			throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Cannot create new xwiki workspace: "+wikiName, e);
		}
	}

	public boolean exists(final XWikiDocument doc, XWikiContext context) throws XWikiException {
		try {
			return ((Boolean)executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws RepositoryException, XWikiException {
					return new Boolean(session.isNodeExist( getDocPath(doc) ));
				}
			})).booleanValue();
		} catch (Exception e) {
			Object[] args = { doc.getFullName() };
			throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Exception while check exists document {0}", e, args);
		}
	}
	// Locks
	public XWikiLock loadLock(final long docId, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
        	return (XWikiLock) executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					Node docNode = getDocNodeById(session, docId);
					if (docNode==null)
						return null;
					XWikiLock lock = (XWikiLock) session.loadObject(docNode.getPath() + "/lock");
					if (lock==null)
						return null;
					lock.setDocId(docId);
					return lock;
				}
        	});
        } catch (Exception e) {
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_LOCK,
                    "Exception while loading lock", e);
        }
	}

	public void saveLock(final XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException {
		try {
			final String docPath = getDocPathById(context, lock.getDocId());
			if (docPath == null) // XXX: how to create lock to non existed document? create doc?
				return;
			executeWrite(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					Node docNode = session.getNode(docPath);
					Node lockNode = JcrUtil.getOrCreateSubNode(docNode, "lock", ntXWikiLock);
					session.updateObject(lockNode, lock);
					return null;
				}
			});
        }
        catch (Exception e) {
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_SAVING_LOCK,
                    "Exception while locking document", e);
        }
	}

	public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException {
		try {
			final String docPath = getDocPathById(context, lock.getDocId());
			executeWrite(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					session.removeObject(docPath+"/lock");
					return null;
				}
			});
        }
        catch (Exception e) {
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_DELETING_LOCK,
                    "Exception while deleting lock", e);
        }		
	}

	public List loadLinks(final long docId, XWikiContext context, boolean bTransaction) throws XWikiException {
		final List links=new ArrayList();
		try {
			executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession ses) throws Exception {
					Node docNode = getDocNodeById(ses, docId);
					if (docNode==null) return null;
					try {
						Node linksNode = docNode.getNode("links");
						NodeIterator ni = linksNode.getNodes();
						while (ni.hasNext()) {
							Node linkNode = ni.nextNode();
							XWikiLink link = (XWikiLink) ses.loadObject(linkNode.getPath());
							link.setLink( decode( linkNode.getName() ) );
							links.add(link);
						}
					} catch (PathNotFoundException e) {};
					return null;
				}
        	});
        }
        catch (Exception e) {
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_LINKS,
                    "Exception while loading links", e);
        }
        return links;
	}

	public List loadBacklinks(final String fullName, XWikiContext context, boolean bTransaction) throws XWikiException {
		final List backlinks = new ArrayList();
        try {
        	executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession ses) throws Exception {
					Query q = ses.getWorkspace().getQueryManager().createQuery("//element("+fullName+",xwiki:link)/@fullName", Query.XPATH);
					RowIterator ri =  q.execute().getRows();
					while (ri.hasNext()) {
						Row row = ri.nextRow();
						String s = row.getValues()[0].getString();
						backlinks.add(s);
					}
					return null;
				}
        	});
        } catch (Exception e) {
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_BACKLINKS,
                    "Exception while loading backlinks", e);
        }
        return backlinks;
	}
	
	public void saveLinks(final XWikiDocument doc, final XWikiContext context, boolean bTransaction) throws XWikiException {
		try {
            // need to delete existing links before saving the page's one
            deleteLinks(doc.getId(), context, bTransaction);
            // necessary to blank links from doc
            context.remove("links");
            // call to RenderEngine and converting the list of links into a list of backlinks
            XWikiRenderer renderer = context.getWiki().getRenderingEngine().getRenderer("wiki");
            renderer.render(doc.getContent(), doc, doc, context);
            final List links = (List)context.get("links");
            if (links == null || links.size()==0) return;
            executeWrite(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession ses) throws Exception {
					Node docNode = ses.getNode( getDocPath(doc) );
					Node linksNode = JcrUtil.getOrCreateSubNode(docNode, "links", ntXWikiLinks);
					clearNodeChildrens(linksNode);
					for (int i=0;i<links.size();i++) {
						XWikiLink link = new XWikiLink();
	                    link.setDocId(doc.getId());
	                    link.setLink((String)links.get(i));
	                    link.setFullName(doc.getFullName());
	                    String nodeName = link.getLink();
	                    nodeName = encode(nodeName);
	                    ses.insertObject(linksNode, nodeName, link);
	                }
			        ses.save();
					return null;
				}
            });
        } catch (Exception e) {
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_SAVING_LINKS,
                    "Exception while saving links", e);
        }
	}

	public void deleteLinks(final long docId, XWikiContext context, boolean bTransaction) throws XWikiException {
		try {
			final String docPath = getDocPathById(context, docId);
			if (docPath==null) return;
			executeWrite(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession ses) throws Exception {					
					try {
						Node linksNode = (Node) ses.getItem(docPath+"/links");
						if (linksNode!=null)
							linksNode.remove();
						ses.save();
					} catch (PathNotFoundException e) {};
					return null;
				}
			});
        } catch (Exception e) {
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_DELETING_LINKS,
                    "Exception while deleting links", e);
        }
	}
	
	public List getClassList(XWikiContext context) throws XWikiException {
		try {
			return (List) executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					Filter filter = session.getObjectQueryManager().createFilter(XWikiDocument.class)
						.addNotNull("xWikiClassXML");
					return searchStringAttribute(session, filter, "fullName");
				}
			});
		} catch (Exception e) {
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_SEARCH,
                    "Exception while searching class list", e);
        } 
	}

	public List getTranslationList(final XWikiDocument doc, final XWikiContext context) throws XWikiException {
		try {
			return (List) executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					Filter filter = session.getObjectQueryManager().createFilter(XWikiDocument.class);
					filter.addEqualTo("name", doc.getName());
					filter.addEqualTo("web", doc.getWeb());
					filter.addNotEqualTo("language", "");
					return searchStringAttribute(session, filter, "language");
				}
			});
		} catch (Exception e) {
			throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Exception while getTranslationList", e);
		}
	}
	
	private List searchStringAttribute(XWikiJcrSession session, Filter filter, String returnatt) throws InvalidQueryException, RepositoryException {
		String sq = session.getObjectQueryManager().buildJCRExpression(
						session.getObjectQueryManager().createQuery(filter) );
		sq += "/@" + returnatt;
		return searchStringAttribute(session, sq);
	}
	private List searchStringAttribute(XWikiJcrSession session, String xpath) throws InvalidQueryException, RepositoryException {
		List result = new ArrayList();
		RowIterator ri = session.getQueryManager().createQuery(xpath, Query.XPATH).execute().getRows();
		while (ri.hasNext()) {
			result.add( ri.nextRow().getValues()[0].getString() );
		}
		return result;		
	}
	private List searchNodeNames(XWikiJcrSession session, String xpath) throws InvalidQueryException, RepositoryException {
		List result = new ArrayList();
		NodeIterator ri = session.getQueryManager().createQuery(xpath, Query.XPATH).execute().getNodes();
		while (ri.hasNext()) {
			result.add( decode( ri.nextNode().getName() ) );
		}
		return result;
	}
	
	public List getAllDocuments(final XWikiContext context) throws XWikiException {
		try {
			return (List) executeRead(context, new JcrCallBack(){
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					Filter filter = session.getObjectQueryManager().createFilter(XWikiDocument.class);
					return searchStringAttribute(session, filter, "fullName");
				}
			});
		} catch (Exception e) {
			throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Exception while getAllDocuments", e);
		}
	}

	public List getSpaces(XWikiContext context) throws XWikiException {
		try {
			return (List) executeRead(context, new JcrCallBack(){
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					return searchNodeNames(session, "store/*");
				}
			});
		} catch (Exception e) {
			throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Exception while getSpaces", e);
		}
	}

	public List getSpaceDocsName(final String spaceName, XWikiContext context) throws XWikiException {
		try {
			return (List) executeRead(context, new JcrCallBack(){
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					Filter filter = session.getObjectQueryManager().createFilter(XWikiDocument.class)
						.addEqualTo("web", spaceName);
					return searchStringAttribute(session, filter, "name");
				}
			});
		} catch (Exception e) {
			throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Exception while getSpaceDocsName", e);
		}
	}
	public List listGroupsForUser(final String username, XWikiContext context) throws XWikiException {
		final String shortname = Util.getName(username);
		final List result = new ArrayList();
		try {
			executeRead(context, new JcrCallBack(){
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					// XXX: if username contains "'" ?
					String xpath = "store/*/*/obj/XWiki/XWikiGroups/member[@value='"+username+"' or @value='"+shortname+"']";
					Query query = session.getQueryManager().createQuery(xpath, Query.XPATH);
					NodeIterator ni = query.execute().getNodes();
					while (ni.hasNext()) {
						Node docnode = ni.nextNode().getParent().getParent().getParent().getParent();
						result.add(docnode.getParent().getName() + "." + docnode.getName());
					}
					return null;
				}
			});
			return result;
		} catch (Exception e) {
			throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Exception while listGroupsForUser", e);
		}
	}
	
	public void notSupportedCall() {
		new NotSupportedException().printStackTrace();
	}
	//	 Not supported:
	public List getCustomMappingPropertyList(BaseClass bclass) {
		notSupportedCall();
		return null;
	}

	public boolean injectCustomMapping(BaseClass doc1class, XWikiContext xWikiContext) throws XWikiException {
		notSupportedCall();
		return false;
	}

	public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return false;
	}

	public void injectCustomMappings(XWikiContext context) throws XWikiException {
		notSupportedCall();
	}

	public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException {
		notSupportedCall();
	}

	public List searchDocuments(String wheresql, boolean distinctbyname, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}

	public List searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}

	public List searchDocuments(String wheresql, boolean distinctbyname, int nb, int start, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}

	public List searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, int nb, int start, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}

	public List searchDocuments(String wheresql, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}

	public List searchDocuments(String wheresql, int nb, int start, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}

	public List searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}

	public List searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}

	public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return false;
	}

	public List searchDocuments(String wheresql, boolean distinctbyname, boolean customMapping, boolean checkRight, int nb, int start, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}

	public List searchDocumentsNames(String wheresql, int nb, int start, String selectColumns, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}

	public List search(String sql, int nb, int start, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}

	public List search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context) throws XWikiException {
		notSupportedCall();
		return null;
	}
	//
	public List getAllObjectsByClass(final Class cl, XWikiContext context) throws XWikiException {
		QueryManager qm = getObjectQueryManager(context);
		return getObjects( qm.createQuery( qm.createFilter(cl) ), context );		
	}
	public QueryManager getObjectQueryManager(XWikiContext context) throws XWikiException {
		// TODO: store query manager in class.
		try {
			return (QueryManager) executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					return session.getObjectQueryManager();
				}
			 });
		} catch (Exception e) {
			throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Exception while getObjectQueryManager", e);
		}
	}

	public List getObjects(final org.apache.portals.graffito.jcr.query.Query query, XWikiContext context) throws XWikiException {
		final List result = new ArrayList();
		try {
			executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					String xpath = session.getObjectQueryManager().buildJCRExpression(query);
					NodeIterator ni = session.getQueryManager().createQuery(xpath, Query.XPATH).execute().getNodes();
					while (ni.hasNext()) {
						Node node = ni.nextNode();
						Object object = session.loadObject(node.getPath());
						result.add(object);
					}
					return null;
				}
			});
		} catch (Exception e) {
			throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_OTHER, "Exception while getObjects", e);
		}
		return result;
	}
}
