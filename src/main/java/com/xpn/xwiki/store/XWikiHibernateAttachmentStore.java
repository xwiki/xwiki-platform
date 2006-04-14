package com.xpn.xwiki.store;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import org.hibernate.Session;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 10 mars 2006
 * Time: 14:02:58
 * To change this template use File | Settings | File Templates.
 */
public class XWikiHibernateAttachmentStore extends XWikiHibernateBaseStore implements XWikiAttachmentStoreInterface {
    private static final Log log = LogFactory.getLog(XWikiHibernateAttachmentStore.class);

    /**
     * THis allows to initialize our storage engine.
     * The hibernate config file path is taken from xwiki.cfg
     * or directly in the WEB-INF directory.
     * @param xwiki
     * @param context
     */
    public XWikiHibernateAttachmentStore(XWiki xwiki, XWikiContext context) {
        super(xwiki, context);
    }

    /**
     * Initialize the storage engine with a specific path
     * This is used for tests.
     * @param hibpath
     */
    public XWikiHibernateAttachmentStore(String hibpath) {
        super(hibpath);
    }

    public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        saveAttachmentContent(attachment, true, context, bTransaction);
    }

    public void saveAttachmentContent(XWikiAttachment attachment, boolean parentUpdate, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            XWikiAttachmentContent content = attachment.getAttachment_content();
            if (content.isContentDirty()) {
                attachment.updateContentArchive(context);
            }
            XWikiAttachmentArchive archive = attachment.getAttachment_archive();

            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }
            Session session = getSession(context);


            Query query = session.createQuery("select attach.id from XWikiAttachmentContent as attach where attach.id = :id");
            query.setLong("id", content.getId());
            if (query.uniqueResult()==null)
                session.save(content);
            else
                session.update(content);

            query = session.createQuery("select attach.id from XWikiAttachmentArchive as attach where attach.id = :id");
            query.setLong("id", archive.getId());
            if (query.uniqueResult()==null)
                session.save(archive);
            else
                session.update(archive);

            if (parentUpdate)
                context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(), context, true);
            if (bTransaction) {
                endTransaction(context, true);
            }
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
                    "Exception while saving attachment {0} of document {1}", e, args);
        } finally {
            try {
                if (bTransaction)
                    endTransaction(context, false);
            } catch (Exception e) {}
        }

    }

    public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);


            XWikiAttachmentContent content = new XWikiAttachmentContent(attachment);
            attachment.setAttachment_content(content);

            session.load(content, new Long(content.getId()));

            if (bTransaction)
                endTransaction(context, false, false);
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT,
                    "Exception while loading attachment {0} of document {1}", e, args);
        } finally {
            try {
                if (bTransaction)
                    endTransaction(context, false, false);
            } catch (Exception e) {}
        }
    }

    public void loadAttachmentArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(false, context);
            }
            Session session = getSession(context);


            XWikiAttachmentArchive archive = new XWikiAttachmentArchive();
            archive.setAttachment(attachment);
            attachment.setAttachment_archive(archive);

            session.load(archive, new Long(archive.getId()));

            if (bTransaction)
                endTransaction(context, false, false);
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT,
                    "Exception while loading attachment {0} of document {1}", e, args);
        } finally {
            try {
                if (bTransaction)
                    endTransaction(context, false, false);
            } catch (Exception e) {}
        }
    }

    public void deleteXWikiAttachment(XWikiAttachment attachment,  XWikiContext context, boolean bTransaction) throws XWikiException {
        deleteXWikiAttachment(attachment, true, context, bTransaction);
    }

    public void deleteXWikiAttachment(XWikiAttachment attachment, boolean parentUpdate, XWikiContext context, boolean bTransaction) throws XWikiException {
        try {
            if (bTransaction) {
                checkHibernate(context);
                bTransaction = beginTransaction(context);
            }

            Session session = getSession(context);

            // Delete the three attachement entries
            try {
                loadAttachmentContent(attachment, context, false);
            } catch (Exception e) {
                if (log.isWarnEnabled())
                    log.warn("Error loading attachment content when deleting attachment " + attachment.getFilename() + " of doc " + attachment.getDoc().getFullName());
            }
            try {
                session.delete(attachment.getAttachment_content());
            } catch (Exception e) {
                if (log.isWarnEnabled())
                    log.warn("Error deleting attachment content " + attachment.getFilename() + " of doc " + attachment.getDoc().getFullName());
            }
            try {
                loadAttachmentContent(attachment, context, false);
            } catch(XWikiException e){
                if (log.isWarnEnabled())
                    log.warn("Error loading attachment archive when deleting attachment " + attachment.getFilename() + " of doc " + attachment.getDoc().getFullName());
            }

            try {
                session.delete(attachment.getAttachment_archive());
            } catch (Exception e) {
                if (log.isWarnEnabled())
                    log.warn("Error deleting attachment archive " + attachment.getFilename() + " of doc " + attachment.getDoc().getFullName());
            }

            try {
                session.delete(attachment);
            } catch (Exception e) {
                if (log.isWarnEnabled())
                    log.warn("Error deleting attachment meta data " + attachment.getFilename() + " of doc " + attachment.getDoc().getFullName());
            }

            try {
                if (parentUpdate) {
                    List list = attachment.getDoc().getAttachmentList();
                    for (int i=0;i<list.size();i++) {
                        XWikiAttachment attach = (XWikiAttachment) list.get(i);
                        if (attachment.getFilename().equals(attach.getFilename())) {
                            list.remove(i);
                            break;
                        }
                    }
                    context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(), context, false);
                }
            } catch (Exception e) {
                if (log.isWarnEnabled())
                    log.warn("Error updating document when deleting attachment " + attachment.getFilename() + " of doc " + attachment.getDoc().getFullName());
            }

            if (bTransaction) {
                endTransaction(context, true);
            }
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_ATTACHMENT,
                    "Exception while deleting attachment {0} of document {1}", e, args);
        } finally {
            try {
                if (bTransaction)
                    endTransaction(context, false);
            } catch (Exception e) {}
        }
    }
}
