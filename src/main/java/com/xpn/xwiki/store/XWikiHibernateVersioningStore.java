/*
 * Copyright 2005-2007, XpertNet SARL, and individual contributors.
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
package com.xpn.xwiki.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.util.ToString;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

public class XWikiHibernateVersioningStore extends XWikiHibernateBaseStore implements XWikiVersioningStoreInterface {

    private static final Log log = LogFactory.getLog(XWikiHibernateVersioningStore.class);

    /**
     * {@inheritDoc}
     */
    public XWikiHibernateVersioningStore(XWiki xwiki, XWikiContext context) {
        super(xwiki, context);
    }

    /**
     * {@inheritDoc}
     */
    public XWikiHibernateVersioningStore(String hibpath) {
        super(hibpath);
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
                versions[versions.length-1-i] = node.getId().getVersion();
            }
            return versions;
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_REVISIONS,
                    "Exception while reading document {0} revisions", e, args);
        }
    }

    public XWikiDocumentArchive getXWikiDocumentArchive(XWikiDocument doc, XWikiContext context) throws XWikiException {
        XWikiDocumentArchive archiveDoc = doc.getDocumentArchive();
        if (archiveDoc != null)
            return archiveDoc;
        String key = ((doc.getDatabase()==null)?"xwiki":doc.getDatabase()) + ":" + doc.getFullName();
        synchronized (key) {
            archiveDoc = (XWikiDocumentArchive) context.getDocumentArchive(key);
            if (archiveDoc==null) {
                String db = context.getDatabase();
                try {
                    if (doc.getDatabase()!=null)
                        context.setDatabase(doc.getDatabase());
                    archiveDoc = new XWikiDocumentArchive(doc.getId());
                    loadXWikiDocArchive(archiveDoc, true, context);
                    doc.setDocumentArchive(archiveDoc);
                } finally {
                    context.setDatabase(db);
                }
                // This will also make sure that the Archive has a strong reference
                // and will not be discarded as long as the context exists.
                context.addDocumentArchive(key, archiveDoc);
            }
            return archiveDoc;
        }
    }

    public void loadXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context) throws XWikiException {
        try {
            List nodes = loadAllRCSNodeInfo(context, archivedoc.getId(), bTransaction);
            archivedoc.setNodes(nodes);
        } catch (Exception e) {
            Object[] args = { new Long(archivedoc.getId()) };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
                    "Exception while loading archive {0}", e, args);
        }
    }

    public void saveXWikiDocArchive(final XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context) throws XWikiException {
        executeWrite(context, bTransaction, new HibernateCallBack(){
            public Object doInHibernate(Session session) throws Exception
            {
                for (Iterator it = archivedoc.getDeletedNodeInfo().iterator(); it.hasNext(); ) {
                    XWikiRCSNodeInfo nodeInfo = (XWikiRCSNodeInfo) it.next();
                    session.delete(nodeInfo);
                    it.remove();
                }
                for (Iterator it = archivedoc.getUpdatedNodeInfos().iterator(); it.hasNext(); ) {
                    XWikiRCSNodeInfo nodeInfo = (XWikiRCSNodeInfo) it.next();
                    session.saveOrUpdate(nodeInfo);
                    it.remove();
                }
                for (Iterator it = archivedoc.getUpdatedNodeContents().iterator(); it.hasNext(); ) {
                    XWikiRCSNodeContent nodeContent = (XWikiRCSNodeContent) it.next();
                    session.update(nodeContent);
                    it.remove();
                }
                return null;
            }
        });
    }
    
    public XWikiDocument loadXWikiDoc(XWikiDocument basedoc, String sversion, XWikiContext context) throws XWikiException {
        XWikiDocument doc = new XWikiDocument(basedoc.getSpace(), basedoc.getName());
        doc.setDatabase(basedoc.getDatabase());
        doc.setStore(basedoc.getStore());
        
        XWikiDocumentArchive archive = getXWikiDocumentArchive(basedoc, context);
        Version ver = new Version(sversion);
        XWikiRCSNodeInfo nodeInfo = archive.getNode(ver);
        if (nodeInfo==null) {
            Object[] args = { doc.getFullName(), sversion };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_UNEXISTANT_VERSION,
                "Version {1} does not exist while reading document {0}", null, args);
        }
        try {    
            Version nearestFullVersion = archive.getNearestFullVersion(ver);
            
            List lstContent = loadRCSNodeContents(context, archive, nearestFullVersion, ver, true);
            List origText = new ArrayList();
            for (Iterator it = lstContent.iterator(); it.hasNext();) {
                XWikiRCSNodeContent nodeContent = (XWikiRCSNodeContent) it.next();
                nodeContent.getPatch().patch(origText);
            }
            
            String content = ToString.arrayToString(origText.toArray());
            doc.fromXML(content);
            // Make sure the document has the same name
            // as the new document (in case there was a name change
            doc.setName(basedoc.getName());
            doc.setSpace(basedoc.getSpace());
            
            doc.setVersion(sversion);
            doc.setComment(nodeInfo.getComment());
            doc.setDate(nodeInfo.getDate());
            doc.setAuthor(nodeInfo.getAuthor());
            doc.setMinorEdit( nodeInfo.isMinorEdit() );
            return doc;
        } catch (Exception e) {
            Object[] args = { basedoc.getFullName(), sversion };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_VERSION,
                    "Exception while reading document {0} version {1}", e, args);
        }
    }

    public void resetRCSArchive(final XWikiDocument doc, boolean bTransaction, final XWikiContext context) throws XWikiException {
        executeWrite(context, true, new HibernateCallBack() {
            public Object doInHibernate(Session session) throws Exception
            {
                XWikiDocumentArchive archive = getXWikiDocumentArchive(doc, context);
                archive.resetArchive();
                archive.getDeletedNodeInfo().clear();
                doc.setMinorEdit(false);
                
                session.createQuery("delete from "+XWikiRCSNodeInfo.class.getName()+" where id.docId=?")
                    .setLong(0, doc.getId())
                    .executeUpdate();
                updateXWikiDocArchive(doc, false, context);
                return null;
            }
        });
    }

    public void updateXWikiDocArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException {
        try {
            XWikiDocumentArchive archiveDoc = getXWikiDocumentArchive(doc, context);
            archiveDoc.updateArchive(doc.getAuthor(), doc.getDate(), doc.getComment(), doc.isMinorEdit(), doc, context);
            doc.setVersion( archiveDoc.getLatestVersion().toString() );
            saveXWikiDocArchive(archiveDoc, bTransaction, context);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT,
                    "Exception while updating archive {0}", e, args);
        }
    }
    
    protected List loadAllRCSNodeInfo(XWikiContext context, final long id, boolean bTransaction) throws XWikiException {
        return (List) executeRead(context, bTransaction, new HibernateCallBack() {
            public Object doInHibernate(Session session) throws Exception
            {
                return session.createCriteria(XWikiRCSNodeInfo.class)
                    .add(Restrictions.eq("id.docId", Long.valueOf(id)))
                    .list();
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public XWikiRCSNodeContent loadRCSNodeContent(XWikiContext context, final XWikiRCSNodeId id, boolean bTransaction) throws XWikiException
    {
        return (XWikiRCSNodeContent) executeRead(context, bTransaction, new HibernateCallBack() {
            public Object doInHibernate(Session session) throws Exception
            {
                XWikiRCSNodeContent content = new XWikiRCSNodeContent(id);
                session.load( content, content.getId() );
                return content;
            }
        });
    }
    
    protected List loadRCSNodeContents(final XWikiContext context, final XWikiDocumentArchive archive, final Version vfrom, final Version vto, boolean bTransaction) throws XWikiException
    {
        List result = new ArrayList();
        for (Iterator it=archive.getNodes(vfrom, vto).iterator(); it.hasNext(); ) {
            XWikiRCSNodeInfo nodeInfo = (XWikiRCSNodeInfo) it.next();
            XWikiRCSNodeContent nodeContent = nodeInfo.getContent(context);
            result.add(nodeContent);
        }
        return result;
    }
}
