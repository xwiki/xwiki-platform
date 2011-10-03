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

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;

/**
 * Realization of {@link AttachmentVersioningStore} for Hibernate-based storage.
 * 
 * @version $Id$
 * @since 1.4M2
 */
@Component
@Named("hibernate")
@Singleton
public class HibernateAttachmentVersioningStore extends XWikiHibernateBaseStore implements AttachmentVersioningStore
{
    /** logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateAttachmentVersioningStore.class);

    /**
     * @param context the current context.
     * @deprecated 1.6M1. Use ComponentManager.lookup(AttachmentVersioningStore.class) instead.
     */
    @Deprecated
    public HibernateAttachmentVersioningStore(XWikiContext context)
    {
        super(context.getWiki(), context);
    }

    /**
     * Empty constructor needed for component manager.
     */
    public HibernateAttachmentVersioningStore()
    {
    }

    @Override
    public XWikiAttachmentArchive loadArchive(final XWikiAttachment attachment, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        try {
            final XWikiAttachmentArchive archive = new XWikiAttachmentArchive();
            archive.setAttachment(attachment);
            executeRead(context, bTransaction, new HibernateCallback<Object>()
            {
                @Override
                public Object doInHibernate(Session session) throws HibernateException
                {
                    try {
                        session.load(archive, archive.getId());
                    } catch (ObjectNotFoundException e) {
                        // if none found then return empty created archive
                    }
                    return null;
                }
            });
            attachment.setAttachment_archive(archive);
            return archive;
        } catch (Exception e) {
            Object[] args = {attachment.getFilename(), attachment.getDoc()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT,
                "Exception while loading attachment archive {0} of document {1}", e, args);
        }
    }

    @Override
    public void saveArchive(final XWikiAttachmentArchive archive, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        executeWrite(context, bTransaction, new HibernateCallback<Object>()
        {
            @Override
            public Object doInHibernate(Session session) throws HibernateException, XWikiException
            {
                session.saveOrUpdate(archive);
                return null;
            }
        });
    }

    @Override
    public void deleteArchive(final XWikiAttachment attachment, final XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        try {
            executeWrite(context, bTransaction, new HibernateCallback<Object>()
            {
                @Override
                public Object doInHibernate(Session session) throws HibernateException, XWikiException
                {
                    XWikiAttachmentArchive archive = new XWikiAttachmentArchive();
                    archive.setAttachment(attachment);
                    session.delete(archive);
                    return null;
                }
            });
        } catch (Exception e) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn(String.format("Error deleting attachment archive [%s] of doc [%s]",
                    attachment.getFilename(), attachment.getDoc().getFullName()), e);
            }
        }
    }
}
