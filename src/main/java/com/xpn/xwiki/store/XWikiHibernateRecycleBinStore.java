/*
 * Copyright 2007, XpertNet SARL, and individual contributors.
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

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
/**
 * Realization of {@link XWikiRecycleBinStoreInterface} for Hibernate store.
 * @version $Id: $
 */
public class XWikiHibernateRecycleBinStore extends XWikiHibernateBaseStore implements
    XWikiRecycleBinStoreInterface
{
    /**
     * @param context used for environment
     */
    public XWikiHibernateRecycleBinStore(XWikiContext context)
    {
        super(context.getWiki(), context);
    }
    /**
     * {@inheritDoc}
     */
    public void saveToRecycleBin(XWikiDocument doc, String deleter, Date date,
        boolean bTransaction, XWikiContext context) throws XWikiException
    {
        final XWikiDeletedDocument trashdoc = new XWikiDeletedDocument(doc, deleter, date,
            context);
        executeWrite(context, bTransaction, new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException
            {
                session.save(trashdoc);
                return null;
            }
        });
    }
    /**
     * {@inheritDoc}
     */
    public XWikiDocument restoreFromRecycleBin(final XWikiDocument doc, final long index, 
        final XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return (XWikiDocument) executeRead(context, bTransaction, new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                XWikiDeletedDocument trashdoc = (XWikiDeletedDocument) session.load(
                    XWikiDeletedDocument.class, Long.valueOf(index));
                return trashdoc.restoreDocument(null, context);
            }
        });
    }
    /**
     * {@inheritDoc}
     */
    public XWikiDeletedDocument[] getAllDeletedDocuments(final XWikiDocument doc, 
        XWikiContext context, boolean bTransaction) throws XWikiException
    {
        return (XWikiDeletedDocument[]) executeRead(context, bTransaction, new HibernateCallback(){
            public Object doInHibernate(Session session) throws HibernateException,
                XWikiException
            {
                List lst = session.createCriteria(XWikiDeletedDocument.class)
                    .add(Restrictions.eq("fullName", doc.getFullName()))
                    .add(Restrictions.eq("language", doc.getLanguage()))
                    .addOrder(Order.desc("date"))
                    .list();
                XWikiDeletedDocument[] result = new XWikiDeletedDocument[lst.size()];
                return lst.toArray(result);
            }
        });
    }
    /**
     * {@inheritDoc}
     */
    public void deleteFromRecycleBin(XWikiDocument doc, final long index, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        executeWrite(context, bTransaction, new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException,
                XWikiException
            {
                session.createQuery("delete from "+XWikiDeletedDocument.class.getName()
                    + " where id=?").setLong(0, index)
                    .executeUpdate();
                return null;
            }
        });
    }
}
