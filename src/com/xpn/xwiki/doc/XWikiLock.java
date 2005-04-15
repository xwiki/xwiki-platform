package com.xpn.xwiki.doc;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Alex
 * Date: 17 mars 2005
 * Time: 15:33:41
 * To change this template use File | Settings | File Templates.
 */
public class XWikiLock extends Object {
    protected String userName;
    protected long docId;
    protected Date date;
    public XWikiLock(long docId, String userName) {
        this.setDocId(docId);
        this.setUserName(userName);
        this.setDate(new Date());
    }

    public XWikiLock()
    {
        this.setDate(null);
        this.setUserName(null);
        this.setDocId(0);
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getDocId() {
        return docId;
    }

    public void setDocId(long docId) {
        this.docId = docId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
