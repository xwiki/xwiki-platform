package com.xpn.xwiki.store;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import java.util.List;

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
