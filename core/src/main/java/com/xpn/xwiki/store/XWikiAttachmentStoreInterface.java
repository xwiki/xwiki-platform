package com.xpn.xwiki.store;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 10 mars 2006
 * Time: 13:58:58
 * To change this template use File | Settings | File Templates.
 */
public interface XWikiAttachmentStoreInterface {
    public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void saveAttachmentContent(XWikiAttachment attachment, boolean bParentUpdate, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void saveAttachmentsContent(List attachments, XWikiDocument doc, boolean bParentUpdate, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void loadAttachmentArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void deleteXWikiAttachment(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void deleteXWikiAttachment(XWikiAttachment attachment, boolean parentUpdate, XWikiContext context, boolean bTransaction) throws XWikiException;
    public void cleanUp(XWikiContext context);
}
