package com.xpn.xwiki.store;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.Node;
import org.suigeneris.jrcs.rcs.Version;

/**
 * Created by IntelliJ IDEA.                                                   
 * User: ludovic
 * Date: 6 juin 2006
 * Time: 21:32:07
 * To change this template use File | Settings | File Templates.
 */
public class XWikiHibernateVersioningStore extends XWikiHibernateBaseStore implements XWikiVersioningStoreInterface {

    private static final Log log = LogFactory.getLog(XWikiHibernateVersioningStore.class);

    /**
     * THis allows to initialize our storage engine.
     * The hibernate config file path is taken from xwiki.cfg
     * or directly in the WEB-INF directory.
     * @param xwiki
     * @param context
     */
    public XWikiHibernateVersioningStore(XWiki xwiki, XWikiContext context) {
        super(xwiki, context);
    }

    /**
     * Initialize the storage engine with a specific path
     * This is used for tests.
     * @param hibpath
     */
    public XWikiHibernateVersioningStore(String hibpath) {
        super(hibpath);
    }

    public Version[] getXWikiDocVersions(XWikiDocument doc, XWikiContext context) throws XWikiException {
        try {
            Archive archive = getXWikiDocumentArchive(doc, context).getRCSArchive();
            if (archive==null)
                return new Version[0];

            Node[] nodes = archive.changeLog();
            Version[] versions = new Version[nodes.length];
            for (int i=0;i<nodes.length;i++) {
                versions[i] = nodes[i].getVersion();
            }
            return versions;
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_REVISIONS,
                    "Exception while reading document {0} revisions", e, args);
        }
    }

    /*
    public Archive getXWikiDocRCSArchive(XWikiDocument doc, XWikiContext context) throws XWikiException {
        try {
            XWikiDocumentArchive archivedoc = getXWikiDocumentArchive(doc, context);
            return archivedoc.getRCSArchive();
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_REVISIONS,
                    "Exception while reading document {0} revisions", e, args);
        }
    }
    */

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

    public void loadXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);

            try {
                session.load(archivedoc, new Long(archivedoc.getId()));
            }
            catch (ObjectNotFoundException e) {
                return;
            }
            if (bTransaction) {
                endTransaction(context, false, false);
            }
        } catch (Exception e) {
            Object[] args = { new Long(archivedoc.getId()) };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while loading archive {0}", e, args);
        } finally {
            try {
                if (bTransaction)
                    endTransaction(context, false, false);
            } catch (Exception e) {}
        }
    }

    public void saveXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);
            session.update(archivedoc);
            if (bTransaction) {
                endTransaction(context, true, false);
            }
        } catch (Exception e) {
            Object[] args = { new Long(archivedoc.getId()) };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while saving archive {0}", e, args);
        } finally {
            try {
                if (bTransaction)
                    endTransaction(context, false, false);
            } catch (Exception e) {}
        }
    }

    public XWikiDocument loadXWikiDoc(XWikiDocument basedoc,String version, XWikiContext context) throws XWikiException {
        XWikiDocument doc = new XWikiDocument(basedoc.getSpace(), basedoc.getName());
        doc.setDatabase(basedoc.getDatabase());
        MonitorPlugin monitor = Util.getMonitorPlugin(context);
        try {
            // Start monitoring timer
            if (monitor!=null)
                monitor.startTimer("hibernate");
            doc.setStore(basedoc.getStore());

            Archive archive = getXWikiDocumentArchive(doc, context).getRCSArchive();
            Version v = null;
            try {
                v = archive.getRevisionVersion(version);
            } catch (Exception e) {}

            if (v==null) {
                Object[] args = { doc.getFullName(), version.toString() };
                throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_UNEXISTANT_VERSION,
                        "Version {1} does not exist while reading document {0}", null,args);
            }

            if (!version.equals(v.toString())) {
                doc.setVersion(version);
                return doc;
            }
            Object[] text = (Object[]) archive.getRevision(version);
            StringBuffer content = new StringBuffer();
            for (int i=0;i<text.length;i++) {
                String line = text[i].toString();
                content.append(line);
                content.append("\n");
            }
            doc.fromXML(content.toString());
            // Make sure the document has the same name
            // as the new document (in case there was a name change
            doc.setName(basedoc.getName());
            doc.setSpace(basedoc.getSpace());
        } catch (Exception e) {
            if (e instanceof XWikiException)
             throw (XWikiException) e;
            Object[] args = { doc.getFullName(), version.toString() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_VERSION,
                    "Exception while reading document {0} version {1}", e, args);
        } finally {
            // End monitoring timer
            if (monitor!=null)
                monitor.endTimer("hibernate");
        }
        return doc;
    }

    public void resetRCSArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);

            XWikiDocumentArchive archivedoc = new XWikiDocumentArchive(doc.getId());
            archivedoc.resetArchive(doc.getFullName(), doc.toXML(context), doc.getVersion());
            saveXWikiDocArchive(archivedoc, bTransaction, context);
            if (bTransaction) {
                endTransaction(context, true, false);
            }
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while loading archive {0}", e, args);
        } finally {
            try {
                if (bTransaction)
                    endTransaction(context, false, false);
            } catch (Exception e) {}
        }
    }

    public void updateXWikiDocArchive(XWikiDocument doc, String text, boolean bTransaction, XWikiContext context) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);

            XWikiDocumentArchive archivedoc = getXWikiDocumentArchive(doc, context);
            archivedoc.updateArchive(doc.getFullName(), text);
            saveXWikiDocArchive(archivedoc, bTransaction, context);
            if (bTransaction) {
                endTransaction(context, true, false);
            }
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while loading archive {0}", e, args);
        } finally {
            try {
                if (bTransaction)
                    endTransaction(context, false, false);
            } catch (Exception e) {}
        }
    }
}

