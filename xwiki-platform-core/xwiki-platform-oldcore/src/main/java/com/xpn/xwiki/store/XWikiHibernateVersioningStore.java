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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.criteria.impl.RevisionCriteriaFactory;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.store.hibernate.query.VersioningStoreQueryFactory;
import com.xpn.xwiki.web.Utils;

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
        return getXWikiDocVersions(doc, new RevisionCriteriaFactory().createRevisionCriteria(true), context)
            .toArray(new Version[0]);
    }

    @Override
    public Collection<Version> getXWikiDocVersions(XWikiDocument doc, RevisionCriteria criteria, XWikiContext context)
        throws XWikiException
    {
        try {
            XWikiDocumentArchive archive = getXWikiDocumentArchive(doc, criteria, context);
            if (archive == null) {
                return List.of();
            }
            Collection<XWikiRCSNodeInfo> nodes = archive.getNodes();
            Deque<Version> versions = new LinkedList<>();
            for (XWikiRCSNodeInfo node : nodes) {
                versions.addFirst(node.getId().getVersion());
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
        return getXWikiDocumentArchive(doc, new RevisionCriteriaFactory().createRevisionCriteria(true), inputxcontext);
    }

    private XWikiDocumentArchive getXWikiDocumentArchive(XWikiDocument doc, RevisionCriteria criteria,
        XWikiContext inputxcontext) throws XWikiException
    {
        XWikiDocumentArchive archiveDoc = doc.getDocumentArchive();
        if (archiveDoc == null) {
            archiveDoc = getXWikiDocumentArchiveFromDatabase(doc, criteria, inputxcontext);
        // if there's an archive doc and the criteria is to not return everything then we filter, else we just return
        } else if (!criteria.isAllInclusive()) {
            archiveDoc = filterArchiveFromCriteria(doc, archiveDoc, criteria);
        }
        return archiveDoc;
    }

    private XWikiDocumentArchive filterArchiveFromCriteria(XWikiDocument doc, XWikiDocumentArchive archiveDoc,
        RevisionCriteria criteria)
    {
        XWikiDocumentArchive result =
            new XWikiDocumentArchive(doc.getDocumentReference().getWikiReference(), doc.getId());
        Collection<XWikiRCSNodeInfo> nodes = archiveDoc.getNodes();
        XWikiRCSNodeInfo nodeinfo = null;
        List<XWikiRCSNodeInfo> results = new ArrayList<>();

        // Iterate over all versions and get the ones matching the criteria
        for (XWikiRCSNodeInfo nextNodeinfo : nodes) {
            if (nodeinfo != null && (criteria.getIncludeMinorVersions() || !nextNodeinfo.isMinorEdit())) {
                if (isAuthorMatching(criteria, nodeinfo) && isDateMatching(criteria, nodeinfo)) {
                    results.add(nodeinfo);
                }
            }
            nodeinfo = nextNodeinfo;
        }
        if (nodeinfo != null && isAuthorMatching(criteria, nodeinfo) && isDateMatching(criteria, nodeinfo)) {
            results.add(nodeinfo);
        }

        // getRange().subList only applies on String: so we apply it on the Version (e.g.: 1.1,2.1,etc) and
        // we ensure to return them in the ascending order: nodes are returned from the archive in descending order
        List<String> versionList = criteria.getRange().subList(
            results
                .stream()
                .map(XWikiRCSNodeInfo::getVersion)
                .map(Version::toString)
                .collect(
                    Collectors.collectingAndThen(
                        Collectors.toList(),
                        l -> {
                            Collections.reverse(l); return l;
                        }
                    )
                )
        );
        // We retrieve the actual nodes from the versions we kept just before
        result.setNodes(results.stream()
            .filter(node -> versionList.contains(node.getVersion().toString())).toList());
        return result;
    }

    private static boolean isAuthorMatching(RevisionCriteria criteria, XWikiRCSNodeInfo nodeinfo)
    {
        return criteria.getAuthor().isEmpty() || criteria.getAuthor().equals(nodeinfo.getAuthor());
    }

    private static boolean isDateMatching(RevisionCriteria criteria, XWikiRCSNodeInfo nodeinfo)
    {
        Date versionDate = nodeinfo.getDate();
        return (versionDate.after(criteria.getMinDate()) && versionDate.before(criteria.getMaxDate()));
    }

    private XWikiDocumentArchive getXWikiDocumentArchiveFromDatabase(XWikiDocument doc, RevisionCriteria criteria,
        XWikiContext inputxcontext) throws XWikiException
    {
        XWikiDocumentArchive archiveDoc = null;
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        String db = context.getWikiId();
        try {
            if (doc.getDatabase() != null) {
                context.setWikiId(doc.getDatabase());
            }
            archiveDoc = new XWikiDocumentArchive(doc.getDocumentReference().getWikiReference(), doc.getId());
            loadXWikiDocArchive(archiveDoc, criteria, context);
            // We only store the archive if it is a complete one.
            if (criteria.isAllInclusive()) {
                doc.setDocumentArchive(archiveDoc);
            }
        } finally {
            context.setWikiId(db);

            restoreExecutionXContext();
        }

        return archiveDoc;
    }

    @Override
    public void loadXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context)
        throws XWikiException
    {
        loadXWikiDocArchive(archivedoc, new RevisionCriteriaFactory().createRevisionCriteria(true), context);
    }

    private void loadXWikiDocArchive(XWikiDocumentArchive archivedoc, RevisionCriteria criteria, XWikiContext context)
        throws XWikiException
    {
        try {
            List<XWikiRCSNodeInfo> nodes = loadRCSNodeInfo(context, archivedoc.getId(), criteria);
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
        executeWrite(context, session -> {
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
        });
    }

    @Override
    public XWikiDocument loadXWikiDoc(XWikiDocument basedoc, String sversion, XWikiContext inputxcontext)
        throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
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
        } finally {
            restoreExecutionXContext();
        }
    }

    @Override
    public void resetRCSArchive(final XWikiDocument doc, boolean bTransaction, final XWikiContext inputxcontext)
        throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            executeWrite(context, session -> {
                XWikiDocumentArchive archive = getXWikiDocumentArchive(doc, context);
                archive.resetArchive();
                archive.getDeletedNodeInfo().clear();
                doc.setMinorEdit(false);
                deleteArchive(doc, false, context);
                updateXWikiDocArchive(doc, false, context);
                return null;
            });
        } finally {
            restoreExecutionXContext();
        }
    }

    @Override
    public void updateXWikiDocArchive(XWikiDocument doc, boolean bTransaction, XWikiContext inputxcontext)
        throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            XWikiDocumentArchive archiveDoc = getXWikiDocumentArchive(doc, context);
            UserReferenceSerializer<String> userReferenceSerializer = Utils.getComponent(
                new DefaultParameterizedType(null, UserReferenceSerializer.class, String.class));
            String author = userReferenceSerializer.serialize(doc.getAuthors().getOriginalMetadataAuthor());
            archiveDoc.updateArchive(doc, author, doc.getDate(), doc.getComment(), doc.getRCSVersion(),
                context);
            doc.setRCSVersion(archiveDoc.getLatestVersion());
            saveXWikiDocArchive(archiveDoc, bTransaction, context);
        } catch (Exception e) {
            Object[] args = { doc.getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT, "Exception while updating archive {0}", e,
                args);
        } finally {
            restoreExecutionXContext();
        }
    }

    /**
     * Loads all the RCS nodes present in the archive of a given document.
     *
     * @param context the XWiki context
     * @param id {@link XWikiRCSNodeContent#getId()}
     * @param bTransaction should store to use old transaction(false) or create new (true)
     * @return loaded RCS nodes content
     * @throws XWikiException if any error
     */
    protected List<XWikiRCSNodeInfo> loadAllRCSNodeInfo(XWikiContext context, final long id, boolean bTransaction)
        throws XWikiException
    {
        return loadRCSNodeInfo(context, id, new RevisionCriteriaFactory().createRevisionCriteria(true));
    }

    /**
     * Loads a part of the RCS nodes present in the archive of a given document, based on criteria.
     *
     * @param context the XWiki context
     * @param id {@link XWikiRCSNodeContent#getId()}
     * @param criteria the criteria matching the nodes to load
     * @return loaded RCS nodes content
     * @throws XWikiException if any error
     */
    private List<XWikiRCSNodeInfo> loadRCSNodeInfo(XWikiContext context, final long id, RevisionCriteria criteria)
        throws XWikiException
    {
        return executeRead(context, session -> {
            try {
                List<XWikiRCSNodeInfo> nodes =
                    VersioningStoreQueryFactory.getRCSNodeInfoQuery(session, id, criteria).getResultList();

                // Remember the wiki where the nodes are from
                nodes.forEach(n -> n.getId().setWikiReference(context.getWikiReference()));

                return nodes;
            } catch (IllegalArgumentException e) {
                throw new XWikiException(
                    String.format("Encountered invalid history when fetching archive for document [%s]", id), e);
            }
        });
    }

    @Override
    public XWikiRCSNodeContent loadRCSNodeContent(final XWikiRCSNodeId id, boolean bTransaction, XWikiContext context)
        throws XWikiException
    {
        WikiReference currentWiki = context.getWikiReference();

        try {
            if (id.getWikiReference() != null) {
                context.setWikiReference(id.getWikiReference());
            }

            return executeRead(context, session -> {
                XWikiRCSNodeContent content = new XWikiRCSNodeContent(id);
                session.load(content, content.getId());

                return content;
            });
        } finally {
            context.setWikiReference(currentWiki);
        }
    }

    @Override
    public void deleteArchive(final XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException
    {
        executeWrite(context, session -> {
            VersioningStoreQueryFactory.getDeleteArchiveQuery(session, doc.getId()).executeUpdate();
            return null;
        });
    }

    @Override
    public long getXWikiDocVersionsCount(XWikiDocument doc, RevisionCriteria criteria, XWikiContext context)
        throws XWikiException
    {
        return getRCSNodeInfoCount(context, doc.getId(), criteria);
    }

    /**
     * Counts the number of RCS nodes present in the archive of a given document, based on criteria.
     *
     * @param context the XWiki context
     * @param id {@link XWikiRCSNodeContent#getId()}
     * @param criteria the criteria matching the nodes to count
     * @return the number of matching RCS nodes
     * @throws XWikiException if any error
     */
    private long getRCSNodeInfoCount(XWikiContext context, final long id, RevisionCriteria criteria)
        throws XWikiException
    {
        return executeRead(context, session -> {
            try {
                return VersioningStoreQueryFactory.getRCSNodeInfoCountQuery(session, id, criteria).getSingleResult();
            } catch (IllegalArgumentException e) {
                throw new XWikiException(
                    String.format("Encountered invalid history when computing archive size for document [%s]", id), e);
            }
        });
    }
}
