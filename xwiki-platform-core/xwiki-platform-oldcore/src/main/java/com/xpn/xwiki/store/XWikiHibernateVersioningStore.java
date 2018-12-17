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
package com.xpn.xwiki.store;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

/**
 * Realization of {@link XWikiVersioningStoreInterface} for Hibernate-based storage.
 *
 * @version $Id$
 */
@Component
@Named(XWikiHibernateBaseStore.HINT)
@Singleton
public class XWikiHibernateVersioningStore extends XWikiHibernateBaseStore implements XWikiVersioningStoreInterface
{
    /** Logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiHibernateVersioningStore.class);

    /**
     * This allows to initialize our storage engine. The hibernate config file path is taken from xwiki.cfg or directly
     * in the WEB-INF directory.
     *
     * @param xwiki The xwiki object
     * @param context The current context
     * @deprecated 1.6M1. use ComponentManager.lookup(XWikiVersioningStoreInterface.class) instead.
     */
    @Deprecated
    public XWikiHibernateVersioningStore(XWiki xwiki, XWikiContext context)
    {
        super(xwiki, context);
    }

    /**
     * Initialize the storage engine with a specific path This is used for tests.
     *
     * @param hibpath path to hibernate.hbm.xml file
     * @deprecated 1.6M1. use ComponentManager.lookup(XWikiVersioningStoreInterface.class) instead.
     */
    @Deprecated
    public XWikiHibernateVersioningStore(String hibpath)
    {
        super(hibpath);
    }

    /**
     * @see #XWikiHibernateVersioningStore(XWiki, XWikiContext)
     * @param context The current context
     * @deprecated 1.6M1. use ComponentManager.lookup(XWikiVersioningStoreInterface.class) instead.
     */
    @Deprecated
    public XWikiHibernateVersioningStore(XWikiContext context)
    {
        this(context.getWiki(), context);
    }

    /**
     * Empty constructor needed for component manager.
     */
    public XWikiHibernateVersioningStore()
    {
    }

    @Override
    public Version[] getXWikiDocVersions(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        try {
            XWikiDocumentArchive archive = getXWikiDocumentArchive(doc, context);
            if (archive == null) {
                return new Version[0];
            }
            Collection<XWikiRCSNodeInfo> nodes = archive.getNodes();
            Version[] versions = new Version[nodes.size()];
            Iterator<XWikiRCSNodeInfo> it = nodes.iterator();
            for (int i = 0; i < versions.length; i++) {
                XWikiRCSNodeInfo node = it.next();
                versions[versions.length - 1 - i] = node.getId().getVersion();
            }
            return versions;
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_REVISIONS,
                "Exception while reading document {0} revisions", e, args);
        }
    }

    @Override
    public XWikiDocumentArchive getXWikiDocumentArchive(XWikiDocument doc, XWikiContext inputxcontext)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext(inputxcontext);

        XWikiDocumentArchive archiveDoc = doc.getDocumentArchive();
        if (archiveDoc != null) {
            return archiveDoc;
        }

        String db = context.getWikiId();
        try {
            if (doc.getDatabase() != null) {
                context.setWikiId(doc.getDatabase());
            }
            archiveDoc = new XWikiDocumentArchive(doc.getId());
            loadXWikiDocArchive(archiveDoc, true, context);
            doc.setDocumentArchive(archiveDoc);
        } finally {
            context.setWikiId(db);
        }
        return archiveDoc;
    }

    @Override
    public void loadXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context)
        throws XWikiException
    {
        try {
            List<XWikiRCSNodeInfo> nodes = loadAllRCSNodeInfo(context, archivedoc.getId(), bTransaction);
            archivedoc.setNodes(nodes);
        } catch (Exception e) {
            Object[] args = { Long.valueOf(archivedoc.getId()) };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT, "Exception while loading archive {0}", e,
                args);
        }
    }

    @Override
    public void saveXWikiDocArchive(final XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context)
        throws XWikiException
    {
        executeWrite(context, bTransaction, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException
            {
                for (XWikiRCSNodeInfo ni : archivedoc.getDeletedNodeInfo()) {
                    session.delete(ni);
                }
                archivedoc.getDeletedNodeInfo().clear();
                for (XWikiRCSNodeInfo ni : archivedoc.getUpdatedNodeInfos()) {
                    session.saveOrUpdate(ni);
                }
                archivedoc.getUpdatedNodeInfos().clear();
                for (XWikiRCSNodeContent nc : archivedoc.getUpdatedNodeContents()) {
                    session.update(nc);
                }
                archivedoc.getUpdatedNodeContents().clear();
                return null;
            }
        });
    }

    @Override
    public XWikiDocument loadXWikiDoc(XWikiDocument basedoc, String sversion, XWikiContext inputxcontext)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext(inputxcontext);

        XWikiDocumentArchive archive = getXWikiDocumentArchive(basedoc, context);
        Version version = new Version(sversion);

        XWikiDocument doc = archive.loadDocument(version, context);
        if (doc == null) {
            Object[] args = { basedoc.getDocumentReferenceWithLocale(), version.toString() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_UNEXISTANT_VERSION,
                "Version {1} does not exist while reading document {0}", null, args);
        }

        // Make sure the document has the same name
        // as the new document (in case there was a name change
        // FIXME: is this really needed ?
        doc.setDocumentReference(basedoc.getDocumentReference());

        doc.setStore(basedoc.getStore());

        // Make sure the attachment of the revision document have the right store
        for (XWikiAttachment revisionAttachment : doc.getAttachmentList()) {
            XWikiAttachment attachment = basedoc.getAttachment(revisionAttachment.getFilename());

            if (attachment != null) {
                revisionAttachment.setContentStore(attachment.getContentStore());
                revisionAttachment.setArchiveStore(attachment.getArchiveStore());
            }
        }

        return doc;
    }

    @Override
    public void resetRCSArchive(final XWikiDocument doc, boolean bTransaction, final XWikiContext inputxcontext)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext(inputxcontext);

        executeWrite(context, true, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                XWikiDocumentArchive archive = getXWikiDocumentArchive(doc, context);
                archive.resetArchive();
                archive.getDeletedNodeInfo().clear();
                doc.setMinorEdit(false);
                deleteArchive(doc, false, context);
                updateXWikiDocArchive(doc, false, context);
                return null;
            }
        });
    }

    @Override
    public void updateXWikiDocArchive(XWikiDocument doc, boolean bTransaction, XWikiContext inputxcontext)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext(inputxcontext);

        try {
            XWikiDocumentArchive archiveDoc = getXWikiDocumentArchive(doc, context);
            archiveDoc.updateArchive(doc, doc.getAuthor(), doc.getDate(), doc.getComment(), doc.getRCSVersion(),
                context);
            doc.setRCSVersion(archiveDoc.getLatestVersion());
            saveXWikiDocArchive(archiveDoc, bTransaction, context);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT, "Exception while updating archive {0}", e,
                args);
        }
    }

    /**
     * @param context the XWiki context
     * @param id {@link XWikiRCSNodeContent#getId()}
     * @param bTransaction should store to use old transaction(false) or create new (true)
     * @return loaded rcs node content
     * @throws XWikiException if any error
     */
    protected List<XWikiRCSNodeInfo> loadAllRCSNodeInfo(XWikiContext context, final long id, boolean bTransaction)
        throws XWikiException
    {
        return executeRead(context, bTransaction, new HibernateCallback<List<XWikiRCSNodeInfo>>()
        {
            @SuppressWarnings("unchecked")
            @Override
            public List<XWikiRCSNodeInfo> doInHibernate(Session session) throws HibernateException
            {
                try {
                    return session.createCriteria(XWikiRCSNodeInfo.class)
                        .add(Restrictions.eq("id.docId", Long.valueOf(id))).add(Restrictions.isNotNull("diff")).list();
                } catch (IllegalArgumentException ex) {
                    // This happens when the database has wrong values...
                    LOGGER.warn("Invalid history for document " + id);
                    return Collections.emptyList();
                }
            }
        });
    }

    @Override
    public XWikiRCSNodeContent loadRCSNodeContent(final XWikiRCSNodeId id, boolean bTransaction, XWikiContext context)
        throws XWikiException
    {
        return executeRead(context, bTransaction, new HibernateCallback<XWikiRCSNodeContent>()
        {
            @Override
            public XWikiRCSNodeContent doInHibernate(Session session) throws HibernateException
            {
                XWikiRCSNodeContent content = new XWikiRCSNodeContent(id);
                session.load(content, content.getId());
                return content;
            }
        });
    }

    @Override
    public void deleteArchive(final XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException
    {
        executeWrite(context, bTransaction, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                session.createQuery("delete from " + XWikiRCSNodeInfo.class.getName() + " where id.docId=?")
                    .setLong(0, doc.getId()).executeUpdate();
                return null;
            }
        });
    }
}
