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

import java.util.Date;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;

/**
 * Realization of {@link AttachmentRecycleBinStore} for Hibernate-based storage.
 * 
 * @version $Id$
 * @since 1.4M1
 */
@Component
@Named("hibernate")
@Singleton
public class HibernateAttachmentRecycleBinStore extends XWikiHibernateBaseStore implements AttachmentRecycleBinStore
{
    /** String used to annotate unchecked exceptions. */
    private static final String ANOTATE_UNCHECKED = "unchecked";

    /** Constant string used to refer Document ID. */
    private static final String DOC_ID = "docId";

    /** Constant string used to refer date. */
    private static final String DATE = "date";

    /**
     * Constructor used by {@link XWiki} during storage initialization.
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
    public void saveToRecycleBin(XWikiAttachment attachment, String deleter, Date date, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        final DeletedAttachment trashAtachment = new DeletedAttachment(attachment, deleter, date, context);
        executeWrite(context, bTransaction, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException
            {
                session.save(trashAtachment);
                return null;
            }
        });
    }

    @Override
    public XWikiAttachment restoreFromRecycleBin(final XWikiAttachment attachment, final long index,
        final XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return executeRead(context, bTransaction, new HibernateCallback<XWikiAttachment>()
        {
            @Override
            public XWikiAttachment doInHibernate(Session session) throws HibernateException, XWikiException
            {
                try {
                    DeletedAttachment trashAttachment =
                        (DeletedAttachment) session.load(DeletedAttachment.class, Long.valueOf(index));
                    return trashAttachment.restoreAttachment(attachment, context);
                } catch (Exception ex) {
                    // Invalid recycle entry.
                    return null;
                }
            }
        });
    }

    @Override
    public DeletedAttachment getDeletedAttachment(final long index, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        return executeRead(context, bTransaction, new HibernateCallback<DeletedAttachment>()
        {
            @Override
            public DeletedAttachment doInHibernate(Session session) throws HibernateException, XWikiException
            {
                return (DeletedAttachment) session.get(DeletedAttachment.class, Long.valueOf(index));
            }
        });
    }

    @Override
    public List<DeletedAttachment> getAllDeletedAttachments(final XWikiAttachment attachment, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        return executeRead(context, bTransaction, new HibernateCallback<List<DeletedAttachment>>()
        {
            @SuppressWarnings(ANOTATE_UNCHECKED)
            @Override
            public List<DeletedAttachment> doInHibernate(Session session) throws HibernateException, XWikiException
            {
                Criteria c = session.createCriteria(DeletedAttachment.class);
                if (attachment != null) {
                    c.add(Restrictions.eq(DOC_ID, attachment.getDocId()));
                    if (!StringUtils.isBlank(attachment.getFilename())) {
                        c.add(Restrictions.eq("filename", attachment.getFilename()));
                    }
                }
                return c.addOrder(Order.desc(DATE)).list();
            }
        });
    }

    @Override
    public List<DeletedAttachment> getAllDeletedAttachments(final XWikiDocument doc, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        return executeRead(context, bTransaction, new HibernateCallback<List<DeletedAttachment>>()
        {
            @SuppressWarnings(ANOTATE_UNCHECKED)
            @Override
            public List<DeletedAttachment> doInHibernate(Session session) throws HibernateException, XWikiException
            {
                assert doc != null;
                return session.createCriteria(DeletedAttachment.class).add(Restrictions.eq(DOC_ID, doc.getId()))
                    .addOrder(Order.desc(DATE)).list();
            }
        });
    }

    @Override
    public void deleteFromRecycleBin(final long index, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        executeWrite(context, bTransaction, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                try {
                    session.createQuery("delete from " + DeletedAttachment.class.getName() + " where id=?")
                        .setLong(0, index).executeUpdate();
                } catch (Exception ex) {
                    // Invalid ID?
                }
                return null;
            }
        });
    }
}
