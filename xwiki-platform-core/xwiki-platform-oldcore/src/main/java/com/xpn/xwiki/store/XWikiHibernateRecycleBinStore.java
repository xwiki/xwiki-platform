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
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Realization of {@link XWikiRecycleBinStoreInterface} for Hibernate store.
 *
 * @version $Id$
 */
@Component
@Named("hibernate")
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
            c.add(Restrictions.eq("fullName", this.document.getFullName()));

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
     * Name of the language property in the Hibernate mapping.
     */
    private static final String LANGUAGE_PROPERTY_NAME = "language";

    /**
     * @param context used for environment
     * @deprecated 1.6M1. Use ComponentManager.lookup(XWikiRecycleBinStoreInterface.class) instead.
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

    @Override
    public void saveToRecycleBin(XWikiDocument doc, String deleter, Date date, XWikiContext inputxcontext,
        boolean bTransaction) throws XWikiException
    {
        XWikiContext context = getXWikiContext(inputxcontext);

        final XWikiDeletedDocument trashdoc = new XWikiDeletedDocument(doc, deleter, date, context);

        executeWrite(context, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException
            {
                session.save(trashdoc);
                return null;
            }
        });
    }

    @Override
    public XWikiDocument restoreFromRecycleBin(final XWikiDocument doc, final long index,
        final XWikiContext inputxcontext, boolean bTransaction) throws XWikiException
    {
        XWikiContext context = getXWikiContext(inputxcontext);

        return executeRead(context, new HibernateCallback<XWikiDocument>()
        {
            @Override
            public XWikiDocument doInHibernate(Session session) throws HibernateException, XWikiException
            {
                XWikiDeletedDocument trashdoc =
                    (XWikiDeletedDocument) session.load(XWikiDeletedDocument.class, Long.valueOf(index));
                return trashdoc.restoreDocument(null, context);
            }
        });
    }

    @Override
    public XWikiDeletedDocument getDeletedDocument(XWikiDocument doc, final long index, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        return executeRead(context, new HibernateCallback<XWikiDeletedDocument>()
        {
            @Override
            public XWikiDeletedDocument doInHibernate(Session session) throws HibernateException, XWikiException
            {
                return (XWikiDeletedDocument) session.get(XWikiDeletedDocument.class, Long.valueOf(index));
            }
        });
    }

    @Override
    public XWikiDeletedDocument[] getAllDeletedDocuments(XWikiDocument doc, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        return executeRead(context, new DeletedDocumentsHibernateCallback(doc));
    }

    @Override
    public void deleteFromRecycleBin(XWikiDocument doc, final long index, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        executeWrite(context, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                session.createQuery("delete from " + XWikiDeletedDocument.class.getName() + " where id=?")
                    .setLong(0, index).executeUpdate();
                return null;
            }
        });
    }
}
