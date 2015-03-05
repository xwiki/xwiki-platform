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

import java.util.Iterator;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.Query;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
@Named("hibernate")
@Singleton
public class XWikiHibernateAttachmentStore extends XWikiHibernateBaseStore implements XWikiAttachmentStoreInterface
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiHibernateAttachmentStore.class);

    /**
     * This allows to initialize our storage engine. The hibernate config file path is taken from xwiki.cfg or directly
     * in the WEB-INF directory.
     *
     * @param xwiki
     * @param context
     * @deprecated 1.6M1. Use ComponentManager.lookup(XWikiAttachmentStoreInterface.class) instead.
     */
    @Deprecated
    public XWikiHibernateAttachmentStore(XWiki xwiki, XWikiContext context)
    {
        super(xwiki, context);
    }

    /**
     * @see #XWikiHibernateAttachmentStore(XWiki, XWikiContext)
     * @deprecated 1.6M1. Use ComponentManager.lookup(XWikiAttachmentStoreInterface.class) instead.
     */
    @Deprecated
    public XWikiHibernateAttachmentStore(XWikiContext context)
    {
        this(context.getWiki(), context);
    }

    /**
     * Initialize the storage engine with a specific path This is used for tests.
     *
     * @param hibpath
     * @deprecated 1.6M1. Use ComponentManager.lookup(XWikiAttachmentStoreInterface.class) instead.
     */
    @Deprecated
    public XWikiHibernateAttachmentStore(String hibpath)
    {
        super(hibpath);
    }

    /**
     * Empty constructor needed for component manager.
     */
    public XWikiHibernateAttachmentStore()
    {
    }

    @Override
    public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        saveAttachmentContent(attachment, true, context, bTransaction);
    }

    @Override
    public void saveAttachmentContent(XWikiAttachment attachment, boolean parentUpdate, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        XWikiAttachmentContent content = attachment.getAttachment_content();
        // Protect against a corrupted attachment. This can happen if an attachment is listed in the Attachment
        // table but not listed in the Attachment Content table! In this case don't save anything.
        if (content != null) {
            String currentWiki = context.getWikiId();

            try {
                // Switch context wiki to attachment wiki
                String attachmentWiki =
                    (attachment.getReference() == null) ? null : attachment.getReference().getDocumentReference()
                        .getWikiReference().getName();
                if (attachmentWiki != null) {
                    context.setWikiId(attachmentWiki);
                }

                if (bTransaction) {
                    checkHibernate(context);
                    bTransaction = beginTransaction(context);
                }
                Session session = getSession(context);

                Query query =
                    session.createQuery("select attach.id from XWikiAttachmentContent as attach where attach.id = :id");
                query.setLong("id", content.getId());
                if (query.uniqueResult() == null) {
                    session.save(content);
                } else {
                    session.update(content);
                }

                if (attachment.getAttachment_archive() == null) {
                    attachment.loadArchive(context);
                }
                // The archive has been updated in XWikiHibernateStore.saveAttachment()
                context.getWiki().getAttachmentVersioningStore()
                    .saveArchive(attachment.getAttachment_archive(), context, false);

                if (parentUpdate) {
                    context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(), context, true);
                }

                if (bTransaction) {
                    endTransaction(context, true);
                }
            } catch (Exception e) {
                Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
                    "Exception while saving attachment {0} of document {1}", e, args);
            } finally {
                try {
                    if (bTransaction) {
                        endTransaction(context, false);
                    }
                } catch (Exception e) {
                }

                // Restore context wiki
                context.setWikiId(currentWiki);
            }
        } else {
            LOGGER.warn("Failed to save the Attachment content for [{}] at [{}] since no content could be found!",
                attachment.getFilename(), attachment.getDoc());
        }
    }

    @Override
    public void saveAttachmentsContent(List<XWikiAttachment> attachments, XWikiDocument doc, boolean bParentUpdate,
        XWikiContext context, boolean bTransaction) throws XWikiException
    {
        if (attachments == null) {
            return;
        }
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            Iterator<XWikiAttachment> it = attachments.iterator();
            while (it.hasNext()) {
                XWikiAttachment att = it.next();
                saveAttachmentContent(att, false, context, false);
            }
            if (bParentUpdate) {
                context.getWiki().getStore().saveXWikiDoc(doc, context, false);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT, "Exception while saving attachments", e);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }
        }

    }

    @Override
    public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        String currentWiki = context.getWikiId();

        try {
            // Switch context wiki to attachment wiki
            String attachmentWiki =
                (attachment.getReference() == null) ? null : attachment.getReference().getDocumentReference()
                    .getWikiReference().getName();
            if (attachmentWiki != null) {
                context.setWikiId(attachmentWiki);
            }

            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);

            XWikiAttachmentContent content = new XWikiAttachmentContent(attachment);
            session.load(content, new Long(content.getId()));

            // Hibernate calls setContent which causes isContentDirty to be true. This is not what we want.
            content.setContentDirty(false);

            attachment.setAttachment_content(content);

            if (bTransaction) {
                endTransaction(context, false, false);
            }
        } catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT,
                "Exception while loading attachment {0} of document {1}", e, args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false, false);
                }
            } catch (Exception e) {
            }

            // Restore context wiki
            context.setWikiId(currentWiki);
        }
    }

    @Override
    public void deleteXWikiAttachment(XWikiAttachment attachment, XWikiContext context, boolean bTransaction)
        throws XWikiException
    {
        deleteXWikiAttachment(attachment, true, context, bTransaction);
    }

    @Override
    public void deleteXWikiAttachment(XWikiAttachment attachment, boolean parentUpdate, XWikiContext context,
        boolean bTransaction) throws XWikiException
    {
        String currentWiki = context.getWikiId();

        try {
            // Switch context wiki to attachment wiki
            String attachmentWiki =
                (attachment.getReference() == null) ? null : attachment.getReference().getDocumentReference()
                    .getWikiReference().getName();
            if (attachmentWiki != null) {
                context.setWikiId(attachmentWiki);
            }

            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }

            Session session = getSession(context);

            // Delete the three attachment entries
            try {
                session.delete(new XWikiAttachmentContent(attachment));
            } catch (Exception e) {
                LOGGER.warn("Error deleting attachment content [{}] of document [{}]", attachment.getFilename(),
                    attachment.getDoc().getDocumentReference());
            }

            context.getWiki().getAttachmentVersioningStore().deleteArchive(attachment, context, false);

            try {
                session.delete(attachment);
            } catch (Exception e) {
                LOGGER.warn("Error deleting attachment meta data [{}] of document [{}]", attachment.getFilename(),
                    attachment.getDoc().getDocumentReference());
            }

            try {
                if (parentUpdate) {
                    List<XWikiAttachment> list = attachment.getDoc().getAttachmentList();
                    for (int i = 0; i < list.size(); i++) {
                        XWikiAttachment attach = list.get(i);
                        if (attachment.getFilename().equals(attach.getFilename())) {
                            list.remove(i);
                            break;
                        }
                    }
                    context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(), context, false);
                }
            } catch (Exception e) {
                LOGGER.warn("Error updating document when deleting attachment [{}] of document [{}]",
                    attachment.getFilename(), attachment.getDoc().getDocumentReference());
            }

            if (bTransaction) {
                endTransaction(context, true);
            }
        } catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_ATTACHMENT,
                "Exception while deleting attachment {0} of document {1}", e, args);
        } finally {
            try {
                if (bTransaction) {
                    endTransaction(context, false);
                }
            } catch (Exception e) {
            }

            // Restore context wiki
            context.setWikiId(currentWiki);
        }
    }
}
