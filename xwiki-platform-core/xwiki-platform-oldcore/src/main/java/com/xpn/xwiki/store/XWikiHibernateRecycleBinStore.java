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

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDeletedDocumentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.internal.store.hibernate.XWikiHibernateDeletedDocumentContent;

/**
 * Realization of {@link XWikiRecycleBinStoreInterface} for Hibernate store.
 *
 * @version $Id$
 */
@Component
@Named(XWikiHibernateBaseStore.HINT)
@Singleton
public class XWikiHibernateRecycleBinStore extends XWikiHibernateBaseStore implements XWikiRecycleBinStoreInterface
{
    /**
     * {@link HibernateCallback} used to retrieve from the recycle bin store the deleted versions of a document.
     */
    private static class DeletedDocumentsHibernateCallback implements HibernateCallback<XWikiDeletedDocument[]>
    {
        /**
         * The document whose versions are retrieved from the recycle bin store.
         */
        private XWikiDocument document;

        /**
         * Creates a new call-back for the given document.
         *
         * @param document the document whose deleted versions you want to retrieve from the recycle bin store
         */
        DeletedDocumentsHibernateCallback(XWikiDocument document)
        {
            this.document = document;
        }

        @Override
        public XWikiDeletedDocument[] doInHibernate(Session session) throws HibernateException, XWikiException
        {
            Criteria c = session.createCriteria(XWikiDeletedDocument.class);
            c.add(Restrictions.eq(FULL_NAME_FIELD, this.document.getFullName()));

            // Note: We need to support databases who treats empty strings as NULL like Oracle. For those checking
            // for equality when the string is empty is not going to work and thus we need to handle the special
            // empty case separately.
            String language = this.document.getLanguage();
            if (StringUtils.isEmpty(language)) {
                c.add(Restrictions.or(Restrictions.eq(LANGUAGE_PROPERTY_NAME, ""),
                    Restrictions.isNull(LANGUAGE_PROPERTY_NAME)));
            } else {
                c.add(Restrictions.eq(LANGUAGE_PROPERTY_NAME, language));
            }

            c.addOrder(Order.desc("date"));
            @SuppressWarnings("unchecked")
            List<XWikiDeletedDocument> deletedVersions = c.list();
            XWikiDeletedDocument[] result = new XWikiDeletedDocument[deletedVersions.size()];
            return deletedVersions.toArray(result);
        }
    }

    /**
     * {@link HibernateCallback} used to retrieve from the recycle bin store the deleted document versions from a given
     * batch.
     */
    private static class DeletedDocumentsBatchHibernateCallback implements HibernateCallback<XWikiDeletedDocument[]>
    {
        private String batchId;

        /**
         * Creates a new call-back for the given batch.
         *
         * @param batchId the ID of the batch of deleted documents you want to retrieve from the recycle bin store
         */
        DeletedDocumentsBatchHibernateCallback(String batchId)
        {
            this.batchId = batchId;
        }

        @Override
        public XWikiDeletedDocument[] doInHibernate(Session session) throws HibernateException, XWikiException
        {
            Criteria c = session.createCriteria(XWikiDeletedDocument.class);
            c.add(Restrictions.eq("batchId", batchId));

            c.addOrder(Order.asc(FULL_NAME_FIELD));
            @SuppressWarnings("unchecked")
            List<XWikiDeletedDocument> deletedVersions = c.list();
            XWikiDeletedDocument[] result = new XWikiDeletedDocument[deletedVersions.size()];
            return deletedVersions.toArray(result);
        }
    }

    private static final String FULL_NAME_FIELD = "fullName";

    /**
     * Name of the language property in the Hibernate mapping.
     */
    private static final String LANGUAGE_PROPERTY_NAME = "language";

    @Inject
    private StoreConfiguration storeConfiguration;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /**
     * @param context used for environment
     * @deprecated 1.6M1. Use ComponentManager#getInstance(XWikiRecycleBinStoreInterface.class) instead.
     */
    @Deprecated
    public XWikiHibernateRecycleBinStore(XWikiContext context)
    {
        super(context.getWiki(), context);
    }

    /**
     * Empty constructor needed for component manager.
     */
    public XWikiHibernateRecycleBinStore()
    {
    }

    private XWikiRecycleBinContentStoreInterface getDefaultXWikiRecycleBinContentStore() throws XWikiException
    {
        try {
            return this.storeConfiguration.getXWikiRecycleBinContentStore();
        } catch (ComponentLookupException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to lookup recycle bin content store", e);
        }
    }

    private XWikiRecycleBinContentStoreInterface getXWikiRecycleBinContentStore(String storeType)
    {
        if (storeType != null && !storeType.equals(HINT)) {
            try {
                return this.componentManager.getInstance(XWikiRecycleBinContentStoreInterface.class, storeType);
            } catch (ComponentLookupException e) {
                this.logger.warn("Can't find recycle bin content store for type [{}]", storeType, e);
            }
        }

        return null;
    }

    private XWikiDeletedDocument resolveDeletedDocumentContent(XWikiDeletedDocument deletedDocument,
        boolean bTransaction) throws XWikiException
    {
        XWikiRecycleBinContentStoreInterface contentStore =
            getXWikiRecycleBinContentStore(deletedDocument.getXmlStore());

        if (contentStore != null) {
            XWikiDeletedDocumentContent content =
                contentStore.get(deletedDocument.getDocumentReference(), deletedDocument.getId(), bTransaction);

            try {
                FieldUtils.writeDeclaredField(deletedDocument, "content", content, true);
            } catch (IllegalAccessException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Failed to set deleted document content", e);
            }
        }

        return deletedDocument;
    }

    private XWikiDeletedDocument createXWikiDeletedDocument(XWikiDocument doc, String deleter, Date date,
        XWikiRecycleBinContentStoreInterface contentStore, String batchId) throws XWikiException
    {
        XWikiDeletedDocument trashdoc;

        String storeType = null;
        XWikiDeletedDocumentContent deletedDocumentContent = null;

        if (contentStore != null) {
            storeType = contentStore.getHint();
        } else {
            deletedDocumentContent = new XWikiHibernateDeletedDocumentContent(doc);
        }

        trashdoc = new XWikiDeletedDocument(doc.getFullName(), doc.getLocale(), storeType, deleter, date,
            deletedDocumentContent, batchId);

        return trashdoc;
    }

    private void deleteDeletedDocumentContent(XWikiDeletedDocument deletedDocument, boolean bTransaction)
        throws XWikiException
    {
        XWikiRecycleBinContentStoreInterface contentStore =
            getXWikiRecycleBinContentStore(deletedDocument.getXmlStore());

        if (contentStore != null) {
            contentStore.delete(deletedDocument.getDocumentReference(), deletedDocument.getId(), bTransaction);
        }
    }

    @Override
    public void saveToRecycleBin(XWikiDocument doc, String deleter, Date date, XWikiContext inputxcontext,
        boolean bTransaction) throws XWikiException
    {
        saveToRecycleBin(doc, deleter, date, null, inputxcontext, bTransaction);
    }

    @Override
    public void saveToRecycleBin(XWikiDocument doc, String deleter, Date date, String batchId,
        XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            executeWrite(context, new HibernateCallback<Void>()
            {
                @Override
                public Void doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    XWikiRecycleBinContentStoreInterface contentStore = getDefaultXWikiRecycleBinContentStore();

                    XWikiDeletedDocument trashdoc =
                        createXWikiDeletedDocument(doc, deleter, date, contentStore, batchId);

                    // Hibernate store.
                    long index = ((Number) session.save(trashdoc)).longValue();

                    // External store
                    if (contentStore != null) {
                        contentStore.save(doc, index, bTransaction);
                    }

                    return null;
                }
            });
        } finally {
            restoreExecutionXContext();
        }
    }

    @Override
    public XWikiDocument restoreFromRecycleBin(final XWikiDocument doc, final long index,
        final XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        return restoreFromRecycleBin(index, inputxcontext, bTransaction);
    }

    @Override
    public XWikiDocument restoreFromRecycleBin(long index, XWikiContext inputxcontext, boolean bTransaction)
        throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            XWikiDeletedDocument deletedDocument = getDeletedDocument(index, context, bTransaction);
            return deletedDocument.restoreDocument(context);
        } finally {
            restoreExecutionXContext();
        }
    }

    @Override
    public XWikiDeletedDocument getDeletedDocument(XWikiDocument doc, final long index, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        return getDeletedDocument(index, context, bTransaction);
    }

    @Override
    public XWikiDeletedDocument getDeletedDocument(long index, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        return getDeletedDocument(index, context, true, bTransaction);
    }

    private XWikiDeletedDocument getDeletedDocument(final long index, XWikiContext context, boolean resolve,
        boolean bTransaction) throws XWikiException
    {
        return executeRead(context, new HibernateCallback<XWikiDeletedDocument>()
        {
            @Override
            public XWikiDeletedDocument doInHibernate(Session session) throws HibernateException, XWikiException
            {
                XWikiDeletedDocument deletedDocument =
                    (XWikiDeletedDocument) session.get(XWikiDeletedDocument.class, Long.valueOf(index));

                if (deletedDocument != null && resolve) {
                    deletedDocument = resolveDeletedDocumentContent(deletedDocument, false);
                }

                return deletedDocument;
            }
        });
    }

    @Override
    public XWikiDeletedDocument[] getAllDeletedDocuments(XWikiDocument doc, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        XWikiDeletedDocument[] deletedDocuments = executeRead(context, new DeletedDocumentsHibernateCallback(doc));

        // Resolve deleted document content if needed
        for (int i = 0; i < deletedDocuments.length; ++i) {
            deletedDocuments[i] = resolveDeletedDocumentContent(deletedDocuments[i], bTransaction);
        }

        return deletedDocuments;
    }

    @Override
    public XWikiDeletedDocument[] getAllDeletedDocuments(String batchId, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        XWikiDeletedDocument[] deletedDocuments = getAllDeletedDocuments(batchId, true, context, bTransaction);

        return deletedDocuments;
    }

    @Override
    public XWikiDeletedDocument[] getAllDeletedDocuments(String batchId, boolean withContent, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        XWikiDeletedDocument[] deletedDocuments =
            executeRead(context, new DeletedDocumentsBatchHibernateCallback(batchId));

        // Resolve deleted document content if needed
        if (withContent) {
            for (int i = 0; i < deletedDocuments.length; ++i) {
                XWikiDeletedDocument deletedDocument = deletedDocuments[i];
                deletedDocuments[i] = resolveDeletedDocumentContent(deletedDocument, bTransaction);
            }
        }

        return deletedDocuments;
    }

    @Override
    public void deleteFromRecycleBin(XWikiDocument doc, final long index, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        deleteFromRecycleBin(index, context, bTransaction);
    }

    @Override
    public void deleteFromRecycleBin(final long index, XWikiContext context, boolean bTransaction) throws XWikiException
    {
        executeWrite(context, new HibernateCallback<Void>()
        {
            @Override
            public Void doInHibernate(Session session) throws HibernateException, XWikiException
            {
                XWikiDeletedDocument deletedDocument = getDeletedDocument(index, context, false, bTransaction);

                // Delete metadata
                session.delete(deletedDocument);

                // Delete content
                deleteDeletedDocumentContent(deletedDocument, bTransaction);

                return null;
            }
        });
    }
}
