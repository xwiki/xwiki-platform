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
package com.xpn.xwiki.store.hibernate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.AttachmentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.DeletedAttachmentContent;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.internal.store.hibernate.HibernateDeletedAttachmentContent;
import com.xpn.xwiki.store.AttachmentRecycleBinContentStore;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;

/**
 * Realization of {@link AttachmentRecycleBinStore} for Hibernate-based storage.
 *
 * @version $Id$
 * @since 1.4M1
 */
@Component
@Named(XWikiHibernateBaseStore.HINT)
@Singleton
public class HibernateAttachmentRecycleBinStore extends XWikiHibernateBaseStore implements AttachmentRecycleBinStore
{
    /** Constant string used to refer Document ID. */
    private static final String DOC_ID = "docId";

    /** Constant string used to refer date. */
    private static final String DATE = "date";

    @Inject
    private StoreConfiguration storeConfiguration;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /**
     * Constructor used by {@link com.xpn.xwiki.XWiki} during storage initialization.
     *
     * @param context The current context.
     * @deprecated 1.6M1. Use ComponentManager.lookup(AttachmentRecycleBinStore.class) instead.
     */
    @Deprecated
    public HibernateAttachmentRecycleBinStore(XWikiContext context)
    {
        super(context.getWiki(), context);
    }

    /**
     * Empty constructor needed for component manager.
     */
    public HibernateAttachmentRecycleBinStore()
    {
    }

    @Override
    public void saveToRecycleBin(XWikiAttachment attachment, String deleter, Date date, XWikiContext inputxcontext,
        boolean bTransaction) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            executeWrite(context, new HibernateCallback<Object>()
            {
                @Override
                public Object doInHibernate(Session session) throws XWikiException
                {
                    AttachmentRecycleBinContentStore contentStore = getDefaultAttachmentRecycleBinContentStore();

                    DeletedAttachment trashAttachment =
                        createDeletedAttachment(attachment, deleter, date, contentStore);

                    // Hibernate store.
                    long index = ((Number) session.save(trashAttachment)).longValue();

                    // External store
                    if (contentStore != null) {
                        contentStore.save(attachment, date, index, bTransaction);
                    }

                    return null;
                }
            });
        } finally {
            restoreExecutionXContext();
        }
    }

    @Override
    public XWikiAttachment restoreFromRecycleBin(final XWikiAttachment attachment, final long index,
        final XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            DeletedAttachment deletedAttachment = getDeletedAttachment(index, context, bTransaction);
            return deletedAttachment == null ? null : deletedAttachment.restoreAttachment();
        } finally {
            restoreExecutionXContext();
        }
    }

    @Override
    public DeletedAttachment getDeletedAttachment(final long index, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        return loadDeletedAttachment(index, context, true);
    }

    private DeletedAttachment loadDeletedAttachment(final long index, XWikiContext context, boolean resolve)
        throws XWikiException
    {
        return executeRead(context, session -> {
            DeletedAttachment deletedAttachment = session.get(DeletedAttachment.class, Long.valueOf(index));

            if (deletedAttachment != null && resolve) {
                deletedAttachment = resolveDeletedAttachmentContent(deletedAttachment, false, false);
            }

            return deletedAttachment;
        });
    }

    @Override
    public List<DeletedAttachment> getAllDeletedAttachments(final XWikiAttachment attachment, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        List<DeletedAttachment> deletedAttachments = executeRead(context, session -> {

            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<DeletedAttachment> query = builder.createQuery(DeletedAttachment.class);
            Root<DeletedAttachment> root = query.from(DeletedAttachment.class);

            query.select(root);

            if (attachment != null) {
                Predicate[] predicates;
                if (StringUtils.isNotBlank(attachment.getFilename())) {
                    predicates = new Predicate[2];
                    predicates[1] = builder.equal(root.get("filename"), attachment.getFilename());
                } else {
                    predicates = new Predicate[1];
                }
                predicates[0] = builder.equal(root.get(DOC_ID), attachment.getDocId());

                query.where(predicates);
            }

            query.orderBy(builder.desc(root.get(DATE)));

            return session.createQuery(query).getResultList();
        });

        return resolveAttachmentContents(deletedAttachments, bTransaction);
    }

    @Override
    public List<DeletedAttachment> getAllDeletedAttachments(final XWikiDocument doc, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        List<DeletedAttachment> deletedAttachments = executeRead(context, session -> {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<DeletedAttachment> query = builder.createQuery(DeletedAttachment.class);
            Root<DeletedAttachment> root = query.from(DeletedAttachment.class);

            query.select(root);

            query.where(builder.equal(root.get(DOC_ID), doc.getId()));

            query.orderBy(builder.desc(root.get(DATE)));

            return session.createQuery(query).getResultList();
        });

        return resolveAttachmentContents(deletedAttachments, bTransaction);
    }

    private List<DeletedAttachment> resolveAttachmentContents(List<DeletedAttachment> deletedAttachments,
        boolean bTransaction) throws XWikiException
    {
        List<DeletedAttachment> resolvedAttachments = new ArrayList<>(deletedAttachments.size());

        // Resolve deleted attachment content if needed
        for (DeletedAttachment deletedAttachment : deletedAttachments) {
            resolvedAttachments.add(resolveDeletedAttachmentContent(deletedAttachment, bTransaction, false));
        }

        return resolvedAttachments;
    }

    @Override
    public void deleteFromRecycleBin(final long index, XWikiContext inputxcontext, boolean bTransaction)
        throws XWikiException
    {
        XWikiContext context = getExecutionXContext(inputxcontext, true);

        try {
            executeWrite(context, new HibernateCallback<Object>()
            {
                @Override
                public Object doInHibernate(Session session) throws XWikiException
                {
                    DeletedAttachment deletedDocument = loadDeletedAttachment(index, context, false);

                    try {
                        session.createQuery("delete from " + DeletedAttachment.class.getName() + " where id=:id")
                            .setParameter("id", index).executeUpdate();
                    } catch (Exception ex) {
                        // Invalid ID?
                    }

                    // Delete content
                    deleteDeletedAttachmentContent(deletedDocument, bTransaction);

                    return null;
                }
            });
        } finally {
            restoreExecutionXContext();
        }
    }

    private AttachmentRecycleBinContentStore getDefaultAttachmentRecycleBinContentStore() throws XWikiException
    {
        try {
            return this.storeConfiguration.getAttachmentRecycleBinContentStore();
        } catch (ComponentLookupException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to lookup attachment recycle bin content store", e);
        }
    }

    protected AttachmentRecycleBinContentStore getAttachmentRecycleBinContentStore(String storeType)
    {
        if (storeType != null && !storeType.equals(HINT)) {
            try {
                return this.componentManager.getInstance(AttachmentRecycleBinContentStore.class, storeType);
            } catch (ComponentLookupException e) {
                this.logger.warn("Can't find attachment recycle bin content store for type [{}]", storeType, e);
            }
        }

        return null;
    }

    private DeletedAttachment resolveDeletedAttachmentContent(DeletedAttachment deletedAttachment, boolean bTransaction,
        boolean failIfNoContent) throws XWikiException
    {
        AttachmentRecycleBinContentStore contentStore =
            getAttachmentRecycleBinContentStore(deletedAttachment.getContentStore());

        if (contentStore != null) {
            AttachmentReference reference = deletedAttachment.getAttachmentReference();
            DeletedAttachmentContent content =
                contentStore.get(reference, deletedAttachment.getDate(), deletedAttachment.getId(), bTransaction);

            if (content == null) {
                if (failIfNoContent) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                        "Can't find any content for deleted attachment [" + reference + "] with id ["
                            + deletedAttachment.getId() + "]");
                } else {
                    this.logger.warn("Can't find any content for deleted attachment [{}] with id [{}]", reference,
                        deletedAttachment.getId());
                }
            }

            try {
                FieldUtils.writeDeclaredField(deletedAttachment, "content", content, true);
            } catch (IllegalAccessException e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Failed to set deleted document content", e);
            }
        }

        return deletedAttachment;
    }

    private DeletedAttachment createDeletedAttachment(XWikiAttachment attachment, String deleter, Date date,
        AttachmentRecycleBinContentStore contentStore) throws XWikiException
    {
        DeletedAttachment trashdoc;

        String storeType = null;
        DeletedAttachmentContent deletedDocumentContent = null;

        if (contentStore != null) {
            storeType = contentStore.getHint();
        } else {
            deletedDocumentContent = new HibernateDeletedAttachmentContent(attachment);
        }

        trashdoc = new DeletedAttachment(attachment.getDocId(), attachment.getDoc().getFullName(),
            attachment.getFilename(), storeType, deleter, date, deletedDocumentContent);

        return trashdoc;
    }

    private void deleteDeletedAttachmentContent(DeletedAttachment deletedAttachment, boolean bTransaction)
        throws XWikiException
    {
        AttachmentRecycleBinContentStore contentStore =
            getAttachmentRecycleBinContentStore(deletedAttachment.getContentStore());

        if (contentStore != null) {
            contentStore.delete(deletedAttachment.getAttachmentReference(), deletedAttachment.getDate(),
                deletedAttachment.getId(), bTransaction);
        }
    }
}
