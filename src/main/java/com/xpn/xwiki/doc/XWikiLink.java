/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 */

package com.xpn.xwiki.doc;

import java.io.Serializable;


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
