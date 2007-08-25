package com.xpn.xwiki.store.jcr;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;

import org.suigeneris.jrcs.rcs.Version;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;

/** Versions store in jcr property '@archive' of xwiki:document */
public class XWikiJcrPropertyVersioningStore extends XWikiJcrBaseStore implements XWikiVersioningStoreInterface {
	public XWikiJcrPropertyVersioningStore(XWiki xwiki, XWikiContext context) throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		super(xwiki, context);
	}
	
	public XWikiJcrPropertyVersioningStore(XWikiContext context) throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        this(context.getWiki(), context);
    }
	
	public void saveXWikiDocArchive(final XWikiDocumentArchive archivedoc, boolean bTransaction, final XWikiContext context) throws XWikiException {
		try {
			executeWrite(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					Node docNode = getDocNodeById(session, archivedoc.getId());
					if (docNode==null) return null;
					String s = archivedoc.getArchive(context);
					docNode.setProperty("archive", s);
					session.save();
					return null;
				}
			});
        } catch (Exception e) {
            Object[] args = { new Long(archivedoc.getId()) };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_SAVING_OBJECT,
                    "Exception while saving archive {0}", e, args);
        }
	}
	
	public void loadXWikiDocArchive(final XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context) throws XWikiException {
		try {
			executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					Node docNode = getDocNodeById(session, archivedoc.getId());
					if (docNode==null) return null;
					try {
						Property prop = docNode.getProperty("archive");
						String s = prop.getString();
						archivedoc.setArchive( s );
					} catch (PathNotFoundException e) {};
					return null;
				}
			});
        } catch (Exception e) {
            Object[] args = { new Long(archivedoc.getId()) };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_OBJECT,
                    "Exception while loading archive {0}", e, args);
        }
	}

	// From XWikiHibernateVersioningStore:
	public void resetRCSArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException {
		try {
            XWikiDocumentArchive archivedoc = new XWikiDocumentArchive(doc.getId());
            loadXWikiDocArchive(archivedoc, bTransaction, context);
            archivedoc.resetArchive();
            archivedoc.getDeletedNodeInfo().clear();
            doc.setMinorEdit(false);
            updateXWikiDocArchive(doc, false, context);
            saveXWikiDocArchive(archivedoc, bTransaction, context);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_OBJECT,
                    "Exception while loading archive {0}", e, args);
        }
	}

	

	public void updateXWikiDocArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException {
		try {
			XWikiDocumentArchive archivedoc = getXWikiDocumentArchive(doc, context);
            archivedoc.updateArchive(doc, doc.getContentAuthor(), doc.getDate(), doc.getComment(),
                doc.getRCSVersion(), context);
            saveXWikiDocArchive(archivedoc, bTransaction, context);            
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_OBJECT,
                    "Exception while loading archive {0}", e, args);
        }
	}

	public XWikiDocument loadXWikiDoc(XWikiDocument basedoc, String version, XWikiContext context) throws XWikiException {
	    try {
    	    XWikiDocumentArchive archive = getXWikiDocumentArchive(basedoc, context);
    	    XWikiDocument doc = archive.loadDocument(new Version(version), context);
    	    doc.setDatabase(basedoc.getDatabase());
    	    doc.setStore(basedoc.getStore());
    	    doc.setFullName(basedoc.getFullName());
    	    return doc;
        } catch (Exception e) {
            Object[] args = { basedoc.getFullName(), version.toString() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_READING_VERSION,
                    "Exception while reading document {0} version {1}", e, args);
        }
	}

	public Version[] getXWikiDocVersions(XWikiDocument doc, XWikiContext context) throws XWikiException {
		try {
		    XWikiDocumentArchive archive = getXWikiDocumentArchive(doc, context);
            if (archive==null)
                return new Version[0];
            Collection nodes = archive.getNodes();
            Version[] versions = new Version[nodes.size()];
            Iterator it = nodes.iterator();
            for (int i=0; i<versions.length; i++) {
                XWikiRCSNodeInfo node = (XWikiRCSNodeInfo) it.next();
                versions[i] = node.getId().getVersion();
            }
            return versions;            
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_READING_REVISIONS,
                    "Exception while reading document {0} revisions", e, args);
        }
	}

	public XWikiDocumentArchive getXWikiDocumentArchive(XWikiDocument doc, XWikiContext context) throws XWikiException {
        String key = ((doc.getDatabase()==null)?"xwiki":doc.getDatabase()) + ":" + doc.getFullName();
        synchronized (key) {
            XWikiDocumentArchive archivedoc = (XWikiDocumentArchive) context.getDocumentArchive(key);
            if (archivedoc==null) {
                String db = context.getDatabase();
                try {
                    if (doc.getDatabase()!=null)
                        context.setDatabase(doc.getDatabase());
                    archivedoc = new XWikiDocumentArchive(doc.getId());
                    loadXWikiDocArchive(archivedoc, true, context);
                } finally {
                    context.setDatabase(db);
                }
                // This will also make sure that the Archive has a strong reference
                // and will not be discarded as long as the context exists.
                context.addDocumentArchive(key, archivedoc);
            }
            return archivedoc;
        }
    }

    public XWikiRCSNodeContent loadRCSNodeContent(XWikiRCSNodeId id, boolean transaction,
        XWikiContext context) throws XWikiException
    {
        // not needed
        return null;
    }
    
    public void deleteArchive(XWikiDocument doc, boolean transaction, XWikiContext context)
        throws XWikiException
    {
        // not needed
    }
}
