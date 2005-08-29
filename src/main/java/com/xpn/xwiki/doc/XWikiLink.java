package com.xpn.xwiki.doc;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: HAL_2005
 * Date: 26 juil. 2005
 * Time: 11:14:04
 * To change this template use File | Settings | File Templates.
 */

public class XWikiLink extends Object implements Serializable {
    private long docId;
    private String link;
    private String fullName;


    public XWikiLink(){
        this.setDocId(0);
    }

    public XWikiLink(long docId) {
        this.setDocId(docId);
    }

    public XWikiLink(long docId, String link,String fullName) {
        this.setDocId(docId);
        this.setLink(link);
        this.setFullName(fullName);
    }

    public long getDocId() {
        return docId;
    }

    public void setDocId(long docId) {
        this.docId = docId;
    }


    public void setLink(String link) {
        this.link=link;
    }

    public String getLink() {
        return link;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean equals(Object obj) {
      XWikiLink objlink = (XWikiLink) obj;
      return ((objlink.getDocId()==getDocId())&&(objlink.getLink().equals(getLink())));
    }

    public int hashCode(){
    return ("" + getDocId() + link ).hashCode();
    }
}
