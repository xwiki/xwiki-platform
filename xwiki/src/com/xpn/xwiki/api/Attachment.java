package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import org.apache.commons.jrcs.rcs.Archive;
import org.apache.commons.jrcs.rcs.Lines;
import org.apache.commons.jrcs.rcs.Version;

import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 26 févr. 2004
 * Time: 17:30:21
 * To change this template use File | Settings | File Templates.
 */
public class Attachment extends Api {
    private Document doc;
    private XWikiAttachment attachment;

    public Attachment(Document doc, XWikiAttachment attachment, XWikiContext context) {
       super(context);
       this.doc = doc;
       this.attachment = attachment;
    }

    public Document getDocument() {
        return doc;
    }

    public long getId() {
        return attachment.getId();
    }

    public long getDocId() {
        return doc.getId();
    }

    public int getFilesize() {
        return attachment.getFilesize();
    }

    public String getFilename() {
        return attachment.getFilename();
    }

    public String getAuthor() {
        return attachment.getAuthor();
    }

    public String getVersion() {
        return attachment.getVersion();
    }

    public Version getRCSVersion() {
         return attachment.getRCSVersion();
    }

    public String getComment() {
        return attachment.getComment();
    }

    public Date getDate() {
        return attachment.getDate();
    }

    public byte[] getContent() throws XWikiException {
        return attachment.getContent(context);
    }

    public Archive getArchive() {
        return attachment.getArchive();
    }

    public Version[] getVersions() {
        return attachment.getVersions();
    }

    public List getVersionList() throws XWikiException {
        return attachment.getVersionList();
    }

}
